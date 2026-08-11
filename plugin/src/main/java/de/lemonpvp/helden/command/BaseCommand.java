package de.lemonpvp.helden.command;

import de.lemonpvp.helden.HeldenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Gemeinsame Pruefungen fuer alle Befehle des Plugins. */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final HeldenPlugin plugin;
    private final String permission;
    private final boolean playerOnly;

    protected BaseCommand(HeldenPlugin plugin, String permission, boolean playerOnly) {
        this.plugin = plugin;
        this.permission = permission;
        this.playerOnly = playerOnly;
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (permission != null && !sender.hasPermission(permission)) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }
        if (playerOnly && !(sender instanceof Player)) {
            plugin.messages().send(sender, "general.player-only");
            return true;
        }
        execute(sender, args);
        return true;
    }

    protected abstract void execute(CommandSender sender, String[] args);

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }

    /** Filtert Vorschlaege nach dem bereits getippten Praefix. */
    protected List<String> filter(Collection<String> options, String input) {
        String needle = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(needle)) {
                result.add(option);
            }
        }
        return result;
    }

    protected List<String> onlinePlayerNames(String input) {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return filter(names, input);
    }

    /** Zahl aus einem Argument, mit Fehlermeldung bei Unsinn. */
    protected Integer parseInt(CommandSender sender, String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            plugin.messages().send(sender, "general.invalid-number", "%input%", input);
            return null;
        }
    }
}
