package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GCoinsCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GCoinsCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.coins")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage",
                    "usage", "/gcoins add|remove|set|show <player> [amount]"));
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];

        UUID senderUuid = sender instanceof Player p ? p.getUniqueId() : null;

        if (action.equals("show")) {
            resolveUuid(targetName, uuid -> {
                if (uuid == null) { sender.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName)); return; }
                plugin.getPlayerDataManager().getCoins(uuid).thenAccept(coins ->
                        sender.sendMessage(plugin.getMessagesManager().get("coins.show-other",
                                "player", targetName, "coins", String.valueOf(coins))));
            });
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage",
                    "usage", "/gcoins " + action + " <player> <amount>"));
            return true;
        }

        long amount;
        try { amount = Long.parseLong(args[2]); } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-amount"));
            return true;
        }

        final long amt = amount;
        resolveUuid(targetName, uuid -> {
            if (uuid == null) { sender.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName)); return; }
            final UUID finalUuid = uuid;
            switch (action) {
                case "add" -> plugin.getPlayerDataManager().addCoins(finalUuid, amt, "admin", senderUuid)
                        .thenRun(() -> {
                            sender.sendMessage(plugin.getMessagesManager().get("coins.add", "player", targetName, "amount", String.valueOf(amt)));
                            Player target = Bukkit.getPlayer(finalUuid);
                            if (target != null) target.sendMessage(plugin.getMessagesManager().get("coins.receive", "amount", String.valueOf(amt)));
                        });
                case "remove" -> plugin.getPlayerDataManager().removeCoins(finalUuid, amt, "admin", senderUuid)
                        .thenRun(() -> sender.sendMessage(plugin.getMessagesManager().get("coins.remove", "player", targetName, "amount", String.valueOf(amt))));
                case "set" -> plugin.getPlayerDataManager().setCoins(finalUuid, amt, senderUuid)
                        .thenRun(() -> sender.sendMessage(plugin.getMessagesManager().get("coins.set", "player", targetName, "amount", String.valueOf(amt))));
                default -> sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/gcoins add|remove|set|show <player> [amount]"));
            }
        });
        return true;
    }

    private void resolveUuid(String name, java.util.function.Consumer<UUID> callback) {
        var online = Bukkit.getPlayer(name);
        if (online != null) { callback.accept(online.getUniqueId()); return; }
        plugin.getPlayerDataManager().findUUIDByName(name).thenAccept(callback);
    }
}
