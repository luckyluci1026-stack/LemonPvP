package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class FriendCommand implements CommandExecutor {

    private final LemonCore plugin;

    public FriendCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("lemoncore.use.friend")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        // No args, or list/gui → open the friends overview GUI.
        if (args.length == 0
                || args[0].equalsIgnoreCase("list")
                || args[0].equalsIgnoreCase("gui")) {
            new com.lemonpvp.lemoncore.gui.FriendsGUI(plugin, player).open();
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/friend add|remove <player> | /friend list"));
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];

        if (targetName.equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getMessagesManager().get("friend.self"));
            return true;
        }

        resolveUuid(targetName, targetUuid -> {
            if (targetUuid == null) {
                player.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName));
                return;
            }

            PlayerData data = plugin.getPlayerDataManager().getCached(player.getUniqueId());
            switch (action) {
                case "add" -> {
                    if (data != null && data.isFriend(targetUuid)) {
                        player.sendMessage(plugin.getMessagesManager().get("friend.already-friends", "player", targetName));
                        return;
                    }
                    plugin.getDatabaseManager().executeAsync(conn -> {
                        try {
                            String u1 = player.getUniqueId().toString();
                            String u2 = targetUuid.toString();
                            try (var ps = conn.prepareStatement(
                                    "INSERT IGNORE INTO lc_friends (uuid1, uuid2) VALUES (?,?)")) {
                                ps.setString(1, u1); ps.setString(2, u2);
                                ps.executeUpdate();
                            }
                            if (data != null) data.addFriend(targetUuid);
                            PlayerData targetData = plugin.getPlayerDataManager().getCached(targetUuid);
                            if (targetData != null) targetData.addFriend(player.getUniqueId());
                        } catch (Exception e) { plugin.getLogger().severe("Friend add error: " + e.getMessage()); }
                    });
                    player.sendMessage(plugin.getMessagesManager().get("friend.add-success", "player", targetName));
                }
                case "remove" -> {
                    if (data != null && !data.isFriend(targetUuid)) {
                        player.sendMessage(plugin.getMessagesManager().get("friend.not-friends", "player", targetName));
                        return;
                    }
                    plugin.getDatabaseManager().executeAsync(conn -> {
                        try {
                            String u1 = player.getUniqueId().toString();
                            String u2 = targetUuid.toString();
                            try (var ps = conn.prepareStatement(
                                    "DELETE FROM lc_friends WHERE (uuid1=? AND uuid2=?) OR (uuid1=? AND uuid2=?)")) {
                                ps.setString(1, u1); ps.setString(2, u2);
                                ps.setString(3, u2); ps.setString(4, u1);
                                ps.executeUpdate();
                            }
                            if (data != null) data.removeFriend(targetUuid);
                            PlayerData targetData = plugin.getPlayerDataManager().getCached(targetUuid);
                            if (targetData != null) targetData.removeFriend(player.getUniqueId());
                        } catch (Exception e) { plugin.getLogger().severe("Friend remove error: " + e.getMessage()); }
                    });
                    player.sendMessage(plugin.getMessagesManager().get("friend.remove-success", "player", targetName));
                }
                default -> player.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/friend add|remove <player>"));
            }
        });
        return true;
    }

    private void resolveUuid(String name, java.util.function.Consumer<UUID> callback) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) { callback.accept(online.getUniqueId()); return; }
        plugin.getPlayerDataManager().findUUIDByName(name).thenAccept(uuid ->
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(uuid)));
    }
}
