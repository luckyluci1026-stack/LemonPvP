package dev.paperswap.test.stress;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Synchronous event fired by {@code /stress events}; every stress listener counts one hit. */
public final class StressEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private long hits;

    /** Creates a synchronous event (must be fired on the main thread). */
    public StressEvent() {
        super(false);
    }

    void hit() {
        this.hits++;
    }

    long hits() {
        return this.hits;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /** @return the handler list (looked up by name by the server's event manager) */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
