package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.builder.SpawnBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AowBuildSpawnCommand implements CommandExecutor {

    private final LemonPractice plugin;

    public AowBuildSpawnCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be run by a player.");
            return true;
        }

        if (!player.hasPermission("lemonpractice.admin.buildspawn")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        player.sendMessage("§eBuilding spawn island... this may take a few seconds.");

        UUID uuid = player.getUniqueId();
        org.bukkit.World world = player.getWorld();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                new SpawnBuilder(plugin, world).build();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player p = org.bukkit.Bukkit.getPlayer(uuid);
                    if (p != null) p.sendMessage("§aSpawn island built at 0, 64, 0.");
                });
            } catch (Exception e) {
                plugin.getLogger().severe("[AowBuildSpawnCommand] SpawnBuilder failed: " + e.getMessage());
                e.printStackTrace();
                final String errMsg = e.getMessage();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player p = org.bukkit.Bukkit.getPlayer(uuid);
                    if (p != null) p.sendMessage("§cSpawn build failed: " + errMsg);
                });
            }
        });

        return true;
    }
}
