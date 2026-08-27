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

        // On fight backends (FFA/duels/…) /lobby /hub /spawn must move the player
        // to the LOBBY SERVER — a local teleport would strand them on this server.
        // On the lobby itself it stays a local teleport to the spawn point.
        String thisServer = plugin.getConfig().getString("server-name", "lobby");
        String lobbyServer = plugin.getServersConfig().getString("servers.lobby.name", "lobby");
        if (!thisServer.equalsIgnoreCase(lobbyServer)) {
            player.sendMessage(plugin.getMessagesManager().get("teleport.spawn"));
            plugin.getVelocityMessaging().sendToServer(player, lobbyServer);
            return true;
        }

        plugin.teleportToLobby(player);
        player.sendMessage(plugin.getMessagesManager().get("teleport.spawn"));
        return true;
    }
}
