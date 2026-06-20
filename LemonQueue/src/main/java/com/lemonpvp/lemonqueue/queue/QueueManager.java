package com.lemonpvp.lemonqueue.queue;

import com.lemonpvp.lemonqueue.LemonQueue;
import com.lemonpvp.lemonqueue.config.QueueConfig;
import com.lemonpvp.lemonqueue.util.Gradients;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.slf4j.Logger;

import java.time.Duration;
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
    private final QueueConfig config;

    private final Map<String, ServerQueue> queues = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastPosition = new ConcurrentHashMap<>();

    private ScheduledTask processTask;
    private ScheduledTask displayTask;

    public QueueManager(LemonQueue plugin, ProxyServer proxy, Logger logger, QueueConfig config) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.logger = logger;
        this.config = config;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    public void start() {
        processTask = proxy.getScheduler().buildTask(plugin, this::process)
                .repeat(config.getProcessInterval(), TimeUnit.MILLISECONDS)
                .schedule();
        displayTask = proxy.getScheduler().buildTask(plugin, this::updateDisplays)
                .repeat(config.getUpdateInterval(), TimeUnit.MILLISECONDS)
                .schedule();
    }

    public void stop() {
        if (processTask != null) processTask.cancel();
        if (displayTask != null) displayTask.cancel();
        queues.clear();
        lastPosition.clear();
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
                Gradients.lemon("In der Warteschlange"),
                Gradients.fire("Platz " + pos + " von " + total),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofMillis(600))));
        player.sendMessage(Gradients.lemonAnimated(
                "» Der Server ist voll – du wurdest in die Warteschlange aufgenommen (Platz "
                        + pos + "/" + total + ")."));
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
        for (ServerQueue queue : queues.values()) {
            Optional<RegisteredServer> opt = proxy.getServer(queue.getTargetServer());
            if (opt.isEmpty()) continue;
            RegisteredServer target = opt.get();

            int free = config.getMaxPlayers(queue.getTargetServer()) - target.getPlayersConnected().size();
            if (free <= 0) continue;

            int batch = Math.min(free, config.getSendBatch());
            int sent = 0;

            for (QueuedPlayer qp : queue.snapshot()) {
                if (sent >= batch) break;
                if (qp.isSending()) continue;

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
                        player.clearTitle();
                        player.sendActionBar(Gradients.rainbowAnimated("✔ Verbunden mit " + queue.getTargetServer() + "!"));
                        player.playSound(CONNECT);
                    } else {
                        // Target rejected (full again / down): keep the player queued.
                        qp.setSending(false);
                    }
                });
            }
        }
    }

    private void updateDisplays() {
        for (ServerQueue queue : queues.values()) {
            var snapshot = queue.snapshot();
            int total = snapshot.size();
            for (int i = 0; i < snapshot.size(); i++) {
                QueuedPlayer qp = snapshot.get(i);
                Player player = qp.getPlayer();
                if (player == null || !player.isActive()) continue;

                int pos = i + 1;
                Component bar = Component.text()
                        .append(Gradients.fireAnimated("⏳ Warteschlange "))
                        .append(Component.text("» ", NamedTextColor.DARK_GRAY))
                        .append(Gradients.lemonAnimated("Platz " + pos + " / " + total))
                        .build();
                player.sendActionBar(bar);

                // Soft chime when the player advances.
                Integer prev = lastPosition.put(qp.getUuid(), pos);
                if (prev != null && pos < prev) player.playSound(MOVE_UP);
            }
        }
    }
}
