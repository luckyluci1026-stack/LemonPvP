package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.managers.PostMatchManager.Loadout;
import com.lemonpvp.lemonpractice.managers.PostMatchManager.Review;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Read-only view of a player's final duel loadout, laid out like a real
 * inventory: storage on top, hotbar on the bottom row, armor and offhand on the
 * far right. A toggle button switches between the opponent's loadout and the
 * viewer's own. Backed by {@link com.lemonpvp.lemonpractice.managers.PostMatchManager}.
 */
public class MatchInventoryGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // 54-slot chest. Storage 0-26 <- contents[9..35]; hotbar 36-44 <- contents[0..8].
    // Row 3 (27-35) holds armor + offhand so nothing overlaps the storage grid.
    private static final int[] ARMOR_SLOTS = {30, 29, 28, 27};  // getArmorContents order: boots, legs, chest, helmet
    private static final int OFFHAND_SLOT = 32;
    private static final int TOGGLE_SLOT = 53;

    private final LemonPractice plugin;
    private final Player viewer;
    private boolean showOpponent = true;
    private Inventory inv;
    private boolean registered;

    public MatchInventoryGUI(LemonPractice plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    public void open() {
        Review review = plugin.getPostMatchManager().get(viewer.getUniqueId());
        if (review == null) {
            viewer.sendMessage(MM.deserialize("<!italic><gray>No match loadouts to review right now."));
            return;
        }
        Loadout shown = showOpponent ? review.opponent() : review.own();
        String who = shown != null ? shown.name() : "—";

        inv = Bukkit.createInventory(null, 54, MM.deserialize(
                "<!italic><gradient:#fffb00:#00ff00><bold>Loadout</bold></gradient> <dark_gray>» <white>" + who));

        ItemStack pane = pane();
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);

        if (shown != null) {
            ItemStack[] contents = shown.contents();
            if (contents != null) {
                // Hotbar (contents 0-8) on the bottom row
                for (int i = 0; i < 9 && i < contents.length; i++) inv.setItem(36 + i, contents[i]);
                // Main storage (contents 9-35) on the top three rows
                for (int i = 9; i < 36 && i < contents.length; i++) inv.setItem(i - 9, contents[i]);
            }
            ItemStack[] armor = shown.armor(); // [boots, legs, chest, helmet]
            if (armor != null) {
                for (int i = 0; i < ARMOR_SLOTS.length && i < armor.length; i++) inv.setItem(ARMOR_SLOTS[i], armor[i]);
            }
            if (shown.offhand() != null && shown.offhand().getType() != Material.AIR) {
                inv.setItem(OFFHAND_SLOT, shown.offhand());
            }
        }

        inv.setItem(TOGGLE_SLOT, toggleButton());

        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        viewer.openInventory(inv);
    }

    private ItemStack toggleButton() {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic><yellow><bold>"
                    + (showOpponent ? "Viewing: Opponent" : "Viewing: You")));
            meta.lore(java.util.List.of(MM.deserialize("<!italic><green>► Click to switch")));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(viewer.getUniqueId())) return;
        e.setCancelled(true); // read-only view
        if (e.getRawSlot() == TOGGLE_SLOT) {
            showOpponent = !showOpponent;
            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            open(); // re-render the other loadout
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(viewer.getUniqueId())) return;
        if (registered) { HandlerList.unregisterAll(this); registered = false; }
    }

    private ItemStack pane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); item.setItemMeta(meta); }
        return item;
    }
}
