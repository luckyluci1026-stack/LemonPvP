package com.lemonpvp.lemonevents.commands;

import com.lemonpvp.lemonevents.LemonEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /host} — drives a simple hosted event:
 * <ul>
 *   <li>{@code /host start <name>} — enter kit-build mode (needs the host permission)</li>
 *   <li>{@code /host open} — finish the kit, broadcast and accept joins</li>
 *   <li>{@code /host join} — join the open event (anyone)</li>
 *   <li>{@code /host begin} — teleport everyone in and start the fight</li>
 *   <li>{@code /host cancel} — abort</li>
 * </ul>
 */
public class HostCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String HOST_PERM = "lemonevents.host";

    private final LemonEvents plugin;

    public HostCommand(LemonEvents plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }

        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "join" -> plugin.getHostedEventManager().join(player);
            case "start" -> {
                if (denyHost(player)) return true;
                if (args.length < 2) { msg(player, "<red>Usage: <white>/host start <name>"); return true; }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                plugin.getHostedEventManager().startHosting(player, name);
            }
            case "open" -> { if (!denyHost(player)) plugin.getHostedEventManager().openJoins(player); }
            case "begin", "start-fight" -> { if (!denyHost(player)) plugin.getHostedEventManager().begin(player); }
            case "mode" -> {
                if (denyHost(player)) return true;
                if (args.length < 2) { msg(player, "<red>Usage: <white>/host mode <ffa|hostbattle>"); return true; }
                String m = args[1].toLowerCase(Locale.ROOT);
                if (m.startsWith("host") || m.equals("battle") || m.equals("hb")) {
                    plugin.getHostedEventManager().setMode(player,
                            com.lemonpvp.lemonevents.model.HostedEvent.Mode.HOST_BATTLE);
                } else if (m.equals("ffa") || m.startsWith("free")) {
                    plugin.getHostedEventManager().setMode(player,
                            com.lemonpvp.lemonevents.model.HostedEvent.Mode.FFA);
                } else {
                    msg(player, "<red>Unknown mode. Use <white>ffa <red>or <white>hostbattle<red>.");
                }
            }
            case "cancel", "stop" -> plugin.getHostedEventManager().cancel(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private boolean denyHost(Player p) {
        if (!p.hasPermission(HOST_PERM)) {
            msg(p, "<red>You don't have permission to host events.");
            return true;
        }
        return false;
    }

    private void sendHelp(Player p) {
        msg(p, "<gradient:#fffb00:#00ff00><bold>Host an Event</bold></gradient>");
        if (p.hasPermission(HOST_PERM)) {
            msg(p, "<yellow>/host start <name> <gray>— build the event kit");
            msg(p, "<yellow>/host mode <ffa|hostbattle> <gray>— FFA or all-vs-host");
            msg(p, "<yellow>/host open <gray>— broadcast & open joins");
            msg(p, "<yellow>/host begin <gray>— teleport everyone & start");
            msg(p, "<yellow>/host cancel <gray>— abort");
        }
        msg(p, "<yellow>/host join <gray>— join the current event");
    }

    private void msg(Player p, String mini) { p.sendMessage(MM.deserialize("<!italic>" + mini)); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> subs = new ArrayList<>();
            subs.add("join");
            if (sender.hasPermission(HOST_PERM)) {
                subs.add("start"); subs.add("mode"); subs.add("open"); subs.add("begin"); subs.add("cancel");
            }
            List<String> out = new ArrayList<>();
            for (String s : subs) if (s.startsWith(prefix)) out.add(s);
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("mode") && sender.hasPermission(HOST_PERM)) {
            List<String> out = new ArrayList<>();
            for (String m : List.of("ffa", "hostbattle")) if (m.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(m);
            return out;
        }
        return List.of();
    }
}
