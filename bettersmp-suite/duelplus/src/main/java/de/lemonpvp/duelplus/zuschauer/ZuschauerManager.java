package de.lemonpvp.duelplus.zuschauer;

import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.arena.Arena;
import de.lemonpvp.duelplus.db.DuelRecord;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /duel watch <Spieler> - als Zuschauer (SPECTATOR-Modus: keine
 * Kollision, kein Schaden, keine Interaktion) einem laufenden Duell
 * beiwohnen, auch serveruebergreifend: die Anfrage kann von JEDEM
 * Server kommen, das Duell selbst laeuft aber immer auf dem
 * Duels-Server. Nur DORT wird tatsaechlich etwas angewendet - diese
 * Klasse wird zwar ueberall instanziiert (fuer anfordern(), das
 * serveruebergreifend per DB funktionieren muss), ihr PlayerJoinEvent-
 * Handler aber nur auf dem Duels-Server als Listener registriert (siehe
 * DuelPlus.onEnable, gleiches istArenaServer-Gate wie ArenaGuardListener).
 *
 * Absichtlich KEINE Sonderbehandlung in ArenaGuardListener noetig:
 * dessen Sperren (Befehle, Sturz-Erkennung, Schaden) sind ALLE an eine
 * echte DuellSession gebunden (sessionManager().sessionVon(...)) - ein
 * Zuschauer wird nie in eine solche Session aufgenommen, faellt also
 * automatisch durch alle diese Checks durch. Der SPECTATOR-Spielmodus
 * selbst uebernimmt den Rest (keine Kollision, kein Schaden, kein
 * Aufheben von Items).
 */
public final class ZuschauerManager implements Listener {

    private final DuelPlus plugin;

    /** UUID des Zuschauers -> Name der Arena-Welt, die er gerade beobachtet. */
    private final Map<UUID, String> arenaJeZuschauer = new ConcurrentHashMap<>();
    /** UUID des Zuschauers -> Herkunftsserver, fuer die Rueckreise (siehe beenden). */
    private final Map<UUID, String> herkunftJeZuschauer = new ConcurrentHashMap<>();

    public ZuschauerManager(DuelPlus plugin) {
        this.plugin = plugin;
    }

    /** /duel watch <Spieler> - von JEDEM Server aus aufrufbar. */
    public void anfordern(Player zuschauer, String zielName) {
        plugin.db().istBeschaeftigt(zuschauer.getUniqueId()).thenAccept(beschaeftigt -> {
            if (beschaeftigt) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.msgs().send(zuschauer, "watch-busy"));
                return;
            }
            plugin.db().presenceUuidFuerName(zielName, 60).thenAccept(uuidOpt -> {
                if (uuidOpt.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            plugin.msgs().send(zuschauer, "target-offline", "spieler", zielName));
                    return;
                }
                plugin.db().laufendesDuellMitArena(uuidOpt.get()).thenAccept(duellOpt ->
                        Bukkit.getScheduler().runTask(plugin, () -> weiterleiten(zuschauer, zielName, duellOpt)));
            });
        });
    }

    private void weiterleiten(Player zuschauer, String zielName, Optional<DuelRecord> duellOpt) {
        if (duellOpt.isEmpty() || duellOpt.get().arenaWelt() == null) {
            plugin.msgs().send(zuschauer, "watch-not-dueling", "spieler", zielName);
            return;
        }
        String arenaWelt = duellOpt.get().arenaWelt();
        if (plugin.istArenaServer()) {
            zuschauenAnwenden(zuschauer, arenaWelt, plugin.serverName());
            return;
        }
        plugin.db().zuschauerAnfrageSchreiben(zuschauer.getUniqueId(), arenaWelt, plugin.serverName())
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.msgs().send(zuschauer, "watch-teleporting");
                    plugin.bridge().sende(zuschauer, plugin.arenaServerName());
                }));
    }

    /** Nur relevant, wenn als Listener registriert - siehe Klassen-Kommentar (nur auf dem Duels-Server). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void beimJoin(PlayerJoinEvent event) {
        Player spieler = event.getPlayer();
        plugin.db().zuschauerAnfrageHolenUndLoeschen(spieler.getUniqueId()).thenAccept(anfrageOpt ->
                anfrageOpt.ifPresent(anfrage -> Bukkit.getScheduler().runTask(plugin, () ->
                        zuschauenAnwenden(spieler, anfrage.arenaWelt(), anfrage.herkunftServer()))));
    }

    private void zuschauenAnwenden(Player spieler, String arenaWelt, String herkunftServer) {
        Arena arena = plugin.arenaManager().arena(arenaWelt);
        // Arenen existieren dauerhaft (einmal gebaut, nie entfernt) - arena()
        // waere hier also so gut wie IMMER nicht-null, ganz unabhaengig davon,
        // ob dort noch gekaempft wird. istBelegt() prueft das WIRKLICH: faellt
        // das Duell zwischen Anfrage und Ankunft (z.B. serveruebergreifender
        // Sprung) schon zu Ende, landet hier sonst niemand mehr in einer
        // still leeren (oder laengst von einem ANDEREN Duell belegten) Arena,
        // ohne es zu merken.
        if (arena == null || !plugin.arenaManager().istBelegt(arenaWelt)) {
            plugin.msgs().send(spieler, "watch-arena-gone");
            return;
        }
        Location punkt = arena.world().getSpawnLocation().clone().add(0, 6, 0);
        spieler.teleport(punkt);
        spieler.setGameMode(GameMode.SPECTATOR);
        arenaJeZuschauer.put(spieler.getUniqueId(), arenaWelt);
        herkunftJeZuschauer.put(spieler.getUniqueId(), herkunftServer);
        plugin.msgs().send(spieler, "watch-started");
    }

    /** /duel unwatch - zurueck auf den Herkunftsserver, Zuschauer-Status beendet. */
    public void beenden(Player spieler) {
        String arenaWelt = arenaJeZuschauer.remove(spieler.getUniqueId());
        String herkunft = herkunftJeZuschauer.remove(spieler.getUniqueId());
        if (arenaWelt == null) {
            plugin.msgs().send(spieler, "watch-not-watching");
            return;
        }
        spieler.setGameMode(GameMode.SURVIVAL);
        plugin.msgs().send(spieler, "watch-stopped");
        if (herkunft != null && !herkunft.equals(plugin.serverName())) {
            plugin.bridge().sende(spieler, herkunft);
        }
    }

    /**
     * Von DuellSessionManager aufgerufen, sobald eine Arena wieder frei
     * wird (Duell komplett vorbei, siehe ArenaManager.freigeben) - alle
     * dortigen Zuschauer automatisch nach Hause schicken, statt sie
     * einfach in der leeren Arena stehen zu lassen.
     */
    public void arenaBeendet(String arenaWelt) {
        for (UUID uuid : new ArrayList<>(arenaJeZuschauer.keySet())) {
            if (!arenaWelt.equals(arenaJeZuschauer.get(uuid))) {
                continue;
            }
            Player spieler = Bukkit.getPlayer(uuid);
            if (spieler != null) {
                beenden(spieler);
            } else {
                arenaJeZuschauer.remove(uuid);
                herkunftJeZuschauer.remove(uuid);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimVerlassen(PlayerQuitEvent event) {
        arenaJeZuschauer.remove(event.getPlayer().getUniqueId());
        herkunftJeZuschauer.remove(event.getPlayer().getUniqueId());
    }
}
