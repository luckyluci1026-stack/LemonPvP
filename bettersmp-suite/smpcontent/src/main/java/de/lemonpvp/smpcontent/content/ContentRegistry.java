package de.lemonpvp.smpcontent.content;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.util.ConfigProblem;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
    /** Id -> Rezept-Abschnitt (auch aus den Zusatzdateien). */
    private final Map<String, ConfigurationSection> recipeSections = new LinkedHashMap<>();
    /** Id -> fertiger Blockzustand, damit er nicht ständig neu geparst wird. */
    private final Map<String, BlockData> blockData = new LinkedHashMap<>();
    /** Id -> fertiges Item als Vorlage, wird nur noch kopiert. */
    private final Map<String, ItemStack> prototypes = new LinkedHashMap<>();
    private final List<NamespacedKey> registeredRecipes = new ArrayList<>();
    private final List<ConfigProblem.Report> problems = new ArrayList<>();

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
        recipeSections.clear();
        blockData.clear();
        prototypes.clear();
        problems.clear();
        plugin.abilities().clear();

        // 1. Die Hauptdatei
        loadFrom(plugin.getConfig(), "config.yml");

        // 2. Alle Zusatzdateien aus content/ - so bleiben eigene Sachen
        //    übersichtlich getrennt und die config.yml kurz.
        for (Path file : contentFiles()) {
            ConfigProblem.Result result = ConfigProblem.load(file.toFile());
            if (!result.ok()) {
                ConfigProblem.log(plugin.getLogger(), result.problem());
                problems.add(result.problem());
                continue;
            }
            loadFrom(result.config(), file.getFileName().toString());
        }

        loadAnimations();

        if (plugin.getConfig().getBoolean("recipes-enabled", true)) {
            registerRecipes();
        }
        plugin.getLogger().info("Geladen: " + entries.size() + " eigene Inhalte ("
                + stateToId.size() + " Blöcke, " + plugin.abilities().count()
                + " Fähigkeiten, " + plugin.abilities().animationCount() + " eigene Animationen).");
    }

    /** Alle *.yml aus plugins/SMPContent/content/, nach Namen sortiert. */
    private List<Path> contentFiles() {
        Path dir = plugin.contentDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString()
                            .toLowerCase(java.util.Locale.ROOT).endsWith(".yml"))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            plugin.getLogger().warning("content/ nicht lesbar: " + ex.getMessage());
            return List.of();
        }
    }

    /** Liest die eigenen Animationen aus der animationen.yml. */
    private void loadAnimations() {
        Path file = plugin.getDataFolder().toPath().resolve("animationen.yml");
        if (!Files.exists(file)) {
            return;
        }
        ConfigProblem.Result result = ConfigProblem.load(file.toFile());
        if (!result.ok()) {
            ConfigProblem.log(plugin.getLogger(), result.problem());
            problems.add(result.problem());
            return;
        }
        ConfigurationSection root = result.config().getConfigurationSection("animations");
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            Map<String, Object> values = root.getConfigurationSection(name) == null
                    ? Map.of()
                    : root.getConfigurationSection(name).getValues(false);
            plugin.abilities().registerAnimation(name, values);
        }
    }

    /** Fehler aus content/ und animationen.yml (leer = alles in Ordnung). */
    public List<ConfigProblem.Report> problems() {
        return problems;
    }

    private void loadFrom(ConfigurationSection config, String source) {
        loadSection(config, "blocks", true, source);
        loadSection(config, "items", false, source);
    }

    private void loadSection(ConfigurationSection config, String path, boolean isBlock,
                             String source) {
        ConfigurationSection root = config.getConfigurationSection(path);
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) {
                continue;
            }
            if (entries.containsKey(id.toLowerCase(java.util.Locale.ROOT))) {
                plugin.getLogger().warning("'" + id + "' aus " + source
                        + " gibt es schon - übersprungen.");
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
                String taken = stateToId.get(state);
                if (taken != null) {
                    plugin.getLogger().warning("Block " + id + " aus " + source
                            + " benutzt denselben 'state' wie " + taken + " - übersprungen.");
                    continue;
                }
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
                    sec.getBoolean("glow", false),
                    readDrops(sec, id),
                    sec.getInt("experience", 0),
                    new CustomEntry.Extras(
                            sec.getString("slot", ""),
                            sec.getString("rarity", ""),
                            sec.getInt("max-stack", 0),
                            sec.getString("requires-tool", ""),
                            sec.getString("place-sound", ""),
                            sec.getString("break-sound", ""))));

            ConfigurationSection recipe = sec.getConfigurationSection("recipe");
            if (recipe != null) {
                recipeSections.put(id.toLowerCase(java.util.Locale.ROOT), recipe);
            }
            plugin.abilities().register(id, sec.getList("abilities"));
        }
    }

    /**
     * Liest den Abschnitt "drops" eines Blocks.
     *
     * <pre>
     * drops:
     *   - item: "smp:ruby"
     *     amount: 1-3        # oder einfach 2
     *     chance: 1.0
     * </pre>
     *
     * Ohne "drops" fällt weiterhin der Block selbst.
     */
    private List<CustomEntry.Drop> readDrops(ConfigurationSection sec, String id) {
        List<?> raw = sec.getList("drops");
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<CustomEntry.Drop> out = new ArrayList<>();
        for (Object element : raw) {
            if (!(element instanceof Map<?, ?> map)) {
                continue;
            }
            Object item = map.get("item");
            if (item == null) {
                plugin.getLogger().warning("Drop bei " + id + " hat kein 'item' - übersprungen.");
                continue;
            }
            int min = 1;
            int max = 1;
            Object amount = map.get("amount");
            if (amount instanceof Number number) {
                min = max = number.intValue();
            } else if (amount != null) {
                String text = String.valueOf(amount).trim();
                int dash = text.indexOf('-', 1);
                try {
                    if (dash > 0) {
                        min = Integer.parseInt(text.substring(0, dash).trim());
                        max = Integer.parseInt(text.substring(dash + 1).trim());
                    } else {
                        min = max = Integer.parseInt(text);
                    }
                } catch (NumberFormatException ex) {
                    plugin.getLogger().warning("Drop-Menge '" + text + "' bei " + id
                            + " ist keine Zahl - nehme 1.");
                }
            }
            min = Math.max(0, min);
            max = Math.max(min, max);
            double chance = 1.0;
            Object rawChance = map.get("chance");
            if (rawChance instanceof Number number) {
                chance = number.doubleValue();
            }
            boolean fortune = !(map.get("fortune") instanceof Boolean flag) || flag;
            out.add(new CustomEntry.Drop(String.valueOf(item), min, max,
                    Math.max(0, Math.min(1, chance)), fortune));
        }
        return out;
    }

    /**
     * Baut die tatsächlichen Drops eines Blocks - mit Zufall, Glück und
     * Behutsamkeit. Behutsamkeit gibt immer den Block selbst.
     */
    public List<ItemStack> rollDrops(CustomEntry entry, int fortuneLevel, boolean silkTouch) {
        if (silkTouch || entry.drops().isEmpty()) {
            return List.of(create(entry, 1));
        }
        List<ItemStack> out = new ArrayList<>();
        for (CustomEntry.Drop drop : entry.drops()) {
            if (drop.chance() < 1.0 && Math.random() > drop.chance()) {
                continue;
            }
            int amount = drop.min() >= drop.max()
                    ? drop.min()
                    : drop.min() + (int) (Math.random() * (drop.max() - drop.min() + 1));
            if (drop.fortune() && fortuneLevel > 0) {
                // Wie bei Vanilla-Erzen: 0 bis Stufe zusätzliche Züge
                amount *= 1 + (int) (Math.random() * (fortuneLevel + 1));
            }
            if (amount <= 0) {
                continue;
            }
            ItemStack stack = stackFor(drop.item(), amount);
            if (stack != null) {
                out.add(stack);
            } else {
                plugin.getLogger().warning("Drop '" + drop.item() + "' bei " + entry.id()
                        + " ist weder ein Material noch ein eigenes Item.");
            }
        }
        return out;
    }

    /** "smp:ruby" oder "DIAMOND" zu einem echten ItemStack. */
    private ItemStack stackFor(String value, int amount) {
        if (value.toLowerCase(java.util.Locale.ROOT).startsWith("smp:")) {
            CustomEntry other = get(value.substring(4));
            return other == null ? null : create(other, amount);
        }
        Material material = Material.matchMaterial(value);
        return material == null ? null : new ItemStack(material, Math.max(1, Math.min(64, amount)));
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

    /**
     * Ein fertiges Item.
     *
     * Die Vorlage wird einmal gebaut und danach nur noch kopiert - MiniMessage
     * und die Attribut-Berechnung liefen vorher bei jedem Aufruf neu, und das
     * GUI baut pro Seite 45 Stück.
     */
    public ItemStack create(CustomEntry entry, int amount) {
        ItemStack prototype = prototypes.computeIfAbsent(
                entry.id().toLowerCase(java.util.Locale.ROOT), id -> build(entry));
        ItemStack stack = prototype.clone();
        stack.setAmount(Math.max(1, Math.min(stack.getMaxStackSize(), amount)));
        return stack;
    }

    private ItemStack build(CustomEntry entry) {
        ItemStack stack = new ItemStack(entry.material(), 1);
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
        //
        // Geyser erkennt eigene Items ausschliesslich an der CustomModelData -
        // item_model allein sieht ein Bedrock-Spieler nicht. Darum wird sie
        // immer gesetzt; fehlt sie in der Config, wird eine aus der Id
        // abgeleitet, damit das Item auf Bedrock trotzdem ankommt.
        String namespace = plugin.getConfig().getString("texturepack.namespace", "smp");
        try {
            meta.setItemModel(new NamespacedKey(namespace, entry.id()));
            meta.setCustomModelData(entry.modelData() > 0
                    ? entry.modelData()
                    : fallbackModelData(entry.id()));
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
        EquipmentSlotGroup slot = slotFor(entry);
        for (Map.Entry<String, Double> attr : entry.attributes().entrySet()) {
            Attribute attribute = attributeByName(attr.getKey());
            if (attribute == null) {
                plugin.getLogger().warning("Unbekanntes Attribut '" + attr.getKey()
                        + "' bei " + entry.id());
                continue;
            }
            NamespacedKey key = new NamespacedKey(plugin, entry.id() + "_" + attr.getKey());
            meta.addAttributeModifier(attribute, new AttributeModifier(
                    key, attr.getValue(), AttributeModifier.Operation.ADD_NUMBER, slot));
        }
        applyExtras(meta, entry);
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

    /**
     * Wo die Attribute wirken sollen.
     *
     * Vorher stand hier immer MAINHAND - dadurch hat "armor: 3" auf einem Helm
     * nur gezählt, solange man ihn in der Hand hielt. Jetzt wird der Platz am
     * Material erkannt; "slot:" in der Config schlägt das.
     */
    private EquipmentSlotGroup slotFor(CustomEntry entry) {
        String wanted = entry.extras().slot().toLowerCase(java.util.Locale.ROOT).trim();
        if (!wanted.isEmpty()) {
            EquipmentSlotGroup group = switch (wanted) {
                case "head", "kopf", "helm" -> EquipmentSlotGroup.HEAD;
                case "chest", "brust" -> EquipmentSlotGroup.CHEST;
                case "legs", "beine", "hose" -> EquipmentSlotGroup.LEGS;
                case "feet", "fuesse", "schuhe" -> EquipmentSlotGroup.FEET;
                case "armor", "ruestung" -> EquipmentSlotGroup.ARMOR;
                case "offhand", "nebenhand" -> EquipmentSlotGroup.OFFHAND;
                case "hand" -> EquipmentSlotGroup.HAND;
                case "any", "alles", "immer" -> EquipmentSlotGroup.ANY;
                case "mainhand", "haupthand" -> EquipmentSlotGroup.MAINHAND;
                default -> null;
            };
            if (group != null) {
                return group;
            }
            plugin.getLogger().warning("Unbekannter 'slot' bei " + entry.id() + ": " + wanted);
        }
        String material = entry.material().name();
        if (material.endsWith("_HELMET") || material.equals("CARVED_PUMPKIN")) {
            return EquipmentSlotGroup.HEAD;
        }
        if (material.endsWith("_CHESTPLATE") || material.equals("ELYTRA")) {
            return EquipmentSlotGroup.CHEST;
        }
        if (material.endsWith("_LEGGINGS")) {
            return EquipmentSlotGroup.LEGS;
        }
        if (material.endsWith("_BOOTS")) {
            return EquipmentSlotGroup.FEET;
        }
        return EquipmentSlotGroup.MAINHAND;
    }

    /** Seltenheit, Stapelgröße und andere Kleinigkeiten. */
    private void applyExtras(ItemMeta meta, CustomEntry entry) {
        String rarity = entry.extras().rarity().toUpperCase(java.util.Locale.ROOT).trim();
        if (!rarity.isEmpty()) {
            try {
                meta.setRarity(org.bukkit.inventory.ItemRarity.valueOf(rarity));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unbekannte 'rarity' bei " + entry.id() + ": "
                        + rarity + " (erlaubt: COMMON, UNCOMMON, RARE, EPIC)");
            }
        }
        int maxStack = entry.extras().maxStack();
        if (maxStack > 0) {
            meta.setMaxStackSize(Math.max(1, Math.min(99, maxStack)));
        }
    }

    /** Findet ein Attribut über seinen Vanilla-Namen (z.B. "attack_damage"). */
    private Attribute attributeByName(String name) {
        return Registry.ATTRIBUTE.get(NamespacedKey.minecraft(name));
    }

    /**
     * Eine feste Ersatz-CustomModelData für Items ohne eigene Nummer.
     *
     * Bleibt über Neustarts gleich (hängt nur an der Id) und liegt weit über
     * den von Hand vergebenen Nummern, damit sie nicht kollidiert.
     */
    private int fallbackModelData(String id) {
        return 900000 + Math.floorMod(id.toLowerCase(java.util.Locale.ROOT).hashCode(), 90000);
    }

    /**
     * Alle Items mit ihrer CustomModelData - für den Bedrock-Export.
     * Enthält auch die abgeleiteten Nummern, damit die Geyser-Zuordnung
     * wirklich jedes Item erwischt.
     */
    public Map<String, Integer> modelDataByItem() {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (CustomEntry entry : entries.values()) {
            if (!entry.block()) {
                out.put(entry.id(), entry.modelData() > 0
                        ? entry.modelData() : fallbackModelData(entry.id()));
            }
        }
        return out;
    }

    /** Gibt einem Spieler ein Item - was nicht passt, fällt vor seine Füße. */
    public void give(org.bukkit.entity.Player player, CustomEntry entry, int amount) {
        player.getInventory().addItem(create(entry, amount)).values()
                .forEach(rest -> player.getWorld().dropItem(player.getLocation(), rest));
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

    /**
     * Der fertige Blockzustand eines eigenen Blocks.
     * Wird einmal beim Laden gebaut - Bukkit.createBlockData() ist teuer und
     * lief vorher bei jedem einzelnen Setzen eines Blocks neu.
     */
    public BlockData blockDataFor(CustomEntry entry) {
        if (entry == null || entry.state() == null) {
            return null;
        }
        return blockData.computeIfAbsent(entry.id().toLowerCase(java.util.Locale.ROOT), id -> {
            try {
                return Bukkit.createBlockData(entry.state());
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Blockzustand von " + entry.id() + " ist ungültig.");
                return null;
            }
        });
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
            ConfigurationSection sec = recipeSections.get(
                    entry.id().toLowerCase(java.util.Locale.ROOT));
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
