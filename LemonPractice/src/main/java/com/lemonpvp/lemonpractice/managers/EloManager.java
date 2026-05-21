package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EloManager {

    private static final int DEFAULT_ELO = 1000;
    private static final int K_FACTOR = 32;

    private final LemonPractice plugin;
    // player uuid -> (gamemode -> elo)
    private final Map<UUID, Map<String, Integer>> cache = new HashMap<>();

    public EloManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Retrieval
    // -----------------------------------------------------------------------

    public int getElo(UUID playerUuid, String gamemode) {
        Map<String, Integer> playerElos = cache.get(playerUuid);
        if (playerElos == null) return DEFAULT_ELO;
        return playerElos.getOrDefault(gamemode.toLowerCase(), DEFAULT_ELO);
    }

    public CompletableFuture<Integer> loadElo(UUID playerUuid, String gamemode) {
        return plugin.getDatabase().getElo(playerUuid, gamemode).thenApply(elo -> {
            int value = elo != null ? elo : DEFAULT_ELO;
            cache.computeIfAbsent(playerUuid, u -> new HashMap<>())
                    .put(gamemode.toLowerCase(), value);
            return value;
        });
    }

    // -----------------------------------------------------------------------
    // ELO calculation  (K=32, standard Elo formula)
    // -----------------------------------------------------------------------

    /**
     * Calculates the ELO change for the current player.
     *
     * @param currentElo   the player's current ELO
     * @param opponentElo  the opponent's current ELO
     * @param won          true = win, false = loss, use score=0.5 externally for draw
     * @return the delta (can be negative for a loss)
     */
    public int calculateNewElo(int currentElo, int opponentElo, boolean won) {
        return calculateNewEloWithScore(currentElo, opponentElo, won ? 1.0 : 0.0);
    }

    /**
     * Variant that accepts an explicit score (1.0 win, 0.5 draw, 0.0 loss).
     */
    public int calculateNewEloWithScore(int currentElo, int opponentElo, double score) {
        double expected = 1.0 / (1.0 + Math.pow(10.0, (double) (opponentElo - currentElo) / 400.0));
        return (int) Math.round(K_FACTOR * (score - expected));
    }

    // -----------------------------------------------------------------------
    // Apply duel result
    // -----------------------------------------------------------------------

    /**
     * Fetches both players' ELO, computes new values, updates cache + DB.
     *
     * @return CompletableFuture resolving to int[]{winnerChange, loserChange}
     */
    public CompletableFuture<int[]> applyDuelResult(UUID winner, UUID loser, String gamemode) {
        String gm = gamemode.toLowerCase();

        CompletableFuture<Integer> winnerEloFuture = hasEloInCache(winner, gm)
                ? CompletableFuture.completedFuture(getElo(winner, gm))
                : loadElo(winner, gm);

        CompletableFuture<Integer> loserEloFuture = hasEloInCache(loser, gm)
                ? CompletableFuture.completedFuture(getElo(loser, gm))
                : loadElo(loser, gm);

        return winnerEloFuture.thenCombine(loserEloFuture, (winnerElo, loserElo) -> {
            int winnerChange = calculateNewElo(winnerElo, loserElo, true);
            int loserChange = calculateNewElo(loserElo, winnerElo, false);

            int newWinnerElo = Math.max(0, winnerElo + winnerChange);
            int newLoserElo = Math.max(0, loserElo + loserChange);

            // Update cache
            cache.computeIfAbsent(winner, u -> new HashMap<>()).put(gm, newWinnerElo);
            cache.computeIfAbsent(loser, u -> new HashMap<>()).put(gm, newLoserElo);

            // Persist to DB (fire-and-forget)
            plugin.getDatabase().setElo(winner, gm, newWinnerElo);
            plugin.getDatabase().setElo(loser, gm, newLoserElo);

            plugin.getLogger().info("[EloManager] " + winner + " (" + winnerElo + " -> " + newWinnerElo
                    + ", +" + winnerChange + ") defeated " + loser + " (" + loserElo
                    + " -> " + newLoserElo + ", " + loserChange + ") in " + gm);

            return new int[]{winnerChange, loserChange};
        });
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private boolean hasEloInCache(UUID uuid, String gamemode) {
        Map<String, Integer> playerElos = cache.get(uuid);
        return playerElos != null && playerElos.containsKey(gamemode);
    }

    public void evict(UUID playerUuid) {
        cache.remove(playerUuid);
    }
}
