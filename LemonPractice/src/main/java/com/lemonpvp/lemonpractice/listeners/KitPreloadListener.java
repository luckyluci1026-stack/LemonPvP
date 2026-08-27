package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Warms the per-JVM kit cache on join.
 *
 * <p>The {@code KitManager} cache is per-server, so each LemonPractice instance
 * (lobby <i>and</i> duels) must load a player's saved kit arrangement from the
 * shared database before it can be applied. Without this, saved kits would only
 * exist on the server they were edited on (the lobby) and never apply in duels
 * or FFA. Loading is async, so it is started here on join.</p>
 */
public class KitPreloadListener implements Listener {

    private final LemonPractice plugin;

    public KitPreloadListener(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getKitManager().preload(event.getPlayer().getUniqueId());
    }
}
