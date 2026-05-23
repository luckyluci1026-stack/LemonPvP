package com.lemonpvp.lemoncosmetics.listeners;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.ArrowTrailType;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class ArrowTrailListener implements Listener {

    private final LemonCosmetics plugin;

    public ArrowTrailListener(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        ArrowTrailType trail = plugin.getArrowTrailManager().getActiveTrail(player.getUniqueId());
        if (trail == null) return;

        plugin.getArrowTrailManager().startTrail(arrow, trail);
    }
}
