package de.lemonpvp.duelplus.request;

import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.db.DuelDatabase;
import de.lemonpvp.duelplus.db.DuelRecord;
import de.lemonpvp.duelplus.db.SpielerSnapshot;
import de.lemonpvp.duelplus.loot.Nachlieferung;
import de.lemonpvp.duelplus.util.KampfPruefung;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Der Hintergrundabgleich, der alle Server ueber dieselbe Datenbank
 * lose koppelt - kein eigener Netzwerk-Kanal noetig. Laeuft auf JEDEM
 * Server (SMP, Lobby, Duels) und kuemmert sich nur um die Zeilen, die
 * diesen Server konkret betreffen:
 *
 *  - WARTEND, Ziel hier online, noch nicht gezeigt -> Anfrage anzeigen
 *  - ANGENOMMEN, ein Teilnehmer hier online, seine Seite noch offen
 *    -> Inventar schnappschiessen, zur Arena schicken
 *  - ABGELEHNT/ABGELAUFEN, Herausforderer hier -> benachrichtigen
 *  - BEENDET, Teilnehmer wieder auf seinem Herkunftsserver -> Ergebnis-
 *    Nachricht + zurueckgeholtes Inventar anwenden
 *
 * Das eigentliche Ankommen in der Arena (beide da, Kampf starten)
 * macht DuellSessionManager - der laeuft nur auf dem Arena-Server.
 */
public final class AnfragePollTask {

    private static final long UEBERGABE_SPERRE_MILLIS = 20_000L;

    private final DuelPlus plugin;
    private final Set<String> inArbeit = ConcurrentHashMap.newKeySet();
    private final Set<String> kampfHinweis = ConcurrentHashMap.newKeySet();
    private long letztesAufraeumen;

    public AnfragePollTask(DuelPlus plugin) {
        this.plugin = plugin;
    }

    public void starten() {
        long takt = Math.max(10, plugin.getConfig().getInt("anfrage.poll-takt", 20));
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, takt, takt);
    }

    private void tick() {
        if (!plugin.db().bereit()) {
            return;
        }
        int timeout = plugin.getConfig().getInt("anfrage.timeout-sekunden", 60);
        plugin.db().verfalleAlte(timeout);
        // Grosszuegiges, festes Sicherheitsnetz - nicht konfigurierbar,
        // soll nur haengengebliebene Faelle abfangen, kein normaler Weg.
        plugin.db().verfalleFestsitzendeAngenommen(120);

        zeigeNeueAnfragen();
        verarbeiteAngenommen();
        benachrichtigeAbgeschlossen();
        verarbeiteBeendet();
        long jetzt = System.currentTimeMillis();
        if (jetzt - letztesAufraeumen >= 60_000L) {
            letztesAufraeumen = jetzt;
            plugin.db().aufraeumen(24);
        }
    }

    // ------------------------------------------------------------ WARTEND anzeigen

    private void zeigeNeueAnfragen() {
        plugin.db().offeneFuerZielServer(plugin.serverName()).thenAccept(liste -> {
            if (liste.isEmpty()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (DuelRecord anfrage : liste) {
                    Player ziel = Bukkit.getPlayer(anfrage.spielerB());
                    if (ziel != null) {
                        plugin.msgs().send(ziel, "received", "spieler", anfrage.spielerAName());
                    }
                    plugin.db().markiereZielGezeigt(anfrage.id());
                }
            });
        });
    }

    // ------------------------------------------------------------ ANGENOMMEN -> zur Arena

    private void verarbeiteAngenommen() {
        plugin.db().angenommenFuerAServer(plugin.serverName()).thenAccept(liste ->
                fuerJedenSendenWennOnline(liste, true));
        plugin.db().angenommenFuerBServer(plugin.serverName()).thenAccept(liste ->
                fuerJedenSendenWennOnline(liste, false));
    }

    private void fuerJedenSendenWennOnline(List<DuelRecord> liste, boolean istA) {
        if (liste.isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (DuelRecord duell : liste) {
                UUID spielerUuid = istA ? duell.spielerA() : duell.spielerB();
                Player spieler = Bukkit.getPlayer(spielerUuid);
                if (spieler == null) {
                    continue;
                }
                String hinweisSchluessel = duell.id() + ":" + spielerUuid;
                long kampfRest = plugin.kampf().restMillis(spielerUuid);
                if (kampfRest > 0) {
                    if (kampfHinweis.add(hinweisSchluessel)) {
                        plugin.msgs().send(spieler, "waits-for-combat",
                                "sekunden", String.valueOf(KampfPruefung.sekunden(kampfRest)));
                    }
                    continue;
                }
                kampfHinweis.remove(hinweisSchluessel);
                if (!inArbeit.add(hinweisSchluessel)) {
                    continue;
                }
                if (plugin.istLootQuelle()) {
                    spieler.closeInventory();
                    plugin.inventarSperre().sperren(spielerUuid, UEBERGABE_SPERRE_MILLIS);
                }
                // Auf der Loot-Quelle selbst (in der Praxis: SMP) ist das
                // Live-Inventar bereits das "echte". Ueberall sonst (z.B.
                // Lobby) ist das lokale Live-Inventar NICHT, was auf dem
                // Spiel steht - dort stattdessen der staendig aktuelle
                // Spiegel aus duelplus_stamm_inventar (siehe
                // StammInventarService, nur auf der Loot-Quelle aktiv).
                CompletableFuture<Optional<SpielerSnapshot>> quelle = plugin.istLootQuelle()
                        ? CompletableFuture.completedFuture(Optional.of(SpielerSnapshot.von(spieler.getInventory())))
                        : plugin.db().stammInventarLesen(spielerUuid);
                quelle.thenAccept(snapshotOpt -> {
                    if (snapshotOpt.isEmpty()) {
                        ohneSpiegelAbsagen(duell, spieler, hinweisSchluessel);
                        return;
                    }
                    rueberschicken(duell, istA, spieler, snapshotOpt.get(), hinweisSchluessel);
                });
            }
        });
    }

    private void ohneSpiegelAbsagen(DuelRecord duell, Player spieler, String schluessel) {
        plugin.db().statusWechseln(duell.id(), DuelRecord.ANGENOMMEN, DuelRecord.ABGELAUFEN).thenAccept(geaendert ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    inArbeit.remove(schluessel);
                    if (geaendert && spieler.isOnline()) {
                        plugin.msgs().send(spieler, "duel-no-mirror");
                    }
                }));
    }

    private void rueberschicken(DuelRecord duell, boolean istA, Player spieler, SpielerSnapshot snapshot, String schluessel) {
        UUID spielerUuid = spieler.getUniqueId();
        // Erst den Schnappschuss sicher in der DB haben, dann erst
        // schicken - sonst koennte der Spieler auf der Arena ankommen,
        // bevor sein Inventar dort ueberhaupt abholbereit ist.
        plugin.db().zuschauerAnfrageLoeschen(spielerUuid)
                .thenCompose(unused ->
                        plugin.db().snapshotSchreiben(duell.id(), spielerUuid, DuelDatabase.RICHTUNG_HIN, snapshot))
                .thenCompose(unused -> istA ? plugin.db().markiereBearbeitetA(duell.id())
                                             : plugin.db().markiereBearbeitetB(duell.id()))
                .whenComplete((unused, fehler) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    inArbeit.remove(schluessel);
                    if (fehler == null && spieler.isOnline()) {
                        plugin.msgs().send(spieler, "teleporting");
                        plugin.bridge().sende(spieler, plugin.arenaServerName());
                    }
                }));
    }

    // ------------------------------------------------------------ ABGELEHNT/ABGELAUFEN

    private void benachrichtigeAbgeschlossen() {
        plugin.db().abgeschlossenFuerAServer(plugin.serverName()).thenAccept(liste -> {
            if (liste.isEmpty()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (DuelRecord duell : liste) {
                    Player herausforderer = Bukkit.getPlayer(duell.spielerA());
                    if (herausforderer != null) {
                        String key = DuelRecord.ABGELEHNT.equals(duell.status())
                                ? "declined-to-challenger" : "expired-to-challenger";
                        plugin.msgs().send(herausforderer, key, "spieler", duell.spielerBName());
                    }
                    plugin.db().markiereBearbeitetA(duell.id());
                }
            });
        });
    }

    // ------------------------------------------------------------ BEENDET -> zurueck auf dem Herkunftsserver

    private void verarbeiteBeendet() {
        if (plugin.istLootQuelle()) {
            plugin.db().beendetOffenA().thenAccept(liste -> ergebnisAnwenden(liste, true));
            plugin.db().beendetOffenB().thenAccept(liste -> ergebnisAnwenden(liste, false));
            return;
        }
        plugin.db().beendetFuerAServer(plugin.serverName()).thenAccept(liste -> ergebnisAnwenden(liste, true));
        plugin.db().beendetFuerBServer(plugin.serverName()).thenAccept(liste -> ergebnisAnwenden(liste, false));
    }

    private void ergebnisAnwenden(List<DuelRecord> liste, boolean istA) {
        if (liste.isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (DuelRecord duell : liste) {
                UUID spielerUuid = istA ? duell.spielerA() : duell.spielerB();
                Player spieler = Bukkit.getPlayer(spielerUuid);
                String schluessel = duell.id() + ":" + spielerUuid;
                if (spieler == null || !inArbeit.add(schluessel)) {
                    continue;
                }
                String ergebnisKey = duell.gewinner() == null ? "draw-result"
                        : spielerUuid.equals(duell.gewinner()) ? "you-won" : "you-lost";
                // Erst wirklich anwenden, DANACH den Schnappschuss loeschen und
                // als erledigt markieren - sonst wuerde ein Fehler mittendrin
                // (oder ein Spieler, der genau in diesem Moment offline geht)
                // das Ergebnis verlieren, ohne dass es einen zweiten Versuch gibt.
                plugin.db().snapshotLesen(duell.id(), spielerUuid, DuelDatabase.RICHTUNG_ZURUECK)
                        .thenAccept(snapshotOpt -> {
                            if (snapshotOpt.isEmpty()) {
                                // Status faellt beim Gewinner schon auf BEENDET,
                                // BEVOR sein Schnappschuss ueberhaupt existiert -
                                // der wird ja erst nach dem vollen
                                // loot.schutz-sekunden-Fenster geschrieben (siehe
                                // DuellSessionManager.niederlageAusloesen), der
                                // Status-Wechsel selbst aber schon direkt nach dem
                                // (sofortigen) Verlierer-Schnappschuss. Auf GAR
                                // KEINEN Fall hier schon als bearbeitet markieren -
                                // sonst wuerde der naechste Poll-Tick diese Zeile
                                // faelschlich als erledigt liegen lassen, WEIT bevor
                                // der eigentliche Schnappschuss ueberhaupt da ist.
                                // Einfach nichts tun, der naechste Tick versucht es
                                // von selbst erneut.
                                inArbeit.remove(schluessel);
                                return;
                            }
                            SpielerSnapshot snapshot = snapshotOpt.get();
                            if (plugin.istLootQuelle()) {
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    if (!spieler.isOnline()) {
                                        inArbeit.remove(schluessel);
                                        return;
                                    }
                                    snapshot.anwenden(spieler.getInventory());
                                    Nachlieferung.zustellen(plugin, spieler, snapshot.nachlieferung());
                                    abschliessen(duell, istA, spieler, ergebnisKey, schluessel);
                                });
                                return;
                            }
                            // Nicht die Loot-Quelle (z.B. Lobby): das Ergebnis
                            // gehoert in den Spiegel, NICHT in das lokale
                            // Live-Inventar dieses Servers - erst beim
                            // naechsten Beitritt zur Loot-Quelle (SMP) wird
                            // es wirklich uebernommen (StammInventarService).
                            plugin.db().stammInventarSchreiben(spielerUuid, snapshot.ohneNachlieferung())
                                    .thenCompose(unused -> plugin.db().nachlieferungAnhaengen(spielerUuid, snapshot.nachlieferung()))
                                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () ->
                                            abschliessen(duell, istA, spieler, ergebnisKey, schluessel)));
                        });
            }
        });
    }

    private void abschliessen(DuelRecord duell, boolean istA, Player spieler, String ergebnisKey, String schluessel) {
        UUID spielerUuid = istA ? duell.spielerA() : duell.spielerB();
        if (spieler.isOnline()) {
            String gegnerName = istA ? duell.spielerBName() : duell.spielerAName();
            UUID gegnerUuid = istA ? duell.spielerB() : duell.spielerA();
            plugin.gegnerMerken(spielerUuid, gegnerUuid, gegnerName);
            plugin.msgs().send(spieler, ergebnisKey, "gegner", gegnerName);
            if (plugin.getConfig().getBoolean("replay.aktiv", true)) {
                plugin.msgs().send(spieler, Bukkit.getPluginCommand("report") != null ? "replay-hinweis" : "replay-hinweis-smp",
                        "gegner", gegnerName, "tage", String.valueOf(plugin.getConfig().getInt("replay.aufbewahren-tage", 3)));
            }
        }
        plugin.db().snapshotLoeschen(duell.id(), spielerUuid, DuelDatabase.RICHTUNG_ZURUECK);
        CompletableFuture<Void> markiert = istA ? plugin.db().markiereBearbeitetA(duell.id())
                                                : plugin.db().markiereBearbeitetB(duell.id());
        markiert.whenComplete((unused, fehler) -> {
            inArbeit.remove(schluessel);
            if (plugin.istLootQuelle()) {
                plugin.inventarSperre().freigebenWennNichtsOffen(spielerUuid);
            }
        });
    }
}
