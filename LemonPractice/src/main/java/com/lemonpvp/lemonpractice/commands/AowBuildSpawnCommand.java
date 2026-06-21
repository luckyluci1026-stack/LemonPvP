package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.builder.SpawnBuilder;
import org.bukkit.Bukkit;
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
            player.sendMessage("§cNo permission: lemonpractice.admin.buildspawn");
            return true;
        }

        // Must be on the LOBBY server (spawn belongs to the lobby world)
        if (!plugin.getServerType().equals("LOBBY")) {
            player.sendMessage("§cThis command can only be run on the LOBBY server. "
                    + "Current server type: §e" + plugin.getServerType());
            return true;
        }

        // Verify FAWE / WorldEdit is available
        if (Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") == null
                && Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            player.sendMessage("§cFastAsyncWorldEdit (or WorldEdit) is not installed on this server. "
                    + "The spawn builder requires it.");
            return true;
        }

        player.sendMessage("§eBuilding spawn island at 0, 64, 0 — this may take a few seconds...");
        plugin.getLogger().info("[AowBuildSpawnCommand] " + player.getName()
                + " started spawn build in world: " + player.getWorld().getName());

        UUID uuid = player.getUniqueId();
        org.bukkit.World world = player.getWorld();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                new SpawnBuilder(plugin, world).build();
                plugin.getLogger().info("[AowBuildSpawnCommand] Spawn build completed successfully.");
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.sendMessage("§aSpawn island built successfully!");
                });
            } catch (Exception e) {
                plugin.getLogger().severe("[AowBuildSpawnCommand] Build failed: " + e);
                e.printStackTrace();
                String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.sendMessage("§cSpawn build failed: §e" + errMsg
                            + "\n§cCheck the server console for details.");
                });
            }
        });

        return true;
    }
}
