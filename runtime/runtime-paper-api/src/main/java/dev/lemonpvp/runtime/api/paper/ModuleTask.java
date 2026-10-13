package dev.lemonpvp.runtime.api.paper;

import dev.lemonpvp.runtime.api.TrackedResource;

/** A scheduled module task. Closing it cancels the task. */
public interface ModuleTask extends TrackedResource {

    /** @return Bukkit task id */
    int taskId();

    /** @return whether the task was cancelled */
    boolean isCancelled();
}
