package dev.paperswap.runtime.api;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Everything a module may use from the runtime. One context exists per loaded module
 * instance; it becomes unusable once the instance is unloaded.
 */
public interface ModuleContext {

    /** @return the parsed and validated module descriptor */
    ModuleDescriptor descriptor();

    /** @return a logger whose records are prefixed with the module id */
    Logger logger();

    /**
     * Directory reserved for this module's configuration and data
     * ({@code <runtime-root>/module-data/<id>}). Created by the runtime.
     *
     * @return absolute path of the data directory
     */
    Path dataDirectory();

    /**
     * The last valid configuration ({@code config.yml} in the data directory). Returns an
     * empty snapshot when the module has no configuration file.
     *
     * @return current configuration snapshot, never {@code null}
     */
    ConfigSnapshot config();

    /** @return the scope that releases tracked resources when the module is disabled */
    ResourceScope resources();

    /** @return service registry shared between modules */
    ServiceRegistry services();

    /** @return information about the runtime and the server it runs on */
    RuntimeInfo runtime();

    /**
     * Returns the platform facade of the given type, for example
     * {@code dev.paperswap.runtime.api.paper.PaperModulePlatform} on Paper.
     *
     * @param type facade interface
     * @param <T>  facade type
     * @return the facade
     * @throws UnsupportedOperationException if the platform does not provide this facade
     */
    <T> T platform(Class<T> type);

    /**
     * @return {@code true} while the module is enabled. Callbacks that may run late (for
     * example asynchronous work) should check this before touching the server.
     */
    boolean isActive();
}
