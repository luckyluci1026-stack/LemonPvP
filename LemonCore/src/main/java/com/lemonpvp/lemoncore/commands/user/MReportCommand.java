package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class MReportCommand implements CommandExecutor {

    private final LemonCore plugin;

    public MReportCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("lemoncore.use.mreport")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/mreport <player> <reason>"));
            return true;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        resolveUuid(targetName, player, reason);
        return true;
    }

    private void resolveUuid(String targetName, Player reporter, String reason) {
        Player online = Bukkit.getPlayer(targetName);
        if (online != null) {
            plugin.getReportManager().submitMessageReport(reporter.getUniqueId(), reporter.getName(),
                    online.getUniqueId(), online.getName(), reason)
                    .thenRun(() -> reporter.sendMessage(plugin.getMessagesManager().get("mreport.success")));
        } else {
            plugin.getPlayerDataManager().findUUIDByName(targetName).thenAccept(uuid -> {
                if (uuid == null) { reporter.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName)); return; }
                plugin.getReportManager().submitMessageReport(reporter.getUniqueId(), reporter.getName(),
                        uuid, targetName, reason)
                        .thenRun(() -> reporter.sendMessage(plugin.getMessagesManager().get("mreport.success")));
            });
        }
    }
}
