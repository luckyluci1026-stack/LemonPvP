package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * {@code /gwipe <player>} archives and resets a player's stats.
 * {@code /gwipe list <player>} lists that player's wipe snapshots with a
 * clickable restore for each — so an admin never has to remember a date.
 */
public class GWipeCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCore plugin;

    public GWipeCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.wipe")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/gwipe <player> | /gwipe list <player>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (args.length < 2) {
                sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/gwipe list <player>"));
                return true;
            }
            listWipes(sender, args[1]);
            return true;
        }

        String targetName = args[0];
        UUID senderUuid = sender instanceof Player p ? p.getUniqueId() : null;
        resolveUuid(targetName, uuid -> {
            if (uuid == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName)));
                return;
            }
            plugin.getStatsManager().wipeStats(uuid, targetName, senderUuid, sender.getName())
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.getMessagesManager().get("stats.wipe-success", "player", targetName))));
        });
        return true;
    }

    private void listWipes(CommandSender sender, String targetName) {
        resolveUuid(targetName, uuid -> {
            if (uuid == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName)));
                return;
            }
            plugin.getStatsManager().listWipes(uuid).thenAccept(wipes -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (wipes.isEmpty()) {
                    sender.sendMessage(MM.deserialize("<!italic><gray>No wipe snapshots for <white>" + targetName + "<gray>."));
                    return;
                }
                sender.sendMessage(MM.deserialize("<!italic><gradient:#fffb00:#00ff00><bold>Wipes</bold></gradient> "
                        + "<dark_gray>» <white>" + targetName + " <gray>(" + wipes.size() + ")"));
                for (var w : wipes) {
                    sender.sendMessage(MM.deserialize("<!italic><dark_gray>#<white>" + w.id
                            + " <dark_gray>| <gray>" + w.getFormattedDate()
                            + " <dark_gray>| <gray>K/D <white>" + w.kills + "/" + w.deaths
                            + " <dark_gray>| <gray>Coins <white>" + w.coins
                            + "  <click:run_command:'/gunwipe " + targetName + " " + w.id + "'>"
                            + "<hover:show_text:'<green>Restore this snapshot'>"
                            + "<dark_gray>[<green>↺ Restore</green><dark_gray>]</hover></click>"));
                }
                sender.sendMessage(MM.deserialize("<!italic><dark_gray>Or <white>/gunwipe " + targetName
                        + " latest <dark_gray>for the most recent."));
            }));
        });
    }

    private void resolveUuid(String name, Consumer<UUID> callback) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) { callback.accept(online.getUniqueId()); return; }
        plugin.getPlayerDataManager().findUUIDByName(name).thenAccept(callback);
    }
}
