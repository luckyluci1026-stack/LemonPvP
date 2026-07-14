package com.lemonpvp.lemonevents.listeners;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.model.HostedEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Bridges Bukkit events into the {@link com.lemonpvp.lemonevents.managers.HostedEventManager}:
 * turns deaths into eliminations, protects players during the pre-fight freeze,
 * and forfeits players who disconnect mid-event.
 */
public class HostedEventListener implements Listener {

    private final LemonEvents plugin;

    public HostedEventListener(LemonEvents plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        HostedEvent ev = plugin.getHostedEventManager().getActive();
        if (ev == null || ev.getState() != HostedEvent.State.RUNNING) return;
        if (!ev.getAlive().contains(player.getUniqueId())) return;
        // No damage while the countdown freeze is active.
        if (ev.isFrozen()) event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        HostedEvent ev = plugin.getHostedEventManager().getActive();
        if (ev == null || !ev.getAlive().contains(uuid)) return;

        // No item scatter inside the arena; clear the death screen quickly.
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.deathMessage(null);
        plugin.getHostedEventManager().onDeath(uuid);

        // Auto-respawn so the spectator gamemode set in onDeath takes effect.
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null && p.isDead()) p.spigot().respawn();
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getHostedEventManager().handleQuit(event.getPlayer().getUniqueId());
    }
}
