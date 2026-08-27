package com.lemonpvp.lemoncosmetics.model;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.Optional;

/**
 * Win effects — victory celebrations played around a player when they win a
 * duel. {@code FIREWORK_SHOW} has a bespoke animation (real firework entities);
 * everything else is data-driven — a {@link Particle} + {@link Sound} rendered
 * by {@code WinEffectManager}'s rising-spiral fountain.
 *
 * <p>All win effects are purchasable with coins; every effect may also be
 * granted for free via {@code lemoncosmetics.wineffect.<id>}.</p>
 */
public enum WinEffectType {

    // Bespoke (real fireworks)
    FIREWORK_SHOW ("firework_show", "Firework Show",  1500, Material.FIREWORK_ROCKET,  null,                      null),

    // Data-driven (rising spiral fountain)
    HEROS_WELCOME ("heros_welcome", "Hero's Welcome",  800, Material.EMERALD_BLOCK,    Particle.HAPPY_VILLAGER,   Sound.ENTITY_VILLAGER_CELEBRATE),
    GOLD_RUSH     ("gold_rush",     "Gold Rush",       900, Material.GOLD_INGOT,       Particle.WAX_ON,           Sound.ENTITY_PLAYER_LEVELUP),
    ROSE_PETALS   ("rose_petals",   "Rose Petals",     700, Material.PINK_PETALS,      Particle.CHERRY_LEAVES,    Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    DRAGON_ROAR   ("dragon_roar",   "Dragon Roar",    1300, Material.DRAGON_HEAD,      Particle.DRAGON_BREATH,    Sound.ENTITY_ENDER_DRAGON_GROWL),
    THUNDEROUS    ("thunderous",    "Thunderous",     1000, Material.LIGHTNING_ROD,    Particle.ELECTRIC_SPARK,   Sound.ENTITY_LIGHTNING_BOLT_THUNDER),
    SOUL_STORM    ("soul_storm",    "Soul Storm",      900, Material.SOUL_CAMPFIRE,    Particle.SCULK_SOUL,       Sound.PARTICLE_SOUL_ESCAPE),
    ENCHANTED     ("enchanted",     "Enchanted",       800, Material.ENCHANTING_TABLE, Particle.ENCHANT,          Sound.BLOCK_ENCHANTMENT_TABLE_USE),
    TOTEM_TRIUMPH ("totem_triumph", "Totem Triumph",  1100, Material.TOTEM_OF_UNDYING, Particle.TOTEM_OF_UNDYING, Sound.ITEM_TOTEM_USE),
    EMERALD_RAIN  ("emerald_rain",  "Emerald Rain",    800, Material.EMERALD,          Particle.COMPOSTER,        Sound.ENTITY_EXPERIENCE_ORB_PICKUP),
    HEART_SHOW    ("heart_show",    "Heart Show",      700, Material.PINK_TULIP,       Particle.HEART,            Sound.ENTITY_PLAYER_LEVELUP),
    GLOW_UP       ("glow_up",       "Glow Up",         900, Material.GLOWSTONE,        Particle.GLOW,             Sound.BLOCK_BEACON_ACTIVATE),
    SCULK_VICTORY ("sculk_victory", "Sculk Victory",   900, Material.SCULK,            Particle.SCULK_SOUL,       Sound.BLOCK_SCULK_CATALYST_BLOOM),
    FROST_CROWN   ("frost_crown",   "Frost Crown",     800, Material.BLUE_ICE,         Particle.SNOWFLAKE,        Sound.ENTITY_PLAYER_LEVELUP),
    INK_FOUNTAIN  ("ink_fountain",  "Ink Fountain",    700, Material.INK_SAC,          Particle.SQUID_INK,        Sound.ENTITY_SQUID_SQUIRT),
    GLOW_FOUNTAIN ("glow_fountain", "Glow Fountain",   800, Material.GLOW_INK_SAC,     Particle.GLOW_SQUID_INK,   Sound.ENTITY_GLOW_SQUID_AMBIENT),
    CLOUD_NINE    ("cloud_nine",    "Cloud Nine",      700, Material.WHITE_WOOL,       Particle.CLOUD,            Sound.ENTITY_BREEZE_SHOOT),
    CRIT_PARADE   ("crit_parade",   "Crit Parade",     700, Material.IRON_SWORD,       Particle.CRIT,             Sound.ENTITY_PLAYER_ATTACK_CRIT),
    VICTORY_TUNE  ("victory_tune",  "Victory Tune",    700, Material.JUKEBOX,          Particle.NOTE,             Sound.BLOCK_NOTE_BLOCK_PLING),
    EMBER_DANCE   ("ember_dance",   "Ember Dance",     800, Material.CAMPFIRE,         Particle.SMALL_FLAME,      Sound.BLOCK_CAMPFIRE_CRACKLE),
    SPORE_SHOWER  ("spore_shower",  "Spore Shower",    700, Material.SPORE_BLOSSOM,    Particle.SPORE_BLOSSOM_AIR, Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    WAX_SHINE     ("wax_shine",     "Wax Shine",       700, Material.HONEYCOMB,        Particle.WAX_ON,           Sound.ENTITY_EXPERIENCE_ORB_PICKUP),
    OCEAN_WAVE    ("ocean_wave",    "Ocean Wave",      800, Material.HEART_OF_THE_SEA, Particle.DOLPHIN,          Sound.ENTITY_DOLPHIN_PLAY),
    NAUTILUS_CROWN("nautilus_crown","Nautilus Crown",  900, Material.NAUTILUS_SHELL,   Particle.NAUTILUS,         Sound.BLOCK_BEACON_ACTIVATE),
    VOID_WALKER   ("void_walker",   "Void Walker",     900, Material.CRYING_OBSIDIAN,  Particle.REVERSE_PORTAL,   Sound.ENTITY_ENDERMAN_TELEPORT),
    WHITE_ROYALE  ("white_royale",  "White Royale",    800, Material.QUARTZ,           Particle.WHITE_ASH,        Sound.BLOCK_BELL_USE),
    PETAL_WALTZ   ("petal_waltz",   "Petal Waltz",     800, Material.PINK_PETALS,      Particle.CHERRY_LEAVES,    Sound.BLOCK_NOTE_BLOCK_CHIME);

    public final String id;
    public final String displayName;
    public final int price;
    public final Material icon;
    /** Non-null for data-driven effects; null when a bespoke animation handles it. */
    public final Particle particle;
    public final Sound sound;

    WinEffectType(String id, String displayName, int price, Material icon,
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
        return "lemoncosmetics.wineffect." + id;
    }

    public static Optional<WinEffectType> fromId(String id) {
        if (id == null) return Optional.empty();
        for (WinEffectType type : values()) {
            if (type.id.equalsIgnoreCase(id)) return Optional.of(type);
        }
        return Optional.empty();
    }
}
