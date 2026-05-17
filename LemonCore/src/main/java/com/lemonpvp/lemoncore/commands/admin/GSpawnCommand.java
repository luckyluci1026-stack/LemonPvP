package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GSpawnCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GSpawnCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use this command.");
            return true;
        }
        if (!player.hasPermission("lemoncore.admin.gspawn")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        plugin.teleportToLobby(player);
        player.sendMessage(plugin.getMessagesManager().get("teleport.spawn"));
        return true;
    }
}
