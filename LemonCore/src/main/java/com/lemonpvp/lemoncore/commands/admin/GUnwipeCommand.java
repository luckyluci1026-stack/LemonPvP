package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GUnwipeCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GUnwipeCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.unwipe")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/gunwipe <player> <date>"));
            return true;
        }

        String targetName = args[0];
        String date = args[1];

        resolveUuid(targetName, uuid -> {
            if (uuid == null) { sender.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName)); return; }
            plugin.getStatsManager().findWipeByDate(uuid, date).thenAccept(wipe -> {
                if (wipe == null) { sender.sendMessage(plugin.getMessagesManager().get("stats.no-wipe")); return; }
                plugin.getStatsManager().restoreStats(uuid, wipe)
                        .thenRun(() -> sender.sendMessage(plugin.getMessagesManager().get("stats.unwipe-success", "player", targetName, "date", date)));
            });
        });
        return true;
    }

    private void resolveUuid(String name, java.util.function.Consumer<UUID> callback) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) { callback.accept(online.getUniqueId()); return; }
        plugin.getPlayerDataManager().findUUIDByName(name).thenAccept(callback);
    }
}
