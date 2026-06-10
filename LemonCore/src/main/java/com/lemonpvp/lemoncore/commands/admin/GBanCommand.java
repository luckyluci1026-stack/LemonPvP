package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;

public class GBanCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GBanCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.ban")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/gban <player> <reason>"));
            return true;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        UUID senderUuid = sender instanceof Player p ? p.getUniqueId() : null;
        String senderName = sender.getName();
        long duration = plugin.getConfigManager().getBanDuration(reason);

        // Resolve target
        Player online = Bukkit.getPlayer(targetName);
        if (online != null) {
            executeBan(sender, online.getUniqueId(), online.getName(), reason, senderUuid, senderName, duration, online);
        } else {
            plugin.getPlayerDataManager().findUUIDByName(targetName).thenAccept(uuid -> {
                if (uuid == null) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName)));
                    return;
                }
                executeBan(sender, uuid, targetName, reason, senderUuid, senderName, duration, null);
            });
        }
        return true;
    }

    private void executeBan(CommandSender sender, UUID targetUuid, String targetName,
                             String reason, UUID senderUuid, String senderName,
                             long duration, Player onlineTarget) {
        plugin.getBanManager().banPlayer(targetUuid, targetName, reason, senderUuid, senderName, duration)
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
