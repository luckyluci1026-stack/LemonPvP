package dev.paperswap.runtime.testsupport;

import dev.paperswap.runtime.platform.ModuleHandle;
import dev.paperswap.runtime.platform.PlatformBinding;
import dev.paperswap.runtime.platform.RuntimePlatform;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import testplatform.FakeServerApi;

/**
 * In-memory platform: a dedicated "main" thread, a fake event bus, scheduler and command
 * map. Registrations made by modules through {@link FakeServerApi} are tracked through the
 * module's resource tracker exactly like the Paper adapter does it.
 */
public final class TestPlatform implements RuntimePlatform {

    private final ExecutorService main;
    private volatile Thread mainThread;
    private volatile boolean rejecting;
    private final Logger logger;
    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private volatile String minecraftVersion = "26.2";
    private volatile ClassLoader serverLoader = TestPlatform.class.getClassLoader();
    private volatile String buildId = "unknown";
    private volatile dev.paperswap.runtime.plugins.PluginHost pluginHost;
    private final Map<String, ClassLoader> plugins = new ConcurrentHashMap<>();
    private final AtomicInteger commandSyncs = new AtomicInteger();

    /** Live registrations: "owner:event" etc. */
    public final List<Listener> listeners = new CopyOnWriteArrayList<>();
    public final Map<Integer, Task> tasks = new ConcurrentHashMap<>();
    public final Map<String, String> commands = new ConcurrentHashMap<>();
    public final List<String> permissions = new CopyOnWriteArrayList<>();
    private final AtomicInteger taskIds = new AtomicInteger();
    private final List<Thread> asyncThreads = new CopyOnWriteArrayList<>();

    public record Listener(String owner, String event, Consumer<String> handler, dev.paperswap.runtime.platform.CallbackGuard guard) {
    }

    public record Task(String owner, Runnable body, dev.paperswap.runtime.platform.CallbackGuard guard, boolean async,
                       Thread[] runner) {
    }

    public TestPlatform() {
        this.main = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "test-main");
            t.setDaemon(true);
            this.mainThread = t;
            return t;
        });
        this.logger = Logger.getAnonymousLogger();
        this.logger.setUseParentHandlers(false);
        this.logger.setLevel(Level.ALL);
        this.logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                TestPlatform.this.logLines.add(record.getLevel() + " " + record.getMessage()
                        + (record.getThrown() == null ? "" : " " + record.getThrown()));
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    /** Class loader that plays the role of the server class loader (target of core patches). */
    public void serverClassLoader(ClassLoader loader) {
        this.serverLoader = loader;
    }

    public void buildId(String id) {
        this.buildId = id;
    }

    public void pluginHost(dev.paperswap.runtime.plugins.PluginHost host) {
        this.pluginHost = host;
    }

    @Override
    public String serverBuildId() {
        return this.buildId;
    }

    @Override
    public java.util.Optional<dev.paperswap.runtime.plugins.PluginHost> pluginHost() {
        return java.util.Optional.ofNullable(this.pluginHost);
    }

    public void minecraftVersion(String v) {
        this.minecraftVersion = v;
    }

    public void addPlugin(String name, ClassLoader loader) {
        this.plugins.put(name, loader);
    }

    public List<String> logLines() {
        return List.copyOf(this.logLines);
    }

    public int commandSyncs() {
        return this.commandSyncs.get();
    }

    /** Makes the main executor reject tasks, like a stopped server. */
    public void rejectTasks() {
        this.rejecting = true;
    }

    /** Runs code on the main thread and waits for it. */
    public <T> T onMain(Callable<T> c) throws Exception {
        Future<T> f = this.main.submit(c);
        return f.get(30, TimeUnit.SECONDS);
    }

    /** Fires a fake event on the main thread. */
    public void fire(String event, String payload) throws Exception {
        onMain(() -> {
            for (Listener l : this.listeners) {
                if (l.event().equals(event)) {
                    l.guard().run("listener " + event, () -> l.handler().accept(payload));
                }
            }
            return null;
        });
    }

    /** Runs all synchronous tasks once on the main thread. */
    public void tick() throws Exception {
        onMain(() -> {
            for (Task t : this.tasks.values()) {
                if (!t.async()) {
                    t.guard().run("task", t.body());
                }
            }
            return null;
        });
    }

    public void shutdown() {
        this.main.shutdownNow();
    }

    @Override
    public String minecraftVersion() {
        return this.minecraftVersion;
    }

    @Override
    public String serverBrand() {
        return "PaperSwapTest";
    }

    @Override
    public String serverVersion() {
        return "test-" + this.minecraftVersion;
    }

    @Override
    public java.util.concurrent.Executor mainThreadExecutor() {
        return command -> {
            if (this.rejecting) {
                throw new RejectedExecutionException("platform stopped");
            }
            this.main.execute(command);
        };
    }

    @Override
    public boolean isMainThread() {
        return Thread.currentThread() == this.mainThread;
    }

    @Override
    public Logger logger() {
        return this.logger;
    }

    @Override
    public ClassLoader serverClassLoader() {
        return this.serverLoader;
    }

    @Override
    public List<ClassLoader> pluginClassLoaders(List<String> pluginNames) {
        List<ClassLoader> out = new ArrayList<>();
        for (String n : pluginNames) {
            ClassLoader l = this.plugins.get(n);
            if (l == null) {
                throw new IllegalStateException("plugin " + n + " is not enabled");
            }
            out.add(l);
        }
        return out;
    }

    @Override
    public void commandsChanged() {
        this.commandSyncs.incrementAndGet();
    }

    @Override
    public PlatformBinding bind(ModuleHandle handle) {
        String owner = handle.id();
        for (var p : handle.descriptor().permissions()) {
            this.permissions.add(p.node());
        }
        FakeServerApi api = new FakeServerApi() {
            @Override
            public void listen(String event, Consumer<String> handler) {
                Listener l = new Listener(owner, event, handler, handle.callbacks());
                TestPlatform.this.listeners.add(l);
                handle.resources().register("listener", event, () -> TestPlatform.this.listeners.remove(l));
            }

            @Override
            public int schedule(Runnable task) {
                int id = TestPlatform.this.taskIds.incrementAndGet();
                TestPlatform.this.tasks.put(id, new Task(owner, task, handle.callbacks(), false, new Thread[1]));
                handle.resources().register("task", "task #" + id, () -> TestPlatform.this.tasks.remove(id));
                return id;
            }

            @Override
            public int scheduleAsync(Runnable task) {
                int id = TestPlatform.this.taskIds.incrementAndGet();
                Thread[] runner = new Thread[1];
                Task t = new Task(owner, task, handle.callbacks(), true, runner);
                TestPlatform.this.tasks.put(id, t);
                runner[0] = new Thread(() -> handle.callbacks().run("async task", task), "async-" + owner + "-" + id);
                runner[0].setDaemon(true);
                TestPlatform.this.asyncThreads.add(runner[0]);
                runner[0].start();
                handle.resources().register("task", "async task #" + id, () -> TestPlatform.this.tasks.remove(id));
                return id;
            }

            @Override
            public void registerCommand(String name) {
                if (TestPlatform.this.commands.putIfAbsent(name, owner) != null) {
                    throw new IllegalStateException("command " + name + " already registered");
                }
                handle.resources().register("command", name, () -> TestPlatform.this.commands.remove(name, owner));
            }
        };
        return new PlatformBinding() {
            @Override
            public <T> T facade(Class<T> type) {
                return type == FakeServerApi.class ? type.cast(api) : null;
            }

            @Override
            public List<String> runningAsyncWork() {
                List<String> out = new ArrayList<>();
                for (Thread th : TestPlatform.this.asyncThreads) {
                    if (th.getName().startsWith("async-" + owner + "-") && th.isAlive()) {
                        out.add(th.getName());
                    }
                }
                return out;
            }

            @Override
            public List<String> close() {
                for (var p : handle.descriptor().permissions()) {
                    TestPlatform.this.permissions.remove(p.node());
                }
                return List.of();
            }
        };
    }
}
