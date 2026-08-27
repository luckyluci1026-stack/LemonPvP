package com.lemonpvp.lemonlobby.commands;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.gui.DailyRewardGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /daily} — opens the daily-reward calendar. */
public class DailyCommand implements CommandExecutor {

    private final LemonLobby plugin;

    public DailyCommand(LemonLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        new DailyRewardGUI(plugin, player).open();
        return true;
    }
}
