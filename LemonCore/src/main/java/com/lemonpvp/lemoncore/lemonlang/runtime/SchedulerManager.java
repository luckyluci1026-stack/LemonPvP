package com.lemonpvp.lemoncore.lemonlang.runtime;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.lemonlang.ast.EveryDef;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages scheduled {@code every} tasks for LemonLang scripts.
 * All tasks are tracked and can be cancelled on reload/shutdown.
 */
public final class SchedulerManager {

    private static final Logger LOG = Logger.getLogger("LemonLang");

    private final List<BukkitTask> tasks = new ArrayList<>();

    /**
     * Schedules an {@link EveryDef} as a repeating task.
     *
     * @param def          the every definition
     * @param plugin       the owning plugin
     * @param interpreter  interpreter instance to run bodies with
     * @param env          the script environment
     */
    public void schedule(EveryDef def, LemonCore plugin, Interpreter interpreter, Environment env) {
        // Convert ms to ticks (20 ticks/sec)
        long delayTicks = Math.max(1L, def.intervalMs() / 50L);
        long periodTicks = delayTicks;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // every blocks run on the main thread without a player context
            ScriptContext ctx = new ScriptContext(null, "every@" + def.intervalMs() + "ms", env.child(), plugin.getLemonLangManager());
            try {
                interpreter.run(def.body(), ctx);
            } catch (Exception e) {
                LOG.warning("[LemonLang] Fehler in 'every'-Block: " + e.getMessage());
            }
        }, delayTicks, periodTicks);

        tasks.add(task);
    }

    /** Cancels all scheduled tasks. Call on reload or shutdown. */
    public void cancelAll() {
        for (BukkitTask task : tasks) {
            try { task.cancel(); } catch (Exception ignored) {}
        }
        tasks.clear();
    }

    /** Returns the number of active scheduled tasks. */
    public int size() {
        return tasks.size();
    }
}
