package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class LPracticeCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonPractice plugin;

    public LPracticeCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemonpractice.admin.reload")) {
            sender.sendMessage(MM.deserialize("<red>No permission."));
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(MM.deserialize("<yellow>Usage: /lpractice reload"));
            return true;
        }
        plugin.reloadSecondaryConfigs();
        plugin.getGamemodeManager().loadGamemodes();
        plugin.getArenaManager().loadAll();
        sender.sendMessage(MM.deserialize(
            "<green>LemonPractice reloaded — kits, gamemodes, arenas, messages updated from disk."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("reload");
        return List.of();
    }
}
