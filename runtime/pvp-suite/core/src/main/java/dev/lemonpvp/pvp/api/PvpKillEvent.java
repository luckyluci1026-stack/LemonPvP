package dev.lemonpvp.pvp.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired by pvp-core on the main thread after a player kill has been counted. Other
 * modules react to it (killstreak announcements, scoreboard updates) instead of listening
 * to deaths themselves, so every module sees the same, already updated numbers.
 */
public final class PvpKillEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PlayerStats killer;
    private final PlayerStats victim;
    private final int ratingChange;
    private final int endedStreak;
    private final boolean simulated;

    /**
     * @param killer       killer's statistics after the kill
     * @param victim       victim's statistics after the death
     * @param ratingChange rating the killer gained and the victim lost
     * @param endedStreak  the victim's streak that this kill ended (0 if none)
     * @param simulated    whether an administrator simulated the kill ({@code /pvp sim})
     */
    public PvpKillEvent(PlayerStats killer, PlayerStats victim, int ratingChange, int endedStreak, boolean simulated) {
        super(false);
        this.killer = killer;
        this.victim = victim;
        this.ratingChange = ratingChange;
        this.endedStreak = endedStreak;
        this.simulated = simulated;
    }

    /** @return the killer's statistics after the kill */
    public PlayerStats killer() {
        return this.killer;
    }

    /** @return the victim's statistics after the death */
    public PlayerStats victim() {
        return this.victim;
    }

    /** @return rating points the killer gained and the victim lost */
    public int ratingChange() {
        return this.ratingChange;
    }

    /** @return the victim's streak that this kill ended (0 if none) */
    public int endedStreak() {
        return this.endedStreak;
    }

    /** @return whether the kill was simulated with {@code /pvp sim} (no real players involved) */
    public boolean simulated() {
        return this.simulated;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /** @return the handler list (looked up by name by the server's event manager) */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
