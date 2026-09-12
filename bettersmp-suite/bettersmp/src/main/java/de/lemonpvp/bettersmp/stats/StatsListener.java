package de.lemonpvp.bettersmp.stats;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Schreibt Kills, Tode, Mob-Kills, Joins und Spielzeit in die Datenbank.
 */
public final class StatsListener implements Listener {

    private final BetterSMP plugin;

    public StatsListener(BetterSMP plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("stats.enabled", true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled()) {
            return;
        }
        Player player = event.getPlayer();
        plugin.database().recordJoin(player.getUniqueId(), player.getName());
        plugin.stats().startSession(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (enabled()) {
            plugin.stats().endSession(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!enabled()) {
            return;
        }
        Player victim = event.getEntity();
        plugin.database().bump(victim.getUniqueId(), "deaths", 1);
        Player killer = victim.getKiller();
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            plugin.database().bump(killer.getUniqueId(), "kills", 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobDeath(EntityDeathEvent event) {
        if (!enabled() || event instanceof PlayerDeathEvent) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            plugin.database().bump(killer.getUniqueId(), "mob_kills", 1);
        }
    }
}
