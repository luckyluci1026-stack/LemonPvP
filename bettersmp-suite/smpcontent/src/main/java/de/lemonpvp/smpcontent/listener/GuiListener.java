package de.lemonpvp.smpcontent.listener;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.command.ContentCommand;
import de.lemonpvp.smpcontent.content.CustomEntry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Klick im Übersichts-GUI gibt das jeweilige Item.
 */
public final class GuiListener implements Listener {

    private final SMPContent plugin;

    public GuiListener(SMPContent plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ContentCommand.ContentHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || !holder.getInventory().equals(event.getClickedInventory())) {
            return;
        }
        int slot = event.getSlot();
        if (slot < 0 || slot >= holder.ids.size()) {
            return;
        }
        String id = holder.ids.get(slot);
        if (id == null) {
            return;
        }
        CustomEntry entry = plugin.registry().get(id);
        if (entry == null) {
            return;
        }
        int amount = event.isShiftClick() ? 64 : 1;
        player.getInventory().addItem(plugin.registry().create(entry, amount));
        plugin.msgs().send(player, "given-self",
                "amount", String.valueOf(amount), "id", entry.id());
    }
}
