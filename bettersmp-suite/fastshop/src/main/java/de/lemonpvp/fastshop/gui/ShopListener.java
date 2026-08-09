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
 * Klick-Verarbeitung für Haupt-/Kategorie-Menü und Verkauf beim Schließen
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
        } else if (holder instanceof ShopMenus.ActionHolder actionHolder) {
            event.setCancelled(true);
            handleAction(event, actionHolder);
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
        // Fußzeile: Schnellverkauf bzw. Schließen
        int size = event.getInventory().getSize();
        int foot = size - 9;
        if (event.getSlot() == foot + 4) {
            plugin.menus().openSell(player);
        } else if (event.getSlot() == foot + 8) {
            player.closeInventory();
        }
    }

    private void handleCategory(InventoryClickEvent event, ShopMenus.CategoryHolder holder) {
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }
        int slot = event.getSlot();
        switch (slot) {
            case ShopMenus.NAV_PREV -> {
                plugin.menus().openCategory(player, holder.categoryId, holder.page - 1);
                return;
            }
            case ShopMenus.NAV_NEXT -> {
                plugin.menus().openCategory(player, holder.categoryId, holder.page + 1);
                return;
            }
            case ShopMenus.NAV_HOME -> {
                plugin.menus().openMain(player);
                return;
            }
            case ShopMenus.NAV_SELL -> {
                plugin.menus().openSell(player);
                return;
            }
            default -> {
                // normaler Item-Slot - unten weiter
            }
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
            // Bedrock-freundlich: einfacher Klick -> Button-Menü (keine
            // Rechtsklick-/Shift-Kombis nötig)
            plugin.menus().openAction(player, holder.categoryId, holder.page, index);
        } else if (click == ClickType.SHIFT_LEFT) {
            plugin.service().buy(player, item, 64);
        } else if (click == ClickType.RIGHT) {
            plugin.service().sellFromInventory(player, item.material(), 1);
        } else if (click == ClickType.SHIFT_RIGHT) {
            plugin.service().sellFromInventory(player, item.material(), -1);
        }
    }

    private void handleAction(InventoryClickEvent event, ShopMenus.ActionHolder holder) {
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }
        Category category = plugin.shop().category(holder.categoryId);
        if (category == null || holder.itemIndex < 0 || holder.itemIndex >= category.items().size()) {
            plugin.menus().openMain(player);
            return;
        }
        ShopItem item = category.items().get(holder.itemIndex);
        boolean acted = true;
        switch (event.getSlot()) {
            case ShopMenus.ACT_BUY_1 -> plugin.service().buy(player, item, 1);
            case ShopMenus.ACT_BUY_16 -> plugin.service().buy(player, item, 16);
            case ShopMenus.ACT_BUY_64 -> plugin.service().buy(player, item, 64);
            case ShopMenus.ACT_SELL_1 -> plugin.service().sellFromInventory(player, item.material(), 1);
            case ShopMenus.ACT_SELL_16 -> plugin.service().sellFromInventory(player, item.material(), 16);
            case ShopMenus.ACT_SELL_ALL -> plugin.service().sellFromInventory(player, item.material(), -1);
            case ShopMenus.ACT_BACK -> {
                plugin.menus().openCategory(player, holder.categoryId, holder.page);
                return;
            }
            default -> acted = false;
        }
        if (acted) {
            // Guthaben und Mengen im Menü sofort aktualisieren
            plugin.menus().openAction(player, holder.categoryId, holder.page, holder.itemIndex);
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
