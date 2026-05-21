package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.PlayerKit;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

    /** PDC key written to the DELETE barrier item to identify its action. */
    public static final String PDC_KIT_ACTION_KEY = "kit_action";
    public static final String PDC_KIT_ACTION_DELETE = "delete";

    /** Inventory slot used for the DELETE barrier item (slot 8 = last hotbar slot). */
    private static final int DELETE_SLOT = 8;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

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
                // Don't overwrite the DELETE slot with kit contents
                if (slot == DELETE_SLOT) continue;
                if (slot >= 0 && slot < 36) {
                    player.getInventory().setItem(slot, entry.getValue().clone());
                }
            }
        }

        // 3. Place DELETE barrier item in slot 8
        player.getInventory().setItem(DELETE_SLOT, buildDeleteItem());

        // 4. Store gamemode in player's PDC
        player.getPersistentDataContainer().set(
                editingGamemodeKey, PersistentDataType.STRING, gamemode.toLowerCase());

        // 5. Inform player
        player.sendMessage("§aEditing kit for §e" + gamemode
                + "§a. Close inventory to save, or click §cDelete Kit §ato reset.");
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

            // Skip the DELETE barrier item
            if (isDeleteItem(item)) continue;

            kit.setSlot(slot, item.clone());
        }

        plugin.getKitManager().saveKit(player.getUniqueId(), gamemode, kit);

        // Remove editing state from PDC
        player.getPersistentDataContainer().remove(editingGamemodeKey);

        player.sendMessage("§aKit for §e" + gamemode + " §asaved.");
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
            meta.displayName(LEGACY.deserialize("§cDelete Kit"));
            meta.getPersistentDataContainer().set(kitActionKey, PersistentDataType.STRING, PDC_KIT_ACTION_DELETE);
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
