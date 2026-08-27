package com.lemonpvp.lemoncosmetics.display;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Tears down a leaving player's packet cosmetics (viewers are handled by reconcile). */
public class DisplayCosmeticQuitListener implements Listener {

    private final DisplayCosmeticManager manager;

    public DisplayCosmeticQuitListener(DisplayCosmeticManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.clear(event.getPlayer().getUniqueId());
    }
}
