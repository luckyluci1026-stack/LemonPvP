package de.lemonpvp.fastshop.command;

import de.lemonpvp.fastshop.FastShop;
import de.lemonpvp.fastshop.gui.ShopMenus;
import de.lemonpvp.fastshop.shop.ShopItem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * /worth - Verkaufs-/Kaufwert des Items in der Hand.
 */
public final class WorthCommand implements CommandExecutor {

    private final FastShop plugin;

    public WorthCommand(FastShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            plugin.msgs().send(player, "nothing-in-hand");
            return true;
        }
        ShopItem item = plugin.shop().item(hand.getType());
        if (item == null) {
            plugin.msgs().send(player, "worth-none");
            return true;
        }
        String sell = item.sellable()
                ? plugin.economy().format(item.sell() * plugin.shop().sellMultiplier())
                : "-";
        String buy = item.buyable() ? plugin.economy().format(item.buy()) : "-";
        plugin.msgs().send(player, "worth",
                "item", ShopMenus.prettyName(hand.getType()), "sell", sell, "buy", buy);
        return true;
    }
}
