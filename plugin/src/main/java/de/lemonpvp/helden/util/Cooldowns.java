package de.lemonpvp.helden.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Kleiner Cooldown-Speicher: Spieler -> Schluessel -> Ablaufzeitpunkt. */
public final class Cooldowns {

    private final Map<UUID, Map<String, Long>> expiry = new HashMap<>();

    public void set(UUID player, String key, long seconds) {
        if (seconds <= 0) {
            return;
        }
        expiry.computeIfAbsent(player, ignored -> new HashMap<>())
                .put(key, System.currentTimeMillis() + seconds * 1000L);
    }

    /** Verbleibende Sekunden, aufgerundet. 0 = frei. */
    public long remaining(UUID player, String key) {
        Map<String, Long> entries = expiry.get(player);
        if (entries == null) {
            return 0L;
        }
        Long until = entries.get(key);
        if (until == null) {
            return 0L;
        }
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0L) {
            entries.remove(key);
            return 0L;
        }
        return TimeUtil.toSecondsCeil(remaining);
    }

    public boolean isReady(UUID player, String key) {
        return remaining(player, key) <= 0L;
    }

    public void clear(UUID player, String key) {
        Map<String, Long> entries = expiry.get(player);
        if (entries != null) {
            entries.remove(key);
        }
    }

    public void clear(UUID player) {
        expiry.remove(player);
    }

    public void clearAll() {
        expiry.clear();
    }
}
