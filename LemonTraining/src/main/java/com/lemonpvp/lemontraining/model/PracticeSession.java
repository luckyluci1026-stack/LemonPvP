package com.lemonpvp.lemontraining.model;

import java.util.UUID;

public class PracticeSession {

    private final UUID uuid;
    private final PracticeMode mode;
    private final long startedAt;
    private int hits;
    private SwordDifficulty swordDifficulty;

    public PracticeSession(UUID uuid, PracticeMode mode, long startedAt) {
        this.uuid = uuid;
        this.mode = mode;
        this.startedAt = startedAt;
        this.hits = 0;
        this.swordDifficulty = SwordDifficulty.EASY;
    }

    public UUID getUuid() {
        return uuid;
    }

    public PracticeMode getMode() {
        return mode;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public int getHits() {
        return hits;
    }

    public void setHits(int hits) {
        this.hits = hits;
    }

    public void incrementHits() {
        this.hits++;
    }

    public SwordDifficulty getSwordDifficulty() {
        return swordDifficulty;
    }

    public void setSwordDifficulty(SwordDifficulty swordDifficulty) {
        this.swordDifficulty = swordDifficulty;
    }
}
