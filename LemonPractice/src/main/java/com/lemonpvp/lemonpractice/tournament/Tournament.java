package com.lemonpvp.lemonpractice.tournament;

import java.util.UUID;

/**
 * An official, server-run tournament that spans several days. Players sign up,
 * then earn standings from their ranked duel wins (counted from the practice
 * server's match log) in the tournament's gamemode during the qualification
 * window. When qualification closes the top N are frozen as finalists; the owner
 * runs the finals and records the champion.
 */
public class Tournament {

    public enum State {
        /** Created, taking sign-ups; qualification not started. */
        SIGNUP,
        /** Qualification window open — wins in the gamemode count. */
        QUALIFICATION,
        /** Top finalists locked in; finals being played. */
        FINALS,
        /** Champion decided. */
        ENDED
    }

    private final int id;
    private final String name;
    private final String gamemode;
    private State state;
    /** Epoch millis; 0 until qualification opens. */
    private long startAt;
    private long endAt;
    private UUID champion;

    public Tournament(int id, String name, String gamemode, State state, long startAt, long endAt, UUID champion) {
        this.id = id;
        this.name = name;
        this.gamemode = gamemode;
        this.state = state;
        this.startAt = startAt;
        this.endAt = endAt;
        this.champion = champion;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getGamemode() { return gamemode; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public long getStartAt() { return startAt; }
    public void setStartAt(long startAt) { this.startAt = startAt; }
    public long getEndAt() { return endAt; }
    public void setEndAt(long endAt) { this.endAt = endAt; }

    public UUID getChampion() { return champion; }
    public void setChampion(UUID champion) { this.champion = champion; }

    /** Millis remaining in the qualification window (0 if not running or over). */
    public long qualificationRemaining(long now) {
        if (state != State.QUALIFICATION || endAt <= 0) return 0;
        return Math.max(0, endAt - now);
    }
}
