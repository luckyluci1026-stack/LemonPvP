package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.gui.ReplayBrowserGUI;
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

/**
 * {@code /greplay} — admin replay management. Lets staff view and manage EVERY
 * player's replays (the regular {@code /replay} browser already lists all
 * replays, but this command is gated behind the admin permission and adds
 * delete / extend / purge management):
 * <ul>
 *   <li>{@code /greplay} — open the admin replay browser (right-click a head to delete)</li>
 *   <li>{@code /greplay <name>} — watch a replay</li>
 *   <li>{@code /greplay info <name>} — details</li>
 *   <li>{@code /greplay delete <name>} — delete a replay</li>
 *   <li>{@code /greplay extend <name> <days>} — extend retention</li>
 *   <li>{@code /greplay purge} — delete all expired replays</li>
 * </ul>
 */
public class GReplayCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of(
            "list", "info", "delete", "extend", "purge", "stop");

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String PREFIX =
            "<gradient:#ff5555:#ffaa00><bold>Admin Replay</bold></gradient> <dark_gray>»</dark_gray> ";

    private final LemonPractice plugin;

    public GReplayCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lemonpractice.admin.replay") || args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String sub : SUBS) if (sub.startsWith(prefix)) out.add(sub);
        return out;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemonpractice.admin.replay")) {
            reply(sender, "<red>You don't have permission.");
            return true;
        }

        if (args.length == 0) {
            openBrowser(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list", "browse", "gui" -> openBrowser(sender);
            case "purge" -> plugin.getDatabase().deleteExpiredReplays().thenAccept(n ->
                    Bukkit.getScheduler().runTask(plugin, () ->
                            reply(sender, "<green>Purged <yellow>" + n + " <green>expired replay(s).")));
            case "delete", "remove" -> {
                if (args.length < 2) { reply(sender, "<gray>Usage: /greplay delete <name>"); return true; }
                String name = args[1];
                plugin.getDatabase().deleteReplay(name).thenAccept(deleted ->
                        Bukkit.getScheduler().runTask(plugin, () -> reply(sender,
                                Boolean.TRUE.equals(deleted)
                                        ? "<green>Deleted replay <yellow>" + name
                                        : "<red>Replay <yellow>" + name + " <red>not found.")));
            }
            case "extend", "add" -> {
                if (args.length < 3) { reply(sender, "<gray>Usage: /greplay extend <name> <days>"); return true; }
                int days;
                try { days = Integer.parseInt(args[2]); }
                catch (NumberFormatException e) { reply(sender, "<red>Days must be a number."); return true; }
                if (days <= 0) { reply(sender, "<red>Days must be positive."); return true; }
                String name = args[1];
                plugin.getDatabase().extendReplay(name, days).thenAccept(newExpiry ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (newExpiry == null || newExpiry < 0) {
                                reply(sender, "<red>Replay <yellow>" + name + " <red>not found.");
                            } else {
                                long daysLeft = Math.max(0, (newExpiry - System.currentTimeMillis()) / 86_400_000L);
                                reply(sender, "<green>Extended <yellow>" + name + " <green>by <yellow>"
                                        + days + " days <gray>(" + daysLeft + " days left).");
                            }
                        }));
            }
            case "info" -> {
                if (args.length < 2) { reply(sender, "<gray>Usage: /greplay info <name>"); return true; }
                String name = args[1];
                plugin.getDatabase().getReplayMeta(name).thenAccept(meta ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (meta == null) { reply(sender, "<red>Replay <yellow>" + name + " <red>not found."); return; }
                            long daysLeft = Math.max(0, (meta.expiresAt() - System.currentTimeMillis()) / 86_400_000L);
                            reply(sender, "<white>" + meta.name());
                            plain(sender, "<gray>Players: <white>" + meta.player1() + " <gray>vs <white>" + meta.player2());
                            plain(sender, "<gray>Mode: <white>" + meta.gamemode()
                                    + " <dark_gray>| <gray>Views: <white>" + meta.views()
                                    + " <dark_gray>| <gray>Expires in <white>" + daysLeft + " days");
                            plain(sender, "<yellow>/greplay " + meta.name() + " <gray>to watch.");
                        }));
            }
            case "stop" -> {
                if (sender instanceof Player player) {
                    plugin.getReplayManager().stopPlayback(player.getUniqueId());
                    reply(sender, "<gray>Stopped any active replay.");
                } else {
                    reply(sender, "<red>Only players can watch replays.");
                }
            }
            default -> {
                // /greplay <name> — watch it
                if (sender instanceof Player player) {
                    plugin.getReplayManager().play(player, args[0]);
                } else {
                    reply(sender, "<gray>/greplay | <name> | info <name> | delete <name> | extend <name> <days> | purge");
                }
            }
        }
        return true;
    }

    private void openBrowser(CommandSender sender) {
        if (sender instanceof Player player) {
            new ReplayBrowserGUI(plugin, player, true).open();
        } else {
            reply(sender, "<gray>Console: use /greplay list is in-game only — try info/delete/extend/purge.");
        }
    }

    private void reply(CommandSender s, String mini) {
        s.sendMessage(MM.deserialize("<!italic>" + PREFIX + mini));
    }

    private void plain(CommandSender s, String mini) {
        s.sendMessage(MM.deserialize("<!italic>" + mini));
    }
}
