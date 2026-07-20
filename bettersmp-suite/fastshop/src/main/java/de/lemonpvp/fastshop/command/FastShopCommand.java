package de.lemonpvp.fastshop.command;

import de.lemonpvp.fastshop.FastShop;
import de.lemonpvp.fastshop.gui.ShopMenus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * /fastshop reload | additem &lt;Kategorie&gt; &lt;Kaufpreis&gt; &lt;Verkaufspreis&gt;
 */
public final class FastShopCommand implements TabExecutor {

    private final FastShop plugin;

    public FastShopCommand(FastShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("fastshop.admin")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.msgs().send(sender, "usage-fastshop");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.msgs().reload();
                plugin.shop().load();
                plugin.msgs().send(sender, "reloaded");
            }
            case "additem" -> addItem(sender, args);
            default -> plugin.msgs().send(sender, "usage-fastshop");
        }
        return true;
    }

    private void addItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msgs().send(sender, "players-only");
            return;
        }
        if (args.length < 4) {
            plugin.msgs().send(sender, "additem-usage");
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            plugin.msgs().send(player, "nothing-in-hand");
            return;
        }
        String category = args[1];
        double buy;
        double sell;
        try {
            buy = Double.parseDouble(args[2]);
            sell = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            plugin.msgs().send(player, "additem-usage");
            return;
        }
        if (plugin.shop().addItem(category, hand.getType(), buy, sell)) {
            plugin.msgs().send(player, "additem-done",
                    "item", ShopMenus.prettyName(hand.getType()),
                    "category", category,
                    "buy", String.valueOf(buy), "sell", String.valueOf(sell));
        } else {
            plugin.msgs().send(player, "additem-no-category", "category", category);
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "additem").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("additem")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return plugin.shop().categories().keySet().stream()
                    .filter(id -> id.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
