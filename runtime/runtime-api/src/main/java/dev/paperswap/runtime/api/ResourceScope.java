package dev.paperswap.runtime.api;

/**
 * Tracks resources of a module instance and releases them in reverse registration order
 * when the module is disabled (or when enabling fails).
 *
 * <p>Platform registrations made through the platform facade (listeners, commands, tasks,
 * services) are tracked automatically. Use this scope for everything else that must be
 * closed, e.g. executors, sockets or file watchers.</p>
 *
 * <p>Registering a resource after the scope was closed fails with an
 * {@link IllegalStateException} and closes the passed resource immediately, so late
 * registrations from stray callbacks cannot leak.</p>
 */
public interface ResourceScope {

    /**
     * Tracks an {@link AutoCloseable}.
     *
     * @param description human readable description used in diagnostics
     * @param resource    resource to close on cleanup
     * @param <T>         resource type
     * @return the same resource
     */
    <T extends AutoCloseable> T track(String description, T resource);

    /**
     * Registers a cleanup action.
     *
     * @param description human readable description used in diagnostics
     * @param cleanup     action to run on cleanup
     * @return handle that can release the resource early
     */
    TrackedResource onCleanup(String description, Runnable cleanup);

    /** @return number of currently tracked, not yet released resources */
    int size();
}
