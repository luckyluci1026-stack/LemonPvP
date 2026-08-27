package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

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
            player.sendMessage(plugin.getMessagesManager().get("coins.show", "coins", com.lemonpvp.lemoncore.util.TextUtil.formatCoins(data.getCoins())));
        } else {
            UUID uuid = player.getUniqueId();
            plugin.getPlayerDataManager().getCoins(uuid).thenAccept(coins ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) p.sendMessage(plugin.getMessagesManager().get("coins.show", "coins", com.lemonpvp.lemoncore.util.TextUtil.formatCoins(coins)));
                    }));
        }
        return true;
    }
}
