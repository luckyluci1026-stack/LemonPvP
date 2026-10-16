package dev.lemonpvp.runtime.paper;

import dev.lemonpvp.runtime.api.TrackedResource;
import dev.lemonpvp.runtime.api.paper.ModuleScheduler;
import dev.lemonpvp.runtime.api.paper.ModuleTask;
import dev.lemonpvp.runtime.platform.ModuleHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitTask;

/**
 * Module tasks on the Bukkit scheduler, owned by the runtime host. Task bodies run through
 * the callback guard, so a task that was already queued when the module was disabled
 * does nothing. Asynchronous task ids are remembered to report work that is still running.
 *
 * <p>One-shot tasks ({@code run}, {@code runLater}, {@code runAsync}) release their tracker
 * entry and forget their async task id once they have run; otherwise a module that
 * schedules short tasks all the time (for example one per event) would grow its tracker
 * without bound until it is disabled.</p>
 */
final class ModuleSchedulerImpl implements ModuleScheduler {

    private final Server server;
    private final RuntimeHostPlugin host;
    private final ModuleHandle handle;
    private final Set<Integer> asyncTaskIds = ConcurrentHashMap.newKeySet();

    ModuleSchedulerImpl(Server server, RuntimeHostPlugin host, ModuleHandle handle) {
        this.server = server;
        this.host = host;
        this.handle = handle;
    }

    @Override
    public ModuleTask run(Runnable task) {
        return schedule("task", task, false, true, r -> this.server.getScheduler().runTask(this.host, r));
    }

    @Override
    public ModuleTask runLater(Runnable task, long delayTicks) {
        return schedule("delayed task", task, false, true, r -> this.server.getScheduler().runTaskLater(this.host, r, delayTicks));
    }

    @Override
    public ModuleTask runTimer(Runnable task, long delayTicks, long periodTicks) {
        return schedule("timer", task, false, false, r -> this.server.getScheduler().runTaskTimer(this.host, r, delayTicks, periodTicks));
    }

    @Override
    public ModuleTask runAsync(Runnable task) {
        return schedule("async task", task, true, true, r -> this.server.getScheduler().runTaskAsynchronously(this.host, r));
    }

    @Override
    public ModuleTask runTimerAsync(Runnable task, long delayTicks, long periodTicks) {
        return schedule("async timer", task, true, false,
                r -> this.server.getScheduler().runTaskTimerAsynchronously(this.host, r, delayTicks, periodTicks));
    }

    private ModuleTask schedule(String kind, Runnable body, boolean async, boolean oneShot,
                                Function<Runnable, BukkitTask> submit) {
        Objects.requireNonNull(body, "task");
        if (!this.handle.resources().isOpen()) {
            throw new IllegalStateException("module '" + this.handle.id() + "' is not enabled");
        }
        OneShot completion = oneShot ? new OneShot() : null;
        BukkitTask task = submit.apply(() -> {
            try {
                this.handle.callbacks().run(kind, body);
            } finally {
                if (completion != null) {
                    completion.finished();
                }
            }
        });
        if (async) {
            this.asyncTaskIds.add(task.getTaskId());
        }
        TrackedResource tracked = this.handle.resources().register("task", kind + " #" + task.getTaskId(), () -> {
            // A finished one-shot task needs no cancel; a running async one keeps its id, so
            // runningAsync() still sees it and the module is not unloaded under its feet.
            if (completion == null || !completion.isFinished()) {
                task.cancel();
            }
        });
        if (completion != null) {
            completion.registered(tracked, async ? task.getTaskId() : null);
        }
        return new ModuleTask() {
            @Override
            public int taskId() {
                return task.getTaskId();
            }

            @Override
            public boolean isCancelled() {
                return task.isCancelled();
            }

            @Override
            public String description() {
                return tracked.description();
            }

            @Override
            public boolean isReleased() {
                return tracked.isReleased();
            }

            @Override
            public void close() {
                tracked.close();
            }
        };
    }

    /**
     * Completion of a one-shot task. The task may finish before {@link #schedule} has
     * registered it (async tasks run on another thread), so whichever of "finished" and
     * "registered" comes second releases the tracker entry.
     */
    private final class OneShot {
        private TrackedResource tracked;
        private Integer asyncId;
        private boolean registered;
        private boolean finished;

        synchronized boolean isFinished() {
            return this.finished;
        }

        void registered(TrackedResource tracked, Integer asyncId) {
            synchronized (this) {
                this.tracked = tracked;
                this.asyncId = asyncId;
                this.registered = true;
                if (!this.finished) {
                    return;
                }
            }
            release();
        }

        void finished() {
            synchronized (this) {
                this.finished = true;
                if (!this.registered) {
                    return;
                }
            }
            release();
        }

        private void release() {
            if (this.asyncId != null) {
                ModuleSchedulerImpl.this.asyncTaskIds.remove(this.asyncId);
            }
            this.tracked.close(); // idempotent; also fine after the module was disabled
        }
    }

    /** @return asynchronous tasks of this module that are executing right now */
    List<String> runningAsync() {
        List<String> out = new ArrayList<>();
        for (Integer id : this.asyncTaskIds) {
            if (this.server.getScheduler().isCurrentlyRunning(id)) {
                out.add("async task #" + id);
            }
        }
        return out;
    }
}
