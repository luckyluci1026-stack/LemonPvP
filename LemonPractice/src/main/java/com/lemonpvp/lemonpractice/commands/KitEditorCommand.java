package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.gui.QueueGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /kiteditor} — opens the kit editor gamemode picker. Exists primarily
 * so the LOBBY server (LemonLobby's hotbar book) can open the editor locally
 * without a compile-time dependency between the plugins.
 */
public class KitEditorCommand implements CommandExecutor {

    private final LemonPractice plugin;

    public KitEditorCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        new QueueGUI(plugin, player).openKitEditor();
        return true;
    }
}
