package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class GUnbanCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GUnbanCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.unban")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/gunban <player|banID>"));
            return true;
        }

        String target = args[0];
        String adminName = sender.getName();
        plugin.getBanManager().unban(target).thenAccept(record ->
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (record != null) {
                    sender.sendMessage(plugin.getMessagesManager().get("ban.unban-success", "player", record.username));
                    plugin.getDiscordWebhookManager().sendUnban(record.username, adminName, record.id);
                } else {
                    sender.sendMessage(plugin.getMessagesManager().get("ban.not-banned", "player", target));
                }
            }));
        return true;
    }
}
