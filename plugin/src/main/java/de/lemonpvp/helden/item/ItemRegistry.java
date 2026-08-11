package de.lemonpvp.helden.item;

import de.lemonpvp.helden.config.ConfigFile;
import de.lemonpvp.helden.util.Compat;
import de.lemonpvp.helden.util.Keys;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Laedt und liefert alle Artefakte aus items.yml. */
public final class ItemRegistry {

    private final Plugin plugin;
    private final ConfigFile file;
    private final Map<String, CustomItem> items = new LinkedHashMap<>();

    public ItemRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.file = new ConfigFile(plugin, "items.yml");
    }

    public void reload() {
        items.clear();
        file.reload();

        ConfigurationSection root = file.get().getConfigurationSection("items");
        if (root == null) {
            plugin.getLogger().warning("items.yml enthaelt keinen 'items'-Abschnitt.");
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            Material material = Compat.material(section.getString("material", ""));
            if (material == null) {
                plugin.getLogger().warning("Artefakt '" + id + "' hat ein unbekanntes Material: "
                        + section.getString("material"));
                continue;
            }

            Map<String, Integer> enchantments = new LinkedHashMap<>();
            ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
            if (enchantSection != null) {
                for (String key : enchantSection.getKeys(false)) {
                    enchantments.put(key, Math.max(1, enchantSection.getInt(key, 1)));
                }
            }

            CustomItem item = CustomItem.builder(id, material)
                    .customModelData(section.getInt("custom-model-data", 0))
                    .bedrockIdentifier(section.getString("bedrock-identifier", ""))
                    .display(section.getString("display", id))
                    .lore(section.getStringList("lore"))
                    .unbreakable(section.getBoolean("unbreakable", false))
                    .glow(section.getBoolean("glow", false))
                    .hideFlags(section.getBoolean("hide-flags", false))
                    .enchantments(enchantments)
                    .build();
            items.put(id.toLowerCase(Locale.ROOT), item);
        }

        plugin.getLogger().info("Artefakte geladen: " + items.size());
    }

    public CustomItem get(String id) {
        return id == null ? null : items.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String id) {
        return get(id) != null;
    }

    public Set<String> ids() {
        return new LinkedHashSet<>(items.keySet());
    }

    public Collection<CustomItem> all() {
        return items.values();
    }

    public int size() {
        return items.size();
    }

    /** Liest die Artefakt-ID aus dem PersistentDataContainer eines Items. */
    public static String idOf(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(Keys.itemId(), PersistentDataType.STRING);
    }

    public static boolean is(ItemStack stack, String id) {
        return id != null && id.equalsIgnoreCase(idOf(stack));
    }

    /** Baut ein Artefakt oder {@code null}, wenn die ID unbekannt ist. */
    public ItemStack stack(String id, int amount) {
        CustomItem item = get(id);
        return item == null ? null : item.build(amount);
    }
}
