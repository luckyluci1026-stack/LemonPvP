package de.lemonpvp.fastshop.shop;

import de.lemonpvp.fastshop.FastShop;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lädt shop.yml in Kategorien/Items und bietet Nachschlagewerte für Preise.
 */
public final class ShopConfig {

    private final FastShop plugin;
    private final File file;

    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<Material, ShopItem> byMaterial = new LinkedHashMap<>();

    public ShopConfig(FastShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shop.yml");
        if (!file.exists()) {
            plugin.saveResource("shop.yml", false);
        }
        load();
    }

    public void load() {
        categories.clear();
        byMaterial.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection cats = yaml.getConfigurationSection("categories");
        if (cats == null) {
            return;
        }
        for (String id : cats.getKeys(false)) {
            ConfigurationSection sec = cats.getConfigurationSection(id);
            if (sec == null) {
                continue;
            }
            Material icon = matOr(sec.getString("icon"), Material.CHEST);
            String name = sec.getString("name", id);
            int slot = sec.getInt("slot", categories.size());
            List<ShopItem> items = new ArrayList<>();
            for (Map<?, ?> raw : sec.getMapList("items")) {
                ShopItem item = parseItem(raw);
                if (item != null) {
                    items.add(item);
                    byMaterial.putIfAbsent(item.material(), item);
                }
            }
            categories.put(id.toLowerCase(Locale.ROOT), new Category(id, icon, name, slot, items));
        }
    }

    private ShopItem parseItem(Map<?, ?> raw) {
        Object matName = raw.get("material");
        if (matName == null) {
            return null;
        }
        Material material = Material.matchMaterial(String.valueOf(matName));
        if (material == null) {
            plugin.getLogger().warning("Unbekanntes Material im Shop: " + matName);
            return null;
        }
        double buy = toDouble(raw.get("buy"), -1);
        double sell = toDouble(raw.get("sell"), -1);
        String name = raw.get("name") == null ? null : String.valueOf(raw.get("name"));
        List<String> lore = new ArrayList<>();
        if (raw.get("lore") instanceof List<?> list) {
            for (Object line : list) {
                lore.add(String.valueOf(line));
            }
        }
        Map<Enchantment, Integer> enchants = new LinkedHashMap<>();
        if (raw.get("enchants") instanceof Map<?, ?> enchMap) {
            for (Map.Entry<?, ?> e : enchMap.entrySet()) {
                String key = String.valueOf(e.getKey()).toLowerCase(Locale.ROOT);
                Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
                if (enchantment == null) {
                    plugin.getLogger().warning("Unbekannte Verzauberung '" + key + "' bei " + matName);
                    continue;
                }
                enchants.put(enchantment, Math.max(1, (int) toDouble(e.getValue(), 1)));
            }
        }
        return new ShopItem(material, buy, sell, name, lore, enchants);
    }

    private double toDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Material matOr(String name, Material fallback) {
        if (name == null) {
            return fallback;
        }
        Material material = Material.matchMaterial(name);
        return material == null ? fallback : material;
    }

    public double sellMultiplier() {
        return plugin.getConfig().getDouble("settings.sell-multiplier", 1.0);
    }

    public Map<String, Category> categories() {
        return categories;
    }

    public Category category(String id) {
        return categories.get(id.toLowerCase(Locale.ROOT));
    }

    public ShopItem item(Material material) {
        return byMaterial.get(material);
    }

    /** Setzt Kauf-/Verkaufspreis eines Items in einer Kategorie. field = "buy" oder "sell". */
    public boolean setPrice(String categoryId, Material material, String field, double value) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yaml.getConfigurationSection("categories." + categoryId);
        if (sec == null) {
            return false;
        }
        List<Map<?, ?>> items = sec.getMapList("items");
        boolean changed = false;
        List<Map<?, ?>> updated = new ArrayList<>();
        for (Map<?, ?> raw : items) {
            Map<String, Object> entry = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : raw.entrySet()) {
                entry.put(String.valueOf(e.getKey()), e.getValue());
            }
            if (material.name().equalsIgnoreCase(String.valueOf(entry.get("material")))) {
                entry.put(field, value);
                changed = true;
            }
            updated.add(entry);
        }
        if (!changed) {
            return false;
        }
        sec.set("items", updated);
        return saveReload(yaml);
    }

    /** Entfernt ein Item aus einer Kategorie. */
    public boolean removeItem(String categoryId, Material material) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yaml.getConfigurationSection("categories." + categoryId);
        if (sec == null) {
            return false;
        }
        List<Map<?, ?>> items = sec.getMapList("items");
        List<Map<?, ?>> kept = new ArrayList<>();
        boolean removed = false;
        for (Map<?, ?> raw : items) {
            if (material.name().equalsIgnoreCase(String.valueOf(raw.get("material")))) {
                removed = true;
            } else {
                kept.add(raw);
            }
        }
        if (!removed) {
            return false;
        }
        sec.set("items", kept);
        return saveReload(yaml);
    }

    /** Legt eine neue, leere Kategorie an. */
    public boolean addCategory(String id, Material icon, int slot, String name) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.isConfigurationSection("categories." + id)) {
            return false;
        }
        yaml.set("categories." + id + ".icon", icon.name());
        yaml.set("categories." + id + ".name", name);
        yaml.set("categories." + id + ".slot", slot);
        yaml.set("categories." + id + ".items", new ArrayList<>());
        return saveReload(yaml);
    }

    /** Entfernt eine ganze Kategorie. */
    public boolean removeCategory(String id) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.isConfigurationSection("categories." + id)) {
            return false;
        }
        yaml.set("categories." + id, null);
        return saveReload(yaml);
    }

    private boolean saveReload(YamlConfiguration yaml) {
        try {
            yaml.save(file);
            load();
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Konnte shop.yml nicht speichern: " + e.getMessage());
            return false;
        }
    }

    /** Fügt ein Item einer bestehenden Kategorie hinzu und speichert shop.yml. */
    public boolean addItem(String categoryId, Material material, double buy, double sell) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection cats = yaml.getConfigurationSection("categories");
        if (cats == null || !cats.isConfigurationSection(categoryId)) {
            return false;
        }
        ConfigurationSection sec = cats.getConfigurationSection(categoryId);
        List<Map<?, ?>> items = sec.getMapList("items");
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("material", material.name());
        entry.put("buy", buy);
        entry.put("sell", sell);
        List<Map<?, ?>> updated = new ArrayList<>(items);
        updated.add(entry);
        sec.set("items", updated);
        try {
            yaml.save(file);
            load();
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Konnte shop.yml nicht speichern: " + e.getMessage());
            return false;
        }
    }
}
