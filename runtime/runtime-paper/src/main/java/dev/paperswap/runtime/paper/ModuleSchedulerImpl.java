package dev.paperswap.runtime.paper;

import dev.paperswap.runtime.api.TrackedResource;
import dev.paperswap.runtime.api.paper.ModuleScheduler;
import dev.paperswap.runtime.api.paper.ModuleTask;
import dev.paperswap.runtime.platform.CallbackGuard;
import dev.paperswap.runtime.platform.ModuleHandle;
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
 *
 * <p>The scheduler only ever holds a {@link TaskBody}, which lets go of the module once the
 * task is released (see there).</p>
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
        TaskBody wrapper = new TaskBody(kind, this.handle.callbacks(), body, completion);
        BukkitTask task = submit.apply(wrapper);
        if (async) {
            this.asyncTaskIds.add(task.getTaskId());
        }
        TrackedResource tracked = this.handle.resources().register("task", kind + " #" + task.getTaskId(), () -> {
            // A finished one-shot task needs no cancel; a running async one keeps its id, so
            // runningAsync() still sees it and the module is not unloaded under its feet.
            if (completion == null || !completion.isFinished()) {
                task.cancel();
            }
            wrapper.clear();
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
     * What the Bukkit scheduler holds for a module task. Bukkit can keep that object after the
     * task has run or was cancelled: CraftScheduler keeps the most recently added task as the
     * head of its queue until the next task arrives. Measured in CI (heap dump, test-chaos):
     * the last async task of an unloaded module kept its class loader alive that way. So once
     * the task is released, this drops everything that leads to the module (its code, its
     * callback guard with the class loader, the completion).
     */
    private static final class TaskBody implements Runnable {
        private final String kind;
        private volatile CallbackGuard guard;
        private volatile Runnable body;
        private volatile OneShot completion;

        TaskBody(String kind, CallbackGuard guard, Runnable body, OneShot completion) {
            this.kind = kind;
            this.guard = guard;
            this.body = body;
            this.completion = completion;
        }

        @Override
        public void run() {
            CallbackGuard g = this.guard;
            Runnable b = this.body;
            OneShot c = this.completion;
            if (g == null || b == null) {
                return; // released: cancelled or the module was disabled
            }
            try {
                g.run(this.kind, b);
            } finally {
                if (c != null) {
                    c.finished();
                }
            }
        }

        void clear() {
            this.guard = null;
            this.body = null;
            this.completion = null;
        }
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
