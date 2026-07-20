package de.lemonpvp.fastshop.command;

import de.lemonpvp.fastshop.FastShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * /sell hand|all|gui
 */
public final class SellCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("hand", "all", "gui");

    private final FastShop plugin;

    public SellCommand(FastShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        if (!player.hasPermission("fastshop.sell")) {
            plugin.msgs().send(player, "no-permission");
            return true;
        }
        String sub = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "hand";
        switch (sub) {
            case "hand" -> plugin.service().sellHand(player);
            case "all" -> plugin.service().sellAll(player);
            case "gui" -> plugin.menus().openSell(player);
            default -> plugin.msgs().send(player, "usage-sell");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
