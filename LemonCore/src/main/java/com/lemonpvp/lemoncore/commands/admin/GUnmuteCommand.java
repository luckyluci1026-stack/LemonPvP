package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class GUnmuteCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GUnmuteCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.unmute")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/gunmute <player>"));
            return true;
        }

        plugin.getMuteManager().unmute(args[0]).thenAccept(success -> {
            if (success) {
                sender.sendMessage(plugin.getMessagesManager().get("mute.unmute-success", "player", args[0]));
            } else {
                sender.sendMessage(plugin.getMessagesManager().get("mute.not-muted", "player", args[0]));
            }
        });
        return true;
    }
}
