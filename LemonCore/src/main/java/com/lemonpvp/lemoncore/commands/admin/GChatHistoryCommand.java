package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.ChatLogManager;
import com.lemonpvp.lemoncore.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * {@code /gchathistory <player> [count]} — shows the recent chat messages of a
 * player (default 30, max 100), oldest first, for staff review.
 */
public class GChatHistoryCommand implements CommandExecutor {

    private static final String PERMISSION = "lemoncore.admin.history";
    private static final int DEFAULT_COUNT = 30;
    private static final int MAX_COUNT = 100;
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault());

    private final LemonCore plugin;

    public GChatHistoryCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage",
                    "usage", "/gchathistory <player> [count]"));
            return true;
        }

        String targetName = args[0];
        int count = DEFAULT_COUNT;
        if (args.length >= 2) {
            try {
                count = Math.max(1, Math.min(MAX_COUNT, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
                // keep default on non-numeric count
            }
        }
        final int limit = count;

        Player online = Bukkit.getPlayer(targetName);
        if (online != null) {
            show(sender, online.getUniqueId(), online.getName(), limit);
            return true;
        }
        plugin.getPlayerDataManager().findUUIDByName(targetName).thenAccept(uuid -> {
            if (uuid == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.getMessagesManager().get("player-not-found",
                                "player", targetName)));
                return;
            }
            show(sender, uuid, targetName, limit);
        });
        return true;
    }

    private void show(CommandSender sender, UUID uuid, String name, int limit) {
        ChatLogManager log = plugin.getChatLogManager();
        if (log == null) {
            sender.sendMessage(TextUtil.parse("<red>Chat logging is unavailable."));
            return;
        }
        log.getRecent(uuid, limit).thenAccept(entries -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (entries == null || entries.isEmpty()) {
                sender.sendMessage(TextUtil.parse("<gray>No chat history found for <yellow>"
                        + TextUtil.escapeTags(name) + "<gray>."));
                return;
            }
            sender.sendMessage(TextUtil.parse("<gold><bold>Chat history</bold> <gray>for <yellow>"
                    + TextUtil.escapeTags(name) + " <gray>(last " + entries.size() + "):"));
            // getRecent returns newest-first; show oldest-first so it reads chronologically.
            for (int i = entries.size() - 1; i >= 0; i--) {
                ChatLogManager.Entry e = entries.get(i);
                String time = TIME_FMT.format(Instant.ofEpochMilli(e.ts()));
                String server = e.server() != null ? e.server() : "?";
                sender.sendMessage(TextUtil.parse("<dark_gray>[<gray>" + time + "<dark_gray>] "
                        + "<dark_gray>(<aqua>" + TextUtil.escapeTags(server) + "<dark_gray>) "
                        + "<white>" + TextUtil.escapeTags(e.message())));
            }
        }));
    }
}
