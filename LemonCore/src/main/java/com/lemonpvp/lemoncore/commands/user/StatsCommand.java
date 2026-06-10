package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.gui.StatsGUI;
import com.lemonpvp.lemoncore.managers.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class StatsCommand implements CommandExecutor {

    private final LemonCore plugin;

    public StatsCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("lemoncore.use.stats")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }

        if (args.length >= 1) {
            String targetName = args[0];
            Player online = Bukkit.getPlayer(targetName);
            if (online != null) {
                PlayerData data = plugin.getPlayerDataManager().getCached(online.getUniqueId());
                if (data != null) {
                    openStatsGUI(player, data);
                } else {
                    UUID playerUuid = player.getUniqueId();
                    plugin.getPlayerDataManager().loadOfflinePlayer(targetName)
                            .thenAccept(d -> Bukkit.getScheduler().runTask(plugin, () -> {
                                Player p = Bukkit.getPlayer(playerUuid);
                                if (p == null) return;
                                if (d == null) {
                                    p.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName));
                                } else {
                                    openStatsGUI(p, d);
                                }
                            }));
                }
            } else {
                plugin.getPlayerDataManager().loadOfflinePlayer(targetName)
                        .thenAccept(d -> {
                            if (d == null) {
                                player.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName));
                            } else {
                                Bukkit.getScheduler().runTask(plugin, () -> openStatsGUI(player, d));
                            }
                        });
            }
        } else {
            PlayerData data = plugin.getPlayerDataManager().getCached(player.getUniqueId());
            if (data != null) {
                openStatsGUI(player, data);
            } else {
                player.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", player.getName()));
            }
        }
        return true;
    }

    private void openStatsGUI(Player viewer, PlayerData data) {
        new StatsGUI(plugin, data).open(viewer);
    }
}
