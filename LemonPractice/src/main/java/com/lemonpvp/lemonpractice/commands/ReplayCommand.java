package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /replay}:
 * <ul>
 *   <li>{@code /replay <name>} — watch a replay</li>
 *   <li>{@code /replay <name> add <days>} — extend a replay's retention</li>
 *   <li>{@code /replay stop} — stop watching</li>
 *   <li>{@code /replay list} — recent replays</li>
 *   <li>{@code /replay info <name>} — details</li>
 * </ul>
 */
public class ReplayCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String PREFIX =
            "<gradient:#fffb00:#00ff00><bold>Replay</bold></gradient> <dark_gray>»</dark_gray> ";

    private final LemonPractice plugin;

    public ReplayCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (!player.hasPermission("lemonpractice.replay")) {
            msg(player, "<red>You don't have permission.");
            return true;
        }
        if (args.length == 0) {
            new com.lemonpvp.lemonpractice.gui.ReplayBrowserGUI(plugin, player).open();
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list", "browse", "gui" -> new com.lemonpvp.lemonpractice.gui.ReplayBrowserGUI(plugin, player).open();
            case "stop" -> {
                plugin.getReplayManager().stopPlayback(player.getUniqueId());
                msg(player, "<gray>Stopped any active replay.");
            }
            case "play" -> {
                if (args.length < 2) { usage(player); return true; }
                plugin.getReplayManager().play(player, args[1]);
            }
            case "pause" -> {
                if (plugin.getReplayManager().togglePause(player.getUniqueId()))
                    msg(player, "<yellow>Toggled pause.");
                else msg(player, "<red>You're not watching a replay.");
            }
            case "restart" -> {
                if (plugin.getReplayManager().restart(player.getUniqueId()))
                    msg(player, "<green>Replay restarted.");
                else msg(player, "<red>You're not watching a replay.");
            }
            case "next", "prev" -> {
                boolean fwd = args[0].equalsIgnoreCase("next");
                if (plugin.getReplayManager().jumpBookmark(player.getUniqueId(), fwd))
                    msg(player, "<gold>★ <green>Jumped to " + (fwd ? "next" : "previous") + " highlight.");
                else msg(player, "<gray>No " + (fwd ? "more" : "earlier") + " highlights.");
            }
            case "speed" -> {
                if (args.length < 2) { msg(player, "<gray>Usage: /replay speed <0.25-8>"); return true; }
                double sp;
                try { sp = Double.parseDouble(args[1]); }
                catch (NumberFormatException e) { msg(player, "<red>Speed must be a number."); return true; }
                if (plugin.getReplayManager().setSpeed(player.getUniqueId(), sp))
                    msg(player, "<green>Speed set to <yellow>" + sp + "x<green>.");
                else msg(player, "<red>You're not watching a replay.");
            }
            case "follow" -> {
                if (args.length < 2) { msg(player, "<gray>Usage: /replay follow <1|2|off>"); return true; }
                int f = switch (args[1].toLowerCase()) { case "1" -> 1; case "2" -> 2; default -> 0; };
                if (plugin.getReplayManager().setFollow(player.getUniqueId(), f))
                    msg(player, f == 0 ? "<gray>Free camera." : "<green>Following player " + f + ".");
                else msg(player, "<red>You're not watching a replay.");
            }
            case "info" -> {
                if (args.length < 2) { usage(player); return true; }
                handleInfo(player, args[1]);
            }
            case "share" -> {
                if (args.length < 2) { msg(player, "<gray>Usage: /replay share <name> [player]"); return true; }
                handleShare(player, args[1], args.length >= 3 ? args[2] : null);
            }
            default -> {
                // /replay <name> [add <days>]
                String name = args[0];
                if (args.length >= 3 && args[1].equalsIgnoreCase("add")) {
                    handleAddDays(player, name, args[2]);
                } else if (args.length == 1) {
                    plugin.getReplayManager().play(player, name);
                } else {
                    usage(player);
                }
            }
        }
        return true;
    }

    private void handleAddDays(Player player, String name, String daysArg) {
        int days;
        try { days = Integer.parseInt(daysArg); }
        catch (NumberFormatException e) { msg(player, "<red>Days must be a number."); return; }
        if (days <= 0) { msg(player, "<red>Days must be positive."); return; }
        plugin.getDatabase().extendReplay(name, days).thenAccept(newExpiry ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(player.getUniqueId());
                    if (p == null) return;
                    if (newExpiry == null || newExpiry < 0) {
                        msg(p, "<red>Replay <yellow>" + name + " <red>not found.");
                    } else {
                        long daysLeft = Math.max(0, (newExpiry - System.currentTimeMillis()) / 86_400_000L);
                        msg(p, PREFIX + "<green>Added <yellow>" + days + " days <green>to <white>" + name
                                + "<green>. <gray>(" + daysLeft + " days left)");
                    }
                }));
    }

    private void handleShare(Player player, String name, String targetName) {
        plugin.getDatabase().getReplayMeta(name).thenAccept(meta ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(player.getUniqueId());
                    if (p == null) return;
                    if (meta == null) { msg(p, "<red>Replay <yellow>" + name + " <red>not found."); return; }
                    var card = MM.deserialize("<gradient:#fffb00:#00ff00><bold>Replay</bold></gradient> "
                            + "<dark_gray>»</dark_gray> <white>" + p.getName() + " <gray>shared "
                            + "<yellow>" + meta.player1() + " <gray>vs <yellow>" + meta.player2() + " "
                            + "<click:run_command:'/replay " + meta.name() + "'>"
                            + "<hover:show_text:'<green>Click to watch <gray>(" + meta.name() + ")'>"
                            + "<aqua>[▶ Watch]</aqua></hover></click>");
                    if (targetName != null) {
                        Player target = Bukkit.getPlayerExact(targetName);
                        if (target == null) { msg(p, "<red>Player <yellow>" + targetName + " <red>is not online."); return; }
                        target.sendMessage(card);
                        msg(p, "<green>Shared the replay with <yellow>" + target.getName() + "<green>.");
                    } else {
                        // No target: hand the clickable card to the sharer to click or pass on.
                        p.sendMessage(card);
                        msg(p, "<gray>Tip: <white>/replay share " + meta.name() + " <player> <gray>to send it to someone.");
                    }
                }));
    }

    private void handleInfo(Player player, String name) {
        plugin.getDatabase().getReplayMeta(name).thenAccept(meta ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(player.getUniqueId());
                    if (p == null) return;
                    if (meta == null) { msg(p, "<red>Replay <yellow>" + name + " <red>not found."); return; }
                    long daysLeft = Math.max(0, (meta.expiresAt() - System.currentTimeMillis()) / 86_400_000L);
                    msg(p, PREFIX + "<white>" + meta.name());
                    msg(p, "<gray>Players: <white>" + meta.player1() + " <gray>vs <white>" + meta.player2());
                    msg(p, "<gray>Mode: <white>" + meta.gamemode() + " <dark_gray>| <gray>Expires in <white>" + daysLeft + " days");
                    msg(p, "<yellow>/replay " + meta.name() + " <gray>to watch.");
                }));
    }

    private void handleList(Player player) {
        plugin.getDatabase().listReplays(15).thenAccept(list ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(player.getUniqueId());
                    if (p == null) return;
                    if (list == null || list.isEmpty()) { msg(p, "<gray>No replays available."); return; }
                    msg(p, PREFIX + "<white>Recent replays:");
                    for (var meta : list) {
                        msg(p, "<gray>• <click:run_command:'/replay " + meta.name() + "'>"
                                + "<hover:show_text:'<green>Click to watch'><yellow>" + meta.name()
                                + "</hover></click>");
                    }
                }));
    }

    private void usage(Player p) {
        msg(p, "<gray>/replay <dark_gray>— open the replay browser");
        msg(p, "<gray>/replay <name> <dark_gray>— watch  <dark_gray>| <gray>add <days> <dark_gray>— extend");
        msg(p, "<gray>Controls: <white>pause<gray>, <white>speed <x><gray>, <white>follow <1|2|off><gray>, <white>restart<gray>, <white>stop");
        msg(p, "<gray>/replay info <name> <dark_gray>| <gray>list");
    }

    private void msg(Player p, String mini) {
        p.sendMessage(MM.deserialize("<!italic>" + mini));
    }
}
