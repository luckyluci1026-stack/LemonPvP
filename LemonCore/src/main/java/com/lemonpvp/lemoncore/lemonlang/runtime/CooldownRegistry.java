package com.lemonpvp.lemoncore.lemonlang.runtime;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player, per-key cooldown timestamps for LemonLang items, commands
 * and {@code cooldown} statements. All times are epoch milliseconds.
 */
public class CooldownRegistry {

    /** player UUID -> (cooldown key -> expiry time in epoch ms). */
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    /** True if the given key is currently on cooldown for the player. */
    public boolean isOnCooldown(UUID uuid, String key) {
        return getRemainingMs(uuid, key) > 0L;
    }

    /** Remaining cooldown in ms (0 if none / expired). */
    public long getRemainingMs(UUID uuid, String key) {
        if (uuid == null || key == null) {
            return 0L;
        }
        Map<String, Long> map = cooldowns.get(uuid);
        if (map == null) {
            return 0L;
        }
        Long expiry = map.get(key);
        if (expiry == null) {
            return 0L;
        }
        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0L) {
            map.remove(key);
            return 0L;
        }
        return remaining;
    }

    /** Sets a cooldown for the player+key lasting {@code durationMs} from now. */
    public void setCooldown(UUID uuid, String key, long durationMs) {
        if (uuid == null || key == null) {
            return;
        }
        if (durationMs <= 0L) {
            Map<String, Long> map = cooldowns.get(uuid);
            if (map != null) {
                map.remove(key);
            }
            return;
        }
        cooldowns
            .computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
            .put(key, System.currentTimeMillis() + durationMs);
    }

    /** Removes all cooldowns for a player (called on quit). */
    public void cleanup(UUID uuid) {
        if (uuid != null) {
            cooldowns.remove(uuid);
        }
    }
}