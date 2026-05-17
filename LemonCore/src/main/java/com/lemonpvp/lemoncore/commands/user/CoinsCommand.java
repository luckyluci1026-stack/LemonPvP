package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CoinsCommand implements CommandExecutor {

    private final LemonCore plugin;

    public CoinsCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        PlayerData data = plugin.getPlayerDataManager().getCached(player.getUniqueId());
        if (data != null) {
            player.sendMessage(plugin.getMessagesManager().get("coins.show", "coins", String.valueOf(data.getCoins())));
        } else {
            plugin.getPlayerDataManager().getCoins(player.getUniqueId())
                    .thenAccept(coins -> player.sendMessage(plugin.getMessagesManager().get("coins.show", "coins", String.valueOf(coins))));
        }
        return true;
    }
}
