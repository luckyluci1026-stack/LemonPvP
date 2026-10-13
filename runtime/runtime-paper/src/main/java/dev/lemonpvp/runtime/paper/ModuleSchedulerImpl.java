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
        return schedule("task", task, false, r -> this.server.getScheduler().runTask(this.host, r));
    }

    @Override
    public ModuleTask runLater(Runnable task, long delayTicks) {
        return schedule("delayed task", task, false, r -> this.server.getScheduler().runTaskLater(this.host, r, delayTicks));
    }

    @Override
    public ModuleTask runTimer(Runnable task, long delayTicks, long periodTicks) {
        return schedule("timer", task, false, r -> this.server.getScheduler().runTaskTimer(this.host, r, delayTicks, periodTicks));
    }

    @Override
    public ModuleTask runAsync(Runnable task) {
        return schedule("async task", task, true, r -> this.server.getScheduler().runTaskAsynchronously(this.host, r));
    }

    @Override
    public ModuleTask runTimerAsync(Runnable task, long delayTicks, long periodTicks) {
        return schedule("async timer", task, true,
                r -> this.server.getScheduler().runTaskTimerAsynchronously(this.host, r, delayTicks, periodTicks));
    }

    private ModuleTask schedule(String kind, Runnable body, boolean async, Function<Runnable, BukkitTask> submit) {
        Objects.requireNonNull(body, "task");
        if (!this.handle.resources().isOpen()) {
            throw new IllegalStateException("module '" + this.handle.id() + "' is not enabled");
        }
        BukkitTask task = submit.apply(() -> this.handle.callbacks().run(kind, body));
        if (async) {
            this.asyncTaskIds.add(task.getTaskId());
        }
        TrackedResource tracked = this.handle.resources().register("task", kind + " #" + task.getTaskId(), task::cancel);
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
