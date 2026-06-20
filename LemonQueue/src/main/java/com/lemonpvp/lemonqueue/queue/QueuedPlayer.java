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
    public void setSending(boolean s)   { this.sending = s; }
}
