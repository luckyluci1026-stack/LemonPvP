package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class LobbyHotbarManager {

    private final LemonPractice plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /** PersistentData key used to identify hotbar actions */
    private final NamespacedKey actionKey;

    // Hotbar slot constants
    private static final int SLOT_QUEUE        = 0;
    private static final int SLOT_KIT_EDITOR   = 4;
    private static final int SLOT_COSMETICS    = 7;
    private static final int SLOT_SETTINGS     = 8;

    public LobbyHotbarManager(LemonPractice plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "hotbar_action");
    }

    // -----------------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------------

    public void setupHotbar(Player player) {
        player.getInventory().clear();

        player.getInventory().setItem(SLOT_QUEUE,
                buildItem(
                        Material.IRON_SWORD,
                        plugin.getConfig().getString("hotbar.queue.name", "<green>Queue"),
                        plugin.getConfig().getInt("hotbar.queue.model-data", 0),
                        "queue"));

        player.getInventory().setItem(SLOT_KIT_EDITOR,
                buildItem(
                        Material.BOOK,
                        plugin.getConfig().getString("hotbar.kit-editor.name", "<green>Kit Editor"),
                        plugin.getConfig().getInt("hotbar.kit-editor.model-data", 0),
                        "kit_editor"));

        player.getInventory().setItem(SLOT_COSMETICS,
                buildItem(
                        Material.DIAMOND,
                        plugin.getConfig().getString("hotbar.cosmetics.name", "<green>Cosmetics"),
                        plugin.getConfig().getInt("hotbar.cosmetics.model-data", 0),
                        "cosmetics"));

        player.getInventory().setItem(SLOT_SETTINGS,
                buildItem(
                        Material.COMPASS,
                        plugin.getConfig().getString("hotbar.settings.name", "<green>Settings"),
                        plugin.getConfig().getInt("hotbar.settings.model-data", 0),
                        "settings"));
    }

    // -----------------------------------------------------------------------
    // Clear
    // -----------------------------------------------------------------------

    public void clearHotbar(Player player) {
        player.getInventory().clear();
    }

    // -----------------------------------------------------------------------
    // Read action from an ItemStack
    // -----------------------------------------------------------------------

    /**
     * Returns the hotbar action string stored in the item's PersistentDataContainer,
     * or {@code null} if this is not a hotbar item.
     */
    public String getAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    // -----------------------------------------------------------------------
    // Item builder
    // -----------------------------------------------------------------------

    private ItemStack buildItem(Material material, String miniMessageName, int modelData, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Parse MiniMessage component for the display name
            Component displayName = miniMessage.deserialize(miniMessageName);
            meta.displayName(displayName);

            // Custom model data (0 = disabled / use default)
            if (modelData > 0) {
                meta.setCustomModelData(modelData);
            }

            // Mark the item as unbreakable without showing the flag
            meta.setUnbreakable(true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

            // Store the action identifier
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);

            item.setItemMeta(meta);
        }

        return item;
    }
}
