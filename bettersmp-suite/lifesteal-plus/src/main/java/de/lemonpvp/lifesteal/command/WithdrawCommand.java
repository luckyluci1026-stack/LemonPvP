package de.lemonpvp.lifesteal.command;

import de.lemonpvp.lifesteal.LifestealPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * /withdraw [Anzahl] - wandelt Herzen in Herz-Items um (min. 1 Herz bleibt).
 */
public final class WithdrawCommand implements CommandExecutor {

    private final LifestealPlus plugin;

    public WithdrawCommand(LifestealPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        int amount = 1;
        if (args.length >= 1) {
            try {
                amount = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                amount = 1;
            }
        }
        int current = plugin.hearts().getHearts(player.getUniqueId());
        if (current - amount < 1) {
            plugin.msgs().send(player, "withdraw-too-few");
            return true;
        }

        ItemStack item = plugin.items().heartItem(amount);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        int given = amount;
        if (!leftover.isEmpty()) {
            int notAdded = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            given = amount - notAdded;
            if (given <= 0) {
                plugin.msgs().send(player, "withdraw-inventory-full");
                return true;
            }
        }
        plugin.hearts().addHearts(player.getUniqueId(), -given);
        plugin.msgs().send(player, "withdraw-success", "amount", String.valueOf(given));
        return true;
    }
}
