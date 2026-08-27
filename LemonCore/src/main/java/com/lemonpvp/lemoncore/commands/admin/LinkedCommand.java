package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.DiscordLinkManager.DiscordLink;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class LinkedCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonCore plugin;

    public LinkedCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show own account
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Usage: /linked <player>");
                return true;
            }
            if (!player.hasPermission("lemoncore.use.linked")) {
                player.sendMessage(MM.deserialize("<red>You don't have permission.</red>"));
                return true;
            }
            showLink(sender, player.getUniqueId(), player.getName());
            return true;
        }

        // Show another player's account — requires admin permission
        if (!sender.hasPermission("lemoncore.admin.linked")) {
            sender.sendMessage(MM.deserialize("<red>You don't have permission.</red>"));
            return true;
        }

        String targetName = args[0];
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            showLink(sender, online.getUniqueId(), online.getName());
            return true;
        }

        // Offline player — look up UUID from lc_players
        plugin.getDatabaseManager().queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT uuid FROM lc_players WHERE LOWER(username)=LOWER(?) LIMIT 1")) {
                ps.setString(1, targetName);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
                return UUID.fromString(rs.getString("uuid"));
            } catch (Exception e) {
                plugin.getLogger().severe("UUID lookup error: " + e.getMessage());
                return null;
            }
        }).thenAccept(uuid -> {
            if (uuid == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(MM.deserialize("<red>Player not found.</red>")));
                return;
            }
            showLink(sender, uuid, targetName);
        });
        return true;
    }

    private void showLink(CommandSender sender, UUID uuid, String playerName) {
        plugin.getDiscordLinkManager().getLinkedAccount(uuid).thenAccept(link -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (link == null) {
                    sender.sendMessage(MM.deserialize(
                            "<gray>" + playerName + " has no Discord account linked.</gray>"));
                } else {
                    sender.sendMessage(MM.deserialize(
                            "<gray>Linked Discord: <white>" + link.discordUsername
                            + "</white> (<aqua>" + link.discordId + "</aqua>)</gray>"));
                }
            });
        });
    }
}
