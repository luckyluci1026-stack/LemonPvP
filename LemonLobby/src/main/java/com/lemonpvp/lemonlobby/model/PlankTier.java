package com.lemonpvp.lemonlobby.model;

import org.bukkit.Material;

public enum PlankTier {

    OAK      ("Oak",          0,          -1, 10, Material.OAK_LOG),
    BIRCH    ("Birch",        50_000,     30, 15, Material.BIRCH_LOG),
    SPRUCE   ("Spruce",      125_000,     30, 20, Material.SPRUCE_LOG),
    ACACIA   ("Acacia",      175_000,     30, 25, Material.ACACIA_LOG),
    DARK_OAK ("Dark Oak",    200_000,     30, 30, Material.DARK_OAK_LOG),
    JUNGLE   ("Jungle",      300_000,     30, 40, Material.JUNGLE_LOG);

    public final String displayName;
    public final long appleCost;
    public final int durationDays; // -1 = permanent (OAK)
    public final int planksPerClick;
    public final Material material;

    PlankTier(String displayName, long appleCost, int durationDays, int planksPerClick, Material material) {
        this.displayName    = displayName;
        this.appleCost      = appleCost;
        this.durationDays   = durationDays;
        this.planksPerClick = planksPerClick;
        this.material       = material;
    }

    public boolean isFree() { return appleCost == 0; }

    public String priceDisplay() {
        if (isFree()) return "<green>Free";
        return "<yellow>" + com.lemonpvp.lemonlobby.util.FormatUtil.formatAmount(appleCost) + " <white>Apples";
    }

    public String durationDisplay() {
        if (durationDays < 0) return "<green>Permanent";
        return "<yellow>" + durationDays + " Days";
    }

    public static PlankTier fromName(String name) {
        for (PlankTier t : values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return OAK;
    }
}
