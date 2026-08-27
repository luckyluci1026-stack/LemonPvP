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
    SCULK_TRAIL   ("sculk_trail",   "Sculk Trail",   750, Particle.SCULK_SOUL, null, 0f, Material.SCULK),

    // ── Third wave: dust colours ────────────────────────────────────────────────
    MINT_TRAIL    ("mint_trail",    "Mint Trail",    400, Particle.DUST, Color.fromRGB(152, 255, 190), 1.0f, Material.LIME_DYE),
    CORAL_TRAIL   ("coral_trail",   "Coral Trail",   400, Particle.DUST, Color.fromRGB(255, 127, 80),  1.0f, Material.FIRE_CORAL),
    LAVENDER_TRAIL("lavender_trail","Lavender Trail",400, Particle.DUST, Color.fromRGB(200, 162, 255), 1.0f, Material.PURPLE_DYE),
    TEAL_TRAIL    ("teal_trail",    "Teal Trail",    400, Particle.DUST, Color.fromRGB(0, 150, 152),   1.0f, Material.CYAN_DYE),
    MAGENTA_TRAIL ("magenta_trail", "Magenta Trail", 400, Particle.DUST, Color.fromRGB(255, 0, 200),   1.0f, Material.MAGENTA_DYE),
    SILVER_TRAIL  ("silver_trail",  "Silver Trail",  450, Particle.DUST, Color.fromRGB(200, 205, 210), 1.0f, Material.IRON_INGOT),
    BRONZE_TRAIL  ("bronze_trail",  "Bronze Trail",  450, Particle.DUST, Color.fromRGB(176, 110, 50),  1.0f, Material.COPPER_INGOT),
    RUBY_TRAIL    ("ruby_trail",    "Ruby Trail",    500, Particle.DUST, Color.fromRGB(200, 20, 60),   1.0f, Material.RED_DYE),
    SAPPHIRE_TRAIL("sapphire_trail","Sapphire Trail",500, Particle.DUST, Color.fromRGB(20, 60, 220),   1.0f, Material.LAPIS_BLOCK),
    JADE_TRAIL    ("jade_trail",    "Jade Trail",    500, Particle.DUST, Color.fromRGB(0, 168, 107),   1.0f, Material.EMERALD),
    SUNSET_TRAIL  ("sunset_trail",  "Sunset Trail",  500, Particle.DUST, Color.fromRGB(255, 94, 19),   1.1f, Material.ORANGE_DYE),
    BUBBLEGUM_TRAIL("bubblegum_trail","Bubblegum Trail",450,Particle.DUST,Color.fromRGB(255, 105, 200),1.1f, Material.PINK_DYE),
    LIME_TRAIL    ("lime_trail",    "Lime Trail",    400, Particle.DUST, Color.fromRGB(170, 255, 0),   1.0f, Material.LIME_CONCRETE),
    INDIGO_TRAIL  ("indigo_trail",  "Indigo Trail",  450, Particle.DUST, Color.fromRGB(75, 0, 130),    1.0f, Material.PURPLE_CONCRETE),
    PEARL_TRAIL   ("pearl_trail",   "Pearl Trail",   500, Particle.DUST, Color.fromRGB(245, 245, 235), 1.1f, Material.QUARTZ),

    // ── Third wave: special particles ───────────────────────────────────────────
    WHITE_ASH_TRAIL("white_ash_trail","White Ash Trail",450, Particle.WHITE_ASH, null, 0f, Material.BONE_MEAL),
    COMPOST_TRAIL ("compost_trail", "Compost Trail", 400, Particle.COMPOSTER, null, 0f, Material.COMPOSTER),
    GLOW_TRAIL    ("glow_trail",    "Glow Trail",    600, Particle.GLOW, null, 0f, Material.GLOWSTONE_DUST),
    SMOKE_TRAIL   ("smoke_trail",   "Smoke Trail",   400, Particle.SMOKE, null, 0f, Material.GUNPOWDER),
    WAX_TRAIL     ("wax_trail",     "Wax Trail",     450, Particle.WAX_ON, null, 0f, Material.HONEYCOMB),
    DOLPHIN_TRAIL ("dolphin_trail", "Dolphin Trail", 550, Particle.DOLPHIN, null, 0f, Material.HEART_OF_THE_SEA),
    NAUTILUS_TRAIL("nautilus_trail","Nautilus Trail",650, Particle.NAUTILUS, null, 0f, Material.NAUTILUS_SHELL),
    VOID_TRAIL    ("void_trail",    "Void Trail",    750, Particle.REVERSE_PORTAL, null, 0f, Material.CRYING_OBSIDIAN),
    INK_TRAIL     ("ink_trail",     "Ink Trail",     450, Particle.SQUID_INK, null, 0f, Material.INK_SAC),
    COZY_TRAIL    ("cozy_trail",    "Cozy Trail",    450, Particle.CAMPFIRE_COSY_SMOKE, null, 0f, Material.CAMPFIRE),
    MYCELIUM_TRAIL("mycelium_trail","Mycelium Trail",400, Particle.MYCELIUM, null, 0f, Material.MYCELIUM),
    CRIMSON_SPORE_TRAIL("crimson_spore_trail","Crimson Spore Trail",500, Particle.CRIMSON_SPORE, null, 0f, Material.CRIMSON_FUNGUS),
    WARPED_SPORE_TRAIL ("warped_spore_trail", "Warped Spore Trail", 500, Particle.WARPED_SPORE, null, 0f, Material.WARPED_FUNGUS),
    SPIRIT_TRAIL  ("spirit_trail",  "Spirit Trail",  700, Particle.SOUL, null, 0f, Material.SOUL_SAND),
    SCRAPE_TRAIL  ("scrape_trail",  "Scrape Trail",  450, Particle.SCRAPE, null, 0f, Material.COPPER_BLOCK),
    CANDLE_TRAIL  ("candle_trail",  "Candle Trail",  500, Particle.SMALL_FLAME, null, 0f, Material.CANDLE),
    POOF_TRAIL    ("poof_trail",    "Poof Trail",    400, Particle.POOF, null, 0f, Material.WHITE_DYE),
    ANGRY_TRAIL   ("angry_trail",   "Angry Trail",   500, Particle.ANGRY_VILLAGER, null, 0f, Material.CARVED_PUMPKIN),
    DAMAGE_TRAIL  ("damage_trail",  "Damage Trail",  550, Particle.DAMAGE_INDICATOR, null, 0f, Material.RED_DYE),
    SPORE_TRAIL   ("spore_trail",   "Spore Trail",   550, Particle.SPORE_BLOSSOM_AIR, null, 0f, Material.SPORE_BLOSSOM);

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
