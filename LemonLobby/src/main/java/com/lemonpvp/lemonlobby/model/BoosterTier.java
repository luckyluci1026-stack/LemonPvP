package com.lemonpvp.lemonlobby.model;

public enum BoosterTier {

    L1 (1, "Stufe I",    2_000,   3,  25, 180),
    L2 (2, "Stufe II",   8_000,  10,  20, 300),
    L3 (3, "Stufe III", 15_000,  20,  15, 450),
    L4 (4, "Stufe IV",  25_000,  35,  10, 600),
    L5 (5, "Stufe V",   50_000,  50,   5, 900);

    public final int level;
    public final String displayName;
    public final long appleCost;
    public final int bonusApples;
    public final int maxUses;
    public final int durationSeconds;

    BoosterTier(int level, String displayName, long appleCost, int bonusApples, int maxUses, int durationSeconds) {
        this.level           = level;
        this.displayName     = displayName;
        this.appleCost       = appleCost;
        this.bonusApples     = bonusApples;
        this.maxUses         = maxUses;
        this.durationSeconds = durationSeconds;
    }

    public String priceDisplay() {
        return "<yellow>" + com.lemonpvp.lemonlobby.util.FormatUtil.formatAmount(appleCost) + " <white>Äpfel";
    }

    public String durationDisplay() {
        if (durationSeconds < 60) return durationSeconds + "s";
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return seconds == 0 ? minutes + " Min" : minutes + "m " + seconds + "s";
    }

    public static BoosterTier fromLevel(int level) {
        for (BoosterTier t : values()) {
            if (t.level == level) return t;
        }
        return null;
    }
}
