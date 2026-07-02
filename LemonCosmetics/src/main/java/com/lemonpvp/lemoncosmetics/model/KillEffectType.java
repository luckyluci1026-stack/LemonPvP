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
    STARFALL       ("starfall",       "Starfall",        "lemoncosmetics.effect.starfall",       Material.FIREWORK_STAR,        Particle.FIREWORK,        Sound.ENTITY_FIREWORK_ROCKET_TWINKLE);

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
