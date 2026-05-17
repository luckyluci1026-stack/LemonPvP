package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GCoinsAllCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GCoinsAllCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.coinsall")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/gcoinsall <amount>"));
            return true;
        }

        long amount;
        try { amount = Long.parseLong(args[0]); } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-amount"));
            return true;
        }

        UUID adminUuid = sender instanceof Player p ? p.getUniqueId() : null;
        final long amt = amount;

        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.getPlayerDataManager().addCoins(p.getUniqueId(), amt, "coinsall", adminUuid)
                    .thenRun(() -> p.sendMessage(plugin.getMessagesManager().get("coins.receive", "amount", String.valueOf(amt))));
        }

        sender.sendMessage(plugin.getMessagesManager().get("coins.coinsall", "amount", String.valueOf(amt)));
        return true;
    }
}
