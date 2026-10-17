package dev.paperswap.runtime.config;

/**
 * Settings of live plugin updates ({@code plugin-updates:} section of
 * {@code paperswap.yml}). New plugin jars are taken from the server's update folder
 * ({@code plugins/update/}, Bukkit convention), which Paper also applies on restart.
 *
 * @param enabled           allow {@code /fork live update <plugin>}
 * @param trustedHashesFile optional allow-list of plugin jar digests
 */
public record PluginUpdateConfig(boolean enabled, String trustedHashesFile) {

    public PluginUpdateConfig {
        trustedHashesFile = trustedHashesFile == null ? "" : trustedHashesFile;
    }

    /** @return defaults: disabled, no allow-list */
    public static PluginUpdateConfig defaults() {
        return new PluginUpdateConfig(false, "");
    }
}
