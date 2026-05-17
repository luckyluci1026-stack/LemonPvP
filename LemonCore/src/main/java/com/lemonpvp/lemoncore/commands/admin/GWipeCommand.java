package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GWipeCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GWipeCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.wipe")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/gwipe <player>"));
            return true;
        }

        String targetName = args[0];
        UUID senderUuid = sender instanceof Player p ? p.getUniqueId() : null;

        Player online = Bukkit.getPlayer(targetName);
        if (online != null) {
            plugin.getStatsManager().wipeStats(online.getUniqueId(), online.getName(), senderUuid, sender.getName())
                    .thenRun(() -> sender.sendMessage(plugin.getMessagesManager().get("stats.wipe-success", "player", targetName)));
        } else {
            plugin.getPlayerDataManager().findUUIDByName(targetName).thenAccept(uuid -> {
                if (uuid == null) { sender.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName)); return; }
                plugin.getStatsManager().wipeStats(uuid, targetName, senderUuid, sender.getName())
                        .thenRun(() -> sender.sendMessage(plugin.getMessagesManager().get("stats.wipe-success", "player", targetName)));
            });
        }
        return true;
    }
}
