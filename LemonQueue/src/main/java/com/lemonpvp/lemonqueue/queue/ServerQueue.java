package com.lemonpvp.lemonqueue.queue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * An ordered waiting list of players for a single target server.
 *
 * <p>Order: highest priority first, then earliest join time (FIFO). All access
 * is synchronized because entries are read by the display task and mutated by
 * the process task / event listeners on different threads.</p>
 */
public class ServerQueue {

    private static final Comparator<QueuedPlayer> ORDER = Comparator
            .comparingInt(QueuedPlayer::getPriority).reversed()
            .thenComparingLong(QueuedPlayer::getJoinTime);

    private final String targetServer;
    private final List<QueuedPlayer> players = new ArrayList<>();

    public ServerQueue(String targetServer) {
        this.targetServer = targetServer;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public synchronized void add(QueuedPlayer qp) {
        if (contains(qp.getUuid())) return;
        players.add(qp);
        players.sort(ORDER);
    }

    public synchronized void remove(UUID uuid) {
        players.removeIf(p -> p.getUuid().equals(uuid));
    }

    public synchronized boolean contains(UUID uuid) {
        for (QueuedPlayer p : players) {
            if (p.getUuid().equals(uuid)) return true;
        }
        return false;
    }

    /** 1-based position in the queue, or -1 if not present. */
    public synchronized int position(UUID uuid) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getUuid().equals(uuid)) return i + 1;
        }
        return -1;
    }

    public synchronized int size() {
        return players.size();
    }

    public synchronized boolean isEmpty() {
        return players.isEmpty();
    }

    /** Defensive copy in current priority order for safe iteration off-lock. */
    public synchronized List<QueuedPlayer> snapshot() {
        return new ArrayList<>(players);
    }
}
