package dev.paperswap.runtime.plugins;

import java.nio.file.Path;
import java.util.List;

/**
 * What the plugin updater needs from the server. Implemented by the Paper adapter. All
 * methods are called on the server main thread.
 */
public interface PluginHost {

    /**
     * Class loader of every loaded plugin, used to tell which plugin a loaded class belongs
     * to. Default: none known.
     *
     * @return plugin name per plugin class loader (identity map)
     */
    default java.util.Map<ClassLoader, String> classLoaders() {
        return java.util.Map.of();
    }

    /** @return every installed plugin */
    List<InstalledPlugin> installed();

    /** @return the server's plugin directory ({@code plugins/}) */
    Path pluginDirectory();

    /** @return the server's update folder ({@code plugins/update/} by default) */
    Path updateDirectory();

    /** @return why this server cannot unload plugins, or {@code null} if it can */
    String unloadProblem();

    /**
     * @param name plugin name
     * @return what the server has registered for the plugin now, or {@code null} if it is
     * not installed
     */
    PluginResources resources(String name);

    /**
     * Disables a plugin and removes everything Paper keeps for it: commands, permissions
     * from its descriptor and its entry in the plugin manager. Paper itself already
     * cancels tasks and unregisters listeners, services and channels on disable.
     *
     * @param name plugin name
     * @return what was registered before and is still registered after unloading
     * @throws IllegalStateException if the plugin cannot be removed
     */
    Unloaded unload(String name);

    /**
     * Result of {@link #unload(String)}.
     *
     * @param before      resources right before disabling
     * @param leftovers   resources still registered for the old plugin instance afterwards
     * @param classLoader class loader of the old plugin instance (weakly referenced), or
     *                    {@code null} if unknown
     */
    record Unloaded(PluginResources before, PluginResources leftovers, java.lang.ref.WeakReference<ClassLoader> classLoader) {
    }

    /**
     * Loads and enables a plugin jar. Note: Paper replaces the jar with a jar from the update
     * folder that declares the same plugin name, so callers make sure there is none.
     *
     * @param jar jar inside the plugin directory
     * @return outcome
     */
    LoadResult load(Path jar);

    /** Refreshes the command tree of online players. */
    void commandsChanged();

    /**
     * Outcome of {@link #load(Path)}.
     *
     * @param name    name of the loaded plugin, or {@code null} if it did not load
     * @param version version of the loaded plugin, or {@code null} if it did not load
     * @param enabled whether it is enabled
     * @param error   error message, or {@code null}
     */
    record LoadResult(String name, String version, boolean enabled, String error) {
    }
}
