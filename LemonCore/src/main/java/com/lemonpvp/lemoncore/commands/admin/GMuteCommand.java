package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GMuteCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GMuteCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.mute")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/gmute <player>"));
            return true;
        }

        String targetName = args[0];
        UUID senderUuid = sender instanceof Player p ? p.getUniqueId() : null;
        long duration = plugin.getConfigManager().getMuteDuration(null);

        Player online = Bukkit.getPlayer(targetName);
        if (online != null) {
            plugin.getMuteManager().mutePlayer(online.getUniqueId(), online.getName(), "Muted by admin",
                    senderUuid, sender.getName(), duration)
                    .thenRun(() -> sender.sendMessage(plugin.getMessagesManager().get("mute.success", "player", targetName)));
        } else {
            plugin.getPlayerDataManager().findUUIDByName(targetName).thenAccept(uuid -> {
                if (uuid == null) { sender.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName)); return; }
                plugin.getMuteManager().mutePlayer(uuid, targetName, "Muted by admin",
                        senderUuid, sender.getName(), duration)
                        .thenRun(() -> sender.sendMessage(plugin.getMessagesManager().get("mute.success", "player", targetName)));
            });
        }
        return true;
    }
}
