package com.lemonpvp.lemonlobby.commands;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.gui.ShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final LemonLobby plugin;

    public ShopCommand(LemonLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        new ShopGUI(plugin, player).open();
        return true;
    }
}
