package dev.lemonpvp.pvp.api;

import java.util.UUID;

/**
 * PvP statistics of one player at one moment. Immutable: a new object is created after
 * every change, so it can be passed around (events, other modules) without surprises.
 *
 * @param id         player UUID
 * @param name       last known player name
 * @param kills      players killed
 * @param deaths     deaths (by players or otherwise)
 * @param streak     kills since the last death
 * @param bestStreak highest streak ever reached
 * @param rating     Elo rating (starts at 1000)
 */
public record PlayerStats(UUID id, String name, int kills, int deaths, int streak, int bestStreak, int rating) {

    /** @return kills per death; the kill count while the player has never died */
    public double kd() {
        return this.deaths == 0 ? this.kills : (double) this.kills / this.deaths;
    }
}
