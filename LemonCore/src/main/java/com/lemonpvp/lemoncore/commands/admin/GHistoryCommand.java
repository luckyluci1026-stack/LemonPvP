package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.gui.HistoryGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GHistoryCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GHistoryCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage("Console cannot use this command.");
            return true;
        }
        if (!admin.hasPermission("lemoncore.admin.history")) {
            admin.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            admin.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/ghistory <player>"));
            return true;
        }

        String targetName = args[0];
        Player online = Bukkit.getPlayer(targetName);
        if (online != null) {
            new HistoryGUI(plugin, online.getUniqueId(), online.getName()).open(admin);
        } else {
            plugin.getPlayerDataManager().findUUIDByName(targetName).thenAccept(uuid -> {
                if (uuid == null) {
                    admin.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName));
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () ->
                        new HistoryGUI(plugin, uuid, targetName).open(admin));
            });
        }
        return true;
    }
}
