package de.lemonpvp.fastshop.shop;

import de.lemonpvp.fastshop.FastShop;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lädt shop.yml in Kategorien/Items und bietet Nachschlagewerte für Preise.
 */
public final class ShopConfig {

    private static final String PREIS_STAND_SCHLUESSEL = "preis-stand";
    private static final int PREIS_STAND = 3;
    private static final Set<String> NIE_VERKAUFBAR = Set.of("DRAGON_EGG", "ENCHANTED_BOOK", "WRITTEN_BOOK", "FILLED_MAP",
            "KNOWLEDGE_BOOK", "DEBUG_STICK", "BEDROCK", "BARRIER", "LIGHT", "STRUCTURE_BLOCK", "STRUCTURE_VOID", "JIGSAW",
            "COMMAND_BLOCK", "CHAIN_COMMAND_BLOCK", "REPEATING_COMMAND_BLOCK", "COMMAND_BLOCK_MINECART", "END_PORTAL_FRAME",
            "REINFORCED_DEEPSLATE", "PETRIFIED_OAK_SLAB", "BUDDING_AMETHYST", "SPAWNER", "TRIAL_SPAWNER", "VAULT",
            "PLAYER_HEAD", "TEST_BLOCK", "TEST_INSTANCE_BLOCK", "FROGSPAWN", "FARMLAND", "DIRT_PATH");

    private final FastShop plugin;
    private final File file;

    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<Material, ShopItem> byMaterial = new LinkedHashMap<>();
    private Map<Material, WertRechner.Wert> autoWerte = Map.of();
    private boolean katalogUmgestellt;

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
        if (katalogUmstellen(yaml)) {
            yaml = YamlConfiguration.loadConfiguration(file);
        }
        verzauberungenEntfernen(yaml);
        ConfigurationSection cats = yaml.getConfigurationSection("categories");
        if (cats == null) {
            werteBerechnen();
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
            categories.put(id.toLowerCase(Locale.ROOT), new Category(id, icon, name, slot, items, sec.getBoolean("im-menue", true)));
        }
        werteBerechnen();
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
        return new ShopItem(material, buy, sell, name, lore);
    }

    private void verzauberungenEntfernen(YamlConfiguration yaml) {
        ConfigurationSection cats = yaml.getConfigurationSection("categories");
        if (cats == null) {
            return;
        }
        YamlConfiguration standard = null;
        int bereinigt = 0;
        for (String id : cats.getKeys(false)) {
            ConfigurationSection sec = cats.getConfigurationSection(id);
            if (sec == null) {
                continue;
            }
            List<Map<String, Object>> items = new ArrayList<>();
            boolean geaendert = false;
            for (Map<?, ?> raw : sec.getMapList("items")) {
                Map<String, Object> eintrag = new LinkedHashMap<>();
                raw.forEach((schluessel, wert) -> eintrag.put(String.valueOf(schluessel), wert));
                if (eintrag.containsKey("enchants")) {
                    eintrag.remove("enchants");
                    eintrag.remove("name");
                    eintrag.remove("lore");
                    if (standard == null) {
                        standard = standardKatalog();
                    }
                    Map<?, ?> vorlage = vorlage(standard, id, String.valueOf(eintrag.get("material")));
                    if (vorlage != null && vorlage.get("buy") != null && vorlage.get("sell") != null) {
                        eintrag.put("buy", vorlage.get("buy"));
                        eintrag.put("sell", vorlage.get("sell"));
                    }
                    geaendert = true;
                    bereinigt++;
                }
                items.add(eintrag);
            }
            if (geaendert) {
                sec.set("items", items);
            }
        }
        if (bereinigt == 0) {
            return;
        }
        File vorher = new File(plugin.getDataFolder(), "shop-vorher.yml");
        yaml.options().setHeader(standard.options().getHeader());
        yaml.setComments("categories", standard.getComments("categories"));
        try {
            Files.copy(file.toPath(), vorher.toPath(), StandardCopyOption.REPLACE_EXISTING);
            yaml.save(file);
            plugin.getLogger().info("shop.yml: " + bereinigt + " verzauberte Items sind jetzt unverzaubert "
                    + "(die alte Datei liegt als shop-vorher.yml daneben).");
        } catch (IOException e) {
            plugin.getLogger().warning("shop.yml konnte nicht angepasst werden: " + e.getMessage());
        }
    }

    private boolean katalogUmstellen(YamlConfiguration yaml) {
        if (yaml.getConfigurationSection("categories") == null || yaml.getInt(PREIS_STAND_SCHLUESSEL, 1) >= PREIS_STAND) {
            return false;
        }
        YamlConfiguration standard = standardKatalog();
        if (standard.getConfigurationSection("categories") == null) {
            return false;
        }
        File sicherung = new File(plugin.getDataFolder(), "shop-vor-donut.yml");
        try (InputStream ein = plugin.getResource("shop.yml")) {
            if (ein == null) {
                return false;
            }
            if (!sicherung.exists()) {
                Files.copy(file.toPath(), sicherung.toPath());
            }
            Files.copy(ein, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            katalogUmgestellt = true;
            plugin.getLogger().info("shop.yml: neuer Shop wie auf DonutSMP (End, Nether, Gear, Essen) mit den alten Preisen. "
                    + "Verkaufen geht mit jedem Item. Die alte Datei liegt als shop-vor-donut.yml daneben.");
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("shop.yml konnte nicht umgestellt werden: " + e.getMessage());
            return false;
        }
    }

    private void werteBerechnen() {
        Map<Material, Double> fest = new HashMap<>();
        Set<Material> gesperrt = EnumSet.noneOf(Material.class);
        for (Category kategorie : categories.values()) {
            for (ShopItem item : kategorie.items()) {
                Material material = item.material();
                if (fest.containsKey(material) || gesperrt.contains(material)) {
                    continue;
                }
                if (item.sellable()) {
                    fest.put(material, item.sell());
                } else {
                    gesperrt.add(material);
                }
            }
        }
        List<Material> alle = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isLegacy() || material.name().endsWith("AIR")) {
                continue;
            }
            if (nieVerkaufbar(material)) {
                gesperrt.add(material);
            }
            if (istItem(material)) {
                alle.add(material);
            }
        }
        double standard = Math.max(0, plugin.getConfig().getDouble("settings.standard-wert", 0.1));
        double faktor = Math.max(0.1, Math.min(1.0, plugin.getConfig().getDouble("settings.rezept-faktor", 0.9)));
        autoWerte = WertRechner.berechnen(fest, gesperrt, Rezepte.ausDemServer(), alle, standard, faktor);
    }

    private static boolean istItem(Material material) {
        try {
            return material.isItem();
        } catch (RuntimeException unbekannt) {
            return false;
        }
    }

    public static boolean nieVerkaufbar(Material material) {
        String name = material.name();
        return NIE_VERKAUFBAR.contains(name) || name.endsWith("_SPAWN_EGG");
    }

    private YamlConfiguration standardKatalog() {
        try (InputStream ein = plugin.getResource("shop.yml")) {
            if (ein == null) {
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(ein, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new YamlConfiguration();
        }
    }

    private Map<?, ?> vorlage(YamlConfiguration standard, String kategorie, String material) {
        Map<?, ?> treffer = vorlageIn(standard.getConfigurationSection("categories." + kategorie), material);
        if (treffer != null) {
            return treffer;
        }
        ConfigurationSection alle = standard.getConfigurationSection("categories");
        if (alle == null) {
            return null;
        }
        for (String id : alle.getKeys(false)) {
            treffer = vorlageIn(alle.getConfigurationSection(id), material);
            if (treffer != null) {
                return treffer;
            }
        }
        return null;
    }

    private Map<?, ?> vorlageIn(ConfigurationSection sec, String material) {
        if (sec == null) {
            return null;
        }
        for (Map<?, ?> raw : sec.getMapList("items")) {
            if (material.equalsIgnoreCase(String.valueOf(raw.get("material")))) {
                return raw;
            }
        }
        return null;
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

    public double verkaufswert(Material material) {
        ShopItem item = byMaterial.get(material);
        if (item != null) {
            return item.sellable() ? item.sell() : -1;
        }
        if (!plugin.getConfig().getBoolean("settings.alles-verkaufen", true)) {
            return -1;
        }
        WertRechner.Wert wert = autoWerte.get(material);
        return wert == null || wert.wert() <= 0 ? -1 : wert.wert();
    }

    public boolean automatisch(Material material) {
        return byMaterial.get(material) == null && verkaufswert(material) >= 0;
    }

    public boolean inVerkaufsliste(Material material) {
        ShopItem item = byMaterial.get(material);
        return item != null && item.sellable();
    }

    public boolean katalogUmgestellt() {
        return katalogUmgestellt;
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
