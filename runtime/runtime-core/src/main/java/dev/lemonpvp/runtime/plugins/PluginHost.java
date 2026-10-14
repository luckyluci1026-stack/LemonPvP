package dev.lemonpvp.runtime.plugins;

import java.nio.file.Path;
import java.util.List;

/**
 * What the plugin updater needs from the server. Implemented by the Paper adapter. All
 * methods are called on the server main thread.
 */
public interface PluginHost {

    /** @return every installed plugin */
    List<InstalledPlugin> installed();

    /** @return the server's plugin directory ({@code plugins/}) */
    Path pluginDirectory();

    /** @return the server's update folder ({@code plugins/update/} by default) */
    Path updateDirectory();

    /** @return why this server cannot unload plugins, or {@code null} if it can */
    String unloadProblem();

    /**
     * Disables a plugin and removes everything Paper keeps for it: commands, permissions
     * from its descriptor and its entry in the plugin manager. Paper itself already
     * cancels tasks and unregisters listeners, services and channels on disable.
     *
     * @param name plugin name
     * @throws IllegalStateException if the plugin cannot be removed
     */
    void unload(String name);

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
