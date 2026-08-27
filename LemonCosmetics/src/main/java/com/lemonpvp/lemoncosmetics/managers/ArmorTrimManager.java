package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.ArmorSlotType;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.List;

public class ArmorTrimManager {

    private final LemonCosmetics plugin;

    // All vanilla trim pattern IDs (sorted alphabetically)
    private static final List<String> ALL_PATTERN_IDS = List.of(
            "bolt", "coast", "dune", "eye", "flow", "host", "raiser", "rib",
            "sentry", "shaper", "silence", "snout", "spire", "tide", "vex",
            "ward", "wayfinder", "wild"
    );

    // All vanilla trim material IDs (sorted alphabetically)
    private static final List<String> ALL_MATERIAL_IDS = List.of(
            "amethyst", "copper", "diamond", "emerald", "gold", "iron",
            "lapis", "netherite", "quartz", "redstone", "resin"
    );

    public ArmorTrimManager(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Pattern / material lists
    // -------------------------------------------------------------------------

    /** Returns a sorted, unmodifiable list of all 18 vanilla trim pattern IDs. */
    public List<String> getAllPatternIds() {
        return ALL_PATTERN_IDS;
    }

    /** Returns a sorted, unmodifiable list of all 11 vanilla trim material IDs. */
    public List<String> getAllMaterialIds() {
        return ALL_MATERIAL_IDS;
    }

    // -------------------------------------------------------------------------
    // Icon helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the smithing-template {@link Material} that represents the given
     * trim pattern in GUIs.
     */
    public Material getTrimPatternIcon(String patternId) {
        if (patternId == null) return Material.GOLDEN_SWORD;
        return switch (patternId.toLowerCase()) {
            case "bolt"      -> Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "coast"     -> Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "dune"      -> Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "eye"       -> Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "flow"      -> Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "host"      -> Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "raiser"    -> Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "rib"       -> Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "sentry"    -> Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "shaper"    -> Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "silence"   -> Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "snout"     -> Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "spire"     -> Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "tide"      -> Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "vex"       -> Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "ward"      -> Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "wayfinder" -> Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE;
            case "wild"      -> Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE;
            default          -> Material.GOLDEN_SWORD;
        };
    }

    /**
     * Returns the {@link Material} that represents the given trim material
     * in GUIs.
     */
    public Material getTrimMaterialIcon(String materialId) {
        if (materialId == null) return Material.STONE;
        return switch (materialId.toLowerCase()) {
            case "amethyst"  -> Material.AMETHYST_SHARD;
            case "copper"    -> Material.COPPER_INGOT;
            case "diamond"   -> Material.DIAMOND;
            case "emerald"   -> Material.EMERALD;
            case "gold"      -> Material.GOLD_INGOT;
            case "iron"      -> Material.IRON_INGOT;
            case "lapis"     -> Material.LAPIS_LAZULI;
            case "netherite" -> Material.NETHERITE_INGOT;
            case "quartz"    -> Material.QUARTZ;
            case "redstone"  -> Material.REDSTONE;
            case "resin"     -> Material.RESIN_BRICK;
            default          -> Material.STONE;
        };
    }

    // -------------------------------------------------------------------------
    // Display name helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a human-readable display name for a trim pattern ID, e.g.
     * {@code "wayfinder"} → {@code "Wayfinder"}.
     */
    public String getDisplayName(String patternId) {
        if (patternId == null || patternId.isEmpty()) return "";
        String replaced = patternId.replace('_', ' ');
        return Character.toUpperCase(replaced.charAt(0)) + replaced.substring(1);
    }

    /**
     * Returns a human-readable display name for a trim material ID, e.g.
     * {@code "netherite"} → {@code "Netherite"}.
     */
    public String getMaterialDisplayName(String materialId) {
        if (materialId == null || materialId.isEmpty()) return "";
        String replaced = materialId.replace('_', ' ');
        return Character.toUpperCase(replaced.charAt(0)) + replaced.substring(1);
    }

    // -------------------------------------------------------------------------
    // Applying trims
    // -------------------------------------------------------------------------

    /**
     * Re-applies all stored trims from the player's {@link PlayerCosmetics} to
     * their currently worn armor pieces. Must be called on the main thread.
     */
    public void applyTrimToPlayer(Player player) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager()
                .getPlayerCosmetics(player.getUniqueId());
        if (cosmetics == null) return;

        for (ArmorSlotType slot : ArmorSlotType.values()) {
            String[] trim = cosmetics.getAppliedTrim(slot.name().toLowerCase());
            ItemStack worn = player.getInventory().getItem(slot.getEquipmentSlot());
            if (worn == null || worn.getType() == Material.AIR) continue;
            if (trim != null && trim.length >= 2) {
                applyTrimToItem(worn, trim[0], trim[1]);
                player.getInventory().setItem(slot.getEquipmentSlot(), worn);
            }
        }
    }

    /**
     * Removes the stored trim from the given armor slot and updates the player's
     * worn armor. Must be called on the main thread.
     */
    public void removeTrimFromPlayer(Player player, ArmorSlotType slot) {
        ItemStack worn = player.getInventory().getItem(slot.getEquipmentSlot());
        if (worn == null || worn.getType() == Material.AIR) return;
        if (!(worn.getItemMeta() instanceof ArmorMeta meta)) return;
        meta.setTrim(null);
        worn.setItemMeta(meta);
        player.getInventory().setItem(slot.getEquipmentSlot(), worn);
    }

    /**
     * Applies the given trim pattern and material to an {@link ItemStack} in
     * place using the Paper 1.21.4 API. Does nothing if the item does not have
     * {@link ArmorMeta}, or if either the pattern or material is unknown.
     */
    public void applyTrimToItem(ItemStack item, String patternId, String materialId) {
        if (!(item.getItemMeta() instanceof ArmorMeta meta)) return;

        TrimPattern pattern = Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(patternId));
        TrimMaterial material = Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft(materialId));
        if (pattern == null || material == null) return;

        meta.setTrim(new ArmorTrim(material, pattern));
        item.setItemMeta(meta);
    }
}
