package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EloManager {

    public static final int DEFAULT_ELO = 1000;
    private static final int K_FACTOR = 32;

    public static class EloData {
        public int elo;
        public int matchesPlayed;

        public EloData(int elo, int matchesPlayed) {
            this.elo = elo;
            this.matchesPlayed = matchesPlayed;
        }
    }

    private final LemonPractice plugin;
    // uuid -> (gamemode -> EloData)
    private final Map<UUID, Map<String, EloData>> cache = new HashMap<>();

    public EloManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    public int getPlacementCount() {
        return plugin.getConfig().getInt("placement_count", 10);
    }

    // -----------------------------------------------------------------------
    // Cache access
    // -----------------------------------------------------------------------

    public int getElo(UUID uuid, String gamemode) {
        EloData data = getCached(uuid, gamemode.toLowerCase());
        return data != null ? data.elo : DEFAULT_ELO;
    }

    public int getMatchesPlayed(UUID uuid, String gamemode) {
        EloData data = getCached(uuid, gamemode.toLowerCase());
        return data != null ? data.matchesPlayed : 0;
    }

    public boolean isInPlacement(UUID uuid, String gamemode) {
        return getMatchesPlayed(uuid, gamemode) < getPlacementCount();
    }

    /**
     * Returns the ELO used during matchmaking.
     * Placement players always appear as DEFAULT_ELO so they match into
     * the normal 1000-range bracket.
     */
    public int getEloForMatchmaking(UUID uuid, String gamemode) {
        if (isInPlacement(uuid, gamemode)) return DEFAULT_ELO;
        return getElo(uuid, gamemode);
    }

    private EloData getCached(UUID uuid, String gamemode) {
        Map<String, EloData> playerMap = cache.get(uuid);
        if (playerMap == null) return null;
        return playerMap.get(gamemode);
    }

    // -----------------------------------------------------------------------
    // Load from DB
    // -----------------------------------------------------------------------

    public CompletableFuture<EloData> loadEloData(UUID uuid, String gamemode) {
        String gm = gamemode.toLowerCase();
        return plugin.getDatabase().getEloData(uuid, gm).thenApply(data -> {
            if (data == null) data = new EloData(DEFAULT_ELO, 0);
            cache.computeIfAbsent(uuid, u -> new HashMap<>()).put(gm, data);
            return data;
        });
    }

    // -----------------------------------------------------------------------
    // Apply duel result  (K=32, standard Elo)
    // -----------------------------------------------------------------------

    /**
     * Computes new ELO for both players, increments matches_played,
     * updates cache and DB. Returns int[]{winnerChange, loserChange}.
     */
    public CompletableFuture<int[]> applyDuelResult(UUID winner, UUID loser, String gamemode) {
        String gm = gamemode.toLowerCase();

        CompletableFuture<EloData> wFuture = getCached(winner, gm) != null
                ? CompletableFuture.completedFuture(getCached(winner, gm))
                : loadEloData(winner, gm);

        CompletableFuture<EloData> lFuture = getCached(loser, gm) != null
                ? CompletableFuture.completedFuture(getCached(loser, gm))
                : loadEloData(loser, gm);

        return wFuture.thenCombine(lFuture, (wData, lData) -> {
            int wElo = wData.elo;
            int lElo = lData.elo;

            int wChange = calculateChange(wElo, lElo, true);
            int lChange = calculateChange(lElo, wElo, false);

            wData.elo = Math.max(0, wElo + wChange);
            wData.matchesPlayed++;
            lData.elo = Math.max(0, lElo + lChange);
            lData.matchesPlayed++;

            plugin.getDatabase().saveEloData(winner, gm, wData.elo, wData.matchesPlayed);
            plugin.getDatabase().saveEloData(loser,  gm, lData.elo, lData.matchesPlayed);

            plugin.getLogger().info("[EloManager] " + winner + " (" + wElo + "->" + wData.elo
                    + ") beat " + loser + " (" + lElo + "->" + lData.elo + ") [" + gm + "]");

            return new int[]{wChange, lChange};
        });
    }

    private int calculateChange(int current, int opponent, boolean won) {
        double expected = 1.0 / (1.0 + Math.pow(10.0, (double)(opponent - current) / 400.0));
        return (int) Math.round(K_FACTOR * ((won ? 1.0 : 0.0) - expected));
    }

    // -----------------------------------------------------------------------
    // Admin manipulation
    // -----------------------------------------------------------------------

    public CompletableFuture<Integer> adminAdd(UUID uuid, String gamemode, int amount) {
        return ensureLoaded(uuid, gamemode).thenApply(data -> {
            data.elo = Math.max(0, data.elo + amount);
            plugin.getDatabase().saveEloData(uuid, gamemode.toLowerCase(), data.elo, data.matchesPlayed);
            return data.elo;
        });
    }

    public CompletableFuture<Integer> adminRemove(UUID uuid, String gamemode, int amount) {
        return adminAdd(uuid, gamemode, -amount);
    }

    public CompletableFuture<Integer> adminSet(UUID uuid, String gamemode, int value) {
        return ensureLoaded(uuid, gamemode).thenApply(data -> {
            data.elo = value;
            plugin.getDatabase().saveEloData(uuid, gamemode.toLowerCase(), data.elo, data.matchesPlayed);
            return data.elo;
        });
    }

    /** Resets ELO to 1000 and clears matches_played → triggers placement matches again. */
    public CompletableFuture<Integer> adminReset(UUID uuid, String gamemode) {
        return ensureLoaded(uuid, gamemode).thenApply(data -> {
            data.elo = DEFAULT_ELO;
            data.matchesPlayed = 0;
            plugin.getDatabase().saveEloData(uuid, gamemode.toLowerCase(), DEFAULT_ELO, 0);
            return DEFAULT_ELO;
        });
    }

    private CompletableFuture<EloData> ensureLoaded(UUID uuid, String gamemode) {
        String gm = gamemode.toLowerCase();
        EloData cached = getCached(uuid, gm);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return loadEloData(uuid, gm);
    }

    // -----------------------------------------------------------------------
    // Cache eviction
    // -----------------------------------------------------------------------

    public void evict(UUID uuid) {
        cache.remove(uuid);
    }
}
