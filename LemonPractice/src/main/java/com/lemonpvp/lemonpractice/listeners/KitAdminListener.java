package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Cleans up an admin kit-edit session if the editor disconnects mid-edit, so a
 * stale session (and their snapshotted inventory) doesn't linger. The edit
 * itself is discarded — only an explicit /kitadmin save persists.
 */
public class KitAdminListener implements Listener {

    private final LemonPractice plugin;

    public KitAdminListener(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getKitAdminManager().dropSession(event.getPlayer().getUniqueId());
    }
}
