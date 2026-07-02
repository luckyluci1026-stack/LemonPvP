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
    STARFALL       ("starfall",       "Starfall",        800, Material.FIREWORK_STAR,    Particle.FIREWORK,        Sound.ENTITY_FIREWORK_ROCKET_TWINKLE);

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
