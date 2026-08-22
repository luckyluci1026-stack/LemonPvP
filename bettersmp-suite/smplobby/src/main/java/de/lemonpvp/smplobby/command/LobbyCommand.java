package de.lemonpvp.smplobby.command;

import de.lemonpvp.smplobby.SMPLobby;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /spawn, /setspawn und /smplobby.
 *
 * Alle drei in einer Klasse, weil sie dieselben drei Dinge tun und
 * getrennt nur dreimal derselbe Rahmen waeren.
 */
public final class LobbyCommand implements CommandExecutor, TabCompleter {

    private static final List<String> UNTERBEFEHLE = List.of("reload", "setspawn", "items");

    private final SMPLobby plugin;

    public LobbyCommand(SMPLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        if (name.equals("spawn")) {
            return zumSpawn(sender);
        }
        if (name.equals("setspawn")) {
            return setzeSpawn(sender);
        }

        // /smplobby
        if (args.length == 0) {
            plugin.msgs().send(sender, "hilfe");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.msgs().reload();
                plugin.spawn().lade();
                plugin.board().stop();
                plugin.board().start();
                plugin.msgs().send(sender, "neu-geladen");
            }
            case "setspawn" -> {
                return setzeSpawn(sender);
            }
            case "items" -> {
                if (!(sender instanceof Player spieler)) {
                    plugin.msgs().send(sender, "nur-im-spiel");
                    return true;
                }
                plugin.items().gib(spieler);
                plugin.msgs().send(sender, "items-gegeben");
            }
            default -> plugin.msgs().send(sender, "hilfe");
        }
        return true;
    }

    private boolean zumSpawn(CommandSender sender) {
        if (!(sender instanceof Player spieler)) {
            plugin.msgs().send(sender, "nur-im-spiel");
            return true;
        }
        if (!plugin.spawn().bringe(spieler)) {
            plugin.msgs().send(sender, "spawn-fehlt");
            return true;
        }
        plugin.doppelsprung().erlaube(spieler);
        plugin.msgs().send(sender, "spawn-da");
        return true;
    }

    private boolean setzeSpawn(CommandSender sender) {
        if (!(sender instanceof Player spieler)) {
            plugin.msgs().send(sender, "nur-im-spiel");
            return true;
        }
        if (!spieler.hasPermission("smplobby.admin")) {
            plugin.msgs().send(sender, "keine-rechte");
            return true;
        }
        plugin.spawn().setze(spieler.getLocation());
        plugin.msgs().send(sender, "spawn-gesetzt");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("smplobby") || args.length != 1) {
            return List.of();
        }
        List<String> passend = new ArrayList<>();
        String angefangen = args[0].toLowerCase(Locale.ROOT);
        for (String wort : UNTERBEFEHLE) {
            if (wort.startsWith(angefangen)) {
                passend.add(wort);
            }
        }
        return passend;
    }
}
