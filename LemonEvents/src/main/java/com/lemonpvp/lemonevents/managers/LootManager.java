package com.lemonpvp.lemonevents.managers;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.model.LootItem;
import com.lemonpvp.lemonevents.model.LootRarity;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class LootManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonEvents plugin;
    private final Map<LootRarity, List<LootItem>> lootTables = new EnumMap<>(LootRarity.class);
    private final Map<LootRarity, Integer> rarityWeights = new EnumMap<>(LootRarity.class);
    private final Random random = new Random();

    public LootManager(LemonEvents plugin) {
        this.plugin = plugin;
    }

    public void load() {
        lootTables.clear();
        rarityWeights.clear();

        var cfg = plugin.getLootConfig();
        ConfigurationSection tables = cfg.getConfigurationSection("loot-tables");
        if (tables == null) return;

        for (LootRarity rarity : LootRarity.values()) {
            ConfigurationSection section = tables.getConfigurationSection(rarity.name());
            if (section == null) continue;

            List<LootItem> items = new ArrayList<>();
            List<?> rawItems = section.getList("items");
            if (rawItems != null) {
                for (Object raw : rawItems) {
                    if (!(raw instanceof Map<?, ?> map)) continue;
                    try {
                        Material mat = Material.matchMaterial(String.valueOf(map.get("material")));
                        if (mat == null) continue;
                        int minAmt = getInt(map, "amount-min", 1);
                        int maxAmt = getInt(map, "amount-max", 1);
                        int weight = getInt(map, "weight", 10);
                        int cmd = getInt(map, "custom-model-data", 0);
                        String displayName = map.containsKey("display-name") ? String.valueOf(map.get("display-name")) : null;
                        boolean enchanted = getBool(map, "enchanted", false);

                        List<String> lore = new ArrayList<>();
                        Object rawLore = map.get("lore");
                        if (rawLore instanceof List<?> loreList) {
                            for (Object l : loreList) lore.add(String.valueOf(l));
                        }

                        items.add(new LootItem(mat, minAmt, maxAmt, weight, cmd, displayName, lore, enchanted));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Bad loot entry: " + e.getMessage());
                    }
                }
            }
            lootTables.put(rarity, items);
        }

        ConfigurationSection weights = cfg.getConfigurationSection("rarity-weights");
        if (weights != null) {
            for (LootRarity rarity : LootRarity.values()) {
                rarityWeights.put(rarity, weights.getInt(rarity.name(), 10));
            }
        }

        plugin.getLogger().info("Loaded loot tables: " + lootTables.size() + " rarities");
    }

    /** Roll a random rarity, then roll a random item from that rarity's table. */
    public ItemStack rollLoot() {
        LootRarity rarity = rollRarity();
        List<LootItem> items = lootTables.getOrDefault(rarity, Collections.emptyList());
        if (items.isEmpty()) return new ItemStack(Material.APPLE);

        LootItem item = weightedRandom(items);
        int amount = item.getMinAmount() + random.nextInt(Math.max(1, item.getMaxAmount() - item.getMinAmount() + 1));
        return item.buildItem(amount, MM);
    }

    public ItemStack rollLootOfRarity(LootRarity rarity) {
        List<LootItem> items = lootTables.getOrDefault(rarity, Collections.emptyList());
        if (items.isEmpty()) return new ItemStack(Material.DIAMOND);
        LootItem item = weightedRandom(items);
        int amount = item.getMinAmount() + random.nextInt(Math.max(1, item.getMaxAmount() - item.getMinAmount() + 1));
        return item.buildItem(amount, MM);
    }

    private LootRarity rollRarity() {
        int total = rarityWeights.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) return LootRarity.COMMON;
        int roll = random.nextInt(total);
        int cumulative = 0;
        for (LootRarity rarity : LootRarity.values()) {
            cumulative += rarityWeights.getOrDefault(rarity, 0);
            if (roll < cumulative) return rarity;
        }
        return LootRarity.COMMON;
    }

    private LootItem weightedRandom(List<LootItem> items) {
        int total = items.stream().mapToInt(LootItem::getWeight).sum();
        if (total == 0) return items.get(0);
        int roll = random.nextInt(total);
        int cumulative = 0;
        for (LootItem item : items) {
            cumulative += item.getWeight();
            if (roll < cumulative) return item;
        }
        return items.get(0);
    }

    private static int getInt(Map<?, ?> map, String key, int def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        return def;
    }

    private static boolean getBool(Map<?, ?> map, String key, boolean def) {
        Object v = map.get(key);
        if (v instanceof Boolean b) return b;
        return def;
    }
}
