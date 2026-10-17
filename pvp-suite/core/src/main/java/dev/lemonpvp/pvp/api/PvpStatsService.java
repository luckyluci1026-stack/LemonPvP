package dev.lemonpvp.pvp.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read access to the PvP statistics, published by pvp-core. Use it on the server main
 * thread only. Modules that use it declare a dependency on pvp-core in paperswap-module.yml.
 */
public interface PvpStatsService {

    /**
     * @param player player UUID
     * @return the statistics, or empty if the player has no PvP history yet
     */
    Optional<PlayerStats> stats(UUID player);

    /**
     * @param name player name (case-insensitive)
     * @return the statistics, or empty if unknown
     */
    Optional<PlayerStats> statsByName(String name);

    /**
     * @param limit maximum number of entries
     * @return best players first (rating, then kills, then name)
     */
    List<PlayerStats> top(int limit);

    /**
     * @param player player UUID
     * @return position in {@link #top} (1 = best), or 0 if the player has no statistics
     */
    int rank(UUID player);
}
