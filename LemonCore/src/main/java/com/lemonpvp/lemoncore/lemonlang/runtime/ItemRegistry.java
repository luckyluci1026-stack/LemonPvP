package com.lemonpvp.lemoncore.lemonlang.runtime;

import com.lemonpvp.lemoncore.lemonlang.ast.ItemDef;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages LemonLang custom items. Stores built ItemStacks keyed by item name.
 * Items are tagged with a {@link NamespacedKey} in their PersistentDataContainer
 * so they can be identified at runtime.
 */
public final class ItemRegistry {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Logger LOG = Logger.getLogger("LemonLang");
    private static final String PDC_KEY = "lemonlang_item";

    /** name → built ItemStack */
    private final Map<String, ItemStack> items = new ConcurrentHashMap<>();

    /** Registers a custom item, building its ItemStack from the ItemDef. */
    public void register(ItemDef def, Plugin plugin) {
        Map<String, String> props = def.props();

        // Determine material
        String materialStr = props.getOrDefault("material", "PAPER").toUpperCase();
        Material material;
        try {
            material = Material.valueOf(materialStr);
        } catch (IllegalArgumentException e) {
            LOG.warning("[LemonLang] Unbekanntes Material '" + materialStr + "' fuer Item '" + def.name() + "'. Verwende PAPER.");
            material = Material.PAPER;
        }

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            items.put(def.name(), stack);
            return;
        }

        // Display name
        String nameStr = props.get("name");
        if (nameStr != null && !nameStr.isEmpty()) {
            meta.displayName(MM.deserialize("<!italic>" + nameStr));
        }

        // Lore
        if (!def.lore().isEmpty()) {
            List<net.kyori.adventure.text.Component> loreComponents = new ArrayList<>();
            for (String loreLine : def.lore()) {
                loreComponents.add(MM.deserialize("<!italic>" + loreLine));
            }
            meta.lore(loreComponents);
        }

        // Glow effect
        if ("true".equalsIgnoreCase(props.get("glow"))) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        // Tag item with its LemonLang name in PDC
        NamespacedKey key = new NamespacedKey(plugin, PDC_KEY);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, def.name());

        stack.setItemMeta(meta);
        items.put(def.name(), stack);
        LOG.fine("[LemonLang] Item registriert: " + def.name() + " (" + material + ")");
    }

    /** Returns the built ItemStack for the given name, or null if not found. */
    public ItemStack get(String name) {
        ItemStack stack = items.get(name);
        return stack != null ? stack.clone() : null;
    }

    /**
     * Returns true if the given ItemStack is a registered LemonLang item.
     * Checks the PersistentDataContainer for the lemonlang_item key.
     */
    public boolean isLemonItem(ItemStack stack, Plugin plugin) {
        return getLemonName(stack, plugin) != null;
    }

    /**
     * Returns the LemonLang item name stored in the stack's PDC, or null
     * if the item is not a registered LemonLang item.
     */
    public String getLemonName(ItemStack stack, Plugin plugin) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        NamespacedKey key = new NamespacedKey(plugin, PDC_KEY);
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    /** Removes all registered items. Call on reload. */
    public void clear() {
        items.clear();
    }

    /** Returns the number of registered items. */
    public int size() {
        return items.size();
    }
}
