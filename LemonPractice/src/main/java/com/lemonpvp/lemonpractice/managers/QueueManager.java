package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QueueManager {

    private final LemonPractice plugin;

    /** player uuid -> gamemode they are queued for */
    private final Map<UUID, String> queue = new ConcurrentHashMap<>();

    /** gamemode -> number of players currently in a duel (updated via Velocity) */
    private final Map<String, Integer> playingCounts = new ConcurrentHashMap<>();

    /** gamemode -> number of players currently queuing (updated via Velocity) */
    private final Map<String, Integer> queueCounts = new ConcurrentHashMap<>();

    public QueueManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Queue management
    // -----------------------------------------------------------------------

    public void addToQueue(UUID playerUuid, String gamemode) {
        if (queue.containsKey(playerUuid)) {
            plugin.getLogger().fine("[QueueManager] " + playerUuid + " is already queued, skipping.");
            return;
        }

        queue.put(playerUuid, gamemode.toLowerCase());
        plugin.getLogger().info("[QueueManager] " + playerUuid + " joined queue for " + gamemode);

        tryMatch(gamemode.toLowerCase());
    }

    public void removeFromQueue(UUID playerUuid) {
        String removed = queue.remove(playerUuid);
        if (removed != null) {
            plugin.getLogger().info("[QueueManager] " + playerUuid + " left queue for " + removed);
        }
    }

    // -----------------------------------------------------------------------
    // Matchmaking
    // -----------------------------------------------------------------------

    /**
     * Tries to find two players queued for the same gamemode and starts a duel.
     * Called on every queue addition and from Velocity "QueueMatch" messages.
     */
    public synchronized void tryMatch(String gamemode) {
        String gm = gamemode.toLowerCase();

        List<UUID> waiting = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : queue.entrySet()) {
            if (entry.getValue().equals(gm)) {
                waiting.add(entry.getKey());
            }
        }

        if (waiting.size() < 2) return;

        UUID p1 = waiting.get(0);
        UUID p2 = waiting.get(1);

        queue.remove(p1);
        queue.remove(p2);

        plugin.getLogger().info("[QueueManager] Match found for " + gm + ": " + p1 + " vs " + p2);

        // Start the duel on the main thread so Bukkit API calls are safe
        Bukkit.getScheduler().runTask(plugin, () ->
                plugin.getDuelManager().startDuel(p1, p2, gm));
    }

    // -----------------------------------------------------------------------
    // Velocity-driven match (lobby signals a match to the duel server)
    // -----------------------------------------------------------------------

    /**
     * Called when the lobby server has matched two players and sent them to
     * this duel server.  We defer to the DuelManager directly.
     */
    public void handleMatchFound(UUID p1, UUID p2, String gamemode) {
        queue.remove(p1);
        queue.remove(p2);

        Bukkit.getScheduler().runTask(plugin, () ->
                plugin.getDuelManager().startDuel(p1, p2, gamemode));
    }

    // -----------------------------------------------------------------------
    // Count tracking (populated by Velocity)
    // -----------------------------------------------------------------------

    public void updateCounts(String gamemode, int playing, int queuing) {
        playingCounts.put(gamemode.toLowerCase(), playing);
        queueCounts.put(gamemode.toLowerCase(), queuing);
    }

    public int getPlayingCount(String gamemode) {
        return playingCounts.getOrDefault(gamemode.toLowerCase(), 0);
    }

    public int getQueueCount(String gamemode) {
        // Combine live local queue count with the value pushed by Velocity so
        // the number is never stale when running on a standalone duel server.
        long local = queue.values().stream()
                .filter(gm -> gm.equals(gamemode.toLowerCase()))
                .count();
        int remote = queueCounts.getOrDefault(gamemode.toLowerCase(), 0);
        return (int) Math.max(local, remote);
    }

    public boolean isQueued(UUID playerUuid) {
        return queue.containsKey(playerUuid);
    }

    // -----------------------------------------------------------------------
    // Shutdown
    // -----------------------------------------------------------------------

    public void clearQueue() {
        queue.clear();
        plugin.getLogger().info("[QueueManager] Queue cleared on shutdown.");
    }
}
