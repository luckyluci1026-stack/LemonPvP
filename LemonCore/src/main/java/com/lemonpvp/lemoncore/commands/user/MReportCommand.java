package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;

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
        UUID reporterUuid = reporter.getUniqueId();
        String reporterName = reporter.getName();
        Player online = Bukkit.getPlayer(targetName);
        if (online != null) {
            plugin.getReportManager().submitMessageReport(reporterUuid, reporterName,
                    online.getUniqueId(), online.getName(), reason)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(reporterUuid);
                        if (p != null) p.sendMessage(plugin.getMessagesManager().get("mreport.success"));
                    }));
        } else {
            plugin.getPlayerDataManager().findUUIDByName(targetName).thenAccept(uuid ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(reporterUuid);
                    if (p == null) return;
                    if (uuid == null) { p.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName)); return; }
                    plugin.getReportManager().submitMessageReport(reporterUuid, reporterName,
                            uuid, targetName, reason)
                            .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                                Player pp = Bukkit.getPlayer(reporterUuid);
                                if (pp != null) pp.sendMessage(plugin.getMessagesManager().get("mreport.success"));
                            }));
                }));
        }
    }
}
