package com.lemonpvp.lemoncore.managers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks pending friend requests in memory with a configurable expiry.
 *
 * <p>Requests survive until accepted, denied, or expired (default 2 minutes).
 * A sender can only have one pending request to any given target at a time
 * (re-sending refreshes the expiry). Thread-safe — reads and writes may occur
 * from async command handlers.
 */
public class FriendRequestManager {

    private static final long EXPIRY_MS = 2 * 60 * 1000L;

    /** sender UUID → (target UUID → expiry timestamp in ms) */
    private final Map<UUID, Map<UUID, Long>> outgoing = new ConcurrentHashMap<>();

    /** Stores or refreshes a request from {@code sender} to {@code target}. */
    public void send(UUID sender, UUID target) {
        outgoing.computeIfAbsent(sender, k -> new ConcurrentHashMap<>())
                .put(target, System.currentTimeMillis() + EXPIRY_MS);
    }

    /**
     * Returns true if {@code sender} has a still-valid pending request to
     * {@code target}. Expired entries are cleaned up lazily.
     */
    public boolean hasPending(UUID sender, UUID target) {
        Map<UUID, Long> map = outgoing.get(sender);
        if (map == null) return false;
        Long exp = map.get(target);
        if (exp == null) return false;
        if (System.currentTimeMillis() > exp) {
            map.remove(target);
            return false;
        }
        return true;
    }

    /**
     * Removes the request from {@code sender} to {@code target} and returns
     * {@code true} if such a valid request existed.
     */
    public boolean consume(UUID sender, UUID target) {
        Map<UUID, Long> map = outgoing.get(sender);
        if (map == null) return false;
        Long exp = map.remove(target);
        return exp != null && System.currentTimeMillis() <= exp;
    }

    /**
     * Returns all senders who have a non-expired request directed at
     * {@code target}, sorted by expiry (soonest first).
     */
    public List<UUID> pendingTo(UUID target) {
        List<UUID> senders = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Map<UUID, Long>> e : outgoing.entrySet()) {
            Long exp = e.getValue().get(target);
            if (exp != null && now <= exp) senders.add(e.getKey());
        }
        return senders;
    }

    /** Clears all requests sent by or directed at {@code uuid} (called on quit). */
    public void clear(UUID uuid) {
        outgoing.remove(uuid);
        for (Map<UUID, Long> map : outgoing.values()) map.remove(uuid);
    }
}
