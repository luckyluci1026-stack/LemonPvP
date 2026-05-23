package com.lemonpvp.lemoncosmetics.model;

import org.bukkit.Color;
import org.bukkit.Particle;

public enum ArrowTrailType {
    LEMON_TRAIL("lemon_trail", "Lemon Trail",   300,
            Particle.DUST, Color.fromRGB(255, 251, 0), 0.8f),
    EMERALD_TRAIL("emerald_trail", "Emerald Trail", 300,
            Particle.DUST, Color.fromRGB(0, 255, 68),  0.8f),
    FLAME_TRAIL("flame_trail",  "Flame Trail",  300,
            Particle.FLAME, null, 0f);

    public final String id;
    public final String displayName;
    public final int price;
    public final Particle particle;
    public final Color dustColor;   // null for non-DUST particles
    public final float dustSize;

    ArrowTrailType(String id, String displayName, int price,
                   Particle particle, Color dustColor, float dustSize) {
        this.id = id;
        this.displayName = displayName;
        this.price = price;
        this.particle = particle;
        this.dustColor = dustColor;
        this.dustSize = dustSize;
    }

    public static java.util.Optional<ArrowTrailType> fromId(String id) {
        for (ArrowTrailType t : values()) if (t.id.equals(id)) return java.util.Optional.of(t);
        return java.util.Optional.empty();
    }
}
