package dev.lemonpvp.runtime.api.paper;

import dev.lemonpvp.runtime.api.TrackedResource;
import java.util.function.Consumer;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Event registrations of a module. Handlers run with the module's context class loader;
 * exceptions are logged with the module id and never reach the server. Handlers are not
 * invoked any more once the module starts disabling.
 */
public interface ModuleEvents {

    /**
     * Registers a handler for one event type.
     *
     * @param type            event class (must have a handler list, as for Bukkit listeners)
     * @param priority        Bukkit priority
     * @param ignoreCancelled skip cancelled events
     * @param handler         handler
     * @param <E>             event type
     * @return handle that unregisters the handler when closed
     */
    <E extends Event> TrackedResource listen(Class<E> type, EventPriority priority, boolean ignoreCancelled,
                                             Consumer<? super E> handler);

    /**
     * Registers a handler with {@link EventPriority#NORMAL} that also receives cancelled events.
     *
     * @param type    event class
     * @param handler handler
     * @param <E>     event type
     * @return handle that unregisters the handler when closed
     */
    default <E extends Event> TrackedResource listen(Class<E> type, Consumer<? super E> handler) {
        return listen(type, EventPriority.NORMAL, false, handler);
    }

    /**
     * Registers all public {@code @EventHandler} methods of a listener object. Handler methods
     * must be public and return {@code void}.
     *
     * @param listener listener object
     * @return handle that unregisters the listener when closed
     */
    TrackedResource register(Listener listener);
}
