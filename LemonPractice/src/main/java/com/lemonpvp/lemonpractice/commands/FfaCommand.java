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
 * {@code /ffa} or {@code /ffa join} — join the roaming-zone FFA.
 * {@code /ffa leave} — leave and return to the lobby.
 */
public class FfaCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;

    public FfaCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("lemonpractice.use.ffa")) {
            player.sendMessage(MM.deserialize("<!italic><red>You don't have permission."));
            return true;
        }

        String sub = (args.length == 0) ? "join" : args[0].toLowerCase();
        switch (sub) {
            case "join" -> plugin.getFfaManager().join(player);
            case "leave", "quit", "exit" -> {
                if (!plugin.getFfaManager().isInFfa(player.getUniqueId())) {
                    player.sendMessage(MM.deserialize("<!italic><red>You are not in the FFA."));
                    return true;
                }
                plugin.getFfaManager().leaveFfa(player);
                player.sendMessage(MM.deserialize("<!italic><gray>You left the FFA."));
            }
            default -> sendUsage(player);
        }
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(MM.deserialize("<!italic><dark_gray><st>                        </st>"));
        player.sendMessage(MM.deserialize("<!italic><gradient:#fffb00:#00ff00><bold>FFA</bold></gradient>"));
        player.sendMessage(MM.deserialize("<!italic><yellow>/ffa join <gray>— Join the roaming-zone FFA"));
        player.sendMessage(MM.deserialize("<!italic><yellow>/ffa leave <gray>— Leave and return to the lobby"));
        player.sendMessage(MM.deserialize("<!italic><dark_gray><st>                        </st>"));
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
