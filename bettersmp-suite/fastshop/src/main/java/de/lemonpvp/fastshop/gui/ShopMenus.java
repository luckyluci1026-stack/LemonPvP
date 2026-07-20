package de.lemonpvp.fastshop.gui;

import de.lemonpvp.fastshop.FastShop;
import de.lemonpvp.fastshop.shop.Category;
import de.lemonpvp.fastshop.shop.ShopItem;
import de.lemonpvp.fastshop.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Baut Haupt- und Kategorie-Menues und definiert die zugehoerigen
 * InventoryHolder (fuer sichere Klick-Erkennung).
 */
public final class ShopMenus {

    public static final int ITEMS_PER_PAGE = 45;

    private final FastShop plugin;

    public ShopMenus(FastShop plugin) {
        this.plugin = plugin;
    }

    // ---------------- Hauptmenue ----------------

    public static final class MainHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public void openMain(Player player) {
        int rows = Math.max(1, Math.min(6, plugin.getConfig().getInt("settings.menu-rows", 3)));
        MainHolder holder = new MainHolder();
        Inventory inv = Bukkit.createInventory(holder, rows * 9,
                Text.mm(plugin.getConfig().getString("settings.menu-title", "<gold>Shop")));
        holder.inventory = inv;

        ItemStack border = GuiUtil.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, border);
        }
        for (Category category : plugin.shop().categories().values()) {
            int slot = Math.max(0, Math.min(inv.getSize() - 1, category.slot()));
            inv.setItem(slot, GuiUtil.item(category.icon(), 1, category.name(),
                    List.of("<gray>" + category.items().size() + " Artikel",
                            "", "<yellow>Klick zum Oeffnen")));
        }
        player.openInventory(inv);
    }

    // ---------------- Kategorie-Menue ----------------

    public static final class CategoryHolder implements InventoryHolder {
        private Inventory inventory;
        public final String categoryId;
        public final int page;

        CategoryHolder(String categoryId, int page) {
            this.categoryId = categoryId;
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public void openCategory(Player player, String categoryId, int page) {
        Category category = plugin.shop().category(categoryId);
        if (category == null) {
            openMain(player);
            return;
        }
        List<ShopItem> items = category.items();
        int totalPages = Math.max(1, (items.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(totalPages - 1, page));

        CategoryHolder holder = new CategoryHolder(categoryId, page);
        Inventory inv = Bukkit.createInventory(holder, 54,
                Text.mm(category.name() + " <dark_gray>(" + (page + 1) + "/" + totalPages + ")"));
        holder.inventory = inv;

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < items.size(); i++) {
            inv.setItem(i, displayItem(items.get(start + i)));
        }

        ItemStack border = GuiUtil.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, border);
        }
        inv.setItem(45, GuiUtil.item(Material.ARROW, 1, "<yellow>Zurueck",
                List.of("<gray>Vorherige Seite")));
        inv.setItem(49, GuiUtil.item(Material.BARRIER, 1, "<red>Zum Hauptmenue", List.of()));
        inv.setItem(53, GuiUtil.item(Material.ARROW, 1, "<yellow>Weiter",
                List.of("<gray>Naechste Seite")));

        player.openInventory(inv);
    }

    private ItemStack displayItem(ShopItem item) {
        String name = item.name() != null ? item.name()
                : "<white>" + prettyName(item.material());
        List<String> lore = new java.util.ArrayList<>(item.lore());
        lore.add("");
        if (item.buyable()) {
            lore.add("<gray>Kaufen: <green>" + plugin.economy().format(item.buy()) + "</green> <dark_gray>/ Stueck");
        } else {
            lore.add("<dark_gray>Nicht kaufbar");
        }
        if (item.sellable()) {
            double sell = item.sell() * plugin.shop().sellMultiplier();
            lore.add("<gray>Verkaufen: <gold>" + plugin.economy().format(sell) + "</gold> <dark_gray>/ Stueck");
        } else {
            lore.add("<dark_gray>Nicht verkaufbar");
        }
        lore.add("");
        // Bedrock-freundlich: einfacher Klick oeffnet das Button-Menue
        lore.add("<dark_gray> ▪ <yellow>Klick: <white>Kaufen/Verkaufen-Menue");
        lore.add("<dark_gray> ▪ <gray>Shift-Links: <white>64 kaufen (schnell)");
        lore.add("<dark_gray> ▪ <gray>Rechtsklick: <white>1 verkaufen (schnell)");
        return GuiUtil.item(item.material(), 1, name, lore);
    }

    // ---------------- Aktions-Menue (Bedrock-freundlich, nur Linksklicks) ----------------

    public static final class ActionHolder implements InventoryHolder {
        private Inventory inventory;
        public final String categoryId;
        public final int page;
        public final int itemIndex;

        ActionHolder(String categoryId, int page, int itemIndex) {
            this.categoryId = categoryId;
            this.page = page;
            this.itemIndex = itemIndex;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public void openAction(Player player, String categoryId, int page, int itemIndex) {
        Category category = plugin.shop().category(categoryId);
        if (category == null || itemIndex < 0 || itemIndex >= category.items().size()) {
            openCategory(player, categoryId, page);
            return;
        }
        ShopItem item = category.items().get(itemIndex);
        ActionHolder holder = new ActionHolder(categoryId, page, itemIndex);
        Inventory inv = Bukkit.createInventory(holder, 27,
                Text.mm("<white>" + prettyName(item.material())));
        holder.inventory = inv;

        ItemStack filler = GuiUtil.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        List<String> info = new java.util.ArrayList<>();
        if (item.buyable()) {
            info.add("<gray>Kaufen: <green>" + plugin.economy().format(item.buy()) + "</green> <dark_gray>/ Stueck");
        }
        if (item.sellable()) {
            double sell = item.sell() * plugin.shop().sellMultiplier();
            info.add("<gray>Verkaufen: <gold>" + plugin.economy().format(sell) + "</gold> <dark_gray>/ Stueck");
        }
        inv.setItem(4, GuiUtil.item(item.material(), 1,
                item.name() != null ? item.name() : "<white>" + prettyName(item.material()), info));

        if (item.buyable()) {
            inv.setItem(10, buyButton(item, 1));
            inv.setItem(11, buyButton(item, 16));
            inv.setItem(12, buyButton(item, 64));
        }
        if (item.sellable()) {
            double each = item.sell() * plugin.shop().sellMultiplier();
            inv.setItem(14, sellButton(item, 1, each));
            inv.setItem(15, sellButton(item, 16, each));
            inv.setItem(16, GuiUtil.item(Material.ORANGE_CONCRETE, 64,
                    "<gold>Alle verkaufen",
                    List.of("<gray>Verkauft alle <white>" + prettyName(item.material())
                            + "</white> aus deinem Inventar")));
        }
        inv.setItem(22, GuiUtil.item(Material.ARROW, 1, "<yellow>Zurueck",
                List.of("<gray>Zurueck zur Kategorie")));
        player.openInventory(inv);
    }

    private ItemStack buyButton(ShopItem item, int amount) {
        return GuiUtil.item(Material.LIME_CONCRETE, amount,
                "<green>Kaufen: <white>" + amount + "x",
                List.of("<gray>Preis: <green>" + plugin.economy().format(item.buy() * amount)));
    }

    private ItemStack sellButton(ShopItem item, int amount, double each) {
        return GuiUtil.item(Material.ORANGE_CONCRETE, amount,
                "<gold>Verkaufen: <white>" + amount + "x",
                List.of("<gray>Erloes: <gold>" + plugin.economy().format(each * amount)));
    }

    public static String prettyName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }

    // ---------------- Verkaufs-Menue ----------------

    public static final class SellHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public void openSell(Player player) {
        SellHolder holder = new SellHolder();
        Inventory inv = Bukkit.createInventory(holder, 27,
                Text.mm(plugin.msgs().raw("sell-gui-title")));
        holder.inventory = inv;
        player.openInventory(inv);
        plugin.msgs().send(player, "sell-gui-hint");
    }
}
