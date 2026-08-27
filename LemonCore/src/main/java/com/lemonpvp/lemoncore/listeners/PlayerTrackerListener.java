package com.lemonpvp.lemoncore.listeners;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.Action;

public class PlayerTrackerListener implements Listener {

    private final LemonCore plugin;

    public PlayerTrackerListener(LemonCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerTracker().recordJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Disable fly on leave so it doesn't persist (skip creative — it's native)
        if (player.getGameMode() != GameMode.CREATIVE && player.getAllowFlight()) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        plugin.getPlayerTracker().cleanup(player.getUniqueId());
    }

    /** Left-click arm swing. */
    @EventHandler
    public void onAnimation(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        plugin.getPlayerTracker().recordClick(event.getPlayer().getUniqueId());
    }

    /** Right-click interactions. */
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        plugin.getPlayerTracker().recordClick(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        Entity victim = event.getEntity();
        // Eye-level of attacker to hitbox center of victim
        double reach = attacker.getEyeLocation()
                .distance(victim.getLocation().add(0, victim.getHeight() / 2.0, 0));
        plugin.getPlayerTracker().recordReach(attacker.getUniqueId(),
                Math.round(reach * 100.0) / 100.0);
    }
}
