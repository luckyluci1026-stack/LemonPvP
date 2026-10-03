package de.lemonpvp.bettersmp.storage;

import java.util.UUID;

/**
 * Momentaufnahme der Statistiken eines Spielers.
 */
public record StatSnapshot(
        UUID uuid, String name,
        int kills, int deaths, int mobKills,
        long playtime, int joins, long lastSeen
) {

    public static StatSnapshot empty(UUID uuid) {
        return new StatSnapshot(uuid, null, 0, 0, 0, 0L, 0, 0L);
    }

    public double kd() {
        return deaths == 0 ? kills : Math.round((double) kills / deaths * 100.0) / 100.0;
    }
}
