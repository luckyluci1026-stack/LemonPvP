package dev.paperswap.runtime.lifecycle;

import dev.paperswap.runtime.api.Capability;
import dev.paperswap.runtime.api.ConfigSnapshot;
import dev.paperswap.runtime.api.ConfigurableModule;
import dev.paperswap.runtime.api.ModuleDescriptor;
import dev.paperswap.runtime.api.ModuleState;
import dev.paperswap.runtime.api.RuntimeModule;
import dev.paperswap.runtime.config.ModuleConfigHolder;
import dev.paperswap.runtime.descriptor.ModuleIds;
import dev.paperswap.runtime.live.LiveHistory;
import dev.paperswap.runtime.loader.ArtifactStager;
import dev.paperswap.runtime.loader.ModuleArtifact;
import dev.paperswap.runtime.loader.ModuleClassLoader;
import dev.paperswap.runtime.loader.ScanReport;
import dev.paperswap.runtime.metrics.LeakDetector;
import dev.paperswap.runtime.metrics.ModuleDiagnostics;
import dev.paperswap.runtime.platform.CallbackGuard;
import dev.paperswap.runtime.resources.ResourceTracker;
import dev.paperswap.runtime.security.PathGuard;
import dev.paperswap.runtime.state.RuntimeStateStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Orchestrates module operations.
 *
 * <h2>Threading model</h2>
 * <ul>
 *     <li>File I/O, hashing and class definition run on the runtime worker thread.</li>
 *     <li>Everything that touches module state or executes module code runs on the server
 *     main thread (via {@code RuntimePlatform#mainThreadExecutor()}). The map of instances
 *     is only mutated there.</li>
 *     <li>At most one operation per module id runs at a time; a second request for the
 *     same id is rejected immediately instead of queued.</li>
 *     <li>Asynchronous phases are bounded by {@code operation-timeout-seconds}; main-thread
 *     callbacks cannot be interrupted and are only reported when slow.</li>
 * </ul>
 */
public final class ModuleManager {

    /** Actor name for operations triggered during server start. */
    public static final String ACTOR_STARTUP = "startup";

    private final RuntimeEnvironment env;
    private final ModulePreparer preparer;
    private final ModuleLifecycle lifecycle;
    private final ModuleSwapper swapper;
    private final Map<String, ModuleInstance> instances = new ConcurrentHashMap<>();
    private final Map<String, String> inFlight = new ConcurrentHashMap<>();
    private final Map<String, OperationResult> lastFailures = new ConcurrentHashMap<>();
    private final List<ModuleInstance> retiring = Collections.synchronizedList(new ArrayList<>());
    private volatile ScanReport lastScan;
    private volatile boolean stopped;

    /** @param env shared collaborators */
    public ModuleManager(RuntimeEnvironment env) {
        this.env = env;
        this.preparer = new ModulePreparer(env);
        this.lifecycle = new ModuleLifecycle(env);
        this.swapper = new ModuleSwapper(env, this.lifecycle);
    }

    // ------------------------------------------------------------------ queries

    /** @return summaries of all registered instances, sorted by id */
    public List<ModuleSummary> modules() {
        return this.instances.values().stream()
                .sorted(Comparator.comparing(ModuleInstance::id))
                .map(i -> new ModuleSummary(i.id(), i.descriptor().version(), i.descriptor().name(), i.state(),
                        i.enabledSince(), i.lastError()))
                .toList();
    }

    /** @param id module id @return the descriptor of the registered instance */
    public Optional<ModuleDescriptor> descriptor(String id) {
        ModuleInstance i = this.instances.get(id);
        return i == null ? Optional.empty() : Optional.of(i.descriptor());
    }

    /** @return last scan of the module directory, if any */
    public Optional<ScanReport> lastScan() {
        return Optional.ofNullable(this.lastScan);
    }

    /** @return last failed operation per module id (cleared by the next successful operation) */
    public Map<String, OperationResult> lastFailures() {
        return Map.copyOf(this.lastFailures);
    }

    /** @return ids with an operation in progress and the operation name */
    public Map<String, String> operationsInProgress() {
        return Map.copyOf(this.inFlight);
    }

    /** @return number of old instances that still wait to be unloaded */
    public int retiringCount() {
        return this.retiring.size();
    }

    /**
     * Unloaded module instances and whether their class loaders were collected. Inspects
     * threads; call on demand only.
     *
     * @return oldest first
     */
    public List<LeakDetector.Entry> unloadedInstances() {
        return this.env.leaks().report(null);
    }

    /**
     * Builds diagnostics for a module. Inspects threads; call on demand only.
     *
     * @param id module id
     * @return diagnostics if the module is loaded or has unloaded instances
     */
    public Optional<ModuleDiagnostics> diagnostics(String id) {
        ModuleInstance i = this.instances.get(id);
        List<LeakDetector.Entry> previous = this.env.leaks().report(id);
        if (i == null) {
            if (previous.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new ModuleDiagnostics(id, "-", ModuleState.UNLOADED, "-", List.of(), -1, -1, -1, null,
                    Map.of(), List.of(), 0, 0, 0, List.of(), null, List.of(), List.of(), List.of(), previous));
        }
        ResourceTracker tracker = i.tracker();
        CallbackGuard guard = i.callbacks();
        List<String> running = List.of();
        if (i.binding() != null) {
            try {
                running = i.binding().runningAsyncWork();
            } catch (RuntimeException e) {
                running = List.of("unknown (" + e + ")");
            }
        }
        return Optional.of(new ModuleDiagnostics(i.id(), i.descriptor().version(), i.state(), i.sha256(), i.history(),
                i.loadMillis(), i.enableMillis(), i.disableMillis(), i.enabledSince(),
                tracker == null ? Map.of() : tracker.countsByCategory(), this.env.services().providedBy(id),
                guard == null ? 0 : guard.inFlight(), guard == null ? 0 : guard.failures(), guard == null ? 0 : guard.dropped(),
                running, i.lastError(), i.cleanupProblems(),
                i.descriptor().dependencies().stream().map(d -> d.id() + (d.optional() ? " (optional)" : "")).toList(),
                DependencyResolver.dependents(id, this.instances.values(), false), previous));
    }

    // ------------------------------------------------------------------ operations

    /** @return a fresh scan of the module directory (discovery only, never activates anything) */
    public CompletableFuture<ScanReport> scan() {
        if (this.stopped) {
            return CompletableFuture.failedFuture(new LifecycleException("runtime is stopped"));
        }
        return onWorker(this.preparer::scan).thenApply(r -> {
            this.lastScan = r;
            return r;
        });
    }

    /**
     * Runs every check that loading would run, without loading any class.
     *
     * @param actor who requested it
     * @param id    module id
     * @return the result
     */
    public CompletableFuture<OperationResult> validate(String actor, String id) {
        return validateJar(actor, id).thenApply(Validated::result);
    }

    /**
     * Result of {@link #validateJar}.
     *
     * @param result  the validation result
     * @param sha256  digest of the validated jar, or {@code null} if validation failed before hashing
     */
    public record Validated(OperationResult result, String sha256) {
    }

    /**
     * Like {@link #validate}, but also returns the digest of the validated jar, so that a later
     * confirmed operation can insist on exactly that jar.
     *
     * @param actor who requested it
     * @param id    module id
     * @return result and digest
     */
    public CompletableFuture<Validated> validateJar(String actor, String id) {
        String[] sha = new String[1];
        return guarded(actor, "validate", id, false, start -> onWorker(() -> this.preparer.prepare(id, null, false))
                .thenCompose(p -> onMain(() -> {
                    sha[0] = p.sha256();
                    DependencyResolver.Resolution res = DependencyResolver.resolve(p.artifact().descriptor(), this.instances, false);
                    List<String> details = new ArrayList<>(p.details());
                    details.addAll(res.warnings());
                    if (!res.problems().isEmpty()) {
                        details.addAll(res.problems());
                        return OperationResult.fail("validate", id, p.artifact().descriptor().coordinates()
                                + " is valid but its dependencies are not satisfied", details, start);
                    }
                    return OperationResult.ok("validate", id, p.artifact().descriptor().coordinates() + " passed all checks",
                            details, start);
                }))).thenApply(r -> new Validated(r, r.success() ? sha[0] : null));
    }

    /**
     * Loads a module (class loader, instantiation, {@code onLoad}) without enabling it.
     *
     * @param actor who requested it
     * @param id    module id
     * @return the result
     */
    public CompletableFuture<OperationResult> load(String actor, String id) {
        return guarded(actor, "load", id, true, start -> {
            List<String> details = Collections.synchronizedList(new ArrayList<>());
            return loadInstance(id, null, details).thenApply(inst -> OperationResult.ok("load", id,
                    inst.descriptor().coordinates() + " loaded (not enabled)", details, start));
        });
    }

    /**
     * Enables a module, loading it first if necessary.
     *
     * @param actor who requested it
     * @param id    module id
     * @return the result
     */
    public CompletableFuture<OperationResult> enable(String actor, String id) {
        return enable(actor, id, null);
    }

    /**
     * Enables a module, but only from a jar with exactly this digest (checked again on the
     * staged copy, so a jar replaced after validation is refused).
     *
     * @param actor          who requested it
     * @param id             module id
     * @param expectedSha256 digest the jar must have, or {@code null} for any
     * @return the result
     */
    public CompletableFuture<OperationResult> enable(String actor, String id, String expectedSha256) {
        return guarded(actor, "enable", id, true, start -> {
            List<String> details = Collections.synchronizedList(new ArrayList<>());
            return onMain(() -> {
                ModuleInstance existing = this.instances.get(id);
                if (existing != null && existing.state() == ModuleState.ENABLED) {
                    throw new LifecycleException(existing.descriptor().coordinates() + " is already enabled");
                }
                if (existing != null && (existing.state() == ModuleState.LOADED || existing.state() == ModuleState.DISABLED)) {
                    return existing;
                }
                return null;
            }).thenCompose(existing -> existing != null ? CompletableFuture.completedFuture(existing)
                    : loadInstance(id, expectedSha256, details)
            ).thenCompose(inst -> onMain(() -> {
                requireRegistered(inst);
                List<String> problems = DependencyResolver.checkLinkedEnabled(inst, this.instances);
                if (!problems.isEmpty()) {
                    throw new LifecycleException("cannot enable " + inst.descriptor().coordinates(), problems);
                }
                details.addAll(this.lifecycle.enable(inst));
                return inst;
            })).thenCompose(inst -> persist(actor, inst, true, details).thenApply(v -> OperationResult.ok("enable", id,
                    inst.descriptor().coordinates() + " enabled", details, start)));
        });
    }

    /**
     * Disables an enabled module. Refused while enabled modules depend on it.
     *
     * @param actor who requested it
     * @param id    module id
     * @return the result
     */
    public CompletableFuture<OperationResult> disable(String actor, String id) {
        return guarded(actor, "disable", id, true, start -> {
            List<String> details = Collections.synchronizedList(new ArrayList<>());
            return onMain(() -> {
                ModuleInstance inst = requireLoaded(id);
                if (inst.state() != ModuleState.ENABLED) {
                    throw new LifecycleException(inst.descriptor().coordinates() + " is not enabled (" + inst.state() + ")");
                }
                List<String> dependents = DependencyResolver.dependents(id, this.instances.values(), true);
                if (!dependents.isEmpty()) {
                    throw new LifecycleException("cannot disable '" + id + "': enabled modules depend on it: " + dependents);
                }
                details.addAll(this.lifecycle.disable(inst));
                return inst;
            }).thenCompose(inst -> persist(actor, inst, false, details).thenApply(v -> inst.state() == ModuleState.FAILED
                    ? OperationResult.fail("disable", id, inst.descriptor().coordinates()
                    + " is inactive but its cleanup was incomplete; a server restart is recommended", details, start)
                    : OperationResult.ok("disable", id, inst.descriptor().coordinates() + " disabled", details, start)));
        });
    }

    /**
     * Unloads a module, disabling it first if necessary. Refused while other loaded modules
     * link to it.
     *
     * @param actor who requested it
     * @param id    module id
     * @return the result
     */
    public CompletableFuture<OperationResult> unload(String actor, String id) {
        return guarded(actor, "unload", id, true, start -> {
            List<String> details = Collections.synchronizedList(new ArrayList<>());
            return onMain(() -> {
                ModuleInstance inst = requireLoaded(id);
                List<String> dependents = DependencyResolver.dependents(id, this.instances.values(), false);
                if (!dependents.isEmpty()) {
                    throw new LifecycleException("cannot unload '" + id + "': loaded modules depend on it: " + dependents);
                }
                boolean disabledNow = false;
                if (inst.state() == ModuleState.ENABLED) {
                    details.addAll(this.lifecycle.disable(inst));
                    disabledNow = true;
                }
                try {
                    details.addAll(this.lifecycle.unload(inst));
                } catch (LifecycleException e) {
                    if (!disabledNow) {
                        throw e;
                    }
                    // The disable already happened: say so, and do not re-activate the module after a restart.
                    persist(actor, inst, false, details);
                    List<String> all = new ArrayList<>(e.details());
                    all.addAll(details);
                    all.add("the module was disabled but is still loaded; unload it again once the work has finished");
                    throw new LifecycleException(e.getMessage(), all);
                }
                this.instances.remove(id, inst);
                return inst;
            }).thenCompose(inst -> deleteStaged(inst.stagedJar()).thenCompose(v -> persist(actor, inst, false, details))
                    .thenApply(v -> OperationResult.ok("unload", id, inst.descriptor().coordinates()
                            + " unloaded (class loader closed; classes are freed once nothing references them)", details, start)));
        });
    }

    /**
     * Removes a module: disables and unloads it if necessary and moves its jar (and
     * {@code .sha256} file) out of the module directory into {@code paperswap/modules-removed/}.
     * Nothing is deleted; moving the files back re-installs the module.
     *
     * @param actor who requested it
     * @param id    module id
     * @return the result
     */
    public CompletableFuture<OperationResult> remove(String actor, String id) {
        return guarded(actor, "remove", id, true, start -> {
            List<String> details = Collections.synchronizedList(new ArrayList<>());
            // Find the jar first: nothing is unloaded if there is no file to remove.
            return onWorker(() -> {
                findArtifacts(id);
                return null;
            }).thenCompose(ignored -> onMain(() -> {
                ModuleInstance inst = this.instances.get(id);
                if (inst == null) {
                    return Optional.<ModuleInstance>empty();
                }
                List<String> dependents = DependencyResolver.dependents(id, this.instances.values(), false);
                if (!dependents.isEmpty()) {
                    throw new LifecycleException("cannot remove '" + id + "': loaded modules depend on it: " + dependents);
                }
                if (inst.state() == ModuleState.ENABLED) {
                    details.addAll(this.lifecycle.disable(inst));
                }
                details.addAll(this.lifecycle.unload(inst));
                this.instances.remove(id, inst);
                details.add("unloaded " + inst.descriptor().coordinates());
                return Optional.of(inst);
            })).thenCompose(inst -> inst.isEmpty() ? CompletableFuture.<Void>completedFuture(null)
                    : deleteStaged(inst.get().stagedJar()).thenCompose(v -> persist(actor, inst.get(), false, details)))
                    .thenCompose(v -> onWorker(() -> moveToRemoved(id, details)))
                    .thenApply(target -> OperationResult.ok("remove", id, "removed; files moved to "
                            + this.env.paths().serverDirectory().relativize(target), details, start));
        });
    }

    private List<ModuleArtifact> findArtifacts(String id) throws IOException, LifecycleException {
        List<ModuleArtifact> files = this.preparer.scan().artifacts().stream().filter(a -> a.id().equals(id)).toList();
        if (files.isEmpty()) {
            throw new LifecycleException("no jar of module '" + id + "' found in " + this.env.config().get().moduleDirectory()
                    + "; nothing was changed (use /fork module unload to only unload it)");
        }
        return files;
    }

    private Path moveToRemoved(String id, List<String> details) throws IOException, LifecycleException {
        List<ModuleArtifact> files = findArtifacts(id);
        Path target = this.env.paths().root().resolve("modules-removed")
                .resolve(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(java.time.ZoneOffset.UTC)
                        .format(java.time.Instant.now()));
        Files.createDirectories(target);
        for (ModuleArtifact a : files) {
            Path jar = a.file();
            Files.move(jar, target.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            details.add("moved " + jar.getFileName());
            Path sidecar = jar.resolveSibling(jar.getFileName() + ".sha256");
            if (Files.isRegularFile(sidecar, LinkOption.NOFOLLOW_LINKS)) {
                Files.move(sidecar, target.resolve(sidecar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                details.add("moved " + sidecar.getFileName());
            }
        }
        this.lastScan = null;
        return target;
    }

    /**
     * Replaces a loaded module by the jar currently in the module directory (hot swap).
     * See {@link ModuleSwapper} for the exact semantics and limits.
     *
     * @param actor who requested it
     * @param id    module id
     * @return the result
     */
    public CompletableFuture<OperationResult> reload(String actor, String id) {
        return reload(actor, id, null);
    }

    /**
     * Hot swap from a jar with exactly this digest.
     *
     * @param actor          who requested it
     * @param id             module id
     * @param expectedSha256 digest the new jar must have, or {@code null} for any
     * @return the result
     */
    public CompletableFuture<OperationResult> reload(String actor, String id, String expectedSha256) {
        return guarded(actor, "reload", id, true, start -> {
            List<String> details = Collections.synchronizedList(new ArrayList<>());
            return onMain(() -> {
                ModuleInstance old = requireLoaded(id);
                List<String> dependents = DependencyResolver.dependents(id, this.instances.values(), false);
                if (!dependents.isEmpty()) {
                    throw new LifecycleException("cannot reload '" + id + "' while loaded modules depend on it: " + dependents
                            + ". Unload the dependents first.");
                }
                return old;
            }).thenCompose(old -> onWorker(() -> this.preparer.prepare(id, expectedSha256, true)).thenCompose(prepared -> {
                details.addAll(prepared.details());
                if (prepared.sha256().equals(old.sha256())) {
                    details.add("jar is unchanged (same SHA-256); the module is restarted with a fresh class loader");
                }
                return link(prepared, details).thenCompose(linked -> onMain(() -> {
                    if (this.instances.get(id) != old) {
                        closeQuietly(linked.loader());
                        throw new LifecycleException("module '" + id + "' changed during the reload; try again");
                    }
                    ModuleInstance next = createInstance(prepared, linked, details);
                    ModuleSwapper.Outcome outcome = this.swapper.swap(old, next);
                    this.instances.put(id, outcome.active());
                    if (outcome.retiring() != null) {
                        this.retiring.add(outcome.retiring());
                    }
                    details.addAll(outcome.details());
                    if (!outcome.success()) {
                        // Keep the reasons (e.g. "new version: ... onEnable threw ...") in the result.
                        throw new LifecycleException(outcome.message(), List.copyOf(details));
                    }
                    if (!old.stagedJar().equals(next.stagedJar())) {
                        deleteStaged(old.stagedJar());
                    }
                    return outcome;
                }));
            })).thenCompose(outcome -> persist(actor, outcome.active(), outcome.active().state() == ModuleState.ENABLED, details)
                    .thenApply(v -> OperationResult.ok("reload", id, outcome.message(), details, start)));
        });
    }

    /**
     * Re-reads the module configuration and applies it atomically (category A change).
     *
     * @param actor who requested it
     * @param id    module id
     * @return the result
     */
    public CompletableFuture<OperationResult> reloadConfig(String actor, String id) {
        return guarded(actor, "config", id, false, start -> onMain(() -> requireLoaded(id))
                .thenCompose(inst -> onWorker(() -> inst.config().readCandidate()).thenCompose(candidate -> onMain(() -> {
                    requireRegistered(inst);
                    return applyConfig(inst, candidate, start);
                }))));
    }

    private OperationResult applyConfig(ModuleInstance inst, ConfigSnapshot candidate, long start) {
        String id = inst.id();
        RuntimeModule module = inst.module();
        if (!(module instanceof ConfigurableModule configurable)) {
            if (inst.state() == ModuleState.ENABLED) {
                return OperationResult.fail("config", id, inst.descriptor().coordinates()
                        + " does not support configuration reload; use /fork module reload " + id, List.of(), start);
            }
            inst.config().swap(candidate);
            return OperationResult.ok("config", id, "configuration stored; it is used on the next enable", List.of(), start);
        }
        CallbackGuard guard = new CallbackGuard(id, inst.logger(), inst.classLoader());
        guard.activate();
        List<String> problems = guard.call("validateConfig", () -> configurable.validateConfig(candidate),
                List.of("validateConfig threw an exception (see log)"));
        if (problems == null) {
            problems = List.of();
        }
        if (!problems.isEmpty()) {
            return OperationResult.fail("config", id, "configuration rejected; the previous configuration stays active",
                    problems, start);
        }
        ConfigSnapshot previous = inst.config().swap(candidate);
        if (inst.state() == ModuleState.ENABLED) {
            boolean ok = guard.run("onConfigReload", () -> {
                try {
                    configurable.onConfigReload(candidate);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            if (!ok) {
                inst.config().swap(previous);
                return OperationResult.fail("config", id, "module failed to apply the configuration; the previous "
                        + "configuration was restored", List.of(), start);
            }
        }
        return OperationResult.ok("config", id, "configuration applied", List.of(), start);
    }

    /**
     * Activates {@code startup-modules} and, if enabled, persisted runtime activations.
     *
     * @return results in activation order
     */
    public CompletableFuture<List<OperationResult>> activateStartupModules() {
        boolean persist = this.env.config().get().persistRuntimeState();
        return onWorker(() -> {
            Map<String, RuntimeStateStore.Activation> persisted = persist ? this.env.state().load() : Map.of();
            return new StartupPlan(this.preparer.scan(), persisted);
        }).thenCompose(plan -> {
            this.lastScan = plan.scan();
            List<String> configured = this.env.config().get().startupModules();
            Map<String, String> wanted = new LinkedHashMap<>();
            configured.forEach(id -> wanted.put(id, null));
            plan.persisted().forEach((id, a) -> wanted.putIfAbsent(id, a.sha256()));
            List<OperationResult> results = Collections.synchronizedList(new ArrayList<>());
            List<ModuleDescriptor> descriptors = new ArrayList<>();
            for (String id : wanted.keySet()) {
                plan.scan().find(id).ifPresentOrElse(a -> descriptors.add(a.descriptor()), () -> {
                    OperationResult r = OperationResult.fail("enable", id, "startup module '" + id + "' not found in "
                            + this.env.config().get().moduleDirectory(), plan.scan().problemsFor(id).stream()
                            .map(p -> p.file() + ": " + p.message()).toList(), System.nanoTime());
                    results.add(r);
                    this.lastFailures.put(id, r);
                    this.env.audit().record(ACTOR_STARTUP, "enable", id, false, r.message());
                });
            }
            List<String> cycleProblems = new ArrayList<>();
            List<ModuleDescriptor> order = DependencyResolver.startupOrder(descriptors, cycleProblems);
            for (String p : cycleProblems) {
                results.add(OperationResult.fail("enable", "-", p, List.of(), System.nanoTime()));
            }
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            for (ModuleDescriptor d : order) {
                chain = chain.thenCompose(v -> enable(ACTOR_STARTUP, d.id(), wanted.get(d.id())).thenAccept(results::add));
            }
            return chain.thenApply(v -> List.copyOf(results));
        });
    }

    private record StartupPlan(ScanReport scan, Map<String, RuntimeStateStore.Activation> persisted) {
    }

    /**
     * Stops every module. Must be called on the main thread; runs synchronously because
     * the server is shutting down (or reloading) and will not tick again for the runtime.
     */
    public void shutdown() {
        if (this.stopped) {
            return;
        }
        this.stopped = true;
        this.lifecycle.enterShutdownMode();
        List<ModuleDescriptor> alive = this.instances.values().stream().map(ModuleInstance::descriptor).toList();
        List<ModuleDescriptor> order = new ArrayList<>(DependencyResolver.startupOrder(alive, new ArrayList<>()));
        Collections.reverse(order);
        Set<String> ordered = new HashSet<>();
        order.forEach(d -> ordered.add(d.id()));
        List<String> ids = new ArrayList<>(order.stream().map(ModuleDescriptor::id).toList());
        this.instances.keySet().stream().filter(id -> !ordered.contains(id)).sorted().forEach(ids::add);
        for (String id : ids) {
            ModuleInstance inst = this.instances.get(id);
            if (inst != null && inst.state() == ModuleState.ENABLED) {
                try {
                    List<String> problems = this.lifecycle.disable(inst);
                    if (!problems.isEmpty()) {
                        this.env.platform().logger().warning("Module " + id + " stopped with problems: " + problems);
                    }
                } catch (RuntimeException e) {
                    this.env.platform().logger().log(Level.SEVERE, "Error while stopping module " + id, e);
                }
            }
        }
        List<ModuleInstance> toUnload = new ArrayList<>();
        for (String id : ids) {
            ModuleInstance inst = this.instances.get(id);
            if (inst != null) {
                toUnload.add(inst);
            }
        }
        synchronized (this.retiring) {
            toUnload.addAll(this.retiring);
            this.retiring.clear();
        }
        for (ModuleInstance inst : toUnload) {
            try {
                this.lifecycle.unload(inst);
            } catch (LifecycleException | RuntimeException e) {
                this.env.platform().logger().warning("Module " + inst.id() + " could not be unloaded cleanly at shutdown: "
                        + e.getMessage());
            }
        }
        this.instances.clear();
        this.env.audit().record("server", "shutdown", "*", true, "runtime stopped, " + toUnload.size() + " module instance(s) unloaded");
        this.env.worker().shutdown();
        try {
            if (!this.env.worker().awaitTermination(5, TimeUnit.SECONDS)) {
                this.env.worker().shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        List<String> problems = new ArtifactStager(this.env.paths().staging()).cleanup(Set.of());
        if (!problems.isEmpty()) {
            this.env.platform().logger().warning("Staging cleanup: " + problems);
        }
    }

    /** @return whether {@link #shutdown()} was called */
    public boolean isStopped() {
        return this.stopped;
    }

    // ------------------------------------------------------------------ loading internals

    /** Dependencies resolved on the main thread. */
    private record Resolved(List<ModuleInstance> dependencies, List<ClassLoader> pluginLoaders) {
    }

    /** Linked dependencies and the created class loader for a prepared module. */
    private record Linked(List<ModuleInstance> dependencies, ModuleClassLoader loader) {
    }

    private CompletableFuture<ModuleInstance> loadInstance(String id, String expectedSha256, List<String> details) {
        return onMain(() -> {
            retryRetiring();
            ModuleInstance existing = this.instances.get(id);
            if (existing != null) {
                if (existing.state() != ModuleState.FAILED) {
                    throw new LifecycleException(existing.descriptor().coordinates() + " is already loaded (" + existing.state()
                            + "); use /fork module reload " + id);
                }
                details.add("replacing failed instance of " + existing.descriptor().coordinates());
                this.lifecycle.unload(existing);
                this.instances.remove(id, existing);
            }
            if (this.instances.size() >= this.env.config().get().maxModuleCount()) {
                throw new LifecycleException("max-module-count (" + this.env.config().get().maxModuleCount() + ") reached");
            }
            return id;
        }).thenCompose(x -> onWorker(() -> this.preparer.prepare(id, expectedSha256, true))).thenCompose(prepared -> {
            details.addAll(prepared.details());
            return link(prepared, details).thenCompose(linked -> onMain(() -> {
                if (this.instances.containsKey(id)) {
                    closeQuietly(linked.loader());
                    throw new LifecycleException("module '" + id + "' was loaded concurrently");
                }
                ModuleInstance inst = createInstance(prepared, linked, details);
                this.instances.put(id, inst);
                return inst;
            }));
        });
    }

    /** Resolves dependencies on the main thread, then creates the class loader on the worker. */
    private CompletableFuture<Linked> link(ModulePreparer.Prepared prepared, List<String> details) {
        ModuleDescriptor d = prepared.artifact().descriptor();
        return onMain(() -> {
            DependencyResolver.Resolution res = DependencyResolver.resolve(d, this.instances, false);
            details.addAll(res.warnings());
            if (!res.problems().isEmpty()) {
                throw new LifecycleException("unresolved dependencies of " + d.coordinates(), res.problems());
            }
            List<ClassLoader> plugins;
            try {
                plugins = this.env.platform().pluginClassLoaders(d.pluginDependencies());
            } catch (IllegalStateException e) {
                throw new LifecycleException("plugin dependency of " + d.coordinates() + " not available: " + e.getMessage());
            }
            return new Resolved(res.linked(), plugins);
        }).thenCompose(resolved -> {
            List<ModuleInstance> deps = resolved.dependencies();
            List<ClassLoader> plugins = resolved.pluginLoaders();
            List<ModuleClassLoader> depLoaders = deps.stream().map(ModuleInstance::classLoader).toList();
            return onWorker(() -> this.preparer.createLoader(prepared, depLoaders, plugins)).thenCompose(lr -> {
                details.addAll(lr.warnings());
                return onMain(() -> {
                    for (ModuleInstance dep : deps) {
                        if (this.instances.get(dep.id()) != dep || !DependencyResolver.isAlive(dep.state())) {
                            closeQuietly(lr.loader());
                            throw new LifecycleException("dependency '" + dep.id() + "' changed while " + d.id()
                                    + " was being loaded; try again");
                        }
                    }
                    return new Linked(deps, lr.loader());
                });
            });
        });
    }

    /** Creates and instantiates a new instance on the main thread. */
    private ModuleInstance createInstance(ModulePreparer.Prepared prepared, Linked linked, List<String> details)
            throws LifecycleException {
        ModuleDescriptor d = prepared.artifact().descriptor();
        ModuleInstance inst = new ModuleInstance(prepared.artifact(), prepared.sha256(), prepared.stagedJar(),
                new ModuleConfigHolder(prepared.dataDirectory().resolve(ModuleConfigHolder.FILE_NAME)),
                new ModuleLogger(d.id(), this.env.platform().logger()));
        inst.transition(ModuleState.VALIDATED, null);
        inst.classLoader(linked.loader());
        inst.linkedDependencies(linked.dependencies().stream().map(ModuleInstance::id).toList());
        inst.config().swap(prepared.configCandidate());
        inst.context(new DefaultModuleContext(d, inst.logger(), prepared.dataDirectory(), inst.config(),
                this.env.services().view(d.id(), d.has(Capability.SERVICES), inst::tracker), this.env.runtimeInfo(),
                inst::tracker, inst::binding,
                () -> inst.state() == ModuleState.ENABLED && inst.callbacks() != null && inst.callbacks().isActive()));
        try {
            this.lifecycle.instantiate(inst);
        } catch (LifecycleException e) {
            try {
                this.lifecycle.unload(inst);
            } catch (LifecycleException ignored) {
                // nothing is running yet
            }
            deleteStaged(prepared.stagedJar());
            throw e;
        }
        details.add("instantiated " + d.mainClass() + " (" + inst.loadMillis() + " ms on the main thread)");
        return inst;
    }

    private void retryRetiring() {
        synchronized (this.retiring) {
            this.retiring.removeIf(old -> {
                try {
                    this.lifecycle.unload(old);
                    return true;
                } catch (LifecycleException e) {
                    return false;
                }
            });
        }
    }

    private ModuleInstance requireLoaded(String id) throws LifecycleException {
        retryRetiring();
        ModuleInstance inst = this.instances.get(id);
        if (inst == null) {
            throw new LifecycleException("module '" + id + "' is not loaded");
        }
        return inst;
    }

    private void requireRegistered(ModuleInstance inst) throws LifecycleException {
        if (this.instances.get(inst.id()) != inst) {
            throw new LifecycleException("module '" + inst.id() + "' was replaced concurrently");
        }
    }

    private CompletableFuture<Void> deleteStaged(Path staged) {
        if (staged == null) {
            return CompletableFuture.completedFuture(null);
        }
        for (ModuleInstance i : this.instances.values()) {
            if (staged.equals(i.stagedJar())) {
                return CompletableFuture.completedFuture(null); // still in use by another instance
            }
        }
        return onWorker(() -> {
            new ArtifactStager(this.env.paths().staging()).delete(staged);
            return null;
        }).handle((v, ex) -> null);
    }

    private CompletableFuture<Void> persist(String actor, ModuleInstance inst, boolean enabled, List<String> details) {
        if (ACTOR_STARTUP.equals(actor) || !this.env.config().get().persistRuntimeState()) {
            return CompletableFuture.completedFuture(null);
        }
        RuntimeStateStore.Activation activation = enabled
                ? new RuntimeStateStore.Activation(inst.id(), inst.descriptor().version(), inst.sha256()) : null;
        return onWorker(() -> {
            this.env.state().put(inst.id(), activation);
            return null;
        }).handle((v, ex) -> {
            if (ex != null) {
                details.add("warning: could not persist runtime state: " + describe(ex));
            }
            return null;
        });
    }

    // ------------------------------------------------------------------ async plumbing

    @FunctionalInterface
    private interface Step<T> {
        T run() throws Exception;
    }

    private <T> CompletableFuture<T> onWorker(Step<T> step) {
        long timeout = this.env.config().get().operationTimeout().toMillis();
        return submit(this.env.worker(), step).orTimeout(timeout, TimeUnit.MILLISECONDS);
    }

    private <T> CompletableFuture<T> onMain(Step<T> step) {
        return submit(this.env.platform().mainThreadExecutor(), step);
    }

    private <T> CompletableFuture<T> submit(Executor executor, Step<T> step) {
        CompletableFuture<T> f = new CompletableFuture<>();
        try {
            executor.execute(() -> {
                if (this.stopped) {
                    f.completeExceptionally(new LifecycleException("runtime is stopping"));
                    return;
                }
                try {
                    f.complete(step.run());
                } catch (VirtualMachineError e) {
                    f.completeExceptionally(e);
                    throw e;
                } catch (Throwable t) {
                    f.completeExceptionally(t);
                }
            });
        } catch (RejectedExecutionException e) {
            f.completeExceptionally(new LifecycleException("runtime is stopping"));
        }
        return f;
    }

    private CompletableFuture<OperationResult> guarded(String actor, String action, String id, boolean deploy,
                                                       Function<Long, CompletableFuture<OperationResult>> body) {
        long start = System.nanoTime();
        String safeId = PathGuard.sanitize(id);
        if (this.stopped) {
            return CompletableFuture.completedFuture(OperationResult.fail(action, safeId, "runtime is stopped", List.of(), start));
        }
        String idProblem = ModuleIds.problem(id);
        if (idProblem != null) {
            return CompletableFuture.completedFuture(OperationResult.fail(action, safeId, idProblem, List.of(), start));
        }
        if (deploy && !ACTOR_STARTUP.equals(actor) && !this.env.config().get().allowRuntimeDeploy()) {
            OperationResult r = OperationResult.fail(action, id, "runtime deployment is disabled (allow-runtime-deploy: false)",
                    List.of(), start);
            this.env.audit().record(actor, action, id, false, r.message());
            return CompletableFuture.completedFuture(r);
        }
        String running = this.inFlight.putIfAbsent(id, action);
        if (running != null) {
            return CompletableFuture.completedFuture(OperationResult.fail(action, id, "operation '" + running
                    + "' is already in progress for '" + id + "'", List.of(), start));
        }
        CompletableFuture<OperationResult> f;
        try {
            f = body.apply(start);
        } catch (RuntimeException e) {
            f = CompletableFuture.failedFuture(e);
        }
        return f.handle((r, ex) -> {
            this.inFlight.remove(id, action);
            OperationResult result = r != null ? r : OperationResult.fail(action, id, describe(ex), details(ex), start);
            finish(actor, result);
            return result;
        });
    }

    private void finish(String actor, OperationResult result) {
        this.env.audit().record(actor, result.action(), result.moduleId(), result.success(), result.message());
        if (!result.action().equals("validate")) {
            LiveHistory.INSTANCE.record(LiveHistory.Kind.MODULE, result.moduleId(), result.action(), actor, result.success(),
                    result.message());
        }
        this.env.metrics().record(result.action(), result.success());
        if (!result.action().equals("validate")) {
            if (result.success()) {
                this.lastFailures.remove(result.moduleId());
            } else {
                this.lastFailures.put(result.moduleId(), result);
            }
        }
        if (this.env.config().get().logModuleLifecycle() || !result.success()) {
            Level level = result.success() ? Level.INFO : Level.WARNING;
            this.env.platform().logger().log(level, "[" + actor + "] " + result.action() + " " + result.moduleId() + ": "
                    + result.message() + (result.details().isEmpty() ? "" : " " + result.details()));
        }
    }

    private String describe(Throwable ex) {
        Throwable t = unwrap(ex);
        if (t instanceof LifecycleException le) {
            return le.getMessage();
        }
        if (t instanceof TimeoutException) {
            return "timed out after " + this.env.config().get().operationTimeout().toSeconds()
                    + " s (operation-timeout-seconds); the operation was aborted";
        }
        return t.toString();
    }

    private static List<String> details(Throwable ex) {
        Throwable t = unwrap(ex);
        return t instanceof LifecycleException le ? le.details() : List.of();
    }

    private static Throwable unwrap(Throwable ex) {
        Throwable t = ex;
        while (t instanceof CompletionException && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    private static void closeQuietly(ModuleClassLoader loader) {
        try {
            loader.close();
        } catch (IOException ignored) {
            // best effort
        }
    }
}
