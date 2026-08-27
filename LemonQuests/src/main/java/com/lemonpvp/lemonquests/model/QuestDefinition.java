package com.lemonpvp.lemonquests.model;

public class QuestDefinition {

    private final String id;
    private final String displayName;
    private final String description;
    private final QuestType type;
    private final int target;
    private final QuestRewardType rewardType;
    private final int rewardXp;
    private final int rewardCoins;
    private final QuestDifficulty difficulty;
    private final boolean daily;
    private final String date;
    private final boolean enabled;

    public QuestDefinition(
            String id,
            String displayName,
            String description,
            QuestType type,
            int target,
            QuestRewardType rewardType,
            int rewardXp,
            int rewardCoins,
            QuestDifficulty difficulty,
            boolean daily,
            String date,
            boolean enabled) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.target = target;
        this.rewardType = rewardType;
        this.rewardXp = rewardXp;
        this.rewardCoins = rewardCoins;
        this.difficulty = difficulty;
        this.daily = daily;
        this.date = date;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public QuestType getType() {
        return type;
    }

    public int getTarget() {
        return target;
    }

    public QuestRewardType getRewardType() {
        return rewardType;
    }

    public int getRewardXp() {
        return rewardXp;
    }

    public int getRewardCoins() {
        return rewardCoins;
    }

    public QuestDifficulty getDifficulty() {
        return difficulty;
    }

    public boolean isDaily() {
        return daily;
    }

    public String getDate() {
        return date;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the effective XP reward after applying the difficulty multiplier.
     *
     * @param multiplier the multiplier from the difficulty_multipliers config section
     * @return floored integer result of rewardXp * multiplier
     */
    public int getEffectiveXp(double multiplier) {
        return (int) (rewardXp * multiplier);
    }
}
