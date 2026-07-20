package de.lemonpvp.lifesteal.listener;

import de.lemonpvp.lifesteal.LifestealPlus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Tod (Herz-Transfer) und Beitritt (Herzen synchronisieren, Elimination halten).
 */
public final class LifestealListener implements Listener {

    private final LifestealPlus plugin;

    public LifestealListener(LifestealPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        // Wenn dieser Tod bereits vom CombatLog behandelt wurde: ueberspringen
        if (plugin.consumeCombatLogGuard(victim.getUniqueId())) {
            return;
        }
        Player killer = victim.getKiller();
        boolean pvp = killer != null && !killer.getUniqueId().equals(victim.getUniqueId());
        plugin.killHandler().handle(victim,
                pvp ? killer.getUniqueId() : null,
                pvp ? killer.getName() : "",
                pvp);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.hearts().syncOnJoin(player);
        plugin.eliminations().enforceOnJoin(player);
    }
}
