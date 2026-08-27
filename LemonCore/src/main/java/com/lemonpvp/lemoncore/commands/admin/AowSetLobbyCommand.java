package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AowSetLobbyCommand implements CommandExecutor {

    private final LemonCore plugin;

    public AowSetLobbyCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use this command.");
            return true;
        }
        if (!player.hasPermission("lemoncore.admin.setlobby")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }

        var loc = player.getLocation();
        plugin.getConfig().set("lobby.spawn.world", loc.getWorld().getName());
        plugin.getConfig().set("lobby.spawn.x", loc.getX());
        plugin.getConfig().set("lobby.spawn.y", loc.getY());
        plugin.getConfig().set("lobby.spawn.z", loc.getZ());
        plugin.getConfig().set("lobby.spawn.yaw", (double) loc.getYaw());
        plugin.getConfig().set("lobby.spawn.pitch", (double) loc.getPitch());
        plugin.saveConfig();

        player.sendMessage(plugin.getMessagesManager().get("teleport.lobby-set"));
        return true;
    }
}
