package com.lemonpvp.lemonbuild.commands;

import com.lemonpvp.lemonbuild.LemonBuild;
import com.lemonpvp.lemonbuild.builder.LobbySpawnBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AowBuildLobbyCommand implements CommandExecutor {

    private final LemonBuild plugin;

    public AowBuildLobbyCommand(LemonBuild plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl nutzen.");
            return true;
        }

        if (!player.hasPermission("lemonbuild.admin")) {
            player.sendMessage("§cKeine Berechtigung: lemonbuild.admin");
            return true;
        }

        // Safety confirm step: /aowbuildlobby confirm
        if (args.length == 0 || !args[0].equalsIgnoreCase("confirm")) {
            player.sendMessage("§e⚠ Dies überschreibt Blöcke an Weltmitte (0,64,0) — Radius 32!");
            player.sendMessage("§eBestätige mit: §f/aowbuildlobby confirm");
            return true;
        }

        // Check WorldEdit / FAWE available
        if (Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") == null
                && Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            player.sendMessage("§cFastAsyncWorldEdit (oder WorldEdit) ist nicht installiert!");
            return true;
        }

        player.sendMessage("§eBaue Lobby-Spawn-Insel bei 0, 64, 0 — bitte warten...");
        plugin.getLogger().info("[LemonBuild] " + player.getName()
                + " startet Lobby-Build in Welt: " + player.getWorld().getName());

        UUID uuid = player.getUniqueId();
        org.bukkit.World world = player.getWorld();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                new LobbySpawnBuilder(world).build();
                plugin.getLogger().info("[LemonBuild] Lobby-Build erfolgreich abgeschlossen.");
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.sendMessage("§aLobby-Spawn-Insel wurde erfolgreich gebaut!");
                });
            } catch (Exception e) {
                plugin.getLogger().severe("[LemonBuild] Build fehlgeschlagen: " + e);
                e.printStackTrace();
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.sendMessage("§cBuild fehlgeschlagen: §e" + msg
                            + "\n§cDetails im Server-Log.");
                });
            }
        });

        return true;
    }
}
