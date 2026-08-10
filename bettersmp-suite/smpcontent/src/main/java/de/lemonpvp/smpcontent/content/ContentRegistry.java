package de.lemonpvp.smpcontent.content;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lädt alle eigenen Blöcke und Items aus der config.yml, baut die passenden
 * ItemStacks und registriert die Rezepte.
 */
public final class ContentRegistry {

    private final SMPContent plugin;
    private final NamespacedKey idKey;

    private final Map<String, CustomEntry> entries = new LinkedHashMap<>();
    /** Normalisierter Blockzustand -> Block-Id. */
    private final Map<String, String> stateToId = new LinkedHashMap<>();
    private final List<NamespacedKey> registeredRecipes = new ArrayList<>();

    public ContentRegistry(SMPContent plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "content_id");
        load();
    }

    public NamespacedKey idKey() {
        return idKey;
    }

    public Map<String, CustomEntry> entries() {
        return entries;
    }

    public CustomEntry get(String id) {
        return entries.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    // ---------------- Laden ----------------

    public void load() {
        clearRecipes();
        entries.clear();
        stateToId.clear();

        loadSection("blocks", true);
        loadSection("items", false);

        if (plugin.getConfig().getBoolean("recipes-enabled", true)) {
            registerRecipes();
        }
        plugin.getLogger().info("Geladen: " + entries.size() + " eigene Inhalte ("
                + stateToId.size() + " Blöcke).");
    }

    private void loadSection(String path, boolean isBlock) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection(path);
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) {
                continue;
            }
            Material material = isBlock
                    ? Material.NOTE_BLOCK
                    : Material.matchMaterial(sec.getString("material", "STONE"));
            if (material == null) {
                plugin.getLogger().warning("Unbekanntes Material bei " + id + " - übersprungen.");
                continue;
            }
            String state = sec.getString("state");
            if (isBlock) {
                if (state == null) {
                    plugin.getLogger().warning("Block " + id + " hat keinen 'state' - übersprungen.");
                    continue;
                }
                String normalized = normalize(state);
                if (normalized == null) {
                    plugin.getLogger().warning("Ungültiger 'state' bei " + id + " - übersprungen.");
                    continue;
                }
                state = normalized;
                stateToId.put(state, id);
            }
            Map<String, Double> attributes = new LinkedHashMap<>();
            ConfigurationSection attrSec = sec.getConfigurationSection("attributes");
            if (attrSec != null) {
                for (String key : attrSec.getKeys(false)) {
                    attributes.put(key.toLowerCase(java.util.Locale.ROOT), attrSec.getDouble(key));
                }
            }
            Map<String, Integer> enchants = new LinkedHashMap<>();
            ConfigurationSection enchSec = sec.getConfigurationSection("enchants");
            if (enchSec != null) {
                for (String key : enchSec.getKeys(false)) {
                    enchants.put(key.toLowerCase(java.util.Locale.ROOT), enchSec.getInt(key));
                }
            }
            entries.put(id.toLowerCase(java.util.Locale.ROOT), new CustomEntry(
                    id, isBlock, material,
                    sec.getString("name", id),
                    sec.getStringList("lore"),
                    sec.getInt("model-data", 0),
                    state,
                    attributes,
                    sec.getInt("durability", 0),
                    sec.getBoolean("unbreakable", false),
                    enchants,
                    sec.getBoolean("glow", false)));
        }
    }

    /** Bringt einen Blockzustand in die Schreibweise, die auch Bukkit liefert. */
    private String normalize(String state) {
        try {
            return Bukkit.createBlockData(state).getAsString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---------------- Items bauen ----------------

    public ItemStack create(CustomEntry entry, int amount) {
        ItemStack stack = new ItemStack(entry.material(), Math.max(1, Math.min(64, amount)));
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Text.mm(entry.name()).decoration(TextDecoration.ITALIC, false));
        if (!entry.lore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : entry.lore()) {
                lore.add(Text.mm(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        }
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, entry.id());

        // Java: item_model aus dem Texturepack. Bedrock: CustomModelData (Geyser).
        String namespace = plugin.getConfig().getString("texturepack.namespace", "smp");
        try {
            meta.setItemModel(new NamespacedKey(namespace, entry.id()));
            if (entry.modelData() > 0) {
                meta.setCustomModelData(entry.modelData());
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Textur für " + entry.id() + " nicht setzbar: " + t.getMessage());
        }
        applyStats(meta, entry);
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Setzt Werte für Waffen und Werkzeuge: Angriffsschaden, Angriffstempo,
     * Haltbarkeit, Verzauberungen und den Leucht-Effekt.
     */
    private void applyStats(ItemMeta meta, CustomEntry entry) {
        for (Map.Entry<String, Double> attr : entry.attributes().entrySet()) {
            Attribute attribute = attributeByName(attr.getKey());
            if (attribute == null) {
                plugin.getLogger().warning("Unbekanntes Attribut '" + attr.getKey()
                        + "' bei " + entry.id());
                continue;
            }
            NamespacedKey key = new NamespacedKey(plugin, entry.id() + "_" + attr.getKey());
            meta.addAttributeModifier(attribute, new AttributeModifier(
                    key, attr.getValue(), AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND));
        }
        if (entry.maxDamage() > 0 && meta instanceof Damageable damageable) {
            damageable.setMaxDamage(entry.maxDamage());
        }
        if (entry.unbreakable()) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        for (Map.Entry<String, Integer> ench : entry.enchants().entrySet()) {
            Enchantment enchantment = Registry.ENCHANTMENT.get(
                    NamespacedKey.minecraft(ench.getKey()));
            if (enchantment == null) {
                plugin.getLogger().warning("Unbekannte Verzauberung '" + ench.getKey()
                        + "' bei " + entry.id());
                continue;
            }
            meta.addEnchant(enchantment, Math.max(1, ench.getValue()), true);
        }
        if (entry.glow() && entry.enchants().isEmpty()) {
            meta.setEnchantmentGlintOverride(true);
        }
    }

    /** Findet ein Attribut über seinen Vanilla-Namen (z.B. "attack_damage"). */
    private Attribute attributeByName(String name) {
        return Registry.ATTRIBUTE.get(NamespacedKey.minecraft(name));
    }

    /** Liest die Content-Id aus einem Item (oder null). */
    public String idOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        return stack.getItemMeta().getPersistentDataContainer()
                .get(idKey, PersistentDataType.STRING);
    }

    /** Findet den eigenen Block zu einem Blockzustand (oder null). */
    public CustomEntry blockAt(BlockData data) {
        if (data.getMaterial() != Material.NOTE_BLOCK) {
            return null;
        }
        String id = stateToId.get(data.getAsString());
        return id == null ? null : get(id);
    }

    // ---------------- Rezepte ----------------

    private void clearRecipes() {
        for (NamespacedKey key : registeredRecipes) {
            Bukkit.removeRecipe(key);
        }
        registeredRecipes.clear();
    }

    private void registerRecipes() {
        for (CustomEntry entry : entries.values()) {
            ConfigurationSection sec = plugin.getConfig().getConfigurationSection(
                    (entry.block() ? "blocks." : "items.") + entry.id() + ".recipe");
            if (sec == null) {
                continue;
            }
            List<String> shape = sec.getStringList("shape");
            ConfigurationSection keys = sec.getConfigurationSection("keys");
            if (shape.isEmpty() || keys == null) {
                continue;
            }
            try {
                NamespacedKey key = new NamespacedKey(plugin, "recipe_" + entry.id());
                Bukkit.removeRecipe(key);
                ShapedRecipe recipe = new ShapedRecipe(key,
                        create(entry, sec.getInt("amount", 1)));
                recipe.shape(shape.toArray(new String[0]));

                for (String k : keys.getKeys(false)) {
                    if (k.length() != 1) {
                        continue;
                    }
                    char c = k.charAt(0);
                    String value = String.valueOf(keys.get(k));
                    if (value.toLowerCase(java.util.Locale.ROOT).startsWith("smp:")) {
                        CustomEntry ingredient = get(value.substring(4));
                        if (ingredient == null) {
                            throw new IllegalStateException("Unbekannte Zutat " + value);
                        }
                        recipe.setIngredient(c, new RecipeChoice.ExactChoice(create(ingredient, 1)));
                    } else {
                        Material material = Material.matchMaterial(value);
                        if (material == null) {
                            throw new IllegalStateException("Unbekanntes Material " + value);
                        }
                        recipe.setIngredient(c, material);
                    }
                }
                Bukkit.addRecipe(recipe);
                registeredRecipes.add(key);
            } catch (Exception e) {
                plugin.getLogger().warning("Rezept für " + entry.id()
                        + " ungültig: " + e.getMessage());
            }
        }
    }
}
