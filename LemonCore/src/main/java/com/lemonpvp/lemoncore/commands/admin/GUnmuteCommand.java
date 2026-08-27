package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Bukkit;
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

        String target = args[0];
        String adminName = sender.getName();
        plugin.getMuteManager().unmute(target).thenAccept(record ->
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (record != null) {
                    sender.sendMessage(plugin.getMessagesManager().get("mute.unmute-success", "player", record.username));
                    plugin.getDiscordWebhookManager().sendUnmute(record.username, adminName);
                } else {
                    sender.sendMessage(plugin.getMessagesManager().get("mute.not-muted", "player", target));
                }
            }));
        return true;
    }
}
