package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GRankCommand implements CommandExecutor {

    private final LemonCore plugin;
    private final LuckPerms lp;

    public GRankCommand(LemonCore plugin, LuckPerms lp) {
        this.plugin = plugin;
        this.lp = lp;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.rank")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage",
                    "usage", "/grank add|set|remove|show <player> [group]"));
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];
        String group = args.length >= 3 ? args[2] : null;

        plugin.getPlayerDataManager().findUUIDByName(targetName).thenAccept(uuid -> {
            if (uuid == null) {
                // Try online player
                var onlineTarget = Bukkit.getPlayer(targetName);
                if (onlineTarget != null) {
                    handleAction(sender, onlineTarget.getUniqueId(), targetName, action, group);
                } else {
                    sender.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", targetName));
                }
                return;
            }
            handleAction(sender, uuid, targetName, action, group);
        });
        return true;
    }

    private void handleAction(CommandSender sender, UUID uuid, String name, String action, String group) {
        lp.getUserManager().loadUser(uuid).thenAccept(user -> {
            switch (action) {
                case "add" -> {
                    if (group == null) { sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/grank add <player> <group>")); return; }
                    user.data().add(InheritanceNode.builder(group).build());
                    lp.getUserManager().saveUser(user);
                    sender.sendMessage(plugin.getMessagesManager().get("rank.add", "player", name, "rank", group));
                }
                case "set" -> {
                    if (group == null) { sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/grank set <player> <group>")); return; }
                    // Remove all inheritance nodes first
                    user.data().clear(n -> n.getType() == net.luckperms.api.node.NodeType.INHERITANCE);
                    user.data().add(InheritanceNode.builder(group).build());
                    lp.getUserManager().saveUser(user);
                    sender.sendMessage(plugin.getMessagesManager().get("rank.set", "player", name, "rank", group));
                }
                case "remove" -> {
                    if (group == null) { sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/grank remove <player> <group>")); return; }
                    user.data().remove(InheritanceNode.builder(group).build());
                    lp.getUserManager().saveUser(user);
                    sender.sendMessage(plugin.getMessagesManager().get("rank.remove", "player", name, "rank", group));
                }
                case "show" -> {
                    String primaryGroup = user.getPrimaryGroup();
                    sender.sendMessage(plugin.getMessagesManager().get("rank.show-other", "player", name, "rank", primaryGroup));
                }
                default -> sender.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/grank add|set|remove|show <player> [group]"));
            }
        });
    }
}
