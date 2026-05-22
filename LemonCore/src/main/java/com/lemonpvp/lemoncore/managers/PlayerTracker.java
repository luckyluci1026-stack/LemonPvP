package com.lemonpvp.lemoncore.managers;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerTracker {

    private final Map<UUID, ArrayDeque<Long>> clickTimestamps = new HashMap<>();
    private final Map<UUID, Double> lastReach = new HashMap<>();
    private final Map<UUID, Long> joinTimes = new HashMap<>();

    public void recordJoin(UUID uuid) {
        joinTimes.put(uuid, System.currentTimeMillis());
        clickTimestamps.put(uuid, new ArrayDeque<>());
    }

    public void cleanup(UUID uuid) {
        joinTimes.remove(uuid);
        clickTimestamps.remove(uuid);
        lastReach.remove(uuid);
    }

    public void recordClick(UUID uuid) {
        ArrayDeque<Long> times = clickTimestamps.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        // Deduplicate events that fire within 50 ms of each other (same physical click)
        if (!times.isEmpty() && now - times.peekLast() < 50L) return;
        times.addLast(now);
        while (times.size() > 20) times.pollFirst();
    }

    public int getCps(UUID uuid) {
        ArrayDeque<Long> times = clickTimestamps.get(uuid);
        if (times == null || times.isEmpty()) return 0;
        long cutoff = System.currentTimeMillis() - 1000L;
        int count = 0;
        for (long t : times) {
            if (t >= cutoff) count++;
        }
        return count;
    }

    public void recordReach(UUID uuid, double reach) {
        lastReach.put(uuid, reach);
    }

    public double getLastReach(UUID uuid) {
        return lastReach.getOrDefault(uuid, 0.0);
    }

    public long getOnlineMillis(UUID uuid) {
        Long join = joinTimes.get(uuid);
        return join == null ? 0L : System.currentTimeMillis() - join;
    }
}
