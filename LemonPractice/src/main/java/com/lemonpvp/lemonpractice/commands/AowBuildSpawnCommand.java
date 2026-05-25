package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.builder.SpawnBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        if (!player.hasPermission("lemonpractice.admin")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        player.sendMessage("§eBuilding spawn island... this may take a few seconds.");

        org.bukkit.World world = player.getWorld();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                new SpawnBuilder(plugin, world).build();
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        player.sendMessage("§aSpawn island built at 0, 64, 0."));
            } catch (Exception e) {
                plugin.getLogger().severe("[AowBuildSpawnCommand] SpawnBuilder failed: " + e.getMessage());
                e.printStackTrace();
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        player.sendMessage("§cSpawn build failed: " + e.getMessage()));
            }
        });

        return true;
    }
}
