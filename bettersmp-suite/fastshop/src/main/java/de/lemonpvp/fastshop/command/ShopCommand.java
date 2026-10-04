package de.lemonpvp.fastshop.command;

import de.lemonpvp.fastshop.FastShop;
import de.lemonpvp.fastshop.shop.Category;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /shop [Kategorie]
 */
public final class ShopCommand implements TabExecutor {

    private final FastShop plugin;

    public ShopCommand(FastShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        if (!player.hasPermission("fastshop.use")) {
            plugin.msgs().send(player, "no-permission");
            return true;
        }
        Category kategorie = args.length >= 1 ? plugin.shop().category(args[0]) : null;
        if (kategorie != null && kategorie.imMenue()) {
            plugin.menus().openCategory(player, args[0], 0);
        } else {
            plugin.menus().openMain(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            plugin.shop().categories().forEach((id, kategorie) -> {
                if (kategorie.imMenue() && id.startsWith(prefix)) {
                    out.add(id);
                }
            });
        }
        return out;
    }
}
