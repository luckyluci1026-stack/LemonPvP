package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * {@code /punish <player>} — permanent ban whose reason is fixed in config.yml
 * under the {@code punish} section. The staff member only supplies the player
 * name; the ban never expires.
 */
public class PunishCommand implements CommandExecutor {

    /** Negative duration → permanent (see BanManager: durationSeconds &gt; 0 check). */
    private static final long PERMANENT = -1L;

    private final LemonCore plugin;

    public PunishCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.punish")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/punish <player>"));
            return true;
        }

        String targetName = args[0];
        String reason = plugin.getConfigManager().getPunishReason();
        UUID senderUuid = sender instanceof Player p ? p.getUniqueId() : null;
        String senderName = sender.getName();

        Player online = Bukkit.getPlayer(targetName);
        if (online != null) {
            executeBan(sender, online.getUniqueId(), online.getName(), reason, senderUuid, senderName, online);
        } else {
            plugin.getPlayerDataManager().findUUIDByName(targetName).thenAccept(uuid -> {
                if (uuid == null) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName)));
                    return;
                }
                executeBan(sender, uuid, targetName, reason, senderUuid, senderName, null);
            });
        }
        return true;
    }

    private void executeBan(CommandSender sender, UUID targetUuid, String targetName,
                             String reason, UUID senderUuid, String senderName, Player onlineTarget) {
        plugin.getBanManager().banPlayer(targetUuid, targetName, reason, senderUuid, senderName, PERMANENT)
                .thenAccept(ban -> {
                    if (ban == null) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(plugin.getMessagesManager().get("ban.success", "player", targetName, "reason", reason));
                        if (onlineTarget != null) {
                            plugin.getListenerManager().performBanKick(onlineTarget, ban);
                        }
                    });
                });
    }
}
