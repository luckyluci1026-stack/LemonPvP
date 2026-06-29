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
        if (args.length == 0) { usage(player); return true; }

        switch (args[0].toLowerCase()) {
            case "list" -> handleList(player);
            case "stop" -> {
                plugin.getReplayManager().stopPlayback(player.getUniqueId());
                msg(player, "<gray>Stopped any active replay.");
            }
            case "play" -> {
                if (args.length < 2) { usage(player); return true; }
                plugin.getReplayManager().play(player, args[1]);
            }
            case "info" -> {
                if (args.length < 2) { usage(player); return true; }
                handleInfo(player, args[1]);
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
        msg(p, "<gray>/replay <name> <dark_gray>— watch");
        msg(p, "<gray>/replay <name> add <days> <dark_gray>— extend retention");
        msg(p, "<gray>/replay list <dark_gray>| <gray>info <name> <dark_gray>| <gray>stop");
    }

    private void msg(Player p, String mini) {
        p.sendMessage(MM.deserialize("<!italic>" + mini));
    }
}
