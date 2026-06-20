package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RestartCommand implements CommandExecutor {

    private final LemonCore plugin;

    public RestartCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.restart")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("cancel")) {
            if (!plugin.getRestartManager().isRestarting()) {
                sender.sendMessage(plugin.getMessagesManager().get("restart.not-running"));
                return true;
            }
            plugin.getRestartManager().cancel();
            sender.sendMessage(plugin.getMessagesManager().get("restart.cancelled"));
            return true;
        }

        if (plugin.getRestartManager().isRestarting()) {
            sender.sendMessage(plugin.getMessagesManager().get("restart.already-running"));
            return true;
        }
        plugin.getRestartManager().beginRestart();
        sender.sendMessage(plugin.getMessagesManager().get("restart.initiated"));
        return true;
    }
}
