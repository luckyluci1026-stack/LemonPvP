package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

/**
 * ZonePracticePro / FlowPvP-style item selector for the kit editor.
 *
 * <p>A categorized, paginated palette that lets a player build a kit without
 * creative mode or commands: pick a category from the top row, then click any
 * item to drop a copy into their inventory (the kit-editor canvas). The editor
 * itself stays open underneath — see {@link com.lemonpvp.lemonpractice.listeners.KitEditorListener}.
 */
public class KitItemPaletteGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int SIZE = 54;
    /** Content region: rows 1–4 (36 item slots). */
    private static final int[] CONTENT = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final int PREV_SLOT = 48;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 50;

    /** One palette category: a tab icon + the items it offers. */
    private record Category(String name, Material icon, List<ItemStack> items) {}

    private final LemonPractice plugin;
    private final Player player;
    private final List<Category> categories;
    private Inventory inv;
    private boolean registered;
    private int categoryIndex;
    private int page;

    public KitItemPaletteGUI(LemonPractice plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.categories = buildCategories();
    }

    public void open() {
        inv = Bukkit.createInventory(null, SIZE,
                MM.deserialize("<!italic><gradient:#fffb00:#00ff00>Item Selector</gradient>"));
        render();
        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.3f);
        player.openInventory(inv);
    }

    private List<ItemStack> currentItems() {
        return categories.get(categoryIndex).items();
    }

    private int maxPage() {
        int n = currentItems().size();
        return n == 0 ? 0 : (n - 1) / CONTENT.length;
    }

    private void render() {
        inv.clear();

        // Top row: category tabs.
        for (int i = 0; i < categories.size() && i < 9; i++) {
            Category c = categories.get(i);
            boolean active = i == categoryIndex;
            inv.setItem(i, named(c.icon(),
                    (active ? "<green><bold>" : "<gray>") + c.name(),
                    List.of(active ? "<yellow>● Selected" : "<dark_gray>Click to view")));
        }

        // Bottom-row frame.
        ItemStack pane = pane();
        for (int i = 45; i < SIZE; i++) inv.setItem(i, pane);

        // Items for the current category/page.
        List<ItemStack> items = currentItems();
        int start = page * CONTENT.length;
        for (int i = 0; i < CONTENT.length; i++) {
            int idx = start + i;
            if (idx >= items.size()) break;
            inv.setItem(CONTENT[i], items.get(idx));
        }

        if (page > 0) inv.setItem(PREV_SLOT, named(Material.SPECTRAL_ARROW, "<yellow>← Page " + page, List.of()));
        inv.setItem(BACK_SLOT, named(Material.BARRIER, "<red>Done",
                List.of("<gray>Close and arrange your kit", "<gray>(press <white>E<gray> to open your inventory)")));
        if (page < maxPage()) inv.setItem(NEXT_SLOT, named(Material.SPECTRAL_ARROW, "<yellow>Page " + (page + 2) + " →", List.of()));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(player.getUniqueId())) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        // Category tab.
        if (slot < categories.size() && slot < 9) {
            if (slot != categoryIndex) {
                categoryIndex = slot;
                page = 0;
                render();
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
            }
            return;
        }

        if (slot == BACK_SLOT) { p.closeInventory(); return; }
        if (slot == PREV_SLOT && page > 0) { page--; render(); return; }
        if (slot == NEXT_SLOT && page < maxPage()) { page++; render(); return; }

        // Content item — give a copy to the player.
        ItemStack clicked = inv.getItem(slot);
        if (clicked == null || clicked.getType() == Material.AIR) return;
        // Only react to actual content slots.
        boolean isContent = false;
        for (int c : CONTENT) if (c == slot) { isContent = true; break; }
        if (!isContent) return;

        var leftover = p.getInventory().addItem(clicked.clone());
        if (leftover.isEmpty()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.5f);
            p.sendActionBar(MM.deserialize("<green>+ Added <white>"
                    + clicked.getAmount() + "x " + prettyName(clicked.getType())));
        } else {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            p.sendActionBar(MM.deserialize("<red>Your inventory is full!"));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        unregister();
    }

    private void unregister() {
        if (registered) { HandlerList.unregisterAll(this); registered = false; }
    }

    // ------------------------------------------------------------------
    // Item palette definition
    // ------------------------------------------------------------------

    private List<Category> buildCategories() {
        List<Category> out = new ArrayList<>();

        out.add(new Category("Weapons", Material.DIAMOND_SWORD, List.of(
                one(Material.WOODEN_SWORD), one(Material.STONE_SWORD), one(Material.IRON_SWORD),
                one(Material.GOLDEN_SWORD), one(Material.DIAMOND_SWORD), one(Material.NETHERITE_SWORD),
                one(Material.IRON_AXE), one(Material.DIAMOND_AXE), one(Material.NETHERITE_AXE),
                one(Material.BOW), one(Material.CROSSBOW), one(Material.TRIDENT),
                one(Material.SHIELD), one(Material.FISHING_ROD), one(Material.MACE))));

        out.add(new Category("Armor", Material.DIAMOND_CHESTPLATE, List.of(
                one(Material.LEATHER_HELMET), one(Material.LEATHER_CHESTPLATE), one(Material.LEATHER_LEGGINGS), one(Material.LEATHER_BOOTS),
                one(Material.CHAINMAIL_HELMET), one(Material.CHAINMAIL_CHESTPLATE), one(Material.CHAINMAIL_LEGGINGS), one(Material.CHAINMAIL_BOOTS),
                one(Material.IRON_HELMET), one(Material.IRON_CHESTPLATE), one(Material.IRON_LEGGINGS), one(Material.IRON_BOOTS),
                one(Material.GOLDEN_HELMET), one(Material.GOLDEN_CHESTPLATE), one(Material.GOLDEN_LEGGINGS), one(Material.GOLDEN_BOOTS),
                one(Material.DIAMOND_HELMET), one(Material.DIAMOND_CHESTPLATE), one(Material.DIAMOND_LEGGINGS), one(Material.DIAMOND_BOOTS),
                one(Material.NETHERITE_HELMET), one(Material.NETHERITE_CHESTPLATE), one(Material.NETHERITE_LEGGINGS), one(Material.NETHERITE_BOOTS),
                one(Material.TURTLE_HELMET), one(Material.ELYTRA))));

        out.add(new Category("Blocks", Material.COBBLESTONE, List.of(
                stack(Material.COBBLESTONE, 64), stack(Material.OAK_PLANKS, 64), stack(Material.OAK_LOG, 64),
                stack(Material.OBSIDIAN, 64), stack(Material.END_STONE, 64), stack(Material.NETHERRACK, 64),
                stack(Material.DIRT, 64), stack(Material.SAND, 64), stack(Material.GLASS, 64),
                stack(Material.SLIME_BLOCK, 16), stack(Material.LADDER, 64), stack(Material.SCAFFOLDING, 64),
                stack(Material.COBWEB, 16), stack(Material.HAY_BLOCK, 16))));

        out.add(new Category("Projectiles", Material.ARROW, List.of(
                stack(Material.ARROW, 64), stack(Material.SPECTRAL_ARROW, 16), stack(Material.SNOWBALL, 16),
                stack(Material.ENDER_PEARL, 16), stack(Material.EGG, 16), stack(Material.FIRE_CHARGE, 16),
                stack(Material.WIND_CHARGE, 16))));

        out.add(new Category("Food", Material.GOLDEN_APPLE, List.of(
                stack(Material.GOLDEN_APPLE, 16), stack(Material.ENCHANTED_GOLDEN_APPLE, 8),
                stack(Material.GOLDEN_CARROT, 32), stack(Material.COOKED_BEEF, 32),
                stack(Material.BREAD, 32), stack(Material.COOKED_PORKCHOP, 32),
                stack(Material.MUSHROOM_STEW, 1), stack(Material.CAKE, 1))));

        out.add(new Category("Potions", Material.SPLASH_POTION, List.of(
                potion(Material.POTION, PotionType.HEALING), potion(Material.POTION, PotionType.REGENERATION),
                potion(Material.POTION, PotionType.STRENGTH), potion(Material.POTION, PotionType.SWIFTNESS),
                potion(Material.POTION, PotionType.FIRE_RESISTANCE), potion(Material.POTION, PotionType.SLOW_FALLING),
                potion(Material.SPLASH_POTION, PotionType.HEALING), potion(Material.SPLASH_POTION, PotionType.REGENERATION),
                potion(Material.SPLASH_POTION, PotionType.STRENGTH), potion(Material.SPLASH_POTION, PotionType.SWIFTNESS),
                potion(Material.SPLASH_POTION, PotionType.POISON), potion(Material.SPLASH_POTION, PotionType.WEAKNESS),
                one(Material.MILK_BUCKET), one(Material.HONEY_BOTTLE))));

        out.add(new Category("Utility", Material.ENDER_CHEST, List.of(
                one(Material.WATER_BUCKET), one(Material.LAVA_BUCKET), one(Material.FLINT_AND_STEEL),
                stack(Material.TNT, 16), one(Material.TOTEM_OF_UNDYING), stack(Material.EXPERIENCE_BOTTLE, 32),
                one(Material.ANVIL), one(Material.ENDER_CHEST), one(Material.RESPAWN_ANCHOR),
                stack(Material.END_CRYSTAL, 16))));

        return out;
    }

    private ItemStack one(Material mat) { return new ItemStack(mat, 1); }

    private ItemStack stack(Material mat, int amount) { return new ItemStack(mat, amount); }

    private ItemStack potion(Material mat, PotionType type) {
        ItemStack item = new ItemStack(mat);
        if (item.getItemMeta() instanceof PotionMeta pm) {
            try { pm.setBasePotionType(type); } catch (Throwable ignored) {}
            item.setItemMeta(pm);
        }
        return item;
    }

    // ------------------------------------------------------------------
    // Item helpers
    // ------------------------------------------------------------------

    private ItemStack pane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack named(Material mat, String mini, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + mini));
            if (lore != null && !lore.isEmpty()) {
                List<Component> l = new ArrayList<>();
                for (String s : lore) l.add(MM.deserialize("<!italic>" + s));
                meta.lore(l);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String prettyName(Material mat) {
        String[] parts = mat.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
