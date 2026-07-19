package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * {@code /ping [player]} — the universal latency check. Colour-codes the value
 * (green &lt; 60ms, yellow &lt; 120, orange &lt; 200, red beyond) like the big servers.
 */
public class PingCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCore plugin;

    public PingCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        boolean self;
        if (args.length >= 1) {
            if (!sender.hasPermission("lemoncore.ping.others")) {
                sender.sendMessage(MM.deserialize("<!italic><red>You can only check your own ping."));
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            self = false;
            if (target == null) {
                sender.sendMessage(plugin.getMessagesManager().get("player-not-found", "player", args[0]));
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
            self = true;
        } else {
            sender.sendMessage("Console must specify a player: /ping <player>");
            return true;
        }

        int ping = target.getPing();
        String color = ping < 60 ? "<green>" : ping < 120 ? "<yellow>" : ping < 200 ? "<gold>" : "<red>";
        String who = self ? "Your ping is" : "<white>" + target.getName() + "<gray>'s ping is";
        sender.sendMessage(MM.deserialize("<!italic><gray>" + who + " " + color + "<bold>" + ping + "ms"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !sender.hasPermission("lemoncore.ping.others")) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
