package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Auto-bookmarks highlight moments in duel replays: whenever a recorded duelist
 * is hit by a player (melee or projectile), the current replay frame is marked
 * so viewers can jump between fights with /replay next|prev.
 */
public class ReplayHitListener implements Listener {

    private final LemonPractice plugin;

    public ReplayHitListener(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        boolean fromPlayer = event.getDamager() instanceof Player
                || (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player);
        if (!fromPlayer) return;
        plugin.getReplayManager().markHit(victim.getUniqueId());
    }
}
