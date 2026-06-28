package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class LobbyHotbarManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;
    private final NamespacedKey actionKey;

    public LobbyHotbarManager(LemonPractice plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "hotbar_action");
    }

    public void setupHotbar(Player player) {
        player.getInventory().clear();
        // Queue sword removed from this (duel/practice) server — queueing is done
        // from the main lobby. Re-enable by setting hotbar.queue.enabled: true.
        if (plugin.getConfig().getBoolean("hotbar.queue.enabled", false)) {
            placeItem(player, "queue", "queue", Material.IRON_SWORD);
        }
        placeItem(player, "kit-editor", "kit_editor", Material.BOOK);
        placeItem(player, "cosmetics",  "cosmetics",  Material.DIAMOND);
        placeItem(player, "settings",   "settings",   Material.COMPASS);
    }

    public void clearHotbar(Player player) {
        player.getInventory().clear();
    }

    public String getAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    // -------------------------------------------------------------------------

    private void placeItem(Player player, String configKey, String action, Material fallbackMaterial) {
        String path = "hotbar." + configKey;

        int slot = plugin.getConfig().getInt(path + ".slot", defaultSlot(configKey));

        String rawName = plugin.getConfig().getString(path + ".name", "<!italic><white>" + configKey);
        // Ensure name is never italic — prepend if not already present
        if (!rawName.contains("<!italic>") && !rawName.contains("<italic:false>")) {
            rawName = "<!italic>" + rawName;
        }

        String matName = plugin.getConfig().getString(path + ".material", fallbackMaterial.name());
        Material material;
        try {
            material = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = fallbackMaterial;
        }

        int modelData = plugin.getConfig().getInt(path + ".model-data", 0);

        player.getInventory().setItem(slot, buildItem(material, rawName, modelData, action));
    }

    private int defaultSlot(String key) {
        return switch (key) {
            case "queue"      -> 0;
            case "kit-editor" -> 4;
            case "cosmetics"  -> 7;
            case "settings"   -> 8;
            default           -> 0;
        };
    }

    private ItemStack buildItem(Material material, String miniMessageName, int modelData, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        Component displayName = MM.deserialize(miniMessageName);
        meta.displayName(displayName);

        if (modelData > 0) meta.setCustomModelData(modelData);

        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);

        item.setItemMeta(meta);
        return item;
    }
}
