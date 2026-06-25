package com.lemonpvp.lemonqueue.queue;

import com.velocitypowered.api.proxy.Player;

import java.util.UUID;

/**
 * A single player waiting in a {@link ServerQueue}.
 *
 * <p>Ordering is by {@code priority} (descending) then {@code joinTime}
 * (ascending = FIFO). The {@code sending} flag guards against dispatching the
 * same player twice while an async connection request is still in flight.</p>
 */
public class QueuedPlayer {

    private final Player player;
    private final String target;
    private final int priority;
    private final long joinTime;

    private volatile boolean sending = false;
    private volatile long sendingSince = 0L;
    /** Epoch-ms until which this player must not be retried after a failed connect. */
    private volatile long retryAfter = 0L;

    public QueuedPlayer(Player player, String target, int priority, long joinTime) {
        this.player = player;
        this.target = target;
        this.priority = priority;
        this.joinTime = joinTime;
    }

    public Player getPlayer()   { return player; }
    public UUID getUuid()       { return player.getUniqueId(); }
    public String getTarget()   { return target; }
    public int getPriority()    { return priority; }
    public long getJoinTime()   { return joinTime; }

    public boolean isSending()          { return sending; }

    public void setSending(boolean s) {
        this.sendingSince = s ? System.currentTimeMillis() : 0L;
        this.sending = s; // written last so isSendingStale() never sees sending=true with sendingSince=0
    }

    /**
     * True if a connection request has been in flight longer than {@code maxMs}.
     * Guards against head-of-line blocking when a backend connect future hangs
     * and the {@code sending} flag would otherwise never reset.
     */
    public boolean isSendingStale(long maxMs) {
        return sending && (System.currentTimeMillis() - sendingSince) > maxMs;
    }

    /** True while a retry-cooldown from a previous failed connect is still active. */
    public boolean isOnRetryBackoff() {
        return System.currentTimeMillis() < retryAfter;
    }

    /** Prevents this player from being retried for {@code cooldownMs} milliseconds. */
    public void setRetryBackoff(long cooldownMs) {
        this.retryAfter = System.currentTimeMillis() + cooldownMs;
    }
}
