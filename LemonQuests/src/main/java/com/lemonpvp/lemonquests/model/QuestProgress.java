package com.lemonpvp.lemonquests.model;

public class QuestProgress {

    private final String questId;
    private final String date;
    private int progress;
    private boolean completed;
    private boolean rewardClaimed;

    public QuestProgress(String questId, String date, int progress, boolean completed, boolean rewardClaimed) {
        this.questId = questId;
        this.date = date;
        this.progress = progress;
        this.completed = completed;
        this.rewardClaimed = rewardClaimed;
    }

    public String getQuestId() {
        return questId;
    }

    /**
     * ISO "yyyy-MM-dd" date string for which this progress entry was recorded.
     */
    public String getDate() {
        return date;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isRewardClaimed() {
        return rewardClaimed;
    }

    public void setRewardClaimed(boolean rewardClaimed) {
        this.rewardClaimed = rewardClaimed;
    }
}
