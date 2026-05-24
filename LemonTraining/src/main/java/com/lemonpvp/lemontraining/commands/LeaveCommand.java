package com.lemonpvp.lemontraining.commands;

import com.lemonpvp.lemontraining.LemonTraining;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonTraining plugin;

    public LeaveCommand(LemonTraining plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!plugin.getPracticeManager().isInSession(player.getUniqueId())) {
            player.sendMessage(MM.deserialize(
                    plugin.getMessages().getString("prefix", "") +
                    plugin.getMessages().getString("no-session", "<red>You are not in a practice session.")));
            return true;
        }
        plugin.getPracticeManager().endPractice(player.getUniqueId(), true);
        player.sendMessage(MM.deserialize(
                plugin.getMessages().getString("prefix", "") +
                plugin.getMessages().getString("leave", "<yellow>You left practice.")));
        return true;
    }
}
