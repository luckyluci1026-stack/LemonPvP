package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GPopCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCore plugin;

    public GPopCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.pop")) {
            sender.sendMessage(MM.deserialize("<red>No permission.</red>"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MM.deserialize("<red>Usage: /gpop <player></red>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MM.deserialize("<red>Player not found: " + args[0] + "</red>"));
            return true;
        }

        target.setHealth(0.0);
        sender.sendMessage(MM.deserialize(
                "<green>Set health to 0 on " + target.getName()
                        + " — watch for auto-totem.</green>"));
        return true;
    }
}
