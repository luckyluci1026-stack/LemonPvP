package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class AOWMCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCore plugin;

    public AOWMCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.maintenance")) {
            sender.sendMessage(MM.deserialize("<red>You don't have permission to use this command.</red>"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on" -> handleOn(sender);
            case "off" -> handleOff(sender);
            case "whitelist" -> {
                if (args.length < 2) {
                    sender.sendMessage(MM.deserialize("<red>Usage: /aowm whitelist <player></red>"));
                    return true;
                }
                handleWhitelist(sender, args[1]);
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleOn(CommandSender sender) {
        if (plugin.getMaintenanceManager().isEnabled()) {
            sender.sendMessage(MM.deserialize("<yellow>Maintenance mode is already enabled.</yellow>"));
            return;
        }

        // Auto-whitelist all online admins so they can reconnect if they disconnect
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("lemoncore.admin.maintenance")) {
                plugin.getMaintenanceManager().addToWhitelist(p.getUniqueId(), p.getName());
            }
        }

        plugin.getMaintenanceManager().setEnabled(true);

        String discordLink = plugin.getConfig().getString("discord.invite", "https://discord.gg/eZWP9EGW7");
        Component kickMsg = MM.deserialize(
            "<bold><gradient:#fffb00:#00ff00>LemonPvP</gradient></bold>\n" +
            "<red>The server is currently under maintenance.</red>\n" +
            "<gray>Follow our Discord for updates:</gray>\n" +
            "<aqua>" + discordLink + "</aqua>"
        );

        // Kick all players not on the whitelist and without admin permission
        for (Player p : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            if (!plugin.getMaintenanceManager().isWhitelisted(p.getUniqueId())
                    && !p.hasPermission("lemoncore.admin.maintenance")) {
                p.kick(kickMsg);
            }
        }

        // Broadcast to remaining online players
        Component enableMsg = MM.deserialize(
            "<bold><gradient:#fffb00:#00ff00>LemonPvP</gradient></bold> " +
            "<yellow>Maintenance mode has been enabled.</yellow>"
        );
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(enableMsg);
        }
    }

    private void handleOff(CommandSender sender) {
        if (!plugin.getMaintenanceManager().isEnabled()) {
            sender.sendMessage(MM.deserialize("<yellow>Maintenance mode is already disabled.</yellow>"));
            return;
        }

        plugin.getMaintenanceManager().setEnabled(false);

        Component disableMsg = MM.deserialize(
            "<bold><gradient:#fffb00:#00ff00>LemonPvP</gradient></bold> " +
            "<green>Maintenance mode has been disabled.</green>"
        );
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(disableMsg);
        }
    }

    private void handleWhitelist(CommandSender sender, String playerName) {
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) {
            doToggle(sender, online.getUniqueId(), online.getName());
            return;
        }

        // Offline lookup
        plugin.getPlayerDataManager().findUUIDByName(playerName).thenAccept(uuid -> {
            if (uuid == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(MM.deserialize("<red>Player '" + playerName + "' not found.</red>"))
                );
                return;
            }
            doToggle(sender, uuid, playerName);
        });
    }

    private void doToggle(CommandSender sender, java.util.UUID uuid, String playerName) {
        plugin.getMaintenanceManager().toggleWhitelist(uuid, playerName).thenAccept(added -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (added) {
                    sender.sendMessage(MM.deserialize(
                        "<green>" + playerName + " added to maintenance whitelist.</green>"
                    ));
                } else {
                    sender.sendMessage(MM.deserialize(
                        "<red>" + playerName + " removed from maintenance whitelist.</red>"
                    ));
                }
            });
        });
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MM.deserialize(
            "<yellow>Usage: <white>/aowm <on|off|whitelist <player>></white></yellow>"
        ));
    }
}
