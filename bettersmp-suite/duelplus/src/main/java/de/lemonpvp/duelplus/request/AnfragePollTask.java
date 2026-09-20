package de.lemonpvp.duelplus.request;

import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.db.DuelDatabase;
import de.lemonpvp.duelplus.db.DuelRecord;
import de.lemonpvp.duelplus.db.SpielerSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    private final DuelPlus plugin;

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
        plugin.db().aufraeumen(24);
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
                // Auf der Loot-Quelle selbst (in der Praxis: SMP) ist das
                // Live-Inventar bereits das "echte". Ueberall sonst (z.B.
                // Lobby) ist das lokale Live-Inventar NICHT, was auf dem
                // Spiel steht - dort stattdessen der staendig aktuelle
                // Spiegel aus duelplus_stamm_inventar (siehe
                // StammInventarService, nur auf der Loot-Quelle aktiv).
                CompletableFuture<SpielerSnapshot> quelle = plugin.istLootQuelle()
                        ? CompletableFuture.completedFuture(SpielerSnapshot.von(spieler.getInventory()))
                        : plugin.db().stammInventarLesen(spielerUuid).thenApply(opt -> opt.orElseGet(SpielerSnapshot::leer));
                // Erst den Schnappschuss sicher in der DB haben, dann erst
                // schicken - sonst koennte der Spieler auf der Arena ankommen,
                // bevor sein Inventar dort ueberhaupt abholbereit ist.
                quelle.thenCompose(snapshot ->
                                plugin.db().snapshotSchreiben(duell.id(), spielerUuid, DuelDatabase.RICHTUNG_HIN, snapshot))
                        .thenCompose(unused -> istA ? plugin.db().markiereBearbeitetA(duell.id())
                                                     : plugin.db().markiereBearbeitetB(duell.id()))
                        .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                            if (spieler.isOnline()) {
                                plugin.msgs().send(spieler, "teleporting");
                                plugin.bridge().sende(spieler, plugin.arenaServerName());
                            }
                        }));
            }
        });
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
                if (spieler == null) {
                    continue;
                }
                boolean gewonnen = spielerUuid.equals(duell.gewinner());
                // Erst wirklich anwenden, DANACH als erledigt markieren - sonst
                // wuerde ein Fehler mittendrin die Zeile trotzdem als erledigt
                // stehen lassen und es gaebe keinen zweiten Versuch mehr.
                plugin.db().snapshotHolenUndLoeschen(duell.id(), spielerUuid, DuelDatabase.RICHTUNG_ZURUECK)
                        .thenCompose(snapshotOpt -> {
                            if (plugin.istLootQuelle() || snapshotOpt.isEmpty()) {
                                return CompletableFuture.completedFuture(snapshotOpt);
                            }
                            // Nicht die Loot-Quelle (z.B. Lobby): das Ergebnis
                            // gehoert in den Spiegel, NICHT in das lokale
                            // Live-Inventar dieses Servers - erst beim
                            // naechsten Beitritt zur Loot-Quelle (SMP) wird
                            // es wirklich uebernommen (StammInventarService).
                            return plugin.db().stammInventarSchreiben(spielerUuid, snapshotOpt.get())
                                    .thenApply(unused -> snapshotOpt);
                        })
                        .thenAccept(snapshotOpt -> Bukkit.getScheduler().runTask(plugin, () -> {
                            if (plugin.istLootQuelle()) {
                                snapshotOpt.ifPresent(snap -> snap.anwenden(spieler.getInventory()));
                            }
                            String gegnerName = istA ? duell.spielerBName() : duell.spielerAName();
                            plugin.msgs().send(spieler, gewonnen ? "you-won" : "you-lost", "gegner", gegnerName);
                            if (istA) {
                                plugin.db().markiereBearbeitetA(duell.id());
                            } else {
                                plugin.db().markiereBearbeitetB(duell.id());
                            }
                        }));
            }
        });
    }
}
