package de.lemonpvp.duelplus.presence;

import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.db.SpielerSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * NUR auf dem Server mit ist-loot-quelle: true (in der Praxis: SMP)
 * registriert. Haelt duelplus_stamm_inventar als staendig aktuellen
 * Spiegel des ECHTEN Inventars - das schickt AnfragePollTask tatsaechlich
 * mit in die Arena, wenn von einem ANDEREN Server aus (z.B. der Lobby)
 * herausgefordert wird, statt des dort lokalen, meist leeren/anderen
 * Live-Inventars.
 *
 * Genauso wichtig wie der Weg HIN: ein Duell-Ergebnis, das entstanden
 * ist, waehrend man NICHT auf der Loot-Quelle war (siehe AnfragePollTask.
 * ergebnisAnwenden), landet ebenfalls nur im Spiegel - erst beim
 * naechsten Beitritt HIER wird es wirklich ins echte Inventar
 * uebernommen (beimJoin unten).
 *
 * Gleiches Herzschlag-Muster wie PresenceService: ein Quit alleine
 * wuerde bei einem Absturz ohne sauberes PlayerQuitEvent fuer immer
 * veraltet stehen bleiben.
 */
public final class StammInventarService implements Listener {

    private static final long HERZSCHLAG_TICKS = 30L * 20L;

    private final DuelPlus plugin;

    public StammInventarService(DuelPlus plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::herzschlag, HERZSCHLAG_TICKS, HERZSCHLAG_TICKS);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void beimJoin(PlayerJoinEvent event) {
        Player spieler = event.getPlayer();
        plugin.db().stammInventarLesen(spieler.getUniqueId()).thenAccept(snapshotOpt ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!spieler.isOnline()) {
                        return;
                    }
                    // Leerer Spiegel (z.B. beim allerersten Login) heisst:
                    // nichts zu uebernehmen, das frisch geladene Inventar
                    // bleibt einfach wie es ist.
                    snapshotOpt.ifPresent(snap -> snap.anwenden(spieler.getInventory()));
                    sichern(spieler);
                }));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimVerlassen(PlayerQuitEvent event) {
        sichern(event.getPlayer());
    }

    private void herzschlag() {
        for (Player spieler : Bukkit.getOnlinePlayers()) {
            sichern(spieler);
        }
    }

    private void sichern(Player spieler) {
        plugin.db().stammInventarSchreiben(spieler.getUniqueId(), SpielerSnapshot.von(spieler.getInventory()));
    }
}
