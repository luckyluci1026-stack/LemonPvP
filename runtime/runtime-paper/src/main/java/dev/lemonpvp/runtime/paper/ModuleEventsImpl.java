package dev.lemonpvp.runtime.paper;

import dev.lemonpvp.runtime.api.TrackedResource;
import dev.lemonpvp.runtime.api.paper.ModuleEvents;
import dev.lemonpvp.runtime.platform.ModuleHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

/**
 * Registers module event handlers with Paper's event bus, owned by the runtime host.
 * Every executor is wrapped in the module's callback guard. Unregistration uses
 * {@link HandlerList#unregisterAll(Listener)} with a per-registration listener object.
 */
final class ModuleEventsImpl implements ModuleEvents {

    private final Server server;
    private final RuntimeHostPlugin host;
    private final ModuleHandle handle;

    ModuleEventsImpl(Server server, RuntimeHostPlugin host, ModuleHandle handle) {
        this.server = server;
        this.host = host;
        this.handle = handle;
    }

    /** Identity object used as Bukkit listener for lambda registrations. */
    private static final class Token implements Listener {
    }

    @Override
    public <E extends Event> TrackedResource listen(Class<E> type, EventPriority priority, boolean ignoreCancelled,
                                                    Consumer<? super E> handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(handler, "handler");
        requireOpen();
        Token token = new Token();
        String what = "event " + type.getSimpleName();
        EventExecutor executor = (listener, event) -> {
            if (type.isInstance(event)) {
                this.handle.callbacks().run(what, () -> handler.accept(type.cast(event)));
            }
        };
        this.server.getPluginManager().registerEvent(type, token, priority, executor, this.host, ignoreCancelled);
        return this.handle.resources().register("listener", type.getName() + " @" + priority, () -> HandlerList.unregisterAll(token));
    }

    @Override
    public TrackedResource register(Listener listener) {
        Objects.requireNonNull(listener, "listener");
        requireOpen();
        Set<Method> methods = new LinkedHashSet<>(List.of(listener.getClass().getMethods()));
        methods.addAll(List.of(listener.getClass().getDeclaredMethods()));
        int count = 0;
        try {
            for (Method method : methods) {
                EventHandler annotation = method.getAnnotation(EventHandler.class);
                if (annotation == null || method.isBridge() || method.isSynthetic()) {
                    continue;
                }
                if (method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    throw new IllegalArgumentException("@EventHandler " + method + " must take exactly one Event parameter");
                }
                if (method.getReturnType() != void.class || !Modifier.isPublic(method.getModifiers())
                        || Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalArgumentException("@EventHandler " + method + " must be public, non-static and return void");
                }
                Class<? extends Event> eventType = method.getParameterTypes()[0].asSubclass(Event.class);
                EventExecutor delegate = EventExecutor.create(method, eventType);
                String what = "@EventHandler " + method.getDeclaringClass().getSimpleName() + "#" + method.getName();
                EventExecutor guarded = (l, event) -> {
                    if (eventType.isInstance(event)) {
                        this.handle.callbacks().run(what, () -> {
                            try {
                                delegate.execute(l, event);
                            } catch (EventException e) {
                                throw new IllegalStateException(e.getCause() != null ? e.getCause() : e);
                            }
                        });
                    }
                };
                this.server.getPluginManager().registerEvent(eventType, listener, annotation.priority(), guarded, this.host,
                        annotation.ignoreCancelled());
                count++;
            }
        } catch (RuntimeException e) {
            HandlerList.unregisterAll(listener); // do not leave a half-registered listener behind
            throw e;
        }
        return this.handle.resources().register("listener", listener.getClass().getName() + " (" + count + " handlers)",
                () -> HandlerList.unregisterAll(listener));
    }

    private void requireOpen() {
        if (!this.handle.resources().isOpen()) {
            throw new IllegalStateException("module '" + this.handle.id() + "' is not enabled");
        }
    }
}
