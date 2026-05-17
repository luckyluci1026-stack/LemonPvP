package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BugReportCommand implements CommandExecutor {

    private final LemonCore plugin;

    public BugReportCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("lemoncore.use.bugreport")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/bugreport <description>"));
            return true;
        }

        String description = String.join(" ", args);
        plugin.getReportManager().submitBugReport(player.getUniqueId(), player.getName(), description)
                .thenRun(() -> player.sendMessage(plugin.getMessagesManager().get("bug.success")));
        return true;
    }
}
