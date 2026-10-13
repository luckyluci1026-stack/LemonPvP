package dev.lemonpvp.runtime.api;

/**
 * Entry point of a hot-deployable runtime module.
 *
 * <p>The class named by {@code main} in {@code lemon-module.yml} must implement this
 * interface and provide a public no-argument constructor. All lifecycle callbacks are
 * invoked on the server main thread, one at a time, never concurrently with another
 * lifecycle callback of the same module.</p>
 *
 * <h2>Rules for module authors</h2>
 * <ul>
 *     <li>Register every listener, command, task and service through the
 *     {@link ModuleContext} so that the runtime can remove it again. Resources registered
 *     directly through the server API are invisible to the runtime and will leak.</li>
 *     <li>Do not keep state in static fields that are referenced from outside the module;
 *     such references keep the module class loader alive after an unload.</li>
 *     <li>Threads started by the module must be stopped in {@link #onDisable}; the runtime
 *     cannot stop foreign threads safely.</li>
 * </ul>
 */
public interface RuntimeModule {

    /**
     * Called once after the module class was instantiated, before {@link #onEnable}.
     * Must not have externally visible side effects; the runtime may discard a loaded
     * module without ever enabling it (for example when a hot swap is aborted).
     *
     * @param context the module context
     * @throws Exception to abort loading; the module is then cleaned up and marked failed
     */
    default void onLoad(ModuleContext context) throws Exception {
    }

    /**
     * Activates the module. May be called again after {@link #onDisable} when the module
     * is re-enabled or a failed hot swap is rolled back.
     *
     * @param context the module context
     * @throws Exception to abort enabling; all resources registered so far are released
     */
    void onEnable(ModuleContext context) throws Exception;

    /**
     * Deactivates the module. Resources registered through the context are released by the
     * runtime after this method returns, even if it throws.
     *
     * @param context the module context
     * @throws Exception reported as a cleanup problem; the module is still disabled
     */
    default void onDisable(ModuleContext context) throws Exception {
    }
}
