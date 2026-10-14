package dev.lemonpvp.runtime.platform;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/**
 * Everything the platform-neutral runtime needs from the hosting server. Implemented by
 * the Paper adapter; tests use an in-memory implementation.
 */
public interface RuntimePlatform {

    /** @return running Minecraft version, e.g. {@code "26.2"} */
    String minecraftVersion();

    /** @return server brand name, e.g. {@code "Lemon"} */
    String serverBrand();

    /** @return full server version string */
    String serverVersion();

    /**
     * Executor that runs tasks on the server main thread (next tick). Must reject tasks
     * with {@link java.util.concurrent.RejectedExecutionException} once the runtime stops.
     *
     * @return main thread executor
     */
    Executor mainThreadExecutor();

    /** @return whether the calling thread is the server main thread */
    boolean isMainThread();

    /** @return logger for runtime messages */
    Logger logger();

    /** @return parent class loader for module class loaders (the server class loader) */
    ClassLoader serverClassLoader();

    /**
     * Resolves class loaders of plugins a module depends on.
     *
     * @param pluginNames plugin names from the descriptor
     * @return class loaders in the same order
     * @throws IllegalStateException if a plugin is missing or not enabled
     */
    List<ClassLoader> pluginClassLoaders(List<String> pluginNames);

    /**
     * Creates the platform-specific part of a module enable cycle (listeners, commands,
     * tasks, permissions). Called on the main thread right before {@code onEnable}.
     *
     * @param handle module handle
     * @return the binding
     */
    PlatformBinding bind(ModuleHandle handle);

    /**
     * Access to the server's plugin manager for live plugin updates. Default: none (the
     * platform does not support plugin updates).
     *
     * @return the plugin host, if supported
     */
    default java.util.Optional<dev.lemonpvp.runtime.plugins.PluginHost> pluginHost() {
        return java.util.Optional.empty();
    }

    /**
     * Identity of the running server build, used to match live patches to the build they
     * were made for. Default: unknown (live patches then need the development profile).
     *
     * @return build id, see {@code dev.lemonpvp.runtime.livepatch.BuildIdentity}
     */
    default String serverBuildId() {
        return "unknown";
    }

    /**
     * Called on the main thread after module commands were added or removed, so the
     * platform can refresh client-side command trees. Default: no-op.
     */
    default void commandsChanged() {
    }
}
