package de.lemonpvp.bettersmp.stats;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verfolgt Spielzeit-Sitzungen und schreibt sie regelmaessig in die DB.
 * Kills/Deaths/Mob-Kills laufen ueber den StatsListener direkt in die DB.
 */
public final class StatsManager {

    private final BetterSMP plugin;
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();
    private BukkitTask flushTask;

    public StatsManager(BetterSMP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        long interval = Math.max(10, plugin.getConfig().getLong("stats.flush-interval-seconds", 60)) * 20L;
        flushTask = Bukkit.getScheduler().runTaskTimer(plugin, this::flushAll, interval, interval);
    }

    public void stop() {
        if (flushTask != null) {
            flushTask.cancel();
        }
        flushAll();
        sessionStart.clear();
    }

    public void startSession(UUID uuid) {
        sessionStart.put(uuid, System.currentTimeMillis());
    }

    /** Beendet die Sitzung, schreibt die Spielzeit und last_seen. */
    public void endSession(UUID uuid) {
        Long start = sessionStart.remove(uuid);
        long now = System.currentTimeMillis();
        if (start != null) {
            long seconds = (now - start) / 1000L;
            if (seconds > 0) {
                plugin.database().bump(uuid, "playtime", seconds);
            }
        }
        plugin.database().setLastSeen(uuid, now);
    }

    private void flushAll() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Long start = sessionStart.get(uuid);
            if (start != null) {
                long seconds = (now - start) / 1000L;
                if (seconds > 0) {
                    plugin.database().bump(uuid, "playtime", seconds);
                    sessionStart.put(uuid, now);
                }
            }
        }
    }
}
