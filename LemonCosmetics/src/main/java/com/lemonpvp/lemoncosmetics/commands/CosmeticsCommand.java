package com.lemonpvp.lemoncosmetics.commands;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.gui.CosmeticsMainGUI;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CosmeticsCommand implements CommandExecutor {

    private final LemonCosmetics plugin;

    public CosmeticsCommand(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("lemoncosmetics.use")) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.no-permission",
                            "<red>You don't have permission to use this command.")));
            return true;
        }

        PlayerCosmetics cosmetics = plugin.getCosmeticsManager()
                .getPlayerCosmetics(player.getUniqueId());

        if (cosmetics == null) {
            // Not yet cached — load from DB, then open once loaded
            plugin.getCosmeticsManager().loadPlayer(player.getUniqueId())
                    .thenAccept(c -> Bukkit.getScheduler().runTask(plugin,
                            () -> new CosmeticsMainGUI(plugin, player).open()));
            return true;
        }

        new CosmeticsMainGUI(plugin, player).open();
        return true;
    }
}
