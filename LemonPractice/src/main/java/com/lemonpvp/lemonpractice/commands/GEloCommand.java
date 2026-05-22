package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GEloCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;

    public GEloCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemonpractice.admin.elo")) {
            sender.sendMessage(MM.deserialize("<red>No permission."));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(MM.deserialize(
                    "<red>Usage: /gelo <player> <add|remove|set|reset> <gamemode> [amount]"));
            return true;
        }

        String targetName = args[0];
        String action     = args[1].toLowerCase();
        String gamemode   = args[2].toLowerCase();

        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = offline.getUniqueId();
        String display = offline.getName() != null ? offline.getName() : targetName;

        switch (action) {
            case "add", "remove", "set" -> {
                if (args.length < 4) {
                    sender.sendMessage(MM.deserialize(
                            "<red>Usage: /gelo <player> " + action + " <gamemode> <amount>"));
                    return true;
                }
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MM.deserialize("<red>Invalid amount — must be an integer."));
                    return true;
                }
                CompletableFuture<Integer> future = switch (action) {
                    case "add"    -> plugin.getEloManager().adminAdd(uuid, gamemode, amount);
                    case "remove" -> plugin.getEloManager().adminRemove(uuid, gamemode, amount);
                    default       -> plugin.getEloManager().adminSet(uuid, gamemode, amount);
                };
                future.thenAccept(newElo -> Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(MM.deserialize(
                                "<green>ELO updated for <white>" + display
                                + "</white> [<white>" + gamemode + "</white>]: <white>"
                                + newElo + "</white></green>"))));
            }
            case "reset" -> plugin.getEloManager().adminReset(uuid, gamemode)
                    .thenAccept(newElo -> Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(MM.deserialize(
                                    "<green>ELO reset for <white>" + display
                                    + "</white> [<white>" + gamemode
                                    + "</white>]: <white>" + newElo
                                    + "</white> (placement matches reset)</green>"))));
            default -> sender.sendMessage(MM.deserialize(
                    "<red>Unknown action. Valid: add, remove, set, reset"));
        }
        return true;
    }
}
