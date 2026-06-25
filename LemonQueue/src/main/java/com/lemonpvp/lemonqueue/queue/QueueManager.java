package com.lemonpvp.lemonqueue.queue;

import com.lemonpvp.lemonqueue.LemonQueue;
import com.lemonpvp.lemonqueue.config.Messages;
import com.lemonpvp.lemonqueue.config.QueueConfig;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Owns all per-server queues and the two repeating tasks that drive them:
 * <ul>
 *   <li><b>process</b> – moves players from the limbo to their target server
 *       as soon as a slot frees up (priority + FIFO order).</li>
 *   <li><b>display</b> – animates the action bar with each waiting player's
 *       live position and plays a soft chime when they move up.</li>
 * </ul>
 */
public class QueueManager {

    private static final Sound MOVE_UP = Sound.sound(
            Key.key("minecraft:block.note_block.pling"), Sound.Source.MASTER, 0.6f, 1.4f);
    private static final Sound CONNECT = Sound.sound(
            Key.key("minecraft:entity.player.levelup"), Sound.Source.MASTER, 0.7f, 1.6f);

    private final LemonQueue plugin;
    private final ProxyServer proxy;
    private final Logger logger;
    // volatile so reloadConfig() (command thread) is visible to scheduler threads.
    private volatile QueueConfig config;

    /** Sliding window of release timestamps (ms) used to estimate wait time. */
    private static final long ETA_WINDOW_MS = 60_000L;
    /** How long a "banning" mark is kept before it expires (safety net). */
    private static final long BANNING_TTL_MS = 10_000L;
    /** Max time a connect request may be in flight before we retry the player. */
    private static final long SEND_TIMEOUT_MS = 10_000L;

    private final Map<String, ServerQueue> queues = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastPosition = new ConcurrentHashMap<>();
    private final Deque<Long> recentReleases = new ArrayDeque<>();
    /** UUIDs of players whose next kick must NOT be re-routed to limbo (ban kicks). */
    private final Map<UUID, Long> banningMarks = new ConcurrentHashMap<>();

    // volatile so that reloadConfig() is immediately visible to the scheduler threads.
    private volatile Messages msg;

    private ScheduledTask processTask;
    private ScheduledTask displayTask;
    private ServerHealthCache healthCache;

    public QueueManager(LemonQueue plugin, ProxyServer proxy, Logger logger, QueueConfig config) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.logger = logger;
        this.config = config;
        this.msg = config.getMessages();
    }

    /** Hot-reloads config + messages without interrupting active queues. */
    public void reloadConfig(QueueConfig newConfig) {
        this.config = newConfig;
        this.msg = newConfig.getMessages();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    public void start() {
        // Health monitor: watches limbo + default target, logs once on state change.
        healthCache = new ServerHealthCache(proxy, logger,
                config.getLimboServer(), config.getDefaultTarget());
        healthCache.start(plugin);

        processTask = proxy.getScheduler().buildTask(plugin, this::process)
                .repeat(config.getProcessInterval(), TimeUnit.MILLISECONDS)
                .schedule();
        displayTask = proxy.getScheduler().buildTask(plugin, this::updateDisplays)
                .repeat(config.getUpdateInterval(), TimeUnit.MILLISECONDS)
                .schedule();
    }

    public void stop() {
        if (healthCache != null) { healthCache.stop(); healthCache = null; }
        if (processTask != null) processTask.cancel();
        if (displayTask != null) displayTask.cancel();
        queues.clear();
        lastPosition.clear();
    }

    /** Returns true if the limbo server is currently reachable. */
    public boolean isLimboOnline() {
        return healthCache == null || healthCache.isOnline(config.getLimboServer());
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Returns true if the given target server currently has a free slot. */
    public boolean hasFreeSlot(String target) {
        Optional<RegisteredServer> server = proxy.getServer(target);
        if (server.isEmpty()) return false;
        return server.get().getPlayersConnected().size() < config.getMaxPlayers(target);
    }

    /** Adds a player to the waiting list for {@code target} (no-op if already queued). */
    public void enqueue(Player player, String target) {
        ServerQueue queue = queues.computeIfAbsent(target, ServerQueue::new);
        if (queue.contains(player.getUniqueId())) return;

        int priority = computePriority(player);
        queue.add(new QueuedPlayer(player, target, priority, System.currentTimeMillis()));

        int total = queue.size();
        int pos = queue.position(player.getUniqueId());
        player.showTitle(Title.title(
                msg.joinTitle(),
                msg.joinSubtitle(pos, total),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofMillis(600))));
        player.sendMessage(msg.joinMessage(pos, total));
    }

    /**
     * Marks a player as "about to be ban-kicked". The next
     * {@code KickedFromServerEvent} for this UUID will not be intercepted.
     * The mark expires after {@value #BANNING_TTL_MS} ms automatically.
     */
    public void markBanning(UUID uuid) {
        banningMarks.put(uuid, System.currentTimeMillis() + BANNING_TTL_MS);
    }

    /**
     * Returns true if this player's kick should bypass the limbo redirect, and
     * consumes the mark so it only suppresses a single (the ban) kick — a later
     * unrelated kick within the TTL is handled normally.
     */
    public boolean consumeBanning(UUID uuid) {
        Long expiry = banningMarks.remove(uuid);
        if (expiry == null) return false;
        return System.currentTimeMillis() <= expiry;
    }

    /** Removes a player from every queue (call on disconnect / leave). */
    public void dequeue(UUID uuid) {
        for (ServerQueue q : queues.values()) q.remove(uuid);
        lastPosition.remove(uuid);
    }

    public int positionOf(UUID uuid) {
        for (ServerQueue q : queues.values()) {
            int p = q.position(uuid);
            if (p > 0) return p;
        }
        return -1;
    }

    public int totalQueued() {
        int sum = 0;
        for (ServerQueue q : queues.values()) sum += q.size();
        return sum;
    }

    public Map<String, ServerQueue> getQueues() {
        return queues;
    }

    public void clearQueue(String target) {
        ServerQueue q = queues.get(target);
        if (q != null) {
            for (QueuedPlayer qp : q.snapshot()) lastPosition.remove(qp.getUuid());
            queues.remove(target);
        }
    }

    /** Highest priority the player qualifies for via permissions; 0 if none. */
    public int computePriority(Player player) {
        int best = 0;
        for (Map.Entry<String, Integer> e : config.getPriorities().entrySet()) {
            if (player.hasPermission(e.getKey())) best = Math.max(best, e.getValue());
        }
        return best;
    }

    // ── Tasks ─────────────────────────────────────────────────────────────

    private void process() {
        // Evict expired banning marks so the map doesn't grow unboundedly.
        long now = System.currentTimeMillis();
        banningMarks.values().removeIf(expiry -> now > expiry);

        for (ServerQueue queue : queues.values()) {
            // Skip silently when the target is known offline — ServerHealthCache
            // already logged the state change once; no further spam needed.
            String targetName = queue.getTargetServer();
            if (healthCache != null && !healthCache.isOnline(targetName)) continue;

            Optional<RegisteredServer> opt = proxy.getServer(targetName);
            if (opt.isEmpty()) continue;
            RegisteredServer target = opt.get();

            int free = config.getMaxPlayers(targetName) - target.getPlayersConnected().size();
            if (free <= 0) continue;

            int batch = Math.min(free, config.getSendBatch());
            int sent = 0;

            for (QueuedPlayer qp : queue.snapshot()) {
                if (sent >= batch) break;
                // Skip players with an in-flight request unless it has hung.
                if (qp.isSending() && !qp.isSendingStale(SEND_TIMEOUT_MS)) continue;

                Player player = qp.getPlayer();
                if (player == null || !player.isActive()) {
                    queue.remove(qp.getUuid());
                    lastPosition.remove(qp.getUuid());
                    continue;
                }

                qp.setSending(true);
                sent++;
                player.createConnectionRequest(target).connect().whenComplete((result, err) -> {
                    if (err == null && result != null && result.isSuccessful()) {
                        queue.remove(qp.getUuid());
                        lastPosition.remove(qp.getUuid());
                        recordRelease();
                        if (player.isActive()) {
                            player.clearTitle();
                            player.sendActionBar(msg.actionbarConnected(targetName));
                            player.playSound(CONNECT);
                        }
                    } else {
                        // Target rejected (full again / went down): keep player queued.
                        // Do NOT log here — health cache will log the offline event once.
                        qp.setSending(false);
                    }
                });
            }
        }
    }

    private void updateDisplays() {
        // Title is re-sent every tick; stay must exceed the update interval so the
        // text never fades between refreshes (fade-in 0 keeps the animation smooth).
        Title.Times times = Title.Times.times(
                Duration.ZERO,
                Duration.ofMillis(config.getUpdateInterval() + 1000L),
                Duration.ZERO);

        for (ServerQueue queue : queues.values()) {
            var snapshot = queue.snapshot();
            // Count only players still online so positions/totals match reality.
            int total = 0;
            for (QueuedPlayer qp : snapshot) {
                Player p = qp.getPlayer();
                if (p != null && p.isActive()) total++;
            }
            int pos = 0;
            for (int i = 0; i < snapshot.size(); i++) {
                QueuedPlayer qp = snapshot.get(i);
                Player player = qp.getPlayer();
                if (player == null || !player.isActive()) continue;

                pos++;
                String eta = formatEta(pos);

                player.sendActionBar(msg.actionbarWaiting(pos, total, eta));

                if (config.isTitleEnabled()) {
                    player.showTitle(Title.title(
                            msg.title(pos, total, eta),
                            msg.subtitle(pos, total, eta),
                            times));
                }

                // Soft chime when the player advances.
                Integer prev = lastPosition.put(qp.getUuid(), pos);
                if (prev != null && pos < prev) player.playSound(MOVE_UP);
            }
        }
    }

    // ── ETA helper ────────────────────────────────────────────────────────

    /** Records that a player was just released to a backend (for ETA rate). */
    private void recordRelease() {
        synchronized (recentReleases) {
            recentReleases.addLast(System.currentTimeMillis());
        }
    }

    /**
     * Estimates the wait for a player at {@code pos} from the recent release
     * rate, formatted as {@code "Mm Ss"} / {@code "Ss"}. Falls back to a friendly
     * placeholder until enough samples exist.
     */
    private String formatEta(int pos) {
        long now = System.currentTimeMillis();
        int count;
        long oldest;
        synchronized (recentReleases) {
            while (!recentReleases.isEmpty() && now - recentReleases.peekFirst() > ETA_WINDOW_MS) {
                recentReleases.pollFirst();
            }
            count = recentReleases.size();
            oldest = recentReleases.isEmpty() ? now : recentReleases.peekFirst();
        }
        if (count == 0) return "…";

        double windowSec = Math.max(1.0, (now - oldest) / 1000.0);
        double ratePerSec = count / windowSec;            // releases per second
        if (ratePerSec <= 0.0) return "…";

        long seconds = (long) Math.ceil(pos / ratePerSec);
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        long rem = seconds % 60;
        return rem == 0 ? minutes + "m" : minutes + "m " + rem + "s";
    }
}
