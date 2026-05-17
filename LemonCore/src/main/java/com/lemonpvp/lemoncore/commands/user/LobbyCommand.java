package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LobbyCommand implements CommandExecutor {

    private final LemonCore plugin;

    public LobbyCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("lemoncore.use.spawn")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        plugin.teleportToLobby(player);
        player.sendMessage(plugin.getMessagesManager().get("teleport.spawn"));
        return true;
    }
}
