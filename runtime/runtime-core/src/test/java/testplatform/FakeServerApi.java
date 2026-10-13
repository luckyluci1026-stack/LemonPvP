package testplatform;

import java.util.function.Consumer;

/**
 * Minimal stand-in for a platform facade (like PaperModulePlatform) used by test modules.
 * Lives outside dev.lemonpvp.runtime so module class loaders can see it.
 */
public interface FakeServerApi {

    /** Registers an event handler; tracked by the runtime. */
    void listen(String event, Consumer<String> handler);

    /** Schedules a repeating task; tracked by the runtime. @return task id */
    int schedule(Runnable task);

    /** Schedules a task that runs asynchronously until it returns. @return task id */
    int scheduleAsync(Runnable task);

    /** Registers a command; tracked by the runtime. */
    void registerCommand(String name);
}
