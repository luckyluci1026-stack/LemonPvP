package de.lemonpvp.lifesteal.listener;

import de.lemonpvp.lifesteal.LifestealPlus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Benutzung der Herz-/Revive-Items per Rechtsklick.
 */
public final class ItemListener implements Listener {

    private final LifestealPlus plugin;

    public ItemListener(LifestealPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        Player player = event.getPlayer();

        if (plugin.items().isHeart(item)) {
            event.setCancelled(true);
            if (!player.hasPermission("lifesteal.craft")) {
                plugin.msgs().send(player, "no-permission");
                return;
            }
            if (plugin.hearts().getHearts(player.getUniqueId()) >= plugin.hearts().maximum()) {
                plugin.msgs().send(player, "heart-used-max");
                return;
            }
            plugin.hearts().addHearts(player.getUniqueId(), 1);
            item.setAmount(item.getAmount() - 1);
            plugin.msgs().send(player, "heart-used",
                    "hearts", String.valueOf(plugin.hearts().getHearts(player.getUniqueId())));
            return;
        }

        if (plugin.items().isRevive(item)) {
            event.setCancelled(true);
            if (!player.hasPermission("lifesteal.revive")) {
                plugin.msgs().send(player, "no-permission");
                return;
            }
            plugin.revive().startPrompt(player);
        }
    }
}
