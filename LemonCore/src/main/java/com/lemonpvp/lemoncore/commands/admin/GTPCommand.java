package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GTPCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GTPCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use this command.");
            return true;
        }
        if (!player.hasPermission("lemoncore.admin.tp")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/gtp <player>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(plugin.getMessagesManager().get("player-not-online", "player", args[0]));
            return true;
        }

        player.teleport(target.getLocation());
        player.sendMessage(plugin.getMessagesManager().get("teleport.success", "player", target.getName()));
        return true;
    }
}
