package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * {@code /offend <player>} — temporary ban whose duration and reason are fixed
 * in config.yml under the {@code offend} section. The staff member only supplies
 * the player name; duration and reason are never typed.
 */
public class OffendCommand implements CommandExecutor {

    private final LemonCore plugin;

    public OffendCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.offend")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/offend <player>"));
            return true;
        }

        String targetName = args[0];
        String reason = plugin.getConfigManager().getOffendReason();
        long duration = plugin.getConfigManager().getOffendDuration();
        UUID senderUuid = sender instanceof Player p ? p.getUniqueId() : null;
        String senderName = sender.getName();

        Player online = Bukkit.getPlayer(targetName);
        if (online != null) {
            executeBan(sender, online.getUniqueId(), online.getName(), reason, senderUuid, senderName, duration);
        } else {
            plugin.getPlayerDataManager().findUUIDByName(targetName).thenAccept(uuid -> {
                if (uuid == null) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName)));
                    return;
                }
                executeBan(sender, uuid, targetName, reason, senderUuid, senderName, duration);
            });
        }
        return true;
    }

    private void executeBan(CommandSender sender, UUID targetUuid, String targetName,
                             String reason, UUID senderUuid, String senderName, long duration) {
        plugin.getBanManager().banPlayer(targetUuid, targetName, reason, senderUuid, senderName, duration)
                .thenAccept(ban -> {
                    if (ban == null) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(plugin.getMessagesManager().get("ban.success", "player", targetName, "reason", reason));
                        Player t = Bukkit.getPlayer(targetUuid);
                        if (t != null) plugin.getListenerManager().performBanKick(t, ban);
                    });
                });
    }
}
