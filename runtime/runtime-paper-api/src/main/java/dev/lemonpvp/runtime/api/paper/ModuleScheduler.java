package dev.lemonpvp.runtime.api.paper;

/**
 * Tasks of a module, backed by the Bukkit scheduler. All tasks are cancelled when the
 * module is disabled. Asynchronous tasks that are already running cannot be stopped; the
 * runtime refuses to unload the module until they have returned. Asynchronous tasks must
 * not touch the world; use {@link #run(Runnable)} to get back onto the main thread.
 *
 * <p>Handles of one-shot tasks ({@link #run}, {@link #runLater}, {@link #runAsync}) are
 * released automatically once the task has run ({@link ModuleTask#isReleased()} becomes
 * {@code true}); timers stay tracked until they are closed or the module is disabled.</p>
 */
public interface ModuleScheduler {

    /** @param task task @return handle; runs on the main thread at the next tick */
    ModuleTask run(Runnable task);

    /** @param task task @param delayTicks delay @return handle */
    ModuleTask runLater(Runnable task, long delayTicks);

    /** @param task task @param delayTicks delay @param periodTicks period @return handle */
    ModuleTask runTimer(Runnable task, long delayTicks, long periodTicks);

    /** @param task task @return handle; runs on a scheduler worker thread */
    ModuleTask runAsync(Runnable task);

    /** @param task task @param delayTicks delay @param periodTicks period @return handle */
    ModuleTask runTimerAsync(Runnable task, long delayTicks, long periodTicks);
}
