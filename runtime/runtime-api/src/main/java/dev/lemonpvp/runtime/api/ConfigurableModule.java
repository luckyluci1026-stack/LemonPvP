package dev.lemonpvp.runtime.api;

import java.util.List;

/**
 * Optional mix-in for modules whose configuration can be replaced while they are enabled.
 *
 * <p>A configuration reload is atomic from the module's point of view: the runtime parses
 * the new file, asks {@link #validateConfig(ConfigSnapshot)} for problems and only when the
 * candidate is valid calls {@link #onConfigReload(ConfigSnapshot)}. If validation or the
 * callback fails, {@link ModuleContext#config()} keeps returning the last valid
 * configuration.</p>
 */
public interface ConfigurableModule {

    /**
     * Checks a candidate configuration without applying it. Called on the main thread.
     *
     * @param candidate the parsed candidate configuration
     * @return human readable problems; an empty list means the candidate is valid
     */
    default List<String> validateConfig(ConfigSnapshot candidate) {
        return List.of();
    }

    /**
     * Applies a validated configuration. Called on the main thread while the module is
     * enabled.
     *
     * @param config the new configuration, already visible through {@link ModuleContext#config()}
     * @throws Exception to reject the change; the previous configuration is restored
     */
    void onConfigReload(ConfigSnapshot config) throws Exception;
}
