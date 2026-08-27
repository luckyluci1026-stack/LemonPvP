package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.config.ConfigManager.OffendReason;
import com.lemonpvp.lemoncore.gui.OffendGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * {@code /offend <player> [reason-id]} — temporary ban with a selectable
 * offense category (each with its own duration, see
 * {@code ConfigManager#getOffendReasons()}):
 * <ul>
 *   <li>No reason-id (player sender): opens the {@link OffendGUI} reason picker.</li>
 *   <li>With reason-id (or console): bans directly with that category.</li>
 * </ul>
 */
public class OffendCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCore plugin;

    public OffendCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.offend")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage",
                    "usage", "/offend <player> [reason]"));
            return true;
        }

        String targetName = args[0];

        // Direct mode: reason id supplied (required for console).
        if (args.length >= 2) {
            OffendReason reason = findReason(args[1]);
            if (reason == null) {
                sender.sendMessage(MM.deserialize("<red>Unknown reason <yellow>" + args[1]
                        + "<red>. Valid: <white>" + String.join(", ", reasonIds())));
                return true;
            }
            resolveTarget(sender, targetName, (uuid, name) -> executeBan(sender, uuid, name, reason));
            return true;
        }

        if (!(sender instanceof Player staff)) {
            sender.sendMessage(MM.deserialize("<red>Console must supply a reason: /offend <player> <"
                    + String.join("|", reasonIds()) + ">"));
            return true;
        }

        // GUI mode: resolve the target, then open the reason picker.
        resolveTarget(sender, targetName, (uuid, name) ->
                new OffendGUI(plugin, staff, uuid, name).open());
        return true;
    }

    /** Resolves online first, then by stored name (async); callback runs on the main thread. */
    private void resolveTarget(CommandSender sender, String targetName, BiConsumer<UUID, String> onFound) {
        Player online = Bukkit.getPlayer(targetName);
        if (online != null) {
            onFound.accept(online.getUniqueId(), online.getName());
            return;
        }
        plugin.getPlayerDataManager().findUUIDByName(targetName).thenAccept(uuid ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (uuid == null) {
                        sender.sendMessage(plugin.getMessagesManager().get("player-not-found",
                                "player", targetName));
                        return;
                    }
                    onFound.accept(uuid, targetName);
                }));
    }

    private void executeBan(CommandSender sender, UUID targetUuid, String targetName, OffendReason reason) {
        UUID senderUuid = sender instanceof Player p ? p.getUniqueId() : null;
        plugin.getBanManager()
                .banPlayer(targetUuid, targetName, reason.display(), senderUuid, sender.getName(),
                        reason.durationSeconds())
                .thenAccept(ban -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (ban == null) {
                        sender.sendMessage(MM.deserialize("<red>Ban failed for <yellow>" + targetName
                                + " <red>— a database error occurred."));
                        return;
                    }
                    sender.sendMessage(plugin.getMessagesManager().get("ban.success",
                            "player", targetName, "reason", reason.display()));
                    Player t = Bukkit.getPlayer(targetUuid);
                    if (t != null) plugin.getListenerManager().performBanKick(t, ban);
                }));
    }

    private OffendReason findReason(String id) {
        String lower = id.toLowerCase(Locale.ROOT);
        for (OffendReason r : plugin.getConfigManager().getOffendReasons()) {
            if (r.id().equals(lower)) return r;
        }
        return null;
    }

    private List<String> reasonIds() {
        List<String> ids = new ArrayList<>();
        for (OffendReason r : plugin.getConfigManager().getOffendReasons()) ids.add(r.id());
        return ids;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("lemoncore.admin.offend")) return out;
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(partial)) out.add(p.getName());
            }
        } else if (args.length == 2) {
            String partial = args[1].toLowerCase(Locale.ROOT);
            for (String id : reasonIds()) {
                if (id.startsWith(partial)) out.add(id);
            }
        }
        return out;
    }
}
