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

import java.util.UUID;

public class CosmeticsCommand implements CommandExecutor {

    private final LemonCosmetics plugin;

    public CosmeticsCommand(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Admin reload — usable from console too, and by players/staff.
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("lemoncosmetics.admin")) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>You don't have permission."));
                return true;
            }
            plugin.reloadConfig();
            plugin.reloadParticleDensity();
            plugin.getExplosionParticleManager().reload();
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>LemonCosmetics reloaded <gray>(density "
                    + plugin.getConfig().getDouble("cosmetics.particle-density", 1.0)
                    + ", " + plugin.getExplosionParticleManager().getAll().size() + " explosion presets)."));
            return true;
        }

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
            UUID uuid = player.getUniqueId();
            plugin.getCosmeticsManager().loadPlayer(uuid)
                    .thenAccept(c -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) new CosmeticsMainGUI(plugin, p).open();
                    }));
            return true;
        }

        new CosmeticsMainGUI(plugin, player).open();
        return true;
    }
}
