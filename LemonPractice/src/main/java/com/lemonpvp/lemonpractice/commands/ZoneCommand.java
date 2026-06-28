package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /zone} or {@code /zone join} — join the Zone Practice round.
 * {@code /zone leave} — leave the round and return to the lobby.
 */
public class ZoneCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;

    public ZoneCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        String sub = (args.length == 0) ? "join" : args[0].toLowerCase();

        switch (sub) {
            case "join" -> plugin.getZonePracticeManager().join(player);
            case "leave", "quit", "exit" -> plugin.getZonePracticeManager().leave(player);
            default -> sendUsage(player);
        }
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(MM.deserialize("<dark_gray><st>                        </st>"));
        player.sendMessage(MM.deserialize("<gradient:#ff5555:#ffaa00><bold>Zone Practice</bold></gradient>"));
        player.sendMessage(MM.deserialize("<yellow>/zone join <gray>— Join the shrinking-zone round"));
        player.sendMessage(MM.deserialize("<yellow>/zone leave <gray>— Leave and return to lobby"));
        player.sendMessage(MM.deserialize("<dark_gray><st>                        </st>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String sub : List.of("join", "leave")) {
                if (sub.startsWith(partial)) out.add(sub);
            }
        }
        return out;
    }
}
