package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.managers.KitManager;
import com.lemonpvp.lemonpractice.model.PlayerKit;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sort-only preset kit editor.
 *
 * <p>The editor is the player's own inventory — no separate chest GUI. Opening
 * fills the inventory with the gamemode's <b>preset</b> kit (or the player's
 * saved arrangement of it) and marks the session with a PDC key. The player may
 * only <b>rearrange</b> the hotbar/inventory items — they cannot add or remove
 * items, and the armor slots are locked to the preset (enforced by
 * {@link com.lemonpvp.lemonpractice.listeners.KitEditorListener}). On close the
 * arrangement is persisted via {@link #saveKit(Player, String)}.</p>
 */
public class KitEditorGUI {

    /** PDC key written to the player entity: records which gamemode is being edited. */
    public static final String PDC_EDITING_GAMEMODE_KEY = "editing_gamemode";

    /** PDC key written to the RESET control item to identify its action. */
    public static final String PDC_KIT_ACTION_KEY = "kit_action";
    public static final String PDC_KIT_ACTION_RESET = "reset";

    /** Inventory slot used for the RESET control item (slot 8 = last hotbar slot). */
    private static final int RESET_SLOT = 8;

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
     * Opens the sort-only editor for {@code player} and {@code gamemode}: fills
     * the inventory with their saved arrangement of the preset (or the preset
     * itself), equips the preset armor, places the RESET control, marks the PDC
     * session and instructs the player.
     */
    public void openEditor(Player player, String gamemode) {
        player.getInventory().clear();

        // The player's saved arrangement if any, otherwise the preset.
        PlayerKit kit = plugin.getKitManager().getEffectiveKit(player.getUniqueId(), gamemode);
        if (kit != null) {
            for (Map.Entry<Integer, ItemStack> entry : kit.getSlots().entrySet()) {
                int slot = entry.getKey();
                if (slot == RESET_SLOT) continue; // reserved for the control
                // Sortable area 0–35 plus the armor slots 36–39.
                if (slot >= 0 && slot <= 39) {
                    player.getInventory().setItem(slot, entry.getValue().clone());
                }
            }
        }

        player.getInventory().setItem(RESET_SLOT, buildResetItem());

        player.getPersistentDataContainer().set(
                editingGamemodeKey, PersistentDataType.STRING, gamemode.toLowerCase());

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
        player.sendMessage(MM.deserialize("<!italic><green>Arranging your <yellow>" + gamemode
                + "<green> kit. <gray>You can only <white>sort<gray> the items — they can't be added or "
                + "removed. <gray>Close to <white>save<gray>, or click <red>Reset <gray>for the default layout."));
    }

    // -------------------------------------------------------------------------
    // Save kit
    // -------------------------------------------------------------------------

    /**
     * Persists the player's arrangement: reads the sortable area (slots 0–35,
     * excluding the RESET control), re-attaches the preset armor (authoritative),
     * and validates that the arrangement is a permutation of the preset — if it
     * isn't, the preset default is saved instead (defensive reset).
     */
    public void saveKit(Player player, String gamemode) {
        PlayerKit preset = plugin.getKitManager().getPresetKit(gamemode);

        PlayerKit kit = new PlayerKit(player.getUniqueId(), gamemode);
        List<ItemStack> arranged = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            if (isControlItem(item)) continue;
            kit.setSlot(slot, item.clone());
            arranged.add(item);
        }

        // Re-attach preset armor (the armor slots are locked during editing, but
        // we take them from the preset to be authoritative).
        if (preset != null) {
            for (int slot = KitManager.ARMOR_FEET; slot <= KitManager.ARMOR_HEAD; slot++) {
                ItemStack a = preset.getSlot(slot);
                if (a != null) kit.setSlot(slot, a.clone());
            }
        }

        // Integrity: the sortable items must be a permutation of the preset's
        // non-armor items. If not, something tampered — save the preset instead.
        if (preset != null && !isPermutationOfPreset(arranged, preset)) {
            plugin.getLogger().warning("[KitEditor] " + player.getName() + "'s " + gamemode
                    + " arrangement did not match the preset — reset to default.");
            kit = clonePreset(player.getUniqueId(), gamemode, preset);
        }

        plugin.getKitManager().saveKit(player.getUniqueId(), gamemode, kit);
        player.getPersistentDataContainer().remove(editingGamemodeKey);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        player.sendMessage(MM.deserialize("<!italic><green>Your <yellow>" + gamemode + " <green>kit layout was saved."));
    }

    /** Counts the preset's non-armor items and checks the arrangement matches them. */
    private boolean isPermutationOfPreset(List<ItemStack> arranged, PlayerKit preset) {
        List<ItemStack> presetItems = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> e : preset.getSlots().entrySet()) {
            if (!KitManager.isArmorSlot(e.getKey())) presetItems.add(e.getValue());
        }
        if (arranged.size() != presetItems.size()) return false;

        // Greedy match each arranged item to a distinct preset item.
        boolean[] used = new boolean[presetItems.size()];
        for (ItemStack a : arranged) {
            int match = -1;
            for (int i = 0; i < presetItems.size(); i++) {
                if (!used[i] && a.isSimilar(presetItems.get(i)) && a.getAmount() == presetItems.get(i).getAmount()) {
                    match = i;
                    break;
                }
            }
            if (match < 0) return false;
            used[match] = true;
        }
        return true;
    }

    private PlayerKit clonePreset(java.util.UUID uuid, String gamemode, PlayerKit preset) {
        PlayerKit kit = new PlayerKit(uuid, gamemode);
        for (Map.Entry<Integer, ItemStack> e : preset.getSlots().entrySet()) {
            kit.setSlot(e.getKey(), e.getValue().clone());
        }
        return kit;
    }

    // -------------------------------------------------------------------------
    // Control item
    // -------------------------------------------------------------------------

    /** Builds the RESET barrier item (resets the arrangement to the preset default). */
    public ItemStack buildResetItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic><red><bold>Reset to Default</bold>"));
            meta.lore(List.of(
                    MM.deserialize("<!italic><gray>Restore the preset's default layout."),
                    MM.deserialize("<!italic><dark_gray>Click twice to confirm.")));
            meta.getPersistentDataContainer().set(kitActionKey, PersistentDataType.STRING, PDC_KIT_ACTION_RESET);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Returns {@code true} if {@code item} is any kit-editor control item. */
    public boolean isControlItem(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(kitActionKey, PersistentDataType.STRING);
    }

    /** Returns the gamemode currently being edited, or {@code null} if none. */
    public String getEditingGamemode(Player player) {
        return player.getPersistentDataContainer().get(editingGamemodeKey, PersistentDataType.STRING);
    }

    /** Clears the editing-gamemode PDC key (ends the editing session marker). */
    public void clearEditingState(Player player) {
        player.getPersistentDataContainer().remove(editingGamemodeKey);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public NamespacedKey getKitActionKey() { return kitActionKey; }
    public NamespacedKey getEditingGamemodeKey() { return editingGamemodeKey; }
    public int getResetSlot() { return RESET_SLOT; }
}
