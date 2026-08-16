package de.lemonpvp.smpcontent.gui;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;
import de.lemonpvp.smpcontent.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Übersicht aller eigenen Inhalte - mit Seiten, Filter und Suche.
 *
 * Vorher passten nur 54 Einträge ins Fenster; alles darüber war unsichtbar.
 * Jetzt sind es 45 pro Seite und man blättert unten durch.
 */
public final class ContentGui {

    /** So viele Einträge pro Seite (5 Reihen), unten bleibt Platz für die Leiste. */
    private static final int PER_PAGE = 45;

    private static final int SLOT_PREV = 45;
    private static final int SLOT_FILTER = 47;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_SEARCH = 51;
    private static final int SLOT_NEXT = 53;

    /** Was gerade angezeigt wird. */
    public enum Filter {
        ALL("Alles"), BLOCKS("Nur Blöcke"), ITEMS("Nur Items"),
        FURNITURE("Nur Möbel"), VEHICLES("Nur Fahrzeuge");

        private final String label;

        Filter(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public Filter next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** Merkt sich, was dieses Fenster gerade zeigt. */
    public static final class Holder implements InventoryHolder {

        private Inventory inventory;
        public int page;
        public Filter filter = Filter.ALL;
        public String search = "";
        /** Slot -> Id (null bei Deko und Knöpfen). */
        public final List<String> ids = new ArrayList<>();

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    private final SMPContent plugin;

    public ContentGui(SMPContent plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String search) {
        Holder holder = new Holder();
        holder.search = search == null ? "" : search.toLowerCase(Locale.ROOT).trim();
        holder.inventory = Bukkit.createInventory(holder, 54,
                Text.mm(plugin.msgs().raw("gui-title")));
        render(holder);
        player.openInventory(holder.getInventory());
    }

    /** Baut den Inhalt neu auf - nach Seitenwechsel oder Filterwechsel. */
    public void render(Holder holder) {
        List<CustomEntry> matching = matching(holder);
        int pages = pageCount(matching.size());
        holder.page = Math.max(0, Math.min(holder.page, pages - 1));

        Inventory inv = holder.getInventory();
        inv.clear();
        holder.ids.clear();
        for (int i = 0; i < 54; i++) {
            holder.ids.add(null);
        }

        int from = holder.page * PER_PAGE;
        for (int slot = 0; slot < PER_PAGE; slot++) {
            int index = from + slot;
            if (index >= matching.size()) {
                break;
            }
            CustomEntry entry = matching.get(index);
            inv.setItem(slot, icon(entry));
            holder.ids.set(slot, entry.id());
        }

        // Untere Leiste
        for (int slot = PER_PAGE; slot < 54; slot++) {
            inv.setItem(slot, filler());
        }
        if (holder.page > 0) {
            inv.setItem(SLOT_PREV, button(Material.ARROW, "<yellow>Zurück",
                    List.of("<gray>Seite " + holder.page + " von " + pages)));
        }
        if (holder.page < pages - 1) {
            inv.setItem(SLOT_NEXT, button(Material.ARROW, "<yellow>Weiter",
                    List.of("<gray>Seite " + (holder.page + 2) + " von " + pages)));
        }
        inv.setItem(SLOT_FILTER, button(Material.HOPPER,
                "<gold>Filter: <white>" + holder.filter.label(),
                List.of("<gray>Klick wechselt zu <white>" + holder.filter.next().label())));
        inv.setItem(SLOT_SEARCH, button(Material.SPYGLASS,
                holder.search.isEmpty()
                        ? "<gray>Keine Suche"
                        : "<aqua>Suche: <white>" + holder.search,
                holder.search.isEmpty()
                        ? List.of("<gray>Nutze <white>/smpcontent list <Suchwort>")
                        : List.of("<gray>Klick hebt die Suche auf")));
        inv.setItem(SLOT_INFO, button(Material.BOOK,
                "<green>Seite <white>" + (holder.page + 1) + "<gray>/<white>" + pages,
                List.of("<gray>" + matching.size() + " Einträge"
                                + (holder.search.isEmpty() && holder.filter == Filter.ALL
                                ? "" : " von " + plugin.registry().entries().size()),
                        "",
                        "<yellow>Klick <gray>gibt 1 Stück",
                        "<yellow>Shift-Klick <gray>gibt 64")));
    }

    private List<CustomEntry> matching(Holder holder) {
        return matching(plugin.registry().entries().values(), holder.filter, holder.search,
                fahrzeugIds());
    }

    /** Die Ids der Items, mit denen man ein Fahrzeug hinstellt. */
    private java.util.Set<String> fahrzeugIds() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        plugin.vehicles().types().values()
                .forEach(type -> ids.add(type.item().toLowerCase(Locale.ROOT)));
        return ids;
    }

    /** Die Einträge, die zu Filter und Suche passen. */
    public static List<CustomEntry> matching(Collection<CustomEntry> all, Filter filter,
                                             String search) {
        return matching(all, filter, search, java.util.Set.of());
    }

    /**
     * @param fahrzeuge Ids der Fahrzeug-Items - nur die kennt das GUI selbst
     *                  nicht, sie stehen in der fahrzeuge.yml
     */
    public static List<CustomEntry> matching(Collection<CustomEntry> all, Filter filter,
                                             String search,
                                             java.util.Set<String> fahrzeuge) {
        String needle = search == null ? "" : search.toLowerCase(Locale.ROOT).trim();
        List<CustomEntry> out = new ArrayList<>();
        for (CustomEntry entry : all) {
            if (filter == Filter.BLOCKS && !entry.block()) {
                continue;
            }
            if (filter == Filter.ITEMS && entry.block()) {
                continue;
            }
            if (filter == Filter.FURNITURE && !entry.isFurniture()) {
                continue;
            }
            if (filter == Filter.VEHICLES
                    && !fahrzeuge.contains(entry.id().toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (!needle.isEmpty() && !matches(entry, needle)) {
                continue;
            }
            out.add(entry);
        }
        return out;
    }

    /** Wie viele Seiten es für so viele Einträge gibt (mindestens eine). */
    public static int pageCount(int entries) {
        return Math.max(1, (entries + PER_PAGE - 1) / PER_PAGE);
    }

    public static int perPage() {
        return PER_PAGE;
    }

    /** Sucht in der Id und im sichtbaren Namen (ohne MiniMessage-Tags). */
    private static boolean matches(CustomEntry entry, String search) {
        if (entry.id().toLowerCase(Locale.ROOT).contains(search)) {
            return true;
        }
        String plain = Text.plain(entry.name()).toLowerCase(Locale.ROOT);
        return plain.contains(search);
    }

    private ItemStack icon(CustomEntry entry) {
        ItemStack icon = plugin.registry().create(entry, 1);
        ItemMeta meta = icon.getItemMeta();
        List<Component> lore = meta.lore() == null
                ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(Component.empty());
        lore.add(line("<dark_gray>Id: <gray>" + entry.id()));
        lore.add(line("<dark_gray>" + (entry.block() ? "Block" : "Item")));
        lore.add(line("<yellow>Klick <gray>1x <dark_gray>|</dark_gray> <yellow>Shift <gray>64x"));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack button(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(line(name));
        List<Component> out = new ArrayList<>();
        for (String text : lore) {
            out.add(line(text));
        }
        meta.lore(out);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack filler() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);
        return pane;
    }

    private Component line(String text) {
        return Text.mm(text).decoration(TextDecoration.ITALIC, false);
    }

    // ------------------------------------------------------------------

    public static boolean isPrev(int slot) {
        return slot == SLOT_PREV;
    }

    public static boolean isNext(int slot) {
        return slot == SLOT_NEXT;
    }

    public static boolean isFilter(int slot) {
        return slot == SLOT_FILTER;
    }

    public static boolean isSearch(int slot) {
        return slot == SLOT_SEARCH;
    }
}
