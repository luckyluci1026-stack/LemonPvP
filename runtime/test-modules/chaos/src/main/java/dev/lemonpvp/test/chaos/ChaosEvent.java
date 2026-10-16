package dev.lemonpvp.test.chaos;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Synchronous event fired by {@code /chaos throw-listener}. */
public final class ChaosEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    /** Creates a synchronous event (must be fired on the main thread). */
    public ChaosEvent() {
        super(false);
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
