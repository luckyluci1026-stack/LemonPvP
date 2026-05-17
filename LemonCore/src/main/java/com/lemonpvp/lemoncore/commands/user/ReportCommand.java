package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class ReportCommand implements CommandExecutor {

    private final LemonCore plugin;

    public ReportCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("lemoncore.use.report")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/report <player> <reason>"));
            return true;
        }

        String targetName = args[0];
        if (targetName.equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getMessagesManager().get("report.self-report"));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(plugin.getMessagesManager().get("player-not-online", "player", targetName));
            return true;
        }

        plugin.getReportManager().submitReport(player.getUniqueId(), player.getName(),
                target.getUniqueId(), target.getName(), reason)
                .thenRun(() -> player.sendMessage(plugin.getMessagesManager().get("report.success")));
        return true;
    }
}
