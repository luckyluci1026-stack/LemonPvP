package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.StatsManager.WipeRecord;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * {@code /gunwipe <player> [latest|<id>]} restores a player's stats from a wipe
 * snapshot. Defaults to the most recent wipe; a specific snapshot id (from
 * {@code /gwipe list}) can be given instead. Restores stats, coins and
 * per-gamemode ELO, then consumes the snapshot. Replaces the old date lookup.
 */
public class GUnwipeCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GUnwipeCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.unwipe")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage",
                    "usage", "/gunwipe <player> [latest|<id>]"));
            return true;
        }

        String targetName = args[0];
        // Optional selector: "latest" (default) or a numeric snapshot id.
        Integer id = null;
        if (args.length >= 2 && !args[1].equalsIgnoreCase("latest")) {
            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getMessagesManager().get("invalid-usage",
                        "usage", "/gunwipe <player> [latest|<id>]"));
                return true;
            }
        }
        final Integer wipeId = id;

        resolveUuid(targetName, uuid -> {
            if (uuid == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName)));
                return;
            }
            CompletableFuture<WipeRecord> lookup = wipeId != null
                    ? plugin.getStatsManager().findWipeById(uuid, wipeId)
                    : plugin.getStatsManager().findLatestWipe(uuid);
            lookup.thenAccept(wipe -> {
                if (wipe == null) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.getMessagesManager().get("stats.no-wipe")));
                    return;
                }
                plugin.getStatsManager().restoreStats(uuid, wipe)
                        .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getMessagesManager().get("stats.unwipe-success",
                                    "player", targetName, "date", "#" + wipe.id + " (" + wipe.getFormattedDate() + ")"))))
                        .exceptionally(ex -> { plugin.getLogger().severe("[GUnwipe] restoreStats failed: " + ex.getMessage()); return null; });
            }).exceptionally(ex -> { plugin.getLogger().severe("[GUnwipe] wipe lookup failed: " + ex.getMessage()); return null; });
        });
        return true;
    }

    private void resolveUuid(String name, Consumer<UUID> callback) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) { callback.accept(online.getUniqueId()); return; }
        plugin.getPlayerDataManager().findUUIDByName(name).thenAccept(callback);
    }
}
