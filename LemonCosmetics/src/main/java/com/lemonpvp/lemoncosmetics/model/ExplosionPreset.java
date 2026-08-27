package com.lemonpvp.lemoncosmetics.model;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

/**
 * One explosion-particle preset: when something a player caused explodes
 * (their end crystals, their primed TNT), the vanilla explosion visuals are
 * augmented with this preset's particle burst + sound.
 *
 * <p>Unlike the other cosmetic categories this is NOT an enum: built-in
 * presets are defined in {@code ExplosionParticleManager} and additional
 * presets can be defined by the server owner in {@code explosions.lemon}
 * without recompiling — both end up as instances of this record.</p>
 */
public record ExplosionPreset(
        String id,
        String displayName,
        int price,
        Material icon,
        Particle particle,
        Sound sound,
        boolean builtin) {

    /** The permission node that grants this preset for free. */
    public String permission() {
        return "lemoncosmetics.explosion." + id;
    }
}
