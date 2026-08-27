package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks duel win streaks — the current consecutive-win run and the all-time
 * best — cached in memory and persisted to {@code lp_duel_streaks}. The cache is
 * warmed on join and evicted on quit; a win increments the run and bumps the
 * best, a loss resets the run to zero (the best is kept). Streaks are tracked
 * independently of ranked ELO so they work on a fully casual server too.
 */
public class StreakManager {

    private final LemonPractice plugin;
    /** uuid -> {current, best} */
    private final Map<UUID, int[]> cache = new ConcurrentHashMap<>();

    public StreakManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    /** Warms the cache from the database (call on join). */
    public void preload(UUID uuid) {
        plugin.getDatabase().getStreak(uuid).thenAccept(d ->
                cache.put(uuid, new int[]{d.current(), d.best()}));
    }

    public void evict(UUID uuid) {
        cache.remove(uuid);
    }

    /** Current run for a cached player, or 0 if not loaded. */
    public int getCurrent(UUID uuid) {
        int[] s = cache.get(uuid);
        return s != null ? s[0] : 0;
    }

    /** All-time best run for a cached player, or 0 if not loaded. */
    public int getBest(UUID uuid) {
        int[] s = cache.get(uuid);
        return s != null ? s[1] : 0;
    }

    /**
     * Records a win: increments the run, bumps the best if beaten, and persists.
     * Completes with the new current run so callers can message milestones.
     */
    public CompletableFuture<Integer> recordWin(UUID uuid) {
        int[] cached = cache.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(applyWin(uuid, cached));
        return plugin.getDatabase().getStreak(uuid)
                .thenApply(d -> applyWin(uuid, new int[]{d.current(), d.best()}));
    }

    private int applyWin(UUID uuid, int[] state) {
        int current = state[0] + 1;
        int best = Math.max(state[1], current);
        cache.put(uuid, new int[]{current, best});
        plugin.getDatabase().saveStreak(uuid, current, best);
        return current;
    }

    /** Records a loss: resets the current run to zero (best is kept) and persists. */
    public void recordLoss(UUID uuid) {
        int[] cached = cache.get(uuid);
        if (cached != null) {
            int best = cached[1];
            cache.put(uuid, new int[]{0, best});
            plugin.getDatabase().saveStreak(uuid, 0, best);
        } else {
            plugin.getDatabase().getStreak(uuid).thenAccept(d -> {
                cache.put(uuid, new int[]{0, d.best()});
                plugin.getDatabase().saveStreak(uuid, 0, d.best());
            });
        }
    }
}
