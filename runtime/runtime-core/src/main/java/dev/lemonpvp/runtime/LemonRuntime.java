package dev.lemonpvp.runtime;

import dev.lemonpvp.runtime.api.RuntimeApi;
import dev.lemonpvp.runtime.api.RuntimeInfo;
import dev.lemonpvp.runtime.config.LivePatchConfig;
import dev.lemonpvp.runtime.config.RuntimeConfig;
import dev.lemonpvp.runtime.config.RuntimeConfigLoader;
import dev.lemonpvp.runtime.lifecycle.ModuleManager;
import dev.lemonpvp.runtime.lifecycle.RuntimeEnvironment;
import dev.lemonpvp.runtime.livepatch.InstrumentationAccess;
import dev.lemonpvp.runtime.livepatch.InstrumentationDiagnostics;
import dev.lemonpvp.runtime.livepatch.LivePatchRegistry;
import dev.lemonpvp.runtime.livepatch.LivePatchService;
import dev.lemonpvp.runtime.livepatch.PatchTransformer;
import dev.lemonpvp.runtime.metrics.LeakDetector;
import dev.lemonpvp.runtime.metrics.RuntimeHealth;
import dev.lemonpvp.runtime.metrics.RuntimeMetrics;
import dev.lemonpvp.runtime.platform.RuntimePlatform;
import dev.lemonpvp.runtime.plugins.PluginUpdateManager;
import dev.lemonpvp.runtime.security.AuditLog;
import dev.lemonpvp.runtime.services.ServiceHub;
import dev.lemonpvp.runtime.state.RuntimeStateStore;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Entry point of the hot-deployment runtime.
 *
 * <p>{@link #bootstrap(RuntimePlatform, Path)} reads the configuration and returns an empty
 * optional when the runtime is disabled - in that case nothing else happens: no thread is
 * created, no directory is scanned and no command or listener is registered.</p>
 */
public final class LemonRuntime {

    /** Name of the single worker thread. */
    public static final String WORKER_THREAD_NAME = "Lemon-Runtime-Worker";

    private final RuntimePlatform platform;
    private final RuntimePaths paths;
    private final AtomicReference<RuntimeConfig> config;
    private final ModuleManager modules;
    private final LivePatchService livePatches;
    private final PluginUpdateManager pluginUpdates;
    private final RuntimeEnvironment env;
    private final Instant startedAt = Instant.now();

    private LemonRuntime(RuntimePlatform platform, RuntimePaths paths, RuntimeConfig initial) {
        this.platform = platform;
        this.paths = paths;
        this.config = new AtomicReference<>(initial);
        ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r, WORKER_THREAD_NAME);
                    t.setDaemon(true);
                    return t;
                });
        worker.allowCoreThreadTimeOut(true); // no idle thread while nothing happens
        RuntimeInfo info = new RuntimeInfo(RuntimeVersion.VERSION, RuntimeApi.API_VERSION, platform.minecraftVersion(),
                platform.serverBrand(), platform.serverVersion());
        this.env = new RuntimeEnvironment(this.config::get, paths, platform, worker,
                new AuditLog(paths.auditLog(), worker, platform.logger()), new RuntimeStateStore(paths.stateFile()),
                new RuntimeMetrics(), new RuntimeHealth(), new LeakDetector(), new ServiceHub(), info);
        this.modules = new ModuleManager(this.env);
        this.livePatches = new LivePatchService(this.config::get, paths, platform, worker, this.env.audit(),
                LivePatchRegistry.INSTANCE, PatchTransformer.INSTANCE, InstrumentationAccess::find);
        this.pluginUpdates = platform.pluginHost().map(h -> new PluginUpdateManager(this.config::get, paths, platform, worker,
                this.env.audit(), h)).orElse(null);
    }

    /**
     * Reads (or creates) the configuration and starts the runtime if it is enabled. Never
     * throws: configuration problems keep the runtime disabled and are logged.
     *
     * @param platform        hosting platform
     * @param serverDirectory server working directory
     * @return the started runtime, or empty if it is disabled or misconfigured
     */
    public static Optional<LemonRuntime> bootstrap(RuntimePlatform platform, Path serverDirectory) {
        RuntimePaths paths = RuntimePaths.of(serverDirectory);
        RuntimeConfigLoader.Result result = RuntimeConfigLoader.loadOrCreate(paths.configFile());
        for (String w : result.warnings()) {
            platform.logger().warning("lemon-runtime.yml: " + w);
        }
        if (!result.valid()) {
            platform.logger().severe("Hot deployment stays disabled because " + paths.configFile().getFileName()
                    + " is invalid: " + result.errors());
            return Optional.empty();
        }
        if (!result.config().enabled()) {
            platform.logger().info("Hot deployment is disabled (" + RuntimePaths.DEFAULT_CONFIG + ": enabled: false)");
            return Optional.empty();
        }
        return Optional.of(start(platform, paths, result.config()));
    }

    /**
     * Starts the runtime with an explicit configuration (used by tests and by
     * {@link #bootstrap}).
     *
     * @param platform hosting platform
     * @param paths    file layout
     * @param config   enabled configuration
     * @return the started runtime
     */
    public static LemonRuntime start(RuntimePlatform platform, RuntimePaths paths, RuntimeConfig config) {
        if (!config.enabled()) {
            throw new IllegalArgumentException("runtime is disabled in the configuration");
        }
        LemonRuntime runtime = new LemonRuntime(platform, paths, config);
        platform.logger().warning("Hot deployment is ENABLED. Modules run with full server privileges; see docs/SECURITY.md."
                + " allow-runtime-deploy=" + config.allowRuntimeDeploy() + ", verify-integrity=" + config.verifyIntegrity()
                + ", plugin-updates=" + config.pluginUpdates().enabled() + ", live-patching=" + config.livePatching().enabled());
        InstrumentationDiagnostics.Report instrumentation = runtime.livePatches.diagnostics(true);
        if (config.livePatching().enabled() || runtime.livePatches.agent().instrumentation().isPresent()) {
            platform.logger().info("Core live patching: " + runtime.livePatches.status().agentDetail() + "; profile "
                    + runtime.livePatches.profile().yamlName() + (runtime.livePatches.profileNote() == null ? ""
                    : " (" + runtime.livePatches.profileNote() + ")"));
            for (InstrumentationDiagnostics.Finding f : instrumentation.findings()) {
                if (f.level() == InstrumentationDiagnostics.Level.MISSING || f.level() == InstrumentationDiagnostics.Level.WARNING) {
                    platform.logger().warning("Instrumentation: " + f.text());
                } else {
                    platform.logger().info("Instrumentation: " + f.text());
                }
            }
        } else {
            platform.logger().info("Instrumentation: live-patch agent not loaded, live-patching.enabled: false"
                    + " (/fork instrumentation status explains how to enable it)");
            instrumentation.findings().stream().filter(f -> f.level() == InstrumentationDiagnostics.Level.WARNING)
                    .forEach(f -> platform.logger().warning("Instrumentation: " + f.text()));
        }
        runtime.env.audit().record("server", "start", "*", true, "runtime " + RuntimeVersion.VERSION + " started on "
                + platform.serverBrand() + " " + platform.minecraftVersion());
        return runtime;
    }

    /**
     * Activates startup modules and persisted activations asynchronously.
     *
     * @return completes when every activation attempt finished
     */
    public CompletableFuture<Void> activateStartupModules() {
        return this.modules.activateStartupModules().handle((results, ex) -> {
            if (ex != null) {
                this.platform.logger().log(Level.SEVERE, "Activating startup modules failed", ex);
            } else if (!results.isEmpty()) {
                long ok = results.stream().filter(r -> r.success()).count();
                this.platform.logger().info("Startup modules: " + ok + " of " + results.size() + " activated");
            }
            return null;
        });
    }

    /** Stops all modules and the worker thread. Main thread (or server shutdown) only. */
    public void stop() {
        this.modules.shutdown();
    }

    /** @return module manager */
    public ModuleManager modules() {
        return this.modules;
    }

    /** @return core live patching (always present; operations check live-patching.enabled) */
    public LivePatchService livePatches() {
        return this.livePatches;
    }

    /** @return live plugin updates, if the platform supports them */
    public Optional<PluginUpdateManager> pluginUpdates() {
        return Optional.ofNullable(this.pluginUpdates);
    }

    /** @return current configuration */
    public RuntimeConfig config() {
        return this.config.get();
    }

    /** @return file layout */
    public RuntimePaths paths() {
        return this.paths;
    }

    /** @return hosting platform */
    public RuntimePlatform platform() {
        return this.platform;
    }

    /** @return runtime information */
    public RuntimeInfo info() {
        return this.env.runtimeInfo();
    }

    /** @return health state */
    public RuntimeHealth health() {
        return this.env.health();
    }

    /** @return operation counters */
    public RuntimeMetrics metrics() {
        return this.env.metrics();
    }

    /** @return uptime of the runtime */
    public Duration uptime() {
        return Duration.between(this.startedAt, Instant.now());
    }

    /**
     * Result of reloading {@code lemon-runtime.yml}.
     *
     * @param applied         whether the new configuration is active
     * @param errors          validation errors (nothing was changed)
     * @param warnings        non-fatal findings
     * @param restartRequired settings that changed but only take effect after a restart
     */
    public record ConfigReloadResult(boolean applied, List<String> errors, List<String> warnings,
                                     List<String> restartRequired) {
    }

    /**
     * Re-reads the runtime configuration on the worker thread and applies it atomically on
     * the main thread. Settings that cannot change at runtime keep their current values.
     *
     * @param actor who requested it
     * @return the result
     */
    public CompletableFuture<ConfigReloadResult> reloadConfig(String actor) {
        CompletableFuture<RuntimeConfigLoader.Result> read = new CompletableFuture<>();
        try {
            this.env.worker().execute(() -> read.complete(RuntimeConfigLoader.load(this.paths.configFile())));
        } catch (RuntimeException e) {
            read.completeExceptionally(e);
        }
        return read.orTimeout(this.config.get().operationTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .thenApplyAsync(result -> {
                    if (!result.valid()) {
                        this.env.audit().record(actor, "reload-config", "lemon-runtime.yml", false, String.join("; ", result.errors()));
                        return new ConfigReloadResult(false, result.errors(), result.warnings(), List.of());
                    }
                    RuntimeConfig current = this.config.get();
                    RuntimeConfig next = result.config();
                    List<String> restart = RuntimeConfigLoader.restartRequiredChanges(current, next);
                    RuntimeConfig merged = new RuntimeConfig(current.enabled(), current.moduleDirectory(),
                            next.allowRuntimeDeploy(), current.startupModules(), next.persistRuntimeState(),
                            next.verifyIntegrity(), next.trustedHashesFile(), next.logModuleLifecycle(), next.maxModuleCount(),
                            next.operationTimeout(), next.allowedApiVersions(), next.allowServerInternals(),
                            new LivePatchConfig(current.livePatching().enabled(), current.livePatching().profile(),
                                    current.livePatching().patchDirectory(), next.livePatching().trustedHashesFile(),
                                    next.livePatching().confirmTimeoutSeconds()),
                            next.pluginUpdates());
                    this.config.set(merged);
                    List<String> warnings = new ArrayList<>(result.warnings());
                    if (current.allowServerInternals() != next.allowServerInternals()) {
                        warnings.add("allow-server-internals only affects modules loaded from now on");
                    }
                    this.env.audit().record(actor, "reload-config", "lemon-runtime.yml", true,
                            restart.isEmpty() ? "applied" : "applied; restart required for " + restart);
                    return new ConfigReloadResult(true, List.of(), warnings, restart);
                }, this.platform.mainThreadExecutor());
    }
}
