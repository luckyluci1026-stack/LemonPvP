package com.lemonpvp.lemoncosmetics.model;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.Optional;

/**
 * Kill effects played at a victim's location when a player gets a kill.
 *
 * <p>The four originals have bespoke animation classes (see the effects package);
 * everything else is data-driven — a {@link Particle} + {@link Sound} rendered by
 * {@code KillEffectManager}'s generic burst, so new effects are just enum entries.</p>
 */
public enum KillEffectType {

    // ── Originals (custom animation classes) ────────────────────────────────────
    FIRE_SWARM     ("fire_swarm",     "Fire Swarm",      "lemoncosmetics.effect.fire_swarm",     Material.BLAZE_ROD,        null, null),
    SPOOK_SWARM    ("spook_swarm",    "Spook Swarm",     "lemoncosmetics.effect.spook_swarm",    Material.SOUL_LANTERN,     null, null),
    TOTEM_EXPLOSION("totem_explosion","Totem Explosion", "lemoncosmetics.effect.totem_explosion",Material.TOTEM_OF_UNDYING, null, null),
    GOLDEN_GAP     ("golden_gap",     "Golden Gap",      "lemoncosmetics.effect.golden_gap",     Material.GOLDEN_APPLE,     null, null),

    // ── Data-driven (generic burst) ─────────────────────────────────────────────
    LIGHTNING      ("lightning",      "Lightning Strike","lemoncosmetics.effect.lightning",      Material.LIGHTNING_ROD,        Particle.ELECTRIC_SPARK,  Sound.ENTITY_LIGHTNING_BOLT_THUNDER),
    SOUL_HARVEST   ("soul_harvest",   "Soul Harvest",    "lemoncosmetics.effect.soul_harvest",   Material.SOUL_LANTERN,         Particle.SOUL_FIRE_FLAME, Sound.PARTICLE_SOUL_ESCAPE),
    HEARTBREAK     ("heartbreak",     "Heartbreak",      "lemoncosmetics.effect.heartbreak",     Material.POPPY,                Particle.HEART,           Sound.ENTITY_PLAYER_LEVELUP),
    FIREWORK_FINALE("firework_finale","Firework Finale", "lemoncosmetics.effect.firework_finale",Material.FIREWORK_ROCKET,      Particle.FIREWORK,        Sound.ENTITY_FIREWORK_ROCKET_BLAST),
    ENDER_BURST    ("ender_burst",    "Ender Burst",     "lemoncosmetics.effect.ender_burst",    Material.ENDER_EYE,            Particle.PORTAL,          Sound.ENTITY_ENDERMAN_TELEPORT),
    FROST_NOVA     ("frost_nova",     "Frost Nova",      "lemoncosmetics.effect.frost_nova",     Material.PACKED_ICE,           Particle.SNOWFLAKE,       Sound.BLOCK_GLASS_BREAK),
    EXPLOSIVE      ("explosive",      "Explosive End",   "lemoncosmetics.effect.explosive",      Material.TNT,                  Particle.EXPLOSION,       Sound.ENTITY_GENERIC_EXPLODE),
    ARCANE         ("arcane",         "Arcane Burst",    "lemoncosmetics.effect.arcane",         Material.ENCHANTING_TABLE,     Particle.ENCHANT,         Sound.BLOCK_ENCHANTMENT_TABLE_USE),
    ANGELIC        ("angelic",        "Angelic",         "lemoncosmetics.effect.angelic",        Material.FEATHER,              Particle.END_ROD,         Sound.BLOCK_BELL_USE),
    SPARKLE        ("sparkle",        "Sparkle",         "lemoncosmetics.effect.sparkle",        Material.GLOWSTONE_DUST,       Particle.GLOW,            Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    NATURE_BLOOM   ("nature_bloom",   "Nature Bloom",    "lemoncosmetics.effect.nature_bloom",   Material.OAK_SAPLING,          Particle.HAPPY_VILLAGER,  Sound.BLOCK_GRASS_BREAK),
    CRIT_STRIKE    ("crit_strike",    "Critical Strike", "lemoncosmetics.effect.crit_strike",    Material.IRON_SWORD,           Particle.CRIT,            Sound.ENTITY_PLAYER_ATTACK_CRIT),
    LAVA_BURST     ("lava_burst",     "Lava Burst",      "lemoncosmetics.effect.lava_burst",     Material.MAGMA_BLOCK,          Particle.LAVA,            Sound.BLOCK_FIRE_AMBIENT),
    DRAGON_SOUL    ("dragon_soul",    "Dragon Soul",     "lemoncosmetics.effect.dragon_soul",    Material.DRAGON_HEAD,          Particle.DRAGON_BREATH,   Sound.ENTITY_ENDER_DRAGON_GROWL),
    CHERRY_BLOSSOM ("cherry_blossom", "Cherry Blossom",  "lemoncosmetics.effect.cherry_blossom", Material.CHERRY_SAPLING,       Particle.CHERRY_LEAVES,   Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    CLOUD_BURST    ("cloud_burst",    "Cloud Burst",     "lemoncosmetics.effect.cloud_burst",    Material.WHITE_WOOL,           Particle.CLOUD,           Sound.ENTITY_BREEZE_SHOOT),
    WITHERED       ("withered",       "Withered",        "lemoncosmetics.effect.withered",       Material.WITHER_SKELETON_SKULL,Particle.SMOKE,           Sound.ENTITY_WITHER_SHOOT),
    SONIC_BOOM     ("sonic_boom",     "Sonic Boom",      "lemoncosmetics.effect.sonic_boom",     Material.SCULK_SHRIEKER,       Particle.SONIC_BOOM,      Sound.ENTITY_WARDEN_SONIC_BOOM),
    WITCH_BREW     ("witch_brew",     "Witch Brew",      "lemoncosmetics.effect.witch_brew",     Material.BREWING_STAND,        Particle.WITCH,           Sound.ENTITY_WITCH_CELEBRATE),
    INK_SPLASH     ("ink_splash",     "Ink Splash",      "lemoncosmetics.effect.ink_splash",     Material.INK_SAC,              Particle.SQUID_INK,       Sound.ENTITY_SQUID_SQUIRT),
    GLOW_BURST     ("glow_burst",     "Glow Burst",      "lemoncosmetics.effect.glow_burst",     Material.GLOW_INK_SAC,         Particle.GLOW_SQUID_INK,  Sound.ENTITY_GLOW_SQUID_AMBIENT),
    STARFALL       ("starfall",       "Starfall",        "lemoncosmetics.effect.starfall",       Material.FIREWORK_STAR,        Particle.FIREWORK,        Sound.ENTITY_FIREWORK_ROCKET_TWINKLE),
    SMOKE_BOMB     ("smoke_bomb",     "Smoke Bomb",      "lemoncosmetics.effect.smoke_bomb",     Material.GUNPOWDER,            Particle.SMOKE,           Sound.ENTITY_BLAZE_SHOOT),
    RAGE_POP       ("rage_pop",       "Rage Pop",        "lemoncosmetics.effect.rage_pop",       Material.CARVED_PUMPKIN,       Particle.ANGRY_VILLAGER,  Sound.ENTITY_VILLAGER_NO),
    DAMAGE_BURST   ("damage_burst",   "Damage Burst",    "lemoncosmetics.effect.damage_burst",   Material.RED_DYE,              Particle.DAMAGE_INDICATOR,Sound.ENTITY_PLAYER_ATTACK_STRONG),
    SPIRIT_RELEASE ("spirit_release", "Spirit Release",  "lemoncosmetics.effect.spirit_release", Material.SOUL_SOIL,            Particle.SOUL,            Sound.PARTICLE_SOUL_ESCAPE),
    MYCELIUM_PUFF  ("mycelium_puff",  "Mycelium Puff",   "lemoncosmetics.effect.mycelium_puff",  Material.MYCELIUM,             Particle.MYCELIUM,        Sound.BLOCK_GRASS_BREAK),
    CRIMSON_BLOOM  ("crimson_bloom",  "Crimson Bloom",   "lemoncosmetics.effect.crimson_bloom",  Material.CRIMSON_FUNGUS,       Particle.CRIMSON_SPORE,   Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    WARPED_BLOOM   ("warped_bloom",   "Warped Bloom",    "lemoncosmetics.effect.warped_bloom",   Material.WARPED_FUNGUS,        Particle.WARPED_SPORE,    Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    WHITE_OUT      ("white_out",      "White-Out",       "lemoncosmetics.effect.white_out",      Material.BONE_MEAL,            Particle.WHITE_ASH,       Sound.BLOCK_CANDLE_EXTINGUISH),
    COMPOST_BURST  ("compost_burst",  "Compost Burst",   "lemoncosmetics.effect.compost_burst",  Material.COMPOSTER,            Particle.COMPOSTER,       Sound.BLOCK_GRASS_BREAK),
    GLOW_RING      ("glow_ring",      "Glow Ring",       "lemoncosmetics.effect.glow_ring",      Material.GLOWSTONE,            Particle.GLOW,            Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    DOLPHIN_SPLASH ("dolphin_splash", "Dolphin Splash",  "lemoncosmetics.effect.dolphin_splash", Material.HEART_OF_THE_SEA,     Particle.DOLPHIN,         Sound.ENTITY_DOLPHIN_PLAY),
    NAUTILUS_SWIRL ("nautilus_swirl", "Nautilus Swirl",  "lemoncosmetics.effect.nautilus_swirl", Material.NAUTILUS_SHELL,       Particle.NAUTILUS,        Sound.BLOCK_BEACON_ACTIVATE),
    VOID_RIP       ("void_rip",       "Void Rip",        "lemoncosmetics.effect.void_rip",       Material.CRYING_OBSIDIAN,      Particle.REVERSE_PORTAL,  Sound.ENTITY_ENDERMAN_TELEPORT),
    COZY_END       ("cozy_end",       "Cozy End",        "lemoncosmetics.effect.cozy_end",       Material.CAMPFIRE,             Particle.CAMPFIRE_COSY_SMOKE, Sound.BLOCK_FIRE_EXTINGUISH),
    WAX_FINISH     ("wax_finish",     "Wax Finish",      "lemoncosmetics.effect.wax_finish",     Material.HONEYCOMB,            Particle.WAX_ON,          Sound.ENTITY_EXPERIENCE_ORB_PICKUP),
    SCRAPE_SPARK   ("scrape_spark",   "Scrape Spark",    "lemoncosmetics.effect.scrape_spark",   Material.COPPER_BLOCK,         Particle.SCRAPE,          Sound.ENTITY_EXPERIENCE_ORB_PICKUP),
    CANDLELIGHT    ("candlelight",    "Candlelight",     "lemoncosmetics.effect.candlelight",    Material.CANDLE,               Particle.SMALL_FLAME,     Sound.BLOCK_CANDLE_EXTINGUISH),
    PUFF_AWAY      ("puff_away",      "Puff Away",       "lemoncosmetics.effect.puff_away",      Material.WHITE_DYE,            Particle.POOF,            Sound.ENTITY_PUFFER_FISH_BLOW_UP),
    SPORE_DRIFT    ("spore_drift",    "Spore Drift",     "lemoncosmetics.effect.spore_drift",    Material.SPORE_BLOSSOM,        Particle.SPORE_BLOSSOM_AIR, Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    PEARL_SNAP     ("pearl_snap",     "Pearl Snap",      "lemoncosmetics.effect.pearl_snap",     Material.ENDER_PEARL,          Particle.PORTAL,          Sound.ENTITY_ENDERMAN_TELEPORT),
    BLIZZARD       ("blizzard",       "Blizzard",        "lemoncosmetics.effect.blizzard",       Material.SNOW_BLOCK,           Particle.SNOWFLAKE,       Sound.ENTITY_BREEZE_SHOOT),
    HEARTLINE      ("heartline",      "Heartline",       "lemoncosmetics.effect.heartline",      Material.POPPY,                Particle.HEART,           Sound.BLOCK_NOTE_BLOCK_PLING),
    JUKEBOX        ("jukebox",        "Jukebox",         "lemoncosmetics.effect.jukebox",        Material.JUKEBOX,              Particle.NOTE,            Sound.BLOCK_NOTE_BLOCK_PLING),
    HALO           ("halo",           "Halo",            "lemoncosmetics.effect.halo",           Material.GOLD_INGOT,           Particle.END_ROD,         Sound.BLOCK_BELL_USE),
    STATIC_SHOCK   ("static_shock",   "Static Shock",    "lemoncosmetics.effect.static_shock",   Material.COPPER_INGOT,         Particle.ELECTRIC_SPARK,  Sound.ENTITY_LIGHTNING_BOLT_THUNDER),
    LUCKY_BLOOM    ("lucky_bloom",    "Lucky Bloom",     "lemoncosmetics.effect.lucky_bloom",    Material.RABBIT_FOOT,          Particle.HAPPY_VILLAGER,  Sound.ENTITY_VILLAGER_CELEBRATE),
    GUST           ("gust",           "Gust",            "lemoncosmetics.effect.gust",           Material.WHITE_WOOL,           Particle.CLOUD,           Sound.ENTITY_BREEZE_SHOOT),
    RAZOR          ("razor",          "Razor",           "lemoncosmetics.effect.razor",          Material.SHEARS,               Particle.SWEEP_ATTACK,    Sound.ENTITY_PLAYER_ATTACK_SWEEP),
    SPELLBOUND     ("spellbound",     "Spellbound",      "lemoncosmetics.effect.spellbound",     Material.LECTERN,              Particle.ENCHANT,         Sound.BLOCK_ENCHANTMENT_TABLE_USE),
    DRAGONS_WRATH  ("dragons_wrath",  "Dragon's Wrath",  "lemoncosmetics.effect.dragons_wrath",  Material.DRAGON_EGG,           Particle.DRAGON_BREATH,   Sound.ENTITY_ENDER_DRAGON_HURT),
    PETAL_STORM    ("petal_storm",    "Petal Storm",     "lemoncosmetics.effect.petal_storm",    Material.PINK_PETALS,          Particle.CHERRY_LEAVES,   Sound.BLOCK_NOTE_BLOCK_CHIME),
    MAGMA_POP      ("magma_pop",      "Magma Pop",       "lemoncosmetics.effect.magma_pop",      Material.MAGMA_CREAM,          Particle.LAVA,            Sound.BLOCK_LAVA_POP),
    SCULK_SCREAM   ("sculk_scream",   "Sculk Scream",    "lemoncosmetics.effect.sculk_scream",   Material.SCULK_SENSOR,         Particle.SCULK_SOUL,      Sound.BLOCK_SCULK_CATALYST_BLOOM),
    ALLAY_GIFT     ("allay_gift",     "Allay Gift",      "lemoncosmetics.effect.allay_gift",     Material.COOKIE,               Particle.GLOW,            Sound.ENTITY_ALLAY_ITEM_GIVEN),
    TOTEM_FLASH    ("totem_flash",    "Totem Flash",     "lemoncosmetics.effect.totem_flash",    Material.TOTEM_OF_UNDYING,     Particle.TOTEM_OF_UNDYING,Sound.ITEM_TOTEM_USE);

    private final String id;
    private final String displayName;
    private final String permission;
    private final Material icon;
    /** Non-null for data-driven effects; null when a bespoke effect class handles it. */
    public final Particle particle;
    public final Sound sound;

    KillEffectType(String id, String displayName, String permission, Material icon,
                   Particle particle, Sound sound) {
        this.id = id;
        this.displayName = displayName;
        this.permission = permission;
        this.icon = icon;
        this.particle = particle;
        this.sound = sound;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getPermission() { return permission; }
    public Material getIcon() { return icon; }

    /**
     * Coin price for buying this effect directly in the GUI. Effects remain
     * unlockable via codes/quests and the permission node as before — buying
     * is an additional path. The four bespoke originals are premium-priced.
     */
    public int getPrice() {
        return switch (this) {
            case FIRE_SWARM, SPOOK_SWARM, TOTEM_EXPLOSION, GOLDEN_GAP -> 1500;
            case LIGHTNING, DRAGON_SOUL, SONIC_BOOM, TOTEM_FLASH -> 1000;
            default -> 700;
        };
    }

    /** True when this effect is rendered by the generic particle burst. */
    public boolean isGeneric() { return particle != null; }

    public static Optional<KillEffectType> fromId(String id) {
        if (id == null) return Optional.empty();
        for (KillEffectType type : values()) {
            if (type.id.equalsIgnoreCase(id)) return Optional.of(type);
        }
        return Optional.empty();
    }
}
