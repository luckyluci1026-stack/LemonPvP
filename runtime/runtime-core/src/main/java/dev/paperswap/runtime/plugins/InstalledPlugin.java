package dev.paperswap.runtime.plugins;

/**
 * A plugin as the server currently knows it.
 *
 * @param info    descriptor of the loaded plugin
 * @param enabled whether it is enabled
 */
public record InstalledPlugin(PluginInfo info, boolean enabled) {

    /** @return plugin name */
    public String name() {
        return this.info.name();
    }
}
