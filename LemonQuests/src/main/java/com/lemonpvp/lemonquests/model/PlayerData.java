package com.lemonpvp.lemonquests.model;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private int level;
    private int totalXp;

    public PlayerData(UUID uuid, int totalXp) {
        this.uuid = uuid;
        this.totalXp = totalXp;
        this.level = computeLevel();
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel() {
        return level;
    }

    public int getTotalXp() {
        return totalXp;
    }

    /**
     * XP accumulated within the current level (0–99).
     */
    public int getCurrentLevelXp() {
        return totalXp % 100;
    }

    /**
     * XP required to advance from the current level to the next.
     * Always 100.
     */
    public int getXpForNextLevel() {
        return 100;
    }

    /**
     * Derives the level from the total XP.
     * Level = totalXp / 100 + 1 (minimum level 1).
     */
    public int computeLevel() {
        return totalXp / 100 + 1;
    }

    /**
     * Returns the cumulative XP required to reach the given level from level 1.
     *
     * @param level target level (must be >= 1)
     * @return total XP threshold for that level
     */
    public static int xpRequiredForLevel(int level) {
        return (level - 1) * 100;
    }

    /**
     * Adds the given amount to totalXp, recomputes level, and returns true if
     * the player leveled up as a result.
     *
     * @param amount XP to add (must be >= 0)
     * @return true if at least one level was gained
     */
    public boolean addXp(int amount) {
        int previousLevel = this.level;
        this.totalXp += amount;
        this.level = computeLevel();
        return this.level > previousLevel;
    }

    /**
     * Subtracts the given amount from totalXp, flooring at 0, and recomputes level.
     *
     * @param amount XP to subtract (must be >= 0)
     */
    public void subtractXp(int amount) {
        this.totalXp = Math.max(0, this.totalXp - amount);
        this.level = computeLevel();
    }
}
