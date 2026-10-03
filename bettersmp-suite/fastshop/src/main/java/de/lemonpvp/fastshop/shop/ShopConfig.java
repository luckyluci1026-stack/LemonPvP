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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Lädt shop.yml in Kategorien/Items und bietet Nachschlagewerte für Preise.
 */
public final class ShopConfig {

    private static final String PREIS_STAND_SCHLUESSEL = "preis-stand";
    private static final int PREIS_STAND = 2;
    private static final double KAUF_FAKTOR = 1.5;
    private static final double VERKAUF_FAKTOR = 0.25;

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
        verzauberungenEntfernen(yaml);
        preiseUmstellen(yaml);
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

    private void preiseUmstellen(YamlConfiguration yaml) {
        ConfigurationSection cats = yaml.getConfigurationSection("categories");
        if (cats == null || yaml.getInt(PREIS_STAND_SCHLUESSEL, 1) >= PREIS_STAND) {
            return;
        }
        YamlConfiguration standard = standardKatalog();
        if (standard.getConfigurationSection("categories") == null) {
            return;
        }
        List<String> eigene = new ArrayList<>();
        int bekannt = 0;
        for (String id : cats.getKeys(false)) {
            ConfigurationSection sec = cats.getConfigurationSection(id);
            if (sec == null) {
                continue;
            }
            List<Map<String, Object>> items = new ArrayList<>();
            for (Map<?, ?> raw : sec.getMapList("items")) {
                Map<String, Object> eintrag = new LinkedHashMap<>();
                raw.forEach((schluessel, wert) -> eintrag.put(String.valueOf(schluessel), wert));
                String material = String.valueOf(eintrag.get("material"));
                Map<?, ?> vorlage = vorlage(standard, id, material);
                if (vorlage != null && vorlage.get("buy") != null && vorlage.get("sell") != null) {
                    eintrag.put("buy", vorlage.get("buy"));
                    eintrag.put("sell", vorlage.get("sell"));
                    bekannt++;
                } else {
                    String vorher = eintrag.get("buy") + "/" + eintrag.get("sell");
                    pauschalAnpassen(eintrag, "buy", KAUF_FAKTOR);
                    pauschalAnpassen(eintrag, "sell", VERKAUF_FAKTOR);
                    eigene.add(material + " " + vorher + " -> " + eintrag.get("buy") + "/" + eintrag.get("sell"));
                }
                items.add(eintrag);
            }
            sec.set("items", items);
        }
        yaml.set(PREIS_STAND_SCHLUESSEL, PREIS_STAND);
        File sicherung = new File(plugin.getDataFolder(), "shop-vor-nerf.yml");
        try {
            if (!sicherung.exists()) {
                Files.copy(file.toPath(), sicherung.toPath());
            }
            if (wieStandard(yaml, standard)) {
                try (InputStream ein = plugin.getResource("shop.yml")) {
                    Files.copy(ein, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                yaml.options().setHeader(standard.options().getHeader());
                yaml.setComments("categories", standard.getComments("categories"));
                yaml.setComments(PREIS_STAND_SCHLUESSEL, standard.getComments(PREIS_STAND_SCHLUESSEL));
                yaml.save(file);
            }
            plugin.getLogger().info("shop.yml: Preise generft (" + bekannt + " Items auf die neuen Preise, "
                    + eigene.size() + " eigene Items pauschal: Kaufen x" + KAUF_FAKTOR + ", Verkaufen x"
                    + VERKAUF_FAKTOR + "). Die alte Datei liegt als shop-vor-nerf.yml daneben.");
            for (String zeile : eigene) {
                plugin.getLogger().info("shop.yml: eigenes Item " + zeile);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("shop.yml konnte nicht umgestellt werden: " + e.getMessage());
        }
    }

    private void pauschalAnpassen(Map<String, Object> eintrag, String feld, double faktor) {
        double alt = toDouble(eintrag.get(feld), -1);
        if (alt > 0) {
            eintrag.put(feld, gerundet(alt * faktor));
        }
    }

    private static Number gerundet(double wert) {
        if (wert >= 10) {
            return Math.round(wert);
        }
        double cent = Math.max(0.01, Math.round(wert * 100) / 100.0);
        return cent == Math.rint(cent) ? (Number) Math.round(cent) : (Number) cent;
    }

    private boolean wieStandard(YamlConfiguration yaml, YamlConfiguration standard) {
        ConfigurationSection eigene = yaml.getConfigurationSection("categories");
        ConfigurationSection vorgabe = standard.getConfigurationSection("categories");
        if (eigene == null || vorgabe == null || !yaml.getKeys(false).equals(standard.getKeys(false))
                || !eigene.getKeys(false).equals(vorgabe.getKeys(false))) {
            return false;
        }
        for (String id : vorgabe.getKeys(false)) {
            ConfigurationSection a = eigene.getConfigurationSection(id);
            ConfigurationSection b = vorgabe.getConfigurationSection(id);
            if (a == null || b == null || !a.getKeys(false).equals(b.getKeys(false))) {
                return false;
            }
            for (String schluessel : b.getKeys(false)) {
                if (!schluessel.equals("items") && !String.valueOf(a.get(schluessel)).equals(String.valueOf(b.get(schluessel)))) {
                    return false;
                }
            }
            if (!eintraege(a).equals(eintraege(b))) {
                return false;
            }
        }
        return true;
    }

    private List<Map<String, String>> eintraege(ConfigurationSection sec) {
        List<Map<String, String>> liste = new ArrayList<>();
        for (Map<?, ?> raw : sec.getMapList("items")) {
            Map<String, String> eintrag = new TreeMap<>();
            raw.forEach((schluessel, wert) -> {
                String name = String.valueOf(schluessel);
                boolean preis = name.equals("buy") || name.equals("sell");
                eintrag.put(name, preis ? String.valueOf(toDouble(wert, -1)) : String.valueOf(wert));
            });
            liste.add(eintrag);
        }
        return liste;
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
