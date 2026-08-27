package com.lemonpvp.lemoncosmetics.model;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.Optional;

/**
 * Death effects played at a player's own death location — the counterpart to
 * {@link KillEffectType} (which the killer sees). Fully data-driven: a
 * {@link Particle} + {@link Sound} rendered by {@code DeathEffectManager}'s
 * collapsing-ring animation, so new effects are just enum entries.
 *
 * <p>All death effects are purchasable with coins; every effect may also be
 * granted for free via {@code lemoncosmetics.deatheffect.<id>}.</p>
 */
public enum DeathEffectType {

    SOUL_ASCENT    ("soul_ascent",    "Soul Ascent",     600, Material.SOUL_TORCH,       Particle.SOUL_FIRE_FLAME, Sound.PARTICLE_SOUL_ESCAPE),
    GRAVESTONE     ("gravestone",     "Gravestone",      400, Material.TUFF,             Particle.ASH,             Sound.BLOCK_DEEPSLATE_BREAK),
    FINAL_HEART    ("final_heart",    "Final Heart",     500, Material.PINK_DYE,         Particle.HEART,           Sound.BLOCK_NOTE_BLOCK_BASEDRUM),
    GHOSTLY        ("ghostly",        "Ghostly",         600, Material.GHAST_TEAR,       Particle.WHITE_ASH,       Sound.ENTITY_VEX_AMBIENT),
    IMPLOSION      ("implosion",      "Implosion",       800, Material.RESPAWN_ANCHOR,   Particle.REVERSE_PORTAL,  Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE),
    STORMY_END     ("stormy_end",     "Stormy End",      700, Material.LIGHTNING_ROD,    Particle.ELECTRIC_SPARK,  Sound.ENTITY_LIGHTNING_BOLT_IMPACT),
    WITHER_ROSE    ("wither_rose",    "Wither Rose",     700, Material.WITHER_ROSE,      Particle.SMOKE,           Sound.ENTITY_WITHER_AMBIENT),
    ICE_TOMB       ("ice_tomb",       "Ice Tomb",        600, Material.BLUE_ICE,         Particle.SNOWFLAKE,       Sound.BLOCK_GLASS_BREAK),
    SONIC_FAREWELL ("sonic_farewell", "Sonic Farewell", 1200, Material.SCULK_SHRIEKER,   Particle.SONIC_BOOM,      Sound.ENTITY_WARDEN_SONIC_BOOM),
    FAILED_TOTEM   ("failed_totem",   "Failed Totem",    900, Material.TOTEM_OF_UNDYING, Particle.TOTEM_OF_UNDYING,Sound.ITEM_TOTEM_USE),
    CHERRY_FAREWELL("cherry_farewell","Cherry Farewell", 600, Material.CHERRY_LEAVES,    Particle.CHERRY_LEAVES,   Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    ENDER_ESCAPE   ("ender_escape",   "Ender Escape",    700, Material.ENDER_EYE,        Particle.PORTAL,          Sound.ENTITY_ENDERMAN_TELEPORT),
    WITCHS_CURSE   ("witchs_curse",   "Witch's Curse",   700, Material.BREWING_STAND,    Particle.WITCH,           Sound.ENTITY_WITCH_CELEBRATE),
    STARFALL       ("starfall",       "Starfall",        800, Material.FIREWORK_STAR,    Particle.FIREWORK,        Sound.ENTITY_FIREWORK_ROCKET_TWINKLE),
    LAST_BLAST     ("last_blast",     "Last Blast",      800, Material.TNT,              Particle.EXPLOSION,       Sound.ENTITY_GENERIC_EXPLODE),
    SMOKE_SCREEN   ("smoke_screen",   "Smoke Screen",    500, Material.CAMPFIRE,         Particle.CAMPFIRE_COSY_SMOKE, Sound.BLOCK_FIRE_EXTINGUISH),
    ASCENSION      ("ascension",      "Ascension",       900, Material.END_ROD,          Particle.END_ROD,         Sound.BLOCK_BEACON_ACTIVATE),
    DRAGONS_END    ("dragons_end",    "Dragon's End",   1000, Material.DRAGON_EGG,       Particle.DRAGON_BREATH,   Sound.ENTITY_ENDER_DRAGON_HURT),
    SOUL_DRAIN     ("soul_drain",     "Soul Drain",      600, Material.SOUL_SAND,        Particle.SOUL,            Sound.PARTICLE_SOUL_ESCAPE),
    WHITE_FAREWELL ("white_farewell", "White Farewell",  500, Material.BONE,             Particle.WHITE_ASH,       Sound.BLOCK_CANDLE_EXTINGUISH),
    BACK_TO_NATURE ("back_to_nature", "Back To Nature",  500, Material.COMPOSTER,        Particle.COMPOSTER,       Sound.BLOCK_GRASS_BREAK),
    GLOW_FADE      ("glow_fade",      "Glow Fade",       600, Material.GLOW_LICHEN,      Particle.GLOW,            Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    OCEAN_BURIAL   ("ocean_burial",   "Ocean Burial",    700, Material.HEART_OF_THE_SEA, Particle.DOLPHIN,         Sound.ENTITY_DOLPHIN_PLAY),
    NAUTILUS_REST  ("nautilus_rest",  "Nautilus Rest",   700, Material.NAUTILUS_SHELL,   Particle.NAUTILUS,        Sound.BLOCK_BEACON_ACTIVATE),
    VOID_TAKEN     ("void_taken",     "Taken By The Void",900,Material.CRYING_OBSIDIAN,  Particle.REVERSE_PORTAL,  Sound.ENTITY_ENDERMAN_TELEPORT),
    LAST_SMOKE     ("last_smoke",     "Last Smoke",      400, Material.GUNPOWDER,        Particle.SMOKE,           Sound.BLOCK_FIRE_EXTINGUISH),
    WAX_SEAL       ("wax_seal",       "Wax Seal",        500, Material.HONEYCOMB,        Particle.WAX_ON,          Sound.ENTITY_EXPERIENCE_ORB_PICKUP),
    LAST_SCRAPE    ("last_scrape",    "Last Scrape",     500, Material.COPPER_BLOCK,     Particle.SCRAPE,          Sound.BLOCK_DEEPSLATE_BREAK),
    CANDLE_OUT     ("candle_out",     "Candle Out",      500, Material.CANDLE,           Particle.SMALL_FLAME,     Sound.BLOCK_CANDLE_EXTINGUISH),
    GONE_POOF      ("gone_poof",      "Gone Poof",       400, Material.WHITE_DYE,        Particle.POOF,            Sound.ENTITY_PUFFER_FISH_BLOW_UP),
    SPORE_REST     ("spore_rest",     "Spore Rest",      600, Material.SPORE_BLOSSOM,    Particle.SPORE_BLOSSOM_AIR, Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    MYCELIUM_GRAVE ("mycelium_grave", "Mycelium Grave",  500, Material.MYCELIUM,         Particle.MYCELIUM,        Sound.BLOCK_GRASS_BREAK),
    CRIMSON_REST   ("crimson_rest",   "Crimson Rest",    600, Material.CRIMSON_FUNGUS,   Particle.CRIMSON_SPORE,   Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    WARPED_REST    ("warped_rest",    "Warped Rest",     600, Material.WARPED_FUNGUS,    Particle.WARPED_SPORE,    Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    FINAL_MELODY   ("final_melody",   "Final Melody",    600, Material.NOTE_BLOCK,       Particle.NOTE,            Sound.BLOCK_NOTE_BLOCK_PLING),
    STATIC_END     ("static_end",     "Static End",      700, Material.COPPER_INGOT,     Particle.ELECTRIC_SPARK,  Sound.ENTITY_LIGHTNING_BOLT_IMPACT),
    LAST_LAUGH     ("last_laugh",     "Last Laugh",      600, Material.CARVED_PUMPKIN,   Particle.ANGRY_VILLAGER,  Sound.ENTITY_VILLAGER_NO),
    PETAL_GRAVE    ("petal_grave",    "Petal Grave",     600, Material.PINK_PETALS,      Particle.CHERRY_LEAVES,   Sound.BLOCK_NOTE_BLOCK_CHIME),
    MAGMA_GRAVE    ("magma_grave",    "Magma Grave",     700, Material.MAGMA_BLOCK,      Particle.LAVA,            Sound.BLOCK_LAVA_POP),
    SCULK_TAKEN    ("sculk_taken",    "Sculk Taken",     800, Material.SCULK_SENSOR,     Particle.SCULK_SOUL,      Sound.BLOCK_SCULK_CATALYST_BLOOM),
    ENCHANTED_END  ("enchanted_end",  "Enchanted End",   600, Material.ENCHANTED_BOOK,   Particle.ENCHANT,         Sound.BLOCK_ENCHANTMENT_TABLE_USE),
    GONE_WITH_WIND ("gone_with_wind", "Gone With The Wind",600,Material.FEATHER,         Particle.CLOUD,           Sound.ENTITY_BREEZE_SHOOT),
    CRIT_COLLAPSE  ("crit_collapse",  "Crit Collapse",   600, Material.IRON_SWORD,       Particle.CRIT,            Sound.ENTITY_PLAYER_ATTACK_CRIT);

    public final String id;
    public final String displayName;
    public final int price;
    public final Material icon;
    public final Particle particle;
    public final Sound sound;

    DeathEffectType(String id, String displayName, int price, Material icon,
                    Particle particle, Sound sound) {
        this.id = id;
        this.displayName = displayName;
        this.price = price;
        this.icon = icon;
        this.particle = particle;
        this.sound = sound;
    }

    /** The permission node that grants this effect for free. */
    public String permission() {
        return "lemoncosmetics.deatheffect." + id;
    }

    public static Optional<DeathEffectType> fromId(String id) {
        if (id == null) return Optional.empty();
        for (DeathEffectType type : values()) {
            if (type.id.equalsIgnoreCase(id)) return Optional.of(type);
        }
        return Optional.empty();
    }
}
