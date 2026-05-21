package com.lemonpvp.lemonpractice.model;

import org.bukkit.Material;

public class Gamemode {
    private final String id;
    private final String name;
    private final String displayName;
    private final Material material;
    private final int slot;
    private final boolean enabled;

    public Gamemode(String id, String name, String displayName, Material material, int slot, boolean enabled) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.material = material;
        this.slot = slot;
        this.enabled = enabled;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public int getSlot() { return slot; }
    public boolean isEnabled() { return enabled; }
}
