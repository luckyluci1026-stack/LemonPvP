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

import java.util.ArrayList;
import java.util.List;

/**
 * Baut die Shop-Menüs: Hauptmenü mit Kategorien, Kategorieseiten mit
 * Blätterfunktion und ein Kauf-/Verkaufsmenü mit reinen Klick-Buttons
 * (damit es auch auf Bedrock bequem bedienbar ist).
 */
public final class ShopMenus {

    public static final int ITEMS_PER_PAGE = 45;

    // Navigationsplätze der Kategorieseite (unterste Zeile)
    public static final int NAV_PREV = 45;
    public static final int NAV_BALANCE = 47;
    public static final int NAV_HOME = 49;
    public static final int NAV_SELL = 51;
    public static final int NAV_NEXT = 53;

    // Plätze im Aktionsmenü
    public static final int ACT_INFO = 4;
    public static final int ACT_BUY_1 = 10;
    public static final int ACT_BUY_16 = 11;
    public static final int ACT_BUY_64 = 12;
    public static final int ACT_SELL_1 = 14;
    public static final int ACT_SELL_16 = 15;
    public static final int ACT_SELL_ALL = 16;
    public static final int ACT_BACK = 22;

    private final FastShop plugin;

    public ShopMenus(FastShop plugin) {
        this.plugin = plugin;
    }

    private String money(double amount) {
        return plugin.economy().format(amount);
    }

    private Material material(String path, Material fallback) {
        Material m = Material.matchMaterial(
                plugin.getConfig().getString(path, fallback.name()));
        return m == null ? fallback : m;
    }

    // ================= Hauptmenü =================

    public static final class MainHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public void openMain(Player player) {
        int rows = Math.max(3, Math.min(6, plugin.getConfig().getInt("settings.menu-rows", 5)));
        int size = rows * 9;
        MainHolder holder = new MainHolder();
        Inventory inv = Bukkit.createInventory(holder, size,
                Text.mm(plugin.getConfig()
                        .getString("settings.menu-title", "<gold>Shop")));
        holder.inventory = inv;

        ItemStack border = GuiUtil.filler(
                material("settings.border-material", Material.BLACK_STAINED_GLASS_PANE));
        for (int i = 0; i < size; i++) {
            inv.setItem(i, border);
        }

        // Kopfzeile
        inv.setItem(4, GuiUtil.glowing(Material.NETHER_STAR, 1,
                "<gradient:#6C5CE7:#00D4FF><bold>Shop</bold></gradient>",
                List.of(
                        "<gray>Kaufe und verkaufe Items",
                        "<gray>mit deinem Guthaben.",
                        "",
                        "<dark_gray>» <gray>Kategorie anklicken zum Öffnen")));

        // Fußzeile: Kontostand, Schnellverkauf, Schließen
        int foot = size - 9;
        inv.setItem(foot, balanceItem(player));
        inv.setItem(foot + 4, GuiUtil.item(Material.HOPPER, 1,
                "<gold><bold>Schnellverkauf</bold>",
                List.of("<gray>Öffnet ein Fenster, in das du",
                        "<gray>Items zum Verkaufen legen kannst.",
                        "",
                        "<yellow>Klick zum Öffnen")));
        inv.setItem(foot + 8, GuiUtil.item(Material.BARRIER, 1,
                "<red><bold>Schließen</bold>", List.of()));

        // Kategorien zuletzt - so gewinnt immer die Einstellung aus shop.yml
        for (Category category : plugin.shop().categories().values()) {
            int slot = Math.max(0, Math.min(size - 1, category.slot()));
            inv.setItem(slot, GuiUtil.item(category.icon(), 1, category.name(),
                    List.of("<gray>" + category.items().size() + " Artikel",
                            "", "<yellow>Klick zum Öffnen")));
        }
        player.openInventory(inv);
    }

    private ItemStack balanceItem(Player player) {
        return GuiUtil.head(player,
                "<green><bold>Dein Guthaben</bold>",
                List.of("<white>" + money(plugin.economy().balance(player)),
                        "",
                        "<gray>Spieler: <white>" + player.getName()));
    }

    // ================= Kategorieseite =================

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
                Text.mm(category.name() + " <dark_gray>│ <gray>Seite "
                        + (page + 1) + "/" + totalPages));
        holder.inventory = inv;

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < items.size(); i++) {
            inv.setItem(i, displayItem(player, items.get(start + i)));
        }

        ItemStack border = GuiUtil.filler(
                material("settings.category-border-material", Material.GRAY_STAINED_GLASS_PANE));
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, border);
        }

        if (page > 0) {
            inv.setItem(NAV_PREV, GuiUtil.item(Material.ARROW, 1,
                    "<yellow>Zurück",
                    List.of("<gray>Seite " + page)));
        }
        if (page < totalPages - 1) {
            inv.setItem(NAV_NEXT, GuiUtil.item(Material.ARROW, 1,
                    "<yellow>Weiter ",
                    List.of("<gray>Seite " + (page + 2))));
        }
        inv.setItem(NAV_BALANCE, balanceItem(player));
        inv.setItem(NAV_HOME, GuiUtil.item(Material.NETHER_STAR, 1,
                "<gold>Zum Hauptmenü", List.of()));
        inv.setItem(NAV_SELL, GuiUtil.item(Material.HOPPER, 1,
                "<gold>Schnellverkauf",
                List.of("<gray>Items ablegen und verkaufen")));

        player.openInventory(inv);
    }

    private ItemStack displayItem(Player player, ShopItem item) {
        String name = item.name() != null ? item.name()
                : "<white>" + prettyName(item.material());
        List<String> lore = new ArrayList<>(item.lore());
        lore.add("<dark_gray><st>               </st>");
        if (item.buyable()) {
            lore.add("<gray>Kaufen  <dark_gray>» <green>"
                    + money(item.buy()) + " <dark_gray>/ Stück");
        } else {
            lore.add("<dark_gray>Nicht kaufbar");
        }
        if (item.sellable()) {
            lore.add("<gray>Verkauf <dark_gray>» <gold>"
                    + money(item.sell() * plugin.shop().sellMultiplier())
                    + " <dark_gray>/ Stück");
            int have = plugin.service().countSellable(player, item.material());
            if (have > 0) {
                lore.add("<dark_gray>   Du hast: <white>" + have + "x");
            }
        } else {
            lore.add("<dark_gray>Nicht verkaufbar");
        }
        lore.add("<dark_gray><st>               </st>");
        lore.add("<yellow>Klick <gray>öffnet Kaufen/Verkaufen");
        return GuiUtil.item(item.material(), 1, name, lore);
    }

    // ================= Aktionsmenü =================

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
                Text.mm("<dark_gray>» <white>" + prettyName(item.material())));
        holder.inventory = inv;

        ItemStack border = GuiUtil.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        double balance = plugin.economy().balance(player);
        int have = plugin.service().countSellable(player, item.material());

        List<String> info = new ArrayList<>();
        if (item.buyable()) {
            info.add("<gray>Kaufen  <dark_gray>» <green>" + money(item.buy()));
        }
        if (item.sellable()) {
            info.add("<gray>Verkauf <dark_gray>» <gold>"
                    + money(item.sell() * plugin.shop().sellMultiplier()));
        }
        info.add("");
        info.add("<gray>Dein Guthaben: <green>" + money(balance));
        info.add("<gray>Im Inventar: <white>" + have + "x");
        inv.setItem(ACT_INFO, GuiUtil.glowing(item.material(), 1,
                item.name() != null ? item.name() : "<white>" + prettyName(item.material()), info));

        if (item.buyable()) {
            inv.setItem(ACT_BUY_1, buyButton(item, 1, balance));
            inv.setItem(ACT_BUY_16, buyButton(item, 16, balance));
            inv.setItem(ACT_BUY_64, buyButton(item, 64, balance));
        }
        if (item.sellable()) {
            double each = item.sell() * plugin.shop().sellMultiplier();
            inv.setItem(ACT_SELL_1, sellButton(item, 1, each, have));
            inv.setItem(ACT_SELL_16, sellButton(item, 16, each, have));
            inv.setItem(ACT_SELL_ALL, GuiUtil.item(have > 0 ? Material.GOLD_BLOCK : Material.GRAY_DYE,
                    Math.max(1, Math.min(64, have)),
                    "<gold><bold>Alles verkaufen</bold>",
                    List.of("<gray>Menge: <white>" + have + "x",
                            "<gray>Erlös: <gold>" + money(each * have),
                            "",
                            have > 0 ? "<yellow>Klick zum Verkaufen"
                                     : "<red>Du hast keine")));
        }
        inv.setItem(ACT_BACK, GuiUtil.item(Material.ARROW, 1,
                "<yellow>Zurück",
                List.of("<gray>Zurück zur Kategorie")));
        player.openInventory(inv);
    }

    private ItemStack buyButton(ShopItem item, int amount, double balance) {
        double cost = item.buy() * amount;
        boolean affordable = balance >= cost;
        return GuiUtil.item(affordable ? Material.EMERALD : Material.GRAY_DYE, amount,
                (affordable ? "<green>" : "<red>") + "<bold>Kaufen: " + amount + "x</bold>",
                List.of("<gray>Preis: <green>" + money(cost),
                        "",
                        affordable ? "<yellow>Klick zum Kaufen"
                                   : "<red>Nicht genug Guthaben"));
    }

    private ItemStack sellButton(ShopItem item, int amount, double each, int have) {
        boolean enough = have >= amount;
        return GuiUtil.item(enough ? Material.GOLD_INGOT : Material.GRAY_DYE, amount,
                (enough ? "<gold>" : "<red>") + "<bold>Verkaufen: " + amount + "x</bold>",
                List.of("<gray>Erlös: <gold>" + money(each * amount),
                        "",
                        enough ? "<yellow>Klick zum Verkaufen"
                               : "<red>Du hast nur " + have + "x"));
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

    // ================= Verkaufsfenster =================

    /** Freie Ablagefläche im Verkaufsfenster: Slots 0 bis SELL_AREA-1. */
    public static final int SELL_AREA = 45;
    public static final int SELL_TOTAL = 48;
    public static final int SELL_CONFIRM = 50;
    public static final int SELL_CANCEL = 53;

    public static final class SellHolder implements InventoryHolder {
        private Inventory inventory;
        /** Wird auf true gesetzt, wenn der Verkauf-Knopf gedrückt wurde. */
        public boolean sold;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public void openSell(Player player) {
        SellHolder holder = new SellHolder();
        Inventory inv = Bukkit.createInventory(holder, 54,
                Text.mm(plugin.msgs().raw("sell-gui-title")));
        holder.inventory = inv;

        ItemStack border = GuiUtil.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = SELL_AREA; i < 54; i++) {
            inv.setItem(i, border);
        }
        refreshSell(inv);
        player.openInventory(inv);
    }

    /** Aktualisiert Summe und Knöpfe im Verkaufsfenster. */
    public void refreshSell(Inventory inv) {
        double total = 0;
        int count = 0;
        for (int i = 0; i < SELL_AREA; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            ShopItem item = plugin.shop().item(stack.getType());
            if (item != null && item.sellable() && plugin.service().isSellable(stack)) {
                total += item.sell() * plugin.shop().sellMultiplier() * stack.getAmount();
                count += stack.getAmount();
            }
        }

        inv.setItem(SELL_TOTAL, GuiUtil.item(Material.PAPER, 1,
                "<gold><bold>Gesamtwert</bold>",
                List.of("<gray>Verkaufbar: <white>" + count + "x",
                        "<gray>Erlös: <gold>" + money(total),
                        "",
                        "<dark_gray>Nicht verkaufbare Items",
                        "<dark_gray>bekommst du zurück.")));

        boolean any = count > 0;
        inv.setItem(SELL_CONFIRM, any
                ? GuiUtil.glowing(Material.EMERALD, 1,
                    "<green><bold>Verkaufen</bold>",
                    List.of("<gray>Verkauft alles hier drin",
                            "<gray>für <gold>" + money(total),
                            "",
                            "<yellow>Klick zum Verkaufen"))
                : GuiUtil.item(Material.GRAY_DYE, 1,
                    "<dark_gray><bold>Verkaufen</bold>",
                    List.of("<gray>Leg zuerst Items hinein.")));

        inv.setItem(SELL_CANCEL, GuiUtil.item(Material.BARRIER, 1,
                "<red><bold>Abbrechen</bold>",
                List.of("<gray>Schließt das Fenster und",
                        "<gray>gibt dir alle Items zurück.")));
    }
}
