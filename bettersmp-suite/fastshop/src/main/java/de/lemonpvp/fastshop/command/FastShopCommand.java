package de.lemonpvp.fastshop.command;

import de.lemonpvp.fastshop.FastShop;
import de.lemonpvp.fastshop.gui.ShopMenus;
import org.bukkit.Material;
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
 * /fastshop reload | additem | setbuy | setsell | removeitem
 *           | addcategory | removecategory  (DonutSMP-Stil: alles editierbar)
 */
public final class FastShopCommand implements TabExecutor {

    private static final List<String> SUBS = List.of(
            "reload", "additem", "setbuy", "setsell", "removeitem", "addcategory", "removecategory");

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
            case "setbuy" -> setPrice(sender, args, "buy");
            case "setsell" -> setPrice(sender, args, "sell");
            case "removeitem" -> removeItem(sender, args);
            case "addcategory" -> addCategory(sender, args);
            case "removecategory" -> removeCategory(sender, args);
            default -> plugin.msgs().send(sender, "usage-fastshop");
        }
        return true;
    }

    private ItemStack hand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.msgs().send(sender, "players-only");
            return null;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            plugin.msgs().send(player, "nothing-in-hand");
            return null;
        }
        return item;
    }

    private void addItem(CommandSender sender, String[] args) {
        ItemStack item = hand(sender);
        if (item == null) {
            return;
        }
        if (args.length < 4) {
            plugin.msgs().send(sender, "additem-usage");
            return;
        }
        double buy;
        double sell;
        try {
            buy = Double.parseDouble(args[2]);
            sell = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            plugin.msgs().send(sender, "additem-usage");
            return;
        }
        if (plugin.shop().addItem(args[1], item.getType(), buy, sell)) {
            plugin.msgs().send(sender, "additem-done",
                    "item", ShopMenus.prettyName(item.getType()), "category", args[1],
                    "buy", String.valueOf(buy), "sell", String.valueOf(sell));
        } else {
            plugin.msgs().send(sender, "additem-no-category", "category", args[1]);
        }
    }

    private void setPrice(CommandSender sender, String[] args, String field) {
        ItemStack item = hand(sender);
        if (item == null) {
            return;
        }
        if (args.length < 3) {
            plugin.msgs().send(sender, "setprice-usage");
            return;
        }
        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            plugin.msgs().send(sender, "setprice-usage");
            return;
        }
        if (plugin.shop().setPrice(args[1], item.getType(), field, value)) {
            plugin.msgs().send(sender, "setprice-done",
                    "item", ShopMenus.prettyName(item.getType()), "category", args[1],
                    "field", field.equals("buy") ? "Kaufpreis" : "Verkaufspreis",
                    "value", String.valueOf(value));
        } else {
            plugin.msgs().send(sender, "item-not-in-category",
                    "item", ShopMenus.prettyName(item.getType()), "category", args[1]);
        }
    }

    private void removeItem(CommandSender sender, String[] args) {
        ItemStack item = hand(sender);
        if (item == null) {
            return;
        }
        if (args.length < 2) {
            plugin.msgs().send(sender, "removeitem-usage");
            return;
        }
        if (plugin.shop().removeItem(args[1], item.getType())) {
            plugin.msgs().send(sender, "removeitem-done",
                    "item", ShopMenus.prettyName(item.getType()), "category", args[1]);
        } else {
            plugin.msgs().send(sender, "item-not-in-category",
                    "item", ShopMenus.prettyName(item.getType()), "category", args[1]);
        }
    }

    private void addCategory(CommandSender sender, String[] args) {
        ItemStack item = hand(sender);
        if (item == null) {
            return;
        }
        if (args.length < 4) {
            plugin.msgs().send(sender, "addcategory-usage");
            return;
        }
        int slot;
        try {
            slot = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            plugin.msgs().send(sender, "addcategory-usage");
            return;
        }
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
        if (plugin.shop().addCategory(args[1], item.getType(), slot, name)) {
            plugin.msgs().send(sender, "addcategory-done", "category", args[1]);
        } else {
            plugin.msgs().send(sender, "addcategory-exists", "category", args[1]);
        }
    }

    private void removeCategory(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.msgs().send(sender, "removecategory-usage");
            return;
        }
        if (plugin.shop().removeCategory(args[1])) {
            plugin.msgs().send(sender, "removecategory-done", "category", args[1]);
        } else {
            plugin.msgs().send(sender, "removecategory-not-found", "category", args[1]);
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
        if (args.length == 2 && !args[0].equalsIgnoreCase("addcategory")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return plugin.shop().categories().keySet().stream()
                    .filter(id -> id.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
