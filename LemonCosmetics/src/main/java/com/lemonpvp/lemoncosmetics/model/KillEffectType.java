package com.lemonpvp.lemoncosmetics.model;

import org.bukkit.Material;

import java.util.Optional;

public enum KillEffectType {
    FIRE_SWARM("fire_swarm", "Fire Swarm", "lemoncosmetics.effect.fire_swarm"),
    SPOOK_SWARM("spook_swarm", "Spook Swarm", "lemoncosmetics.effect.spook_swarm"),
    TOTEM_EXPLOSION("totem_explosion", "Totem Explosion", "lemoncosmetics.effect.totem_explosion"),
    GOLDEN_GAP("golden_gap", "Golden Gap", "lemoncosmetics.effect.golden_gap");

    private final String id;
    private final String displayName;
    private final String permission;

    KillEffectType(String id, String displayName, String permission) {
        this.id = id;
        this.displayName = displayName;
        this.permission = permission;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPermission() {
        return permission;
    }

    public Material getIcon() {
        return switch (this) {
            case FIRE_SWARM -> Material.BLAZE_ROD;
            case SPOOK_SWARM -> Material.SOUL_LANTERN;
            case TOTEM_EXPLOSION -> Material.TOTEM_OF_UNDYING;
            case GOLDEN_GAP -> Material.GOLDEN_APPLE;
        };
    }

    public static Optional<KillEffectType> fromId(String id) {
        if (id == null) return Optional.empty();
        for (KillEffectType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
