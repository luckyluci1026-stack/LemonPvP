package dev.paperswap.runtime.lifecycle;

import dev.paperswap.runtime.api.ModuleState;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Replaces a loaded module instance by a new version (hot swap), on the main thread.
 *
 * <h2>Why the old version is stopped before the new one starts</h2>
 * <p>Both versions would register the same commands, listen to the same events and
 * possibly write the same data. Running them side by side for even one tick is therefore
 * not safe in general, so a truly atomic switch is not offered. Instead:</p>
 * <ol>
 *     <li>The new version is fully prepared, verified, defined and instantiated
 *     ({@code onLoad}) <em>before</em> the old one is touched. Any failure up to here leaves
 *     the old version running unchanged.</li>
 *     <li>Old {@code disable} and new {@code enable} run back-to-back inside one main
 *     thread task. Synchronous events and scheduler tasks run on the same thread, so none
 *     of them can observe the gap. Asynchronous events (chat, pre-login) may.</li>
 *     <li>If the new version fails to enable, its resources are released and the old
 *     version is enabled again (rollback). Effects the new version already caused outside
 *     the runtime (world changes, database writes, messages) cannot be rolled back.</li>
 * </ol>
 */
final class ModuleSwapper {

    private final RuntimeEnvironment env;
    private final ModuleLifecycle lifecycle;

    ModuleSwapper(RuntimeEnvironment env, ModuleLifecycle lifecycle) {
        this.env = env;
        this.lifecycle = lifecycle;
    }

    /**
     * Outcome of a swap.
     *
     * @param success    whether the new version is now the active instance
     * @param active     instance that is registered for the module id afterwards
     * @param retiring   old instance that could not be unloaded yet (still running async work), or {@code null}
     * @param message    summary
     * @param details    detail lines
     */
    record Outcome(boolean success, ModuleInstance active, ModuleInstance retiring, String message, List<String> details) {
    }

    /**
     * @param old      currently registered instance (LOADED, ENABLED, DISABLED or FAILED)
     * @param next     new instance in state LOADED ({@code onLoad} already succeeded)
     * @return the outcome
     */
    Outcome swap(ModuleInstance old, ModuleInstance next) {
        List<String> details = new ArrayList<>();
        String from = old.descriptor().coordinates();
        String to = next.descriptor().coordinates();
        boolean wasEnabled = old.state() == ModuleState.ENABLED;

        if (!wasEnabled) {
            ModuleInstance retiring = retire(old, details);
            return new Outcome(true, next, retiring, "replaced " + from + " (" + old.state() + ") with " + to
                    + "; the module stays disabled", details);
        }

        List<String> disableProblems = this.lifecycle.disable(old);
        details.addAll(prefix("old version: ", disableProblems));
        if (old.state() == ModuleState.FAILED) {
            // The old version left resources behind; starting the new one could duplicate them.
            discard(next, details);
            return new Outcome(false, old, null, "aborted: " + from + " could not be cleaned up completely; " + to
                    + " was not started. A server restart is recommended.", details);
        }
        try {
            details.addAll(this.lifecycle.enable(next));
        } catch (LifecycleException e) {
            details.add("new version: " + e.getMessage());
            details.addAll(prefix("new version: ", e.details()));
            discard(next, details);
            try {
                details.addAll(this.lifecycle.enable(old));
                return new Outcome(false, old, null, to + " failed to enable; rolled back to " + from, details);
            } catch (LifecycleException rollback) {
                details.add("rollback: " + rollback.getMessage());
                details.addAll(prefix("rollback: ", rollback.details()));
                this.env.health().recommendRestart(old.id(), "hot swap to " + to + " failed and the rollback to " + from
                        + " failed as well");
                return new Outcome(false, old, null, to + " failed to enable and the rollback to " + from
                        + " failed; the module is inactive. A server restart is recommended.", details);
            }
        }
        ModuleInstance retiring = retire(old, details);
        return new Outcome(true, next, retiring, "switched " + from + " -> " + to, details);
    }

    /** Unloads the old instance; returns it if it must be retried later. */
    private ModuleInstance retire(ModuleInstance old, List<String> details) {
        try {
            details.addAll(prefix("old version: ", this.lifecycle.unload(old)));
            return null;
        } catch (LifecycleException e) {
            details.add("old version not unloaded yet: " + e.getMessage());
            details.addAll(e.details());
            return old;
        }
    }

    private void discard(ModuleInstance next, List<String> details) {
        try {
            details.addAll(prefix("new version: ", this.lifecycle.unload(next)));
        } catch (LifecycleException e) {
            details.add("new version could not be unloaded: " + e.getMessage());
            this.env.platform().logger().log(Level.WARNING, "Could not unload discarded module instance " + next.id()
                    + ": " + e.getMessage());
        }
    }

    private static List<String> prefix(String prefix, List<String> lines) {
        return lines.stream().map(l -> prefix + l).toList();
    }
}
