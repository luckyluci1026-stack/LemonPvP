package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GSpecCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GSpecCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use this command.");
            return true;
        }
        if (!player.hasPermission("lemoncore.admin.spec")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(plugin.getMessagesManager().get("spectator.enabled"));
        return true;
    }
}
