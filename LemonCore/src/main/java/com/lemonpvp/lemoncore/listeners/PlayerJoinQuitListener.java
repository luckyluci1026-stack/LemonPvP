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

        // Load player data async, then check ban
        plugin.getPlayerDataManager().loadPlayer(player.getUniqueId(), player.getName())
                .thenAccept(data -> {
                    // Check for active ban
                    plugin.getBanManager().getActiveBan(player.getUniqueId())
                            .thenAccept(ban -> {
                                if (ban != null) {
                                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                                            plugin.getListenerManager().performBanKick(player, ban));
                                } else {
                                    // Update scoreboard and apply display name
                                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                                        if (!player.isOnline()) return;
                                        plugin.getScoreboardManager().updateScoreboard(player, data);
                                        plugin.getPlayerDataManager().applyNames(player);
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
