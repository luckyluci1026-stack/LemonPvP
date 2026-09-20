package de.lemonpvp.duelplus.session;

import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.arena.Arena;
import de.lemonpvp.duelplus.db.DuelDatabase;
import de.lemonpvp.duelplus.db.DuelRecord;
import de.lemonpvp.duelplus.db.SpielerSnapshot;
import de.lemonpvp.duelplus.loot.LootManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nur auf dem Duels-Server aktiv. Erkennt die Ankunft von Duellanten
 * (per Join-Event, kein Polling noetig - die Ankunft selbst ist das
 * Ereignis), wendet ihr mitgebrachtes Inventar an, startet den Kampf
 * sobald beide da sind, und wickelt das Ende ab (Todeskamera, Loot,
 * Rueckreise). Das eigentliche Zurueckschicken der Ergebnisse auf den
 * jeweiligen Herkunftsserver macht weiterhin AnfragePollTask dort -
 * hier wird nur der "ZURUECK"-Schnappschuss hinterlegt.
 */
public final class DuellSessionManager implements Listener {

    private final DuelPlus plugin;
    private final LootManager loot;

    /** Wer fuer ein noch nicht gestartetes Duell schon angekommen ist (Ankunft + Inventar angewendet, wartet auf den Gegner). */
    private final Map<String, Set<UUID>> wartendAufAnkunft = new ConcurrentHashMap<>();
    private final Map<UUID, DuellSession> sessionNachSpieler = new ConcurrentHashMap<>();

    public DuellSessionManager(DuelPlus plugin) {
        this.plugin = plugin;
        this.loot = new LootManager(plugin);
    }

    public void starten() {
        plugin.getServer().getPluginManager().registerEvents(loot, plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // Arena-Warteschlange: alle paar Sekunden pruefen, ob inzwischen
        // wieder eine Arena frei ist und jemand wartet.
        Bukkit.getScheduler().runTaskTimer(plugin, this::warteschlangeVerarbeiten, 40L, 40L);
    }

    public LootManager loot() {
        return loot;
    }

    public Optional<DuellSession> sessionVon(UUID spieler) {
        return Optional.ofNullable(sessionNachSpieler.get(spieler));
    }

    // ================================================================
    //  Ankunft
    // ================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimJoin(PlayerJoinEvent event) {
        Player spieler = event.getPlayer();
        // Schon in einer laufenden Session (z.B. kurz die Verbindung
        // verloren und wieder da)? Einfach zurueck in die Arena.
        DuellSession laufend = sessionNachSpieler.get(spieler.getUniqueId());
        if (laufend != null) {
            Arena arena = plugin.arenaManager().arena(laufend.arenaName());
            if (arena != null) {
                Location ziel = laufend.spielerA().equals(spieler.getUniqueId()) ? arena.spawnA() : arena.spawnB();
                spieler.teleport(ziel);
            }
            return;
        }
        plugin.db().aktivesDuellFuer(spieler.getUniqueId()).thenAccept(duellOpt ->
                duellOpt.filter(d -> DuelRecord.ANGENOMMEN.equals(d.status())).ifPresent(duell ->
                        Bukkit.getScheduler().runTask(plugin, () -> ankunftVerarbeiten(spieler, duell))));
    }

    private void ankunftVerarbeiten(Player spieler, DuelRecord duell) {
        plugin.db().snapshotHolenUndLoeschen(duell.id(), spieler.getUniqueId(), DuelDatabase.RICHTUNG_HIN)
                .thenAccept(snapshotOpt -> Bukkit.getScheduler().runTask(plugin, () -> {
                    snapshotOpt.ifPresent(snap -> snap.anwenden(spieler.getInventory()));
                    Set<UUID> wartend = wartendAufAnkunft.computeIfAbsent(duell.id(), k -> ConcurrentHashMap.newKeySet());
                    wartend.add(spieler.getUniqueId());
                    if (wartend.contains(duell.spielerA()) && wartend.contains(duell.spielerB())) {
                        wartendAufAnkunft.remove(duell.id());
                        duellStarten(duell);
                    }
                }));
    }

    private void duellStarten(DuelRecord duell) {
        Player a = Bukkit.getPlayer(duell.spielerA());
        Player b = Bukkit.getPlayer(duell.spielerB());
        if (a == null || b == null) {
            // Einer ist zwischenzeitlich wieder weg - beim naechsten Join
            // (siehe oben) wird es erneut versucht.
            return;
        }
        Optional<Arena> arenaOpt = plugin.arenaManager().zuweisen(duell.id());
        if (arenaOpt.isEmpty()) {
            plugin.msgs().send(a, "arena-queue");
            plugin.msgs().send(b, "arena-queue");
            return;
        }
        Arena arena = arenaOpt.get();
        plugin.db().setzeArenaUndAktiv(duell.id(), arena.name());
        plugin.rollback().verfolgungStarten(arena.world());

        a.teleport(arena.spawnA());
        b.teleport(arena.spawnB());
        for (Player p : new Player[]{a, b}) {
            p.setGameMode(GameMode.ADVENTURE);
            var maxHealth = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                p.setHealth(maxHealth.getValue());
            }
            p.setFoodLevel(20);
        }

        DuellSession session = new DuellSession(duell.id(), arena.name(),
                duell.spielerA(), duell.spielerAName(), duell.spielerAServer(),
                duell.spielerB(), duell.spielerBName(), duell.spielerBServer());
        sessionNachSpieler.put(duell.spielerA(), session);
        sessionNachSpieler.put(duell.spielerB(), session);

        int countdown = Math.max(1, plugin.getConfig().getInt("kampf.countdown-sekunden", 5));
        new BukkitRunnable() {
            int rest = countdown;

            @Override
            public void run() {
                if (rest <= 0) {
                    for (Player p : new Player[]{a, b}) {
                        if (p.isOnline()) {
                            p.setGameMode(GameMode.SURVIVAL);
                            plugin.msgs().send(p, "fight");
                        }
                    }
                    session.kampfStarten();
                    cancel();
                    return;
                }
                for (Player p : new Player[]{a, b}) {
                    if (p.isOnline()) {
                        plugin.msgs().send(p, "countdown", "sekunden", String.valueOf(rest));
                    }
                }
                rest--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ================================================================
    //  Ende eines Duells
    // ================================================================

    /**
     * Ein Teilnehmer hat verloren (toedlicher Treffer abgefangen ODER
     * die Verbindung getrennt) - beendet die Session, verteilt das
     * Loot, setzt die Arena zurueck und schickt beide zurueck.
     */
    public void niederlageAusloesen(UUID verliererUuid, boolean nochOnlineFuerTodeskamera) {
        DuellSession session = sessionNachSpieler.remove(verliererUuid);
        if (session == null) {
            return;
        }
        UUID gegnerUuid = session.gegnerVon(verliererUuid);
        sessionNachSpieler.remove(gegnerUuid);
        UUID gewinner = gegnerUuid;

        Player verlierer = Bukkit.getPlayer(verliererUuid);
        Player gewinnerSpieler = Bukkit.getPlayer(gegnerUuid);

        if (verlierer != null) {
            Location ort = verlierer.getLocation();
            loot.verliererLootAbwerfen(verlierer, gewinner, ort);
        }
        SpielerSnapshot gewinnerSnapshot = gewinnerSpieler != null
                ? SpielerSnapshot.von(gewinnerSpieler.getInventory()) : SpielerSnapshot.leer();

        // Erst muessen BEIDE Rueckreise-Schnappschuesse sicher in der DB
        // stehen, DANACH erst den Status auf BEENDET setzen - sonst koennte
        // ein Herkunftsserver schneller pollen, als die Daten geschrieben
        // sind, und das Ergebnis faelschlich als "kein Inventar da"
        // abhaken (siehe AnfragePollTask.ergebnisAnwenden).
        var verliererGeschrieben = plugin.db().snapshotSchreiben(
                session.duellId(), verliererUuid, DuelDatabase.RICHTUNG_ZURUECK, SpielerSnapshot.leer());
        var gewinnerGeschrieben = plugin.db().snapshotSchreiben(
                session.duellId(), gegnerUuid, DuelDatabase.RICHTUNG_ZURUECK, gewinnerSnapshot);
        java.util.concurrent.CompletableFuture.allOf(verliererGeschrieben, gewinnerGeschrieben)
                .thenCompose(unused -> plugin.db().beenden(session.duellId(), gewinner));

        Arena arena = plugin.arenaManager().arena(session.arenaName());
        int anzeigeSekunden = Math.max(1, plugin.getConfig().getInt("kampf.todeskamera-sekunden", 5));

        if (verlierer != null && nochOnlineFuerTodeskamera) {
            todeskameraStarten(verlierer, anzeigeSekunden);
        }
        if (gewinnerSpieler != null) {
            gewinnerSpieler.setGameMode(GameMode.ADVENTURE);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (arena != null) {
                plugin.rollback().zuruecksetzenUndStoppen(arena.world());
                plugin.arenaManager().freigeben(arena.name());
            }
            if (verlierer != null && verlierer.isOnline()) {
                verlierer.setGameMode(GameMode.SURVIVAL);
                plugin.bridge().sende(verlierer, session.herkunftsServerVon(verliererUuid));
            }
            if (gewinnerSpieler != null && gewinnerSpieler.isOnline()) {
                gewinnerSpieler.setGameMode(GameMode.SURVIVAL);
                plugin.bridge().sende(gewinnerSpieler, session.herkunftsServerVon(gegnerUuid));
            }
        }, anzeigeSekunden * 20L);
    }

    private void todeskameraStarten(Player verlierer, int sekunden) {
        Location zentrum = verlierer.getLocation();
        verlierer.setGameMode(GameMode.SPECTATOR);
        new BukkitRunnable() {
            double winkel = 0;
            int ticks = sekunden * 20;

            @Override
            public void run() {
                if (ticks-- <= 0 || !verlierer.isOnline()) {
                    cancel();
                    return;
                }
                winkel += 4;
                double radius = 5.0;
                double x = zentrum.getX() + radius * Math.cos(Math.toRadians(winkel));
                double z = zentrum.getZ() + radius * Math.sin(Math.toRadians(winkel));
                Location kamera = new Location(zentrum.getWorld(), x, zentrum.getY() + 2.5, z);
                kamera.setDirection(zentrum.toVector().subtract(kamera.toVector()));
                verlierer.teleport(kamera);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ================================================================
    //  Arena-Warteschlange
    // ================================================================

    private void warteschlangeVerarbeiten() {
        String duellId = plugin.arenaManager().naechsterAusWarteschlange();
        if (duellId == null) {
            return;
        }
        plugin.db().holeById(duellId).thenAccept(duellOpt -> Bukkit.getScheduler().runTask(plugin, () ->
                duellOpt.filter(d -> DuelRecord.ANGENOMMEN.equals(d.status())).ifPresent(this::duellStarten)));
    }
}
