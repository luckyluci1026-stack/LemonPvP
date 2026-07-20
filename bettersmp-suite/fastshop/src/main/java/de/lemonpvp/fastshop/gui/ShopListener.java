package de.lemonpvp.fastshop.gui;

import de.lemonpvp.fastshop.FastShop;
import de.lemonpvp.fastshop.shop.Category;
import de.lemonpvp.fastshop.shop.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/**
 * Klick-Verarbeitung fuer Haupt-/Kategorie-Menue und Verkauf beim Schliessen
 * des Sell-GUI.
 */
public final class ShopListener implements org.bukkit.event.Listener {

    private final FastShop plugin;

    public ShopListener(FastShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof ShopMenus.MainHolder) {
            event.setCancelled(true);
            handleMain(event);
        } else if (holder instanceof ShopMenus.CategoryHolder categoryHolder) {
            event.setCancelled(true);
            handleCategory(event, categoryHolder);
        }
        // SellHolder: Klicks erlaubt (Spieler legt Items ab) -> nicht abbrechen
    }

    private void handleMain(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }
        for (Category category : plugin.shop().categories().values()) {
            if (category.slot() == event.getSlot()) {
                plugin.menus().openCategory(player, category.id(), 0);
                return;
            }
        }
    }

    private void handleCategory(InventoryClickEvent event, ShopMenus.CategoryHolder holder) {
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }
        int slot = event.getSlot();
        if (slot == 45) {
            plugin.menus().openCategory(player, holder.categoryId, holder.page - 1);
            return;
        }
        if (slot == 49) {
            plugin.menus().openMain(player);
            return;
        }
        if (slot == 53) {
            plugin.menus().openCategory(player, holder.categoryId, holder.page + 1);
            return;
        }
        if (slot >= ShopMenus.ITEMS_PER_PAGE) {
            return;
        }
        Category category = plugin.shop().category(holder.categoryId);
        if (category == null) {
            return;
        }
        List<ShopItem> items = category.items();
        int index = holder.page * ShopMenus.ITEMS_PER_PAGE + slot;
        if (index < 0 || index >= items.size()) {
            return;
        }
        ShopItem item = items.get(index);
        ClickType click = event.getClick();
        if (click == ClickType.LEFT) {
            plugin.service().buy(player, item, 1);
        } else if (click == ClickType.SHIFT_LEFT) {
            plugin.service().buy(player, item, 64);
        } else if (click == ClickType.RIGHT) {
            plugin.service().sellFromInventory(player, item.material(), false);
        } else if (click == ClickType.SHIFT_RIGHT) {
            plugin.service().sellFromInventory(player, item.material(), true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof ShopMenus.SellHolder
                && event.getPlayer() instanceof Player player) {
            plugin.service().sellContents(player, inventory);
        }
    }
}
