package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@code /goffline} — take the server offline (maintenance) with a configurable
 * reason, and manage who may still join.
 *
 * <ul>
 *   <li>{@code /goffline <reason>} — enable maintenance using a reason key from
 *       config (maintenance.reasons.&lt;key&gt;).</li>
 *   <li>{@code /goffline off} — disable maintenance.</li>
 *   <li>{@code /goffline add <player>} — allow a player to join while offline.</li>
 *   <li>{@code /goffline remove <player>} — revoke that allowance.</li>
 *   <li>{@code /goffline list} — show status, reasons and the allowlist.</li>
 * </ul>
 */
public class GOfflineCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX =
            "<gradient:#fffb00:#00ff00><bold>LemonPvP</bold></gradient> <dark_gray>»</dark_gray> ";

    private final LemonCore plugin;

    public GOfflineCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.maintenance")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        var mm = plugin.getMaintenanceManager();

        if (args.length == 0) { sendUsage(sender); return true; }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "off", "on" -> {
                if (sub.equals("off")) {
                    disable(sender);
                } else {
                    enable(sender, mm.getReasonKey());
                }
            }
            case "add" -> {
                if (args.length < 2) { msg(sender, "<red>Usage: /goffline add <player>"); return true; }
                modifyAllow(sender, args[1], true);
            }
            case "remove" -> {
                if (args.length < 2) { msg(sender, "<red>Usage: /goffline remove <player>"); return true; }
                modifyAllow(sender, args[1], false);
            }
            case "list", "status" -> sendStatus(sender);
            default -> {
                // Treat the argument as a reason key → enable maintenance with it.
                if (mm.hasReason(sub)) {
                    enable(sender, sub);
                } else {
                    msg(sender, "<red>Unknown reason '<white>" + sub + "<red>'. Available: <gray>"
                            + String.join(", ", mm.reasonKeys()));
                }
            }
        }
        return true;
    }

    private void enable(CommandSender sender, String reasonKey) {
        var mm = plugin.getMaintenanceManager();
        mm.setReasonKey(reasonKey);

        // Auto-allow online staff so they don't lock themselves out.
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("lemoncore.admin.maintenance")) {
                mm.addToWhitelist(p.getUniqueId(), p.getName());
            }
        }
        mm.setEnabled(true);

        Component kickScreen = TextUtil.parse(mm.resolveReasonMessage());
        // Kick everyone not allowed (routes through KickListener → proxy disconnect).
        for (Player p : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            if (!mm.isWhitelisted(p.getUniqueId()) && !p.hasPermission("lemoncore.admin.maintenance")) {
                p.kick(kickScreen);
            }
        }
        msg(sender, PREFIX + "<yellow>Server is now <red>OFFLINE <yellow>(reason: <white>" + reasonKey + "<yellow>).");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(sender)) p.sendMessage(TextUtil.parse(PREFIX + "<yellow>Maintenance mode enabled."));
        }
    }

    private void disable(CommandSender sender) {
        plugin.getMaintenanceManager().setEnabled(false);
        msg(sender, PREFIX + "<green>Server is back <white>ONLINE<green>.");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(sender)) p.sendMessage(TextUtil.parse(PREFIX + "<green>Maintenance mode disabled."));
        }
    }

    private void modifyAllow(CommandSender sender, String name, boolean add) {
        var mm = plugin.getMaintenanceManager();
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            applyAllow(sender, online.getUniqueId(), online.getName(), add);
            return;
        }
        plugin.getPlayerDataManager().findUUIDByName(name).thenAccept(uuid ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (uuid == null) { msg(sender, "<red>Player '<white>" + name + "<red>' not found."); return; }
                    applyAllow(sender, uuid, name, add);
                }));
    }

    private void applyAllow(CommandSender sender, UUID uuid, String name, boolean add) {
        var mm = plugin.getMaintenanceManager();
        if (add) {
            if (mm.isWhitelisted(uuid)) { msg(sender, "<yellow>" + name + " can already join while offline."); return; }
            mm.addToWhitelist(uuid, name);
            msg(sender, PREFIX + "<green>" + name + " <gray>can now join while offline.");
        } else {
            if (!mm.isWhitelisted(uuid)) { msg(sender, "<yellow>" + name + " is not on the allowlist."); return; }
            mm.toggleWhitelist(uuid, name); // currently whitelisted → toggle removes
            msg(sender, PREFIX + "<red>" + name + " <gray>can no longer join while offline.");
        }
    }

    private void sendStatus(CommandSender sender) {
        var mm = plugin.getMaintenanceManager();
        msg(sender, PREFIX + (mm.isEnabled()
                ? "<red>OFFLINE <gray>(reason: <white>" + mm.getReasonKey() + "<gray>)"
                : "<green>ONLINE"));
        msg(sender, "<gray>Available reasons: <white>" + String.join(", ", mm.reasonKeys()));
        msg(sender, "<gray>Use <white>/goffline add <player> <gray>to allow someone while offline.");
    }

    private void sendUsage(CommandSender sender) {
        msg(sender, "<gray>/goffline <white><reason> <gray>| <white>off <gray>| <white>add <player> <gray>| "
                + "<white>remove <player> <gray>| <white>list");
        msg(sender, "<gray>Reasons: <white>" + String.join(", ", plugin.getMaintenanceManager().reasonKeys()));
    }

    private void msg(CommandSender sender, String mini) {
        sender.sendMessage(TextUtil.parse("<!italic>" + mini));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.maintenance")) return List.of();
        if (args.length == 1) {
            List<String> out = new ArrayList<>(plugin.getMaintenanceManager().reasonKeys());
            out.add("off"); out.add("add"); out.add("remove"); out.add("list");
            String p = args[0].toLowerCase();
            out.removeIf(s -> !s.toLowerCase().startsWith(p));
            return out;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        return List.of();
    }
}
