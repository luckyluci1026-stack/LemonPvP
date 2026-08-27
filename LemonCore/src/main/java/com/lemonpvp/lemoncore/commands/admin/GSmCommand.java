package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GSmCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GSmCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use this command.");
            return true;
        }
        if (!player.hasPermission("lemoncore.admin.sm")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        player.setGameMode(GameMode.SURVIVAL);
        player.sendMessage(plugin.getMessagesManager().get("spectator.survival"));
        return true;
    }
}
