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

        // No WorldEdit/FAWE needed any more — the builder writes through the vanilla API,
        // so this works on servers where FAWE cannot load.

        player.sendMessage("§eBuilding the spawn island at 0, 64, 0.");
        player.sendMessage("§7The server will not respond while it builds — this is a one-time "
                + "setup operation and takes a few seconds.");
        plugin.getLogger().info("[AowBuildSpawnCommand] " + player.getName()
                + " started spawn build in world: " + player.getWorld().getName());

        org.bukkit.World world = player.getWorld();

        // Bukkit block writes must happen on the main thread — unlike the old FAWE path, this
        // cannot be moved off it. Run it on the next tick so the messages above are delivered
        // first, then let the admin know how it went.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            boolean autoSave = world.isAutoSave();
            long started = System.nanoTime();
            try {
                // Stop the world from autosaving mid-build; it would double the work.
                world.setAutoSave(false);
                new SpawnBuilder(plugin, world).build();

                long ms = (System.nanoTime() - started) / 1_000_000L;
                plugin.getLogger().info("[AowBuildSpawnCommand] Spawn build completed in " + ms + "ms.");
                if (player.isOnline()) {
                    player.sendMessage("§aSpawn island built successfully §7(" + ms + "ms)§a. "
                            + "See the console for the block report.");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("[AowBuildSpawnCommand] Build failed: " + e);
                e.printStackTrace();
                String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                if (player.isOnline()) {
                    player.sendMessage("§cSpawn build failed: §e" + errMsg
                            + "\n§cCheck the server console for details.");
                }
            } finally {
                world.setAutoSave(autoSave);
            }
        });

        return true;
    }
}
