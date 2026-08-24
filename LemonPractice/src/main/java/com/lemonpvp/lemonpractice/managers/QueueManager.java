package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QueueManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;

    /** All queued players: uuid → QueueEntry */
    private final Map<UUID, QueueEntry> queue = new ConcurrentHashMap<>();

    /** Counts reported by Velocity */
    private final Map<String, Integer> playingCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> queueCounts   = new ConcurrentHashMap<>();

    private BukkitTask matchmakerTask;
    private BukkitTask actionBarTask;

    public QueueManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void startTasks() {
        // Matchmaking check every 5 seconds (100 ticks)
        matchmakerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::runMatchmaking, 100L, 100L);
        // Action bar update every second
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateActionBars, 20L, 20L);
    }

    public void stopTasks() {
        if (matchmakerTask != null) { matchmakerTask.cancel(); matchmakerTask = null; }
        if (actionBarTask  != null) { actionBarTask.cancel();  actionBarTask  = null; }
    }

    // -----------------------------------------------------------------------
    // Queue management
    // -----------------------------------------------------------------------

    public void addToQueue(UUID uuid, String gamemode) {
        if (queue.containsKey(uuid)) return;
        if (plugin.getDuelManager().isInDuel(uuid)) return;
        if (plugin.getTeamDuelManager() != null && plugin.getTeamDuelManager().isInTeamDuel(uuid)) return;

        // Pre-load ELO data into cache so matchmaking can read it synchronously
        plugin.getEloManager().loadEloData(uuid, gamemode);

        queue.put(uuid, new QueueEntry(uuid, gamemode.toLowerCase(), System.currentTimeMillis()));
        plugin.getLogger().info("[QueueManager] " + uuid + " joined queue for " + gamemode);
    }

    public void removeFromQueue(UUID uuid) {
        QueueEntry removed = queue.remove(uuid);
        if (removed != null) {
            clearActionBar(uuid);
            plugin.getLogger().info("[QueueManager] " + uuid + " left queue for " + removed.gamemode);
        }
    }

    public boolean isQueued(UUID uuid) {
        return queue.containsKey(uuid);
    }

    /** The gamemode this player is queued for, or null when they are not queued. */
    public String getQueuedGamemode(UUID uuid) {
        QueueEntry entry = queue.get(uuid);
        return entry != null ? entry.gamemode : null;
    }

    // -----------------------------------------------------------------------
    // Matchmaking  (runs on main thread every 5 s)
    // -----------------------------------------------------------------------

    private synchronized void runMatchmaking() {
        // Group entries by gamemode
        Map<String, List<QueueEntry>> byGamemode = new HashMap<>();
        for (QueueEntry entry : queue.values()) {
            byGamemode.computeIfAbsent(entry.gamemode, k -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<String, List<QueueEntry>> gme : byGamemode.entrySet()) {
            String gm = gme.getKey();
            List<QueueEntry> entries = gme.getValue();
            if (entries.size() < 2) continue;

            // Sort: longest-waiting first
            entries.sort(Comparator.comparingLong(e -> e.joinTimeMs));

            Set<UUID> matched = new HashSet<>();

            for (QueueEntry seeker : entries) {
                if (matched.contains(seeker.uuid)) continue;

                long waitSecs = (System.currentTimeMillis() - seeker.joinTimeMs) / 1000L;
                int queueSize = entries.size() - matched.size();
                double range = computeRange(queueSize, waitSecs);
                int seekerElo = plugin.getEloManager().getEloForMatchmaking(seeker.uuid, gm);

                for (QueueEntry candidate : entries) {
                    if (candidate.uuid.equals(seeker.uuid)) continue;
                    if (matched.contains(candidate.uuid)) continue;

                    int candidateElo = plugin.getEloManager().getEloForMatchmaking(candidate.uuid, gm);
                    if (Math.abs(seekerElo - candidateElo) <= range) {
                        matched.add(seeker.uuid);
                        matched.add(candidate.uuid);
                        queue.remove(seeker.uuid);
                        queue.remove(candidate.uuid);
                        clearActionBar(seeker.uuid);
                        clearActionBar(candidate.uuid);

                        plugin.getLogger().info("[QueueManager] Matched " + seeker.uuid
                                + " vs " + candidate.uuid + " in " + gm
                                + " (eloDiff=" + Math.abs(seekerElo - candidateElo) + ", range=" + (int) range + ")");

                        UUID p1 = seeker.uuid;
                        UUID p2 = candidate.uuid;
                        Bukkit.getScheduler().runTask(plugin,
                                () -> plugin.getDuelManager().startDuel(p1, p2, gm));
                        break;
                    }
                }
            }
        }

        // Bot fallback: anyone still unmatched after the wait threshold fights a
        // skill-scaled bot instead of staring at "Finding opponent..." forever.
        if (plugin.getConfig().getBoolean("queue.bot.enabled", true)) {
            long afterSecs = plugin.getConfig().getLong("queue.bot.after-seconds", 15);
            for (QueueEntry entry : new ArrayList<>(queue.values())) {
                long waited = (System.currentTimeMillis() - entry.joinTimeMs) / 1000L;
                if (waited < afterSecs) continue;
                Player p = Bukkit.getPlayer(entry.uuid);
                if (p == null || !p.isOnline()) { queue.remove(entry.uuid); continue; }
                queue.remove(entry.uuid);
                clearActionBar(entry.uuid);
                String gm = entry.gamemode;
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getBotDuelManager().start(p, gm));
            }
        }
    }

    /**
     * Dynamic matchmaking range.
     *
     * range = max_range − ((max_range − base_range) × min(queueSize / threshold, 1.0))
     *       + max(0, (waitSecs − wait_expansion_seconds) / 10) × wait_expansion_per_10s
     * range = min(range, max_range)
     */
    private double computeRange(int queueSize, long waitSecs) {
        int baseRange   = plugin.getConfig().getInt("elo_scaling.base_range", 100);
        int maxRange    = plugin.getConfig().getInt("elo_scaling.max_range", 1500);
        int threshold   = plugin.getConfig().getInt("elo_scaling.scale_threshold", 20);
        int waitExpSecs = plugin.getConfig().getInt("elo_scaling.wait_expansion_seconds", 30);
        int waitExpPer10 = plugin.getConfig().getInt("elo_scaling.wait_expansion_per_10s", 50);

        double range = maxRange - ((maxRange - baseRange) * Math.min((double) queueSize / threshold, 1.0));
        range += Math.max(0, (waitSecs - waitExpSecs) / 10.0) * waitExpPer10;
        return Math.min(range, maxRange);
    }

    // -----------------------------------------------------------------------
    // Action bars
    // -----------------------------------------------------------------------

    private void updateActionBars() {
        int placementCount = plugin.getEloManager().getPlacementCount();
        for (QueueEntry entry : queue.values()) {
            Player player = Bukkit.getPlayer(entry.uuid);
            if (player == null) continue;

            int matchesDone = plugin.getEloManager().getMatchesPlayed(entry.uuid, entry.gamemode);
            boolean inPlacement = matchesDone < placementCount;

            Component bar;
            if (inPlacement) {
                int next = matchesDone + 1;
                bar = MM.deserialize("<yellow>Placement Match <white>" + next + "/"
                        + placementCount + " <yellow>— <gray>Finding opponent...");
            } else {
                bar = MM.deserialize("<yellow>Finding opponent...");
            }
            player.sendActionBar(bar);
        }
    }

    private void clearActionBar(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) player.sendActionBar(Component.empty());
    }

    // -----------------------------------------------------------------------
    // Velocity-driven match
    // -----------------------------------------------------------------------

    public void handleMatchFound(UUID p1, UUID p2, String gamemode) {
        queue.remove(p1);
        queue.remove(p2);
        clearActionBar(p1);
        clearActionBar(p2);
        Bukkit.getScheduler().runTask(plugin,
                () -> plugin.getDuelManager().startDuel(p1, p2, gamemode));
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
        long local = queue.values().stream()
                .filter(e -> e.gamemode.equals(gamemode.toLowerCase()))
                .count();
        int remote = queueCounts.getOrDefault(gamemode.toLowerCase(), 0);
        return (int) Math.max(local, remote);
    }

    // -----------------------------------------------------------------------
    // Shutdown
    // -----------------------------------------------------------------------

    public void clearQueue() {
        queue.clear();
        stopTasks();
    }

    // -----------------------------------------------------------------------
    // QueueEntry
    // -----------------------------------------------------------------------

    public static class QueueEntry {
        public final UUID uuid;
        public final String gamemode;
        public final long joinTimeMs;

        public QueueEntry(UUID uuid, String gamemode, long joinTimeMs) {
            this.uuid = uuid;
            this.gamemode = gamemode;
            this.joinTimeMs = joinTimeMs;
        }
    }
}
