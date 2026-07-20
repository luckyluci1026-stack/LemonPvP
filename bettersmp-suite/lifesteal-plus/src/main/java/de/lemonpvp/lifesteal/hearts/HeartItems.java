package de.lemonpvp.lifesteal.hearts;

import de.lemonpvp.lifesteal.LifestealPlus;
import de.lemonpvp.lifesteal.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Erstellt Herz- und Revive-Items, registriert deren Crafting-Rezepte und
 * erkennt sie wieder (ueber PersistentDataContainer-Tags).
 */
public final class HeartItems {

    private final LifestealPlus plugin;
    private final NamespacedKey heartKey;
    private final NamespacedKey reviveKey;

    public HeartItems(LifestealPlus plugin) {
        this.plugin = plugin;
        this.heartKey = new NamespacedKey(plugin, "heart_item");
        this.reviveKey = new NamespacedKey(plugin, "revive_item");
    }

    private Material material(String path, Material fallback) {
        String name = plugin.getConfig().getString(path, fallback.name());
        Material material = Material.matchMaterial(name);
        return material == null ? fallback : material;
    }

    private Component name(String path, String fallback) {
        return Text.mm(plugin.getConfig().getString(path, fallback))
                .decoration(TextDecoration.ITALIC, false);
    }

    private List<Component> lore(String path) {
        List<Component> lines = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList(path)) {
            lines.add(Text.mm(line).decoration(TextDecoration.ITALIC, false));
        }
        return lines;
    }

    public ItemStack heartItem(int amount) {
        ItemStack stack = new ItemStack(material("heart-item.material", Material.NETHER_STAR), amount);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name("heart-item.name", "&c&l❤ Herz"));
        meta.lore(lore("heart-item.lore"));
        meta.getPersistentDataContainer().set(heartKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack reviveItem(int amount) {
        ItemStack stack = new ItemStack(material("revive-item.material", Material.TOTEM_OF_UNDYING), amount);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name("revive-item.name", "&a&l✚ Wiederbelebungs-Totem"));
        meta.lore(lore("revive-item.lore"));
        meta.getPersistentDataContainer().set(reviveKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isHeart(ItemStack stack) {
        return has(stack, heartKey);
    }

    public boolean isRevive(ItemStack stack) {
        return has(stack, reviveKey);
    }

    private boolean has(ItemStack stack, NamespacedKey key) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer()
                .has(key, PersistentDataType.BYTE);
    }

    public void registerRecipes() {
        if (plugin.getConfig().getBoolean("heart-item.crafting-enabled", true)) {
            register(new NamespacedKey(plugin, "heart_recipe"), heartItem(1),
                    plugin.getConfig().getConfigurationSection("heart-item.recipe"));
        }
        if (plugin.getConfig().getBoolean("revive-item.enabled", true)
                && plugin.getConfig().getBoolean("revive-item.crafting-enabled", true)) {
            register(new NamespacedKey(plugin, "revive_recipe"), reviveItem(1),
                    plugin.getConfig().getConfigurationSection("revive-item.recipe"));
        }
    }

    private void register(NamespacedKey key, ItemStack result, ConfigurationSection recipeSec) {
        if (recipeSec == null) {
            return;
        }
        List<String> shape = recipeSec.getStringList("shape");
        ConfigurationSection ingredients = recipeSec.getConfigurationSection("ingredients");
        if (shape.isEmpty() || ingredients == null) {
            return;
        }
        try {
            if (Bukkit.getRecipe(key) != null) {
                Bukkit.removeRecipe(key);
            }
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape(shape.toArray(new String[0]));
            for (Map.Entry<String, Object> entry : ingredients.getValues(false).entrySet()) {
                if (entry.getKey().length() != 1) {
                    continue;
                }
                Material material = Material.matchMaterial(String.valueOf(entry.getValue()));
                if (material != null) {
                    recipe.setIngredient(entry.getKey().charAt(0), material);
                }
            }
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Rezept " + key.getKey() + " ungueltig: " + e.getMessage());
        }
    }
}
