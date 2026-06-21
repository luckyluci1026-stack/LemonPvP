package com.lemonpvp.lemoncore.listeners;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.BanRecord;
import com.lemonpvp.lemoncore.managers.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {

    private final LemonCore plugin;

    public PlayerJoinQuitListener(LemonCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        // Suppress join message
        event.joinMessage(null);

        var player = event.getPlayer();
        java.util.UUID uuid = player.getUniqueId();

        // Load player data async, then check ban
        plugin.getPlayerDataManager().loadPlayer(uuid, player.getName())
                .thenAccept(data -> {
                    // Check for active ban
                    plugin.getBanManager().getActiveBan(uuid)
                            .thenAccept(ban -> {
                                if (ban != null) {
                                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                                        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uuid);
                                        if (p != null) plugin.getListenerManager().performBanKick(p, ban);
                                    });
                                } else {
                                    // Update scoreboard and apply display name
                                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                                        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uuid);
                                        if (p == null) return;
                                        plugin.getScoreboardManager().updateScoreboard(p, data);
                                        plugin.getPlayerDataManager().applyNames(p);
                                    });
                                }
                            });
                });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        // Suppress quit message
        event.quitMessage(null);

        var player = event.getPlayer();
        // Save and remove from cache
        plugin.getPlayerDataManager().savePlayer(player.getUniqueId())
                .thenRun(() -> plugin.getPlayerDataManager().removeCached(player.getUniqueId()));
    }
}
