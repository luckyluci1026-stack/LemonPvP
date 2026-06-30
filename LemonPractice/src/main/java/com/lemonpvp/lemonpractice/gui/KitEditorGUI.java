package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.PlayerKit;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Utility class for the kit editor workflow.
 *
 * The editor is the player's own inventory — no separate chest GUI is needed.
 * Opening fills the player inventory with the current kit and marks the session
 * with PDC keys. {@link com.lemonpvp.lemonpractice.listeners.KitEditorListener}
 * listens for InventoryCloseEvent and calls {@link #saveKit(Player, String)} to persist.
 */
public class KitEditorGUI {

    /** PDC key written to the player entity: records which gamemode is being edited. */
    public static final String PDC_EDITING_GAMEMODE_KEY = "editing_gamemode";

    /** PDC key written to control items (DELETE / item-selector) to identify their action. */
    public static final String PDC_KIT_ACTION_KEY = "kit_action";
    public static final String PDC_KIT_ACTION_DELETE = "delete";
    public static final String PDC_KIT_ACTION_ITEMS = "items";

    /** Inventory slot used for the DELETE barrier item (slot 8 = last hotbar slot). */
    private static final int DELETE_SLOT = 8;
    /** Inventory slot used for the item-selector control (slot 7). */
    private static final int ITEMS_SLOT = 7;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;
    private final NamespacedKey kitActionKey;
    private final NamespacedKey editingGamemodeKey;

    public KitEditorGUI(LemonPractice plugin) {
        this.plugin = plugin;
        this.kitActionKey = new NamespacedKey(plugin, PDC_KIT_ACTION_KEY);
        this.editingGamemodeKey = new NamespacedKey(plugin, PDC_EDITING_GAMEMODE_KEY);
    }

    // -------------------------------------------------------------------------
    // Open editor
    // -------------------------------------------------------------------------

    /**
     * Opens the kit editor for {@code player} for the given {@code gamemode}.
     *
     * <ol>
     *   <li>Clears the player's inventory.</li>
     *   <li>Applies the player's custom kit (or the default kit if none saved).</li>
     *   <li>Places a DELETE barrier item in slot {@value DELETE_SLOT}.</li>
     *   <li>Stores the gamemode being edited in the player's own PDC.</li>
     *   <li>Sends an instructional message.</li>
     * </ol>
     */
    public void openEditor(Player player, String gamemode) {
        // 1. Clear inventory
        player.getInventory().clear();

        // 2. Apply kit (custom > default)
        PlayerKit kit = plugin.getKitManager().getEffectiveKit(player.getUniqueId(), gamemode);
        if (kit != null) {
            for (java.util.Map.Entry<Integer, ItemStack> entry : kit.getSlots().entrySet()) {
                int slot = entry.getKey();
                // Don't overwrite the reserved control slots with kit contents
                if (slot == DELETE_SLOT || slot == ITEMS_SLOT) continue;
                if (slot >= 0 && slot < 36) {
                    player.getInventory().setItem(slot, entry.getValue().clone());
                }
            }
        }

        // 3. Place control items: item-selector (slot 7) + DELETE barrier (slot 8)
        player.getInventory().setItem(ITEMS_SLOT, buildItemSelectorItem());
        player.getInventory().setItem(DELETE_SLOT, buildDeleteItem());

        // 4. Store gamemode in player's PDC
        player.getPersistentDataContainer().set(
                editingGamemodeKey, PersistentDataType.STRING, gamemode.toLowerCase());

        // 5. Inform player
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
        player.sendMessage(MM.deserialize("<!italic><green>Editing your <yellow>" + gamemode
                + "<green> kit. <gray>Right-click the <aqua>Item Selector<gray> to add items, "
                + "close to <white>save<gray>, or click <red>Delete Kit <gray>to reset."));
    }

    // -------------------------------------------------------------------------
    // Save kit
    // -------------------------------------------------------------------------

    /**
     * Reads slots 0–35 (hotbar + main inventory) and persists a {@link PlayerKit}.
     * The DELETE barrier item in slot {@value DELETE_SLOT} is excluded automatically
     * (identified by its PDC key).
     *
     * <p>Called by {@link com.lemonpvp.lemonpractice.listeners.KitEditorListener} on
     * inventory close, and also when the player clicks the DELETE item
     * (in which case the caller should first clear the player inventory).</p>
     */
    public void saveKit(Player player, String gamemode) {
        PlayerKit kit = new PlayerKit(player.getUniqueId(), gamemode);

        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            // Skip control items (DELETE barrier, item-selector)
            if (isControlItem(item)) continue;

            kit.setSlot(slot, item.clone());
        }

        plugin.getKitManager().saveKit(player.getUniqueId(), gamemode, kit);

        // Remove editing state from PDC
        player.getPersistentDataContainer().remove(editingGamemodeKey);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        player.sendMessage(MM.deserialize("<!italic><green>Your <yellow>" + gamemode + " <green>kit was saved."));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the §cDelete Kit barrier item used as a sentinel in the editor.
     */
    public ItemStack buildDeleteItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize("§c§lDelete Kit"));
            meta.getPersistentDataContainer().set(kitActionKey, PersistentDataType.STRING, PDC_KIT_ACTION_DELETE);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Builds the item-selector control item (nether star) used to open the
     * {@link com.lemonpvp.lemonpractice.gui.KitItemPaletteGUI} from the editor.
     */
    public ItemStack buildItemSelectorItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic><aqua><bold>Item Selector</bold>"));
            meta.lore(java.util.List.of(
                    MM.deserialize("<!italic><gray>Right-click to open the item palette"),
                    MM.deserialize("<!italic><gray>and build your kit without creative.")));
            meta.getPersistentDataContainer().set(kitActionKey, PersistentDataType.STRING, PDC_KIT_ACTION_ITEMS);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Returns {@code true} if {@code item} is the DELETE barrier sentinel.
     */
    public boolean isDeleteItem(ItemStack item) {
        if (item == null || item.getType() != Material.BARRIER) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String action = meta.getPersistentDataContainer().get(kitActionKey, PersistentDataType.STRING);
        return PDC_KIT_ACTION_DELETE.equals(action);
    }

    /**
     * Returns {@code true} if {@code item} is any kit-editor control item (it
     * carries the {@link #PDC_KIT_ACTION_KEY} PDC tag).
     */
    public boolean isControlItem(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(kitActionKey, PersistentDataType.STRING);
    }

    /**
     * Returns the gamemode currently being edited for {@code player}, or {@code null}
     * if the player is not in a kit-edit session.
     */
    public String getEditingGamemode(Player player) {
        return player.getPersistentDataContainer().get(editingGamemodeKey, PersistentDataType.STRING);
    }

    /**
     * Clears the editing-gamemode PDC key from the player (ends the editing session marker).
     */
    public void clearEditingState(Player player) {
        player.getPersistentDataContainer().remove(editingGamemodeKey);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public NamespacedKey getKitActionKey() { return kitActionKey; }
    public NamespacedKey getEditingGamemodeKey() { return editingGamemodeKey; }
    public int getDeleteSlot() { return DELETE_SLOT; }
}
