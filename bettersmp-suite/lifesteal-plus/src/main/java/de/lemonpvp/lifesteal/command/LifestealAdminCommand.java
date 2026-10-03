package de.lemonpvp.lifesteal.command;

import de.lemonpvp.lifesteal.LifestealPlus;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /lifesteal reload | set <Spieler> <Herzen> | give <Spieler> [Anzahl]
 *            | eliminate <Spieler> | revive <Spieler>
 */
public final class LifestealAdminCommand implements TabExecutor {

    private static final List<String> SUBS =
            List.of("reload", "set", "give", "eliminate", "revive");

    private final LifestealPlus plugin;

    public LifestealAdminCommand(LifestealPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("lifesteal.admin")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.msgs().send(sender, "admin-usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.msgs().reload();
                plugin.hearts().reload();
                plugin.items().registerRecipes();
                plugin.msgs().send(sender, "reloaded");
            }
            case "set" -> {
                if (args.length < 3) {
                    plugin.msgs().send(sender, "admin-usage");
                    return true;
                }
                OfflinePlayer target = resolve(args[1]);
                if (target == null) {
                    plugin.msgs().send(sender, "player-not-found");
                    return true;
                }
                int hearts = parse(args[2], plugin.hearts().starting());
                plugin.hearts().setHearts(target.getUniqueId(), hearts);
                plugin.msgs().send(sender, "admin-set",
                        "player", String.valueOf(target.getName()),
                        "hearts", String.valueOf(plugin.hearts().getHearts(target.getUniqueId())));
            }
            case "give" -> {
                if (args.length < 2) {
                    plugin.msgs().send(sender, "admin-usage");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    plugin.msgs().send(sender, "player-not-found");
                    return true;
                }
                int amount = args.length >= 3 ? parse(args[2], 1) : 1;
                target.getInventory().addItem(plugin.items().heartItem(amount));
                plugin.msgs().send(sender, "admin-give",
                        "player", target.getName(), "amount", String.valueOf(amount));
            }
            case "eliminate" -> {
                Player target = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : null;
                if (target == null) {
                    plugin.msgs().send(sender, "player-not-found");
                    return true;
                }
                plugin.eliminations().eliminate(target);
            }
            case "revive" -> {
                if (args.length < 2) {
                    plugin.msgs().send(sender, "admin-usage");
                    return true;
                }
                OfflinePlayer target = resolve(args[1]);
                if (target == null) {
                    plugin.msgs().send(sender, "player-not-found");
                    return true;
                }
                UUID id = target.getUniqueId();
                if (!plugin.hearts().isEliminated(id)) {
                    plugin.msgs().send(sender, "revive-not-eliminated");
                    return true;
                }
                plugin.eliminations().revive(id, sender);
            }
            default -> plugin.msgs().send(sender, "admin-usage");
        }
        return true;
    }

    private OfflinePlayer resolve(String name) {
        OfflinePlayer cached = plugin.getServer().getOfflinePlayerIfCached(name);
        return cached != null ? cached : Bukkit.getOfflinePlayer(name);
    }

    private int parse(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
