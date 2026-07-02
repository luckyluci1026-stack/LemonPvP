package com.lemonpvp.lemoncosmetics.model;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;

/**
 * Arrow trail cosmetics. Fully data-driven: each trail is a particle (+ colour
 * for DUST) and a menu icon, so new trails are just new enum entries.
 */
public enum ArrowTrailType {

    // ── Originals ──────────────────────────────────────────────────────────────
    LEMON_TRAIL   ("lemon_trail",   "Lemon Trail",   300, Particle.DUST, Color.fromRGB(255, 251, 0),  0.9f, Material.ARROW),
    EMERALD_TRAIL ("emerald_trail", "Emerald Trail", 300, Particle.DUST, Color.fromRGB(0, 255, 68),   0.9f, Material.EMERALD),
    FLAME_TRAIL   ("flame_trail",   "Flame Trail",   300, Particle.FLAME, null, 0f, Material.BLAZE_ROD),

    // ── Dust colour variants ───────────────────────────────────────────────────
    CRIMSON_TRAIL ("crimson_trail", "Crimson Trail", 400, Particle.DUST, Color.fromRGB(220, 30, 30),  1.0f, Material.REDSTONE),
    OCEAN_TRAIL   ("ocean_trail",   "Ocean Trail",   400, Particle.DUST, Color.fromRGB(0, 120, 255),  1.0f, Material.LAPIS_LAZULI),
    AMETHYST_TRAIL("amethyst_trail","Amethyst Trail",450, Particle.DUST, Color.fromRGB(170, 80, 255), 1.0f, Material.AMETHYST_SHARD),
    ROSE_TRAIL    ("rose_trail",    "Rose Trail",    400, Particle.DUST, Color.fromRGB(255, 105, 180),1.0f, Material.PINK_DYE),
    GOLD_TRAIL    ("gold_trail",    "Gold Trail",    600, Particle.DUST, Color.fromRGB(255, 200, 0),  1.0f, Material.GOLD_INGOT),
    AQUA_TRAIL    ("aqua_trail",    "Aqua Trail",    450, Particle.DUST, Color.fromRGB(0, 255, 255),  1.0f, Material.PRISMARINE_CRYSTALS),
    MIDNIGHT_TRAIL("midnight_trail","Midnight Trail",500, Particle.DUST, Color.fromRGB(25, 25, 40),   1.0f, Material.BLACK_DYE),
    SNOW_TRAIL    ("snow_trail",    "Snow Trail",    400, Particle.DUST, Color.fromRGB(240, 250, 255),1.1f, Material.WHITE_DYE),

    // ── Special particles ───────────────────────────────────────────────────────
    FROST_TRAIL   ("frost_trail",   "Frost Trail",   500, Particle.SNOWFLAKE, null, 0f, Material.SNOWBALL),
    SOUL_TRAIL    ("soul_trail",    "Soul Trail",    700, Particle.SOUL_FIRE_FLAME, null, 0f, Material.SOUL_LANTERN),
    ENDER_TRAIL   ("ender_trail",   "Ender Trail",   700, Particle.PORTAL, null, 0f, Material.ENDER_PEARL),
    HEART_TRAIL   ("heart_trail",   "Heart Trail",   500, Particle.HEART, null, 0f, Material.POPPY),
    NOTE_TRAIL    ("note_trail",    "Note Trail",    500, Particle.NOTE, null, 0f, Material.NOTE_BLOCK),
    HOLY_TRAIL    ("holy_trail",    "Holy Trail",    800, Particle.END_ROD, null, 0f, Material.END_ROD),
    SPARK_TRAIL   ("spark_trail",   "Spark Trail",   600, Particle.ELECTRIC_SPARK, null, 0f, Material.COPPER_INGOT),
    NATURE_TRAIL  ("nature_trail",  "Nature Trail",  450, Particle.HAPPY_VILLAGER, null, 0f, Material.OAK_SAPLING),
    CLOUD_TRAIL   ("cloud_trail",   "Cloud Trail",   400, Particle.CLOUD, null, 0f, Material.WHITE_WOOL),
    CRIT_TRAIL    ("crit_trail",    "Critical Trail",550, Particle.CRIT, null, 0f, Material.IRON_SWORD),
    MAGIC_TRAIL   ("magic_trail",   "Magic Trail",   700, Particle.ENCHANT, null, 0f, Material.ENCHANTING_TABLE),
    DRAGON_TRAIL  ("dragon_trail",  "Dragon Trail",  1000,Particle.DRAGON_BREATH, null, 0f, Material.DRAGON_BREATH),
    CHERRY_TRAIL  ("cherry_trail",  "Cherry Trail",  600, Particle.CHERRY_LEAVES, null, 0f, Material.CHERRY_SAPLING),
    LAVA_TRAIL    ("lava_trail",    "Lava Trail",    600, Particle.LAVA, null, 0f, Material.MAGMA_CREAM),
    COPPER_TRAIL  ("copper_trail",  "Copper Trail",  450, Particle.WAX_OFF, null, 0f, Material.COPPER_BLOCK),

    // ── Second wave ─────────────────────────────────────────────────────────────
    WITCH_TRAIL   ("witch_trail",   "Witch Trail",   550, Particle.WITCH, null, 0f, Material.BREWING_STAND),
    LLAMA_TRAIL   ("llama_trail",   "Llama Trail",   350, Particle.SPIT, null, 0f, Material.LEAD),
    ASH_TRAIL     ("ash_trail",     "Ash Trail",     350, Particle.ASH, null, 0f, Material.TUFF),
    GLOW_INK_TRAIL("glow_ink_trail","Glow Ink Trail",500, Particle.GLOW_SQUID_INK, null, 0f, Material.GLOW_INK_SAC),
    FIREWORK_TRAIL("firework_trail","Firework Trail",650, Particle.FIREWORK, null, 0f, Material.FIREWORK_ROCKET),
    TOTEM_TRAIL   ("totem_trail",   "Totem Trail",   800, Particle.TOTEM_OF_UNDYING, null, 0f, Material.TOTEM_OF_UNDYING),
    SCULK_TRAIL   ("sculk_trail",   "Sculk Trail",   750, Particle.SCULK_SOUL, null, 0f, Material.SCULK);

    public final String id;
    public final String displayName;
    public final int price;
    public final Particle particle;
    public final Color dustColor;   // null for non-DUST particles
    public final float dustSize;
    public final Material icon;

    ArrowTrailType(String id, String displayName, int price,
                   Particle particle, Color dustColor, float dustSize, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.price = price;
        this.particle = particle;
        this.dustColor = dustColor;
        this.dustSize = dustSize;
        this.icon = icon;
    }

    public static java.util.Optional<ArrowTrailType> fromId(String id) {
        for (ArrowTrailType t : values()) if (t.id.equals(id)) return java.util.Optional.of(t);
        return java.util.Optional.empty();
    }
}
