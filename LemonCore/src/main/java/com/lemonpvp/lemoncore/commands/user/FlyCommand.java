package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCore plugin;

    public FlyCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MM.deserialize("<red>Only players can use this command.</red>"));
            return true;
        }
        if (!player.hasPermission("lemoncore.use.fly")) {
            player.sendMessage(MM.deserialize("<red>No permission.</red>"));
            return true;
        }

        // Admins with lemoncore.admin.fly bypass the server restriction
        if (!player.hasPermission("lemoncore.admin.fly")) {
            String thisServer  = plugin.getConfig().getString("server-name", "");
            String lobbyServer = plugin.getConfig().getString("servers.lobby", "lobby");
            if (!thisServer.equalsIgnoreCase(lobbyServer)) {
                player.sendMessage(MM.deserialize("<red>You can only use /fly in the Lobby.</red>"));
                return true;
            }
        }

        boolean current = player.getAllowFlight();
        player.setAllowFlight(!current);
        player.setFlying(!current);

        if (!current) {
            player.sendMessage(MM.deserialize("<green>Fly enabled.</green>"));
        } else {
            player.sendMessage(MM.deserialize("<green>Fly disabled.</green>"));
        }
        return true;
    }
}
