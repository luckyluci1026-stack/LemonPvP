package de.lemonpvp.helden.listener;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.item.ItemRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Rechtsklick auf das Herz-Item.
 *
 * <p>Auf Bedrock ist das lange Antippen bzw. die Nutzen-Taste derselbe
 * Rechtsklick - die Steuerung ist also auf beiden Editionen identisch.</p>
 */
public final class InteractListener implements Listener {

    private final HeldenPlugin plugin;

    public InteractListener(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Nur einmal pro Rechtsklick reagieren - das Event feuert je Hand.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!plugin.settings().heartItemEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (!ItemRegistry.is(stack, plugin.settings().heartItemId())) {
            return;
        }

        event.setCancelled(true);
        if (plugin.hearts().addHearts(player, plugin.settings().heartItemValue())) {
            consumeOne(player, stack);
            plugin.messages().send(player, "item.heart-used");
        }
    }

    private void consumeOne(Player player, ItemStack stack) {
        if (stack.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            stack.setAmount(stack.getAmount() - 1);
        }
    }
}
