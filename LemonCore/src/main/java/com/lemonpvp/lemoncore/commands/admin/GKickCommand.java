package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.UUID;

public class GKickCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GKickCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.kick")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/gkick <player> <reason>"));
            return true;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(plugin.getMessagesManager().get("player-not-online", "player", targetName));
            return true;
        }

        UUID senderUuid = sender instanceof Player p ? p.getUniqueId() : null;

        // Save kick to DB
        plugin.getDatabaseManager().executeAsync(conn -> {
            try (var ps = conn.prepareStatement(
                    "INSERT INTO lc_kicks (uuid, username, reason, kicker_uuid, kicker_name) VALUES (?,?,?,?,?)")) {
                ps.setString(1, target.getUniqueId().toString());
                ps.setString(2, target.getName());
                ps.setString(3, reason);
                ps.setString(4, senderUuid != null ? senderUuid.toString() : null);
                ps.setString(5, sender.getName());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("Kick DB error: " + e.getMessage());
            }
        });

        target.kick(plugin.getMessagesManager().get("kick.message", "reason", reason));
        sender.sendMessage(plugin.getMessagesManager().get("kick.success", "player", targetName, "reason", reason));
        return true;
    }
}
