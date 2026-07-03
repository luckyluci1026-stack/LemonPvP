package com.lemonpvp.lemoncosmetics.listeners;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.ArrowTrailType;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

/**
 * Starts the shooter's active trail on launched projectiles. Arrows (incl.
 * spectral arrows and tridents via {@link AbstractArrow}) always get the trail;
 * ender pearls, snowballs and eggs are included when
 * {@code trails.all-projectiles} is enabled (default on).
 */
public class ArrowTrailListener implements Listener {

    private final LemonCosmetics plugin;

    public ArrowTrailListener(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player player)) return;
        if (!isTrailable(projectile)) return;

        ArrowTrailType trail = plugin.getArrowTrailManager().getActiveTrail(player.getUniqueId());
        if (trail == null) return;

        plugin.getArrowTrailManager().startTrail(projectile, trail);
    }

    private boolean isTrailable(Projectile projectile) {
        if (projectile instanceof AbstractArrow) return true; // arrows, spectral, tridents
        if (!plugin.getConfig().getBoolean("trails.all-projectiles", true)) return false;
        return projectile instanceof EnderPearl
                || projectile instanceof Snowball
                || projectile instanceof Egg;
    }
}
