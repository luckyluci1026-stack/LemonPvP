package de.lemonpvp.smpcontent.listener;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;
import de.lemonpvp.smpcontent.gui.ContentGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Klicks im Übersichts-GUI: Item holen, blättern, filtern, Suche aufheben.
 */
public final class GuiListener implements Listener {

    private final SMPContent plugin;

    public GuiListener(SMPContent plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ContentGui.Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || !holder.getInventory().equals(event.getClickedInventory())) {
            return;
        }
        int slot = event.getSlot();

        // Untere Leiste
        if (ContentGui.isPrev(slot)) {
            holder.page--;
            plugin.gui().render(holder);
            return;
        }
        if (ContentGui.isNext(slot)) {
            holder.page++;
            plugin.gui().render(holder);
            return;
        }
        if (ContentGui.isFilter(slot)) {
            holder.filter = holder.filter.next();
            holder.page = 0;
            plugin.gui().render(holder);
            return;
        }
        if (ContentGui.isSearch(slot)) {
            if (!holder.search.isEmpty()) {
                holder.search = "";
                holder.page = 0;
                plugin.gui().render(holder);
            }
            return;
        }

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
        ItemStack stack = plugin.registry().create(entry, amount);
        // Was nicht mehr ins Inventar passt, landet vor den Füßen
        player.getInventory().addItem(stack).values()
                .forEach(rest -> player.getWorld().dropItem(player.getLocation(), rest));
        plugin.msgs().send(player, "given-self",
                "amount", String.valueOf(amount), "id", entry.id());
    }
}
