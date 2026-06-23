package com.lemonpvp.lemonlobby.commands;

import com.lemonpvp.lemonlobby.LemonLobby;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class LobbyAdminCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonLobby plugin;

    public LobbyAdminCommand(LemonLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemonlobby.admin")) {
            sender.sendMessage(MM.deserialize("<!italic><red>Keine Berechtigung."));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getAppleTreeListener().reload();
            sender.sendMessage(MM.deserialize(
                    "<!italic><gradient:#fffb00:#00ff00><bold>LemonLobby</bold></gradient>"
                    + " <dark_gray>»</dark_gray> <green>Konfiguration neu geladen."));
        } else {
            sender.sendMessage(MM.deserialize("<!italic><gray>Verwendung: <white>/llobby reload"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lemonlobby.admin")) return List.of();
        if (args.length == 1) return List.of("reload").stream()
                .filter(s -> s.startsWith(args[0])).toList();
        return List.of();
    }
}
