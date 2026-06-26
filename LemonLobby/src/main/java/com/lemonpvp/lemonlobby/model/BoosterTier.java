package com.lemonpvp.lemonlobby.model;

/**
 * Represents a single booster tier loaded from boosters.lemon.
 * No longer an enum — instances are managed by BoosterConfig.
 */
public final class BoosterTier {

    public final String id;
    public final int    level;
    public final String displayName;
    public final long   appleCost;
    public final int    bonusApples;
    public final int    durationSeconds;

    public BoosterTier(String id, int level, String displayName,
                       long appleCost, int bonusApples, int durationSeconds) {
        this.id              = id;
        this.level           = level;
        this.displayName     = displayName;
        this.appleCost       = appleCost;
        this.bonusApples     = bonusApples;
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
}
