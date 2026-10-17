package dev.paperswap.runtime.livepatch;

import dev.paperswap.runtime.RuntimePaths;
import dev.paperswap.runtime.config.LivePatchConfig;
import dev.paperswap.runtime.config.RuntimeConfig;
import dev.paperswap.runtime.live.LiveHistory;
import dev.paperswap.runtime.livepatch.LivePatchRegistry.ClassRecord;
import dev.paperswap.runtime.livepatch.LivePatchRegistry.ClassVersion;
import dev.paperswap.runtime.livepatch.LivePatchRegistry.PatchRecord;
import dev.paperswap.runtime.livepatch.LivePatchRegistry.Status;
import dev.paperswap.runtime.livepatch.classfile.ClassFileReader;
import dev.paperswap.runtime.livepatch.classfile.ClassFormatException;
import dev.paperswap.runtime.platform.RuntimePlatform;
import dev.paperswap.runtime.security.AuditLog;
import dev.paperswap.runtime.security.Hashing;
import dev.paperswap.runtime.security.IntegrityVerifier;
import dev.paperswap.runtime.security.PathGuard;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Core live patching: inspects, applies and rolls back patches that change classes of the
 * running server, either through {@link Instrumentation#redefineClasses}
 * ({@link Mechanism#REDEFINE}) or through {@link Instrumentation#retransformClasses} with
 * PaperSwap's {@link PatchTransformer} ({@link Mechanism#RETRANSFORM}).
 *
 * <p>Threading: file I/O, hashing, parsing and planning run on the runtime worker thread.
 * The redefinition itself runs on the server main thread, between two scheduled tasks, so
 * no game logic of the main thread is half-way through a patched method of its own tick
 * code (other threads may still be inside old method versions; the JVM lets them finish
 * with the old code). One live-patch operation runs at a time.</p>
 *
 * <p>Atomicity: all classes of a patch are passed to one {@code redefineClasses} (or
 * {@code retransformClasses}) call. The JVMTI specification guarantees that either all of
 * them get the new definition or none.
 * New classes (if any) are defined before that call; they cannot be removed again, but
 * nothing references them if the redefinition fails.</p>
 */
public final class LivePatchService {

    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);

    private final Supplier<RuntimeConfig> config;
    private final RuntimePaths paths;
    private final RuntimePlatform platform;
    private final Executor worker;
    private final AuditLog audit;
    private final LivePatchRegistry registry;
    private final PatchTransformer transformer;
    private final Supplier<InstrumentationAccess.Lookup> agentLookup;
    private volatile InstrumentationAccess.Lookup agent;
    private final LivePatchConfig.Profile profile;
    private final String profileNote;
    private final AtomicBoolean busy = new AtomicBoolean();
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();

    private record Pending(String archiveSha256, Mechanism mechanism, Instant expires) {
    }

    /**
     * Outcome of an apply or rollback request.
     *
     * @param kind         outcome
     * @param plan         the plan (apply only), or {@code null}
     * @param message      one-line summary
     * @param details      further lines
     * @param verification read-back check after an apply or rollback, or {@code null}
     */
    public record Result(Kind kind, PatchPlan plan, String message, List<String> details,
                         AppliedBytecodeCheck.Report verification) {
        public Result {
            details = List.copyOf(details);
        }

        /** Result without a read-back check. */
        public Result(Kind kind, PatchPlan plan, String message, List<String> details) {
            this(kind, plan, message, details, null);
        }

        /** Outcome kinds. */
        public enum Kind {
            /** Plan is valid; repeat the command with {@code confirm}. */
            CONFIRMATION_REQUIRED,
            /** Refused before anything was changed. */
            REJECTED,
            /** Redefinition or retransformation succeeded. */
            APPLIED,
            /** Rollback succeeded. */
            ROLLED_BACK,
            /** The JVM rejected the operation; no class was changed. */
            FAILED
        }
    }

    /**
     * One file in the patch directory.
     *
     * @param id          patch id (file name without suffix)
     * @param size        archive size in bytes
     * @param problem     why it is unusable, or {@code null}
     * @param description description from the manifest
     * @param classes     number of classes
     * @param sha256      archive digest
     * @param status      status in this JVM, or {@code null} if never applied
     */
    public record InboxEntry(String id, long size, String problem, String description, int classes, String sha256, Status status) {
    }

    /**
     * Snapshot for {@code /fork live patch status}.
     *
     * @param enabled          live-patching.enabled
     * @param profile          effective profile
     * @param profileNote      explanation if the configured profile was not honoured, or {@code null}
     * @param agentDetail      agent lookup result
     * @param agentVersion     version of the agent, or {@code 0}
     * @param agentLoadedBy    how the agent was loaded
     * @param instrumentation  whether the instrumentation instance is available
     * @param redefineSupported whether the JVM supports redefinition at all
     * @param retransformSupported whether classes can be retransformed (retransformation patches, read-back check)
     * @param transformerRegistered whether PaperSwap's patch transformer is registered
     * @param retransformedClasses classes that currently have a retransformation patch layer
     * @param displacedClasses of those, classes whose patch was pushed out by another redefinition
     * @param vm               JVM description
     * @param enhancedFlag     value of AllowEnhancedClassRedefinition
     * @param buildId          server build id
     * @param patchDirectory   upload directory
     * @param trustNote        allow-list situation
     * @param active           active patches in this JVM
     * @param attempts         apply attempts in this JVM
     * @param heapUsedMiB      used heap
     * @param heapMaxMiB       maximum heap
     */
    public record StatusReport(boolean enabled, LivePatchConfig.Profile profile, String profileNote, String agentDetail,
                               int agentVersion, String agentLoadedBy, boolean instrumentation, boolean redefineSupported,
                               boolean retransformSupported, boolean transformerRegistered, int retransformedClasses,
                               int displacedClasses, String vm, String enhancedFlag, String buildId,
                               Path patchDirectory, String trustNote, long active, int attempts, long heapUsedMiB, long heapMaxMiB) {
    }

    /**
     * @param config   configuration supplier (re-read for reloadable settings)
     * @param paths    file layout
     * @param platform hosting platform
     * @param worker   worker executor for I/O
     * @param audit    audit log
     * @param registry    registry of this JVM
     * @param transformer patch transformer of this JVM
     * @param agentLookup finds the agent; called again while no agent was found, so an agent attached to the running
     *                    JVM later ({@code java -jar paperswap-agent.jar attach <pid>}) is picked up
     */
    public LivePatchService(Supplier<RuntimeConfig> config, RuntimePaths paths, RuntimePlatform platform, Executor worker,
                            AuditLog audit, LivePatchRegistry registry, PatchTransformer transformer,
                            Supplier<InstrumentationAccess.Lookup> agentLookup) {
        this.config = config;
        this.paths = paths;
        this.platform = platform;
        this.worker = worker;
        this.audit = audit;
        this.registry = registry;
        this.transformer = transformer;
        this.agentLookup = agentLookup;
        this.agent = agentLookup.get();
        transformer.warnings(m -> platform.logger().warning(m));
        LivePatchConfig lp = config.get().livePatching();
        if (lp.profile() == LivePatchConfig.Profile.DEVELOPMENT
                && !Boolean.parseBoolean(System.getProperty(LivePatchConfig.DEVELOPMENT_PROPERTY))) {
            this.profile = LivePatchConfig.Profile.PRODUCTION;
            this.profileNote = "profile 'development' is configured but the JVM was not started with -D"
                    + LivePatchConfig.DEVELOPMENT_PROPERTY + "=true; production rules apply";
        } else {
            this.profile = lp.profile();
            this.profileNote = null;
        }
    }

    /** @return the effective profile */
    public LivePatchConfig.Profile profile() {
        return this.profile;
    }

    /** @return note about the profile, or {@code null} */
    public String profileNote() {
        return this.profileNote;
    }

    private boolean enabled() {
        return this.config.get().livePatching().enabled();
    }

    /**
     * The agent lookup. While no agent is found, every call looks again: an agent attached
     * to the running JVM is used from then on, without a restart.
     *
     * @return current lookup result
     */
    public InstrumentationAccess.Lookup agent() {
        InstrumentationAccess.Lookup current = this.agent;
        if (current.instrumentation().isEmpty()) {
            InstrumentationAccess.Lookup again = this.agentLookup.get();
            if (again.instrumentation().isPresent()) {
                this.agent = again;
                this.platform.logger().info("Live-patch agent found: " + again.detail());
                return again;
            }
        }
        return current;
    }

    /**
     * @param countClasses also count loaded and modifiable classes
     * @return what this JVM offers for instrumentation and what is missing
     */
    public InstrumentationDiagnostics.Report diagnostics(boolean countClasses) {
        return InstrumentationDiagnostics.collect(agent(), enabled(), countClasses);
    }

    /** @return PaperSwap's patch transformer */
    public PatchTransformer transformer() {
        return this.transformer;
    }

    // ------------------------------------------------------------------ read-only views

    /** @return current status (no I/O) */
    public StatusReport status() {
        InstrumentationAccess.Lookup lookup = agent();
        Optional<Instrumentation> inst = lookup.instrumentation();
        Runtime rt = Runtime.getRuntime();
        List<PatchTransformer.SlotInfo> slots = this.transformer.slots();
        return new StatusReport(enabled(), this.profile, this.profileNote, lookup.detail(), lookup.agentVersion(), lookup.loadedBy(),
                inst.isPresent(), inst.map(Instrumentation::isRedefineClassesSupported).orElse(false),
                inst.map(Instrumentation::isRetransformClassesSupported).orElse(false), this.transformer.registeredAt().isPresent(),
                slots.size(), (int) slots.stream().filter(s -> s.displaced() != null).count(), JvmCapabilities.vmDescription(),
                JvmCapabilities.enhancedRedefinitionFlag(), this.platform.serverBuildId(),
                this.paths.patchDirectory(this.config.get()), trustNote(), this.registry.activeCount(), this.registry.records().size(),
                (rt.totalMemory() - rt.freeMemory()) >> 20, rt.maxMemory() >> 20);
    }

    private String trustNote() {
        Optional<Path> trusted = this.paths.patchTrustedHashes(this.config.get());
        if (trusted.isPresent()) {
            return "patches must be listed in " + this.config.get().livePatching().trustedHashesFile();
        }
        return this.profile == LivePatchConfig.Profile.PRODUCTION
                ? "NO allow-list configured: the production profile refuses every apply"
                : "no allow-list (development profile): the .sha256 sidecar is still required";
    }

    /** @return every attempt in this JVM */
    public List<PatchRecord> history() {
        return this.registry.records();
    }

    /**
     * Runs all capability probes on the worker thread.
     *
     * @return probe results
     */
    public CompletableFuture<Map<Capability, CapabilityProbe.Result>> probe() {
        InstrumentationAccess.Lookup lookup = agent();
        Optional<Instrumentation> inst = lookup.instrumentation();
        if (inst.isEmpty()) {
            return CompletableFuture.failedFuture(new PatchException(lookup.detail()));
        }
        return onWorker(() -> JvmCapabilities.probeAll(inst.get()));
    }

    /** @return files in the patch directory with their verification state */
    public CompletableFuture<List<InboxEntry>> list() {
        if (!enabled()) {
            return CompletableFuture.failedFuture(new PatchException("live patching is disabled (live-patching.enabled: false)"));
        }
        return onWorker(this::scanInbox);
    }

    private List<InboxEntry> scanInbox() throws IOException {
        Path dir = this.paths.patchDirectory(this.config.get());
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.getFileName().toString().endsWith(PatchIds.SUFFIX)).sorted().limit(200).toList();
        }
        List<InboxEntry> out = new ArrayList<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            String id = name.substring(0, name.length() - PatchIds.SUFFIX.length());
            Status status = this.registry.latest(id).map(PatchRecord::status).orElse(null);
            long size = Files.size(file);
            try {
                PatchArchive a = loadVerified(id);
                out.add(new InboxEntry(id, size, null, a.manifest().description(), a.manifest().classes().size(), a.sha256(), status));
            } catch (PatchException e) {
                out.add(new InboxEntry(id, size, e.getMessage(), "", 0, null, status));
            }
        }
        return out;
    }

    /**
     * Creates the plan for a redefinition patch without changing anything.
     *
     * @param id patch id
     * @return the plan
     */
    public CompletableFuture<PatchPlan> inspect(String id) {
        return inspect(id, Mechanism.REDEFINE);
    }

    /**
     * Creates the plan for a patch without changing anything.
     *
     * @param id        patch id
     * @param mechanism how the classes would be installed
     * @return the plan
     */
    public CompletableFuture<PatchPlan> inspect(String id, Mechanism mechanism) {
        try {
            Instrumentation inst = requireUsable(id, mechanism);
            return onWorker(() -> plan(loadVerified(id), inst, mechanism));
        } catch (PatchException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    // ------------------------------------------------------------------ apply

    /**
     * Applies a patch by redefinition, see {@link #apply(String, String, boolean, Mechanism)}.
     *
     * @param actor   who asked
     * @param id      patch id
     * @param confirm second step
     * @return the outcome
     */
    public CompletableFuture<Result> apply(String actor, String id, boolean confirm) {
        return apply(actor, id, confirm, Mechanism.REDEFINE);
    }

    /**
     * Applies a patch. Without {@code confirm} only the plan is created and remembered;
     * with {@code confirm} the remembered plan is re-checked and executed. The confirmation
     * must use the same mechanism as the plan.
     *
     * @param actor     who asked
     * @param id        patch id
     * @param confirm   second step
     * @param mechanism how the classes are installed
     * @return the outcome
     */
    public CompletableFuture<Result> apply(String actor, String id, boolean confirm, Mechanism mechanism) {
        Instrumentation inst;
        try {
            inst = requireUsable(id, mechanism);
        } catch (PatchException e) {
            return CompletableFuture.completedFuture(rejected(null, e.getMessage()));
        }
        if (this.registry.latest(id).filter(r -> r.status() == Status.ACTIVE).isPresent()) {
            // One active version per id: rollback only knows the latest record of an id.
            return CompletableFuture.completedFuture(rejected(null, "patch " + id + " is already active; roll it back first"
                    + " or give the new version a new id"));
        }
        if (!this.busy.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(rejected(null, "another live-patch operation is running"));
        }
        String key = actor + "\u0000apply\u0000" + id;
        CompletableFuture<Result> result;
        if (!confirm) {
            result = onWorker(() -> plan(loadVerified(id), inst, mechanism)).thenApplyAsync(plan -> {
                if (!plan.applicable()) {
                    this.audit.record(actor, "patch-plan", id, false, String.join("; ", plan.errors()));
                    return new Result(Result.Kind.REJECTED, plan, "patch " + id + " cannot be applied", plan.errors());
                }
                this.pending.put(key, new Pending(plan.archiveSha256(), mechanism, Instant.now().plusSeconds(confirmTimeout())));
                this.audit.record(actor, "patch-plan", id, true, "plan (" + mechanism.id() + ") created, waiting for confirmation;"
                        + " archive " + plan.archiveSha256());
                return new Result(Result.Kind.CONFIRMATION_REQUIRED, plan, "plan for " + id + " is ready", List.of());
            }, this.platform.mainThreadExecutor());
        } else {
            Pending p = this.pending.remove(key);
            if (p == null || p.expires().isBefore(Instant.now())) {
                this.busy.set(false);
                return CompletableFuture.completedFuture(rejected(null, "no plan of " + id + " is waiting for confirmation by "
                        + actor + " (it expired or was never created); run '/fork live update " + id + "' first"));
            }
            if (p.mechanism() != mechanism) {
                this.busy.set(false);
                return CompletableFuture.completedFuture(rejected(null, "the plan of " + id + " was made for '" + p.mechanism().id()
                        + "', not '" + mechanism.id() + "'; create the plan again with the mechanism you want"));
            }
            result = onWorker(() -> prepare(id, inst, p.archiveSha256(), mechanism)).thenApplyAsync(prepared -> {
                if (prepared.rejection != null) {
                    this.audit.record(actor, "patch-apply", id, false, prepared.rejection.message());
                    return prepared.rejection;
                }
                return execute(actor, prepared, inst);
            }, this.platform.mainThreadExecutor());
        }
        return result.whenComplete((r, ex) -> this.busy.set(false));
    }

    private static Result rejected(PatchPlan plan, String message) {
        return new Result(Result.Kind.REJECTED, plan, message, List.of());
    }

    /** Worker-side preparation of a confirmed apply. */
    private record Prepared(PatchArchive archive, PatchPlan plan, Map<String, byte[]> previous, Path backupDirectory, Result rejection) {
    }

    private Prepared prepare(String id, Instrumentation inst, String confirmedSha, Mechanism mechanism)
            throws PatchException, IOException {
        PatchArchive archive = loadVerified(id);
        if (!archive.sha256().equals(confirmedSha)) {
            return new Prepared(archive, null, Map.of(), null, rejected(null, id + PatchIds.SUFFIX
                    + " changed after the plan was shown (was " + PatchPlanner.shortSha(confirmedSha) + ", now "
                    + PatchPlanner.shortSha(archive.sha256()) + "); inspect it again"));
        }
        PatchPlan plan = plan(archive, inst, mechanism);
        if (!plan.applicable()) {
            return new Prepared(archive, plan, Map.of(), null, new Result(Result.Kind.REJECTED, plan,
                    "patch " + id + " cannot be applied any more", plan.errors()));
        }
        Map<String, byte[]> previous = new LinkedHashMap<>();
        ClassLoader loader = this.platform.serverClassLoader();
        for (PatchPlan.Entry e : plan.redefinitions()) {
            Optional<ClassVersion> patched = this.registry.current(e.className());
            byte[] bytes = patched.isPresent() ? patched.get().bytes() : PatchPlanner.readResource(loader, e.className());
            if (!Hashing.sha256(bytes).equals(e.runningSha256())) {
                return new Prepared(archive, plan, Map.of(), null, rejected(plan, e.className() + " changed while preparing"));
            }
            previous.put(e.className(), bytes);
        }
        Path backup = writeBackup(id, archive, previous);
        return new Prepared(archive, plan, previous, backup, null);
    }

    private Path writeBackup(String id, PatchArchive archive, Map<String, byte[]> previous) throws IOException {
        Path dir = this.paths.patchBackups().resolve(id).resolve(BACKUP_TIME.format(Instant.now()));
        Files.createDirectories(dir);
        StringBuilder index = new StringBuilder("# Class files that were running before live patch " + id + " (archive "
                + archive.sha256() + ") was applied.\n# Restoring them requires '/fork live rollback " + id
                + "' (same JVM) or a server restart (back to the jar).\n");
        for (Map.Entry<String, byte[]> e : previous.entrySet()) {
            Path file = PathGuard.resolveInside(dir, e.getKey().replace('.', '/') + ".class");
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, e.getValue());
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            index.append(Hashing.sha256(e.getValue())).append("  ").append(e.getKey()).append('\n');
        }
        Files.writeString(dir.resolve("backup.txt"), index);
        return dir;
    }

    /** Main thread: performs the redefinition or retransformation. */
    private Result execute(String actor, Prepared prepared, Instrumentation inst) {
        PatchPlan plan = prepared.plan();
        String id = plan.patchId();
        if (this.registry.version() != plan.registryVersion()) {
            return rejected(plan, "another patch was applied or rolled back meanwhile; inspect " + id + " again");
        }
        ClassLoader loader = this.platform.serverClassLoader();
        List<ClassDefinition> definitions = new ArrayList<>();
        List<ClassRecord> records = new ArrayList<>();
        try {
            for (PatchPlan.Entry e : plan.redefinitions()) {
                Class<?> cls = Class.forName(e.className(), false, loader);
                byte[] next = prepared.archive().classes().get(e.className());
                definitions.add(new ClassDefinition(cls, next));
                String previousOwner = this.registry.current(e.className()).map(ClassVersion::installedBy).orElse(null);
                records.add(new ClassRecord(e.className(), new ClassVersion(prepared.previous().get(e.className()),
                        e.runningSha256(), previousOwner), new ClassVersion(next, e.newSha256(), id)));
            }
        } catch (ClassNotFoundException | LinkageError e) {
            return fail(actor, plan, List.of(), "a target class could not be resolved: " + e, prepared);
        }
        List<String> defined = new ArrayList<>();
        for (PatchPlan.Entry e : plan.definitions()) {
            try {
                Class<?> host = Class.forName(e.hostClass(), false, loader);
                Class<?> added = MethodHandles.privateLookupIn(host, MethodHandles.lookup())
                        .defineClass(prepared.archive().classes().get(e.className()));
                defined.add(e.className());
                // Link (and thereby verify) now, so invalid bytecode fails here and not on first use.
                added.getDeclaredMethods();
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
                return fail(actor, plan, defined, "defining new class " + e.className() + " failed: " + ex
                        + "; no existing class was redefined", prepared);
            }
        }
        long start = System.nanoTime();
        if (plan.mechanism() == Mechanism.RETRANSFORM) {
            String problem = retransform(id, definitions, inst);
            if (problem != null) {
                return fail(actor, plan, defined, problem, prepared, !problem.startsWith(PARTIAL));
            }
        } else {
            try {
                if (!definitions.isEmpty()) {
                    inst.redefineClasses(definitions.toArray(ClassDefinition[]::new));
                }
            } catch (ClassNotFoundException | java.lang.instrument.UnmodifiableClassException | LinkageError | RuntimeException e) {
                return fail(actor, plan, defined, "the JVM rejected the redefinition: " + e
                        + ". Per the JVMTI specification no class of this patch was changed.", prepared);
            }
        }
        long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
        String verb = plan.mechanism() == Mechanism.RETRANSFORM ? "retransformed" : "redefined";
        List<AppliedBytecodeCheck.Expectation> expectations = new ArrayList<>();
        for (int i = 0; i < definitions.size(); i++) {
            expectations.add(new AppliedBytecodeCheck.Expectation(definitions.get(i).getDefinitionClass(),
                    records.get(i).installed().bytes(), records.get(i).previous().bytes()));
        }
        long verifyStart = System.nanoTime();
        AppliedBytecodeCheck.Report verification = AppliedBytecodeCheck.check(inst, expectations);
        long verifyMicros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - verifyStart);
        PatchRecord record = new PatchRecord(id, plan.archiveSha256(), plan.mechanism(), actor, Instant.now(), Status.ACTIVE,
                definitions.size() + " class(es) " + verb + ", " + defined.size() + " defined in " + micros + " µs; "
                        + verification.summary(), records, defined, prepared.backupDirectory().toString(), Instant.now());
        this.registry.applied(record);
        List<String> details = new ArrayList<>();
        for (ClassRecord r : records) {
            details.add(verb + " " + r.name() + " (" + PatchPlanner.shortSha(r.previous().sha256()) + " -> "
                    + PatchPlanner.shortSha(r.installed().sha256()) + ")");
        }
        defined.forEach(n -> details.add("defined new class " + n));
        details.add("JVM " + (plan.mechanism() == Mechanism.RETRANSFORM ? "retransformation" : "redefinition") + " took " + micros
                + " µs on the main thread");
        if (plan.mechanism() == Mechanism.RETRANSFORM) {
            details.add("PaperSwap's transformer supplies the patch on every later retransformation; rollback removes it");
        }
        details.add("backup of the replaced class files: " + this.paths.serverDirectory().relativize(prepared.backupDirectory()));
        details.add(verification.summary() + (expectations.isEmpty() ? "" : " (read-back took " + verifyMicros + " µs)"));
        details.addAll(verification.lines());
        details.add("behaviour NOT validated by PaperSwap - test the changed code path now");
        this.audit.record(actor, "patch-apply", id, true, record.detail() + "; archive " + plan.archiveSha256());
        LiveHistory.INSTANCE.record(LiveHistory.Kind.PATCH, id, "apply", actor, true, record.detail());
        this.platform.logger().warning("Live patch " + id + " (" + plan.mechanism().id() + ") applied by " + actor + ": "
                + record.detail());
        verification.lines().forEach(l -> this.platform.logger().info("Live patch " + id + " check: " + l));
        return new Result(Result.Kind.APPLIED, plan, "patch " + id + " applied"
                + (verification.outcome() == AppliedBytecodeCheck.Outcome.VERIFIED ? " and verified" : ""), details, verification);
    }

    /**
     * Installs the classes through PaperSwap's transformer and one {@code retransformClasses}
     * call, and checks that the transformer really supplied the patch for every class.
     *
     * @return {@code null} on success, otherwise why nothing was changed
     */
    private String retransform(String id, List<ClassDefinition> definitions, Instrumentation inst) {
        if (definitions.isEmpty()) {
            return null;
        }
        Map<Class<?>, byte[]> layer = new LinkedHashMap<>();
        Map<Class<?>, Long> servedBefore = new LinkedHashMap<>();
        for (ClassDefinition d : definitions) {
            layer.put(d.getDefinitionClass(), d.getDefinitionClassFile());
            servedBefore.put(d.getDefinitionClass(), this.transformer.served(d.getDefinitionClass()));
        }
        try {
            this.transformer.register(inst);
            this.transformer.push(id, layer);
        } catch (IllegalStateException e) {
            return "cannot use PaperSwap's transformer: " + e.getMessage() + "; no class was changed";
        }
        try {
            inst.retransformClasses(layer.keySet().toArray(Class<?>[]::new));
        } catch (java.lang.instrument.UnmodifiableClassException | LinkageError | RuntimeException e) {
            this.transformer.remove(id, layer.keySet());
            return "the JVM rejected the retransformation: " + e + ". Per the JVMTI specification no class of this patch was"
                    + " changed; the patch was removed from PaperSwap's transformer.";
        }
        List<String> missed = new ArrayList<>();
        for (Map.Entry<Class<?>, Long> e : servedBefore.entrySet()) {
            if (this.transformer.served(e.getKey()) <= e.getValue()) {
                missed.add(e.getKey().getName());
            }
        }
        if (missed.isEmpty()) {
            return null;
        }
        // The JVM retransformed without taking the patch for some classes (see the server log). Take the
        // patch out again and retransform, so no class keeps half of the patch.
        this.transformer.remove(id, layer.keySet());
        try {
            inst.retransformClasses(layer.keySet().toArray(Class<?>[]::new));
        } catch (java.lang.instrument.UnmodifiableClassException | LinkageError | RuntimeException e) {
            return PARTIAL + "the transformer did not supply the patch for " + missed + ", and undoing the retransformation of"
                    + " the other classes failed (" + e + "): the JVM may run a part of the patch. Restart the server.";
        }
        return "the transformer did not supply the patch for " + missed + " during the retransformation (see the server log);"
                + " the patch was removed again and every class runs its previous code";
    }

    /** Start of a failure message after which the JVM may run a part of the patch. */
    private static final String PARTIAL = "PARTIALLY APPLIED: ";

    private Result fail(String actor, PatchPlan plan, List<String> defined, String message, Prepared prepared) {
        return fail(actor, plan, defined, message, prepared, true);
    }

    private Result fail(String actor, PatchPlan plan, List<String> defined, String message, Prepared prepared, boolean unchanged) {
        PatchRecord record = new PatchRecord(plan.patchId(), plan.archiveSha256(), plan.mechanism(), actor, Instant.now(),
                Status.FAILED, message,
                List.of(), defined, prepared.backupDirectory() == null ? "" : prepared.backupDirectory().toString(), Instant.now());
        this.registry.failed(record);
        List<String> details = new ArrayList<>();
        if (!defined.isEmpty()) {
            details.add("new classes defined before the failure stay loaded until a restart (nothing references them): " + defined);
        }
        details.add(unchanged ? "the server keeps running the previous code; see the server log for the full exception"
                : "the server may run a mix of old and new code; restart it");
        this.audit.record(actor, "patch-apply", plan.patchId(), false, message);
        LiveHistory.INSTANCE.record(LiveHistory.Kind.PATCH, plan.patchId(), "apply", actor, false, message);
        this.platform.logger().warning("Live patch " + plan.patchId() + " FAILED: " + message);
        return new Result(Result.Kind.FAILED, plan, message, details);
    }

    // ------------------------------------------------------------------ rollback

    /**
     * Restores the bytecode that ran before a patch. Only the most recent version of each
     * class can be rolled back (a later patch of the same class must be rolled back first).
     * A redefinition patch is rolled back by redefining the previous class files; a
     * retransformation patch by removing it from PaperSwap's transformer and retransforming.
     * Bytecode rollback does not restore game state or object state.
     *
     * @param actor   who asked
     * @param id      patch id
     * @param confirm second step
     * @return the outcome
     */
    public CompletableFuture<Result> rollback(String actor, String id, boolean confirm) {
        Instrumentation inst;
        try {
            inst = requireUsable(id);
        } catch (PatchException e) {
            return CompletableFuture.completedFuture(rejected(null, e.getMessage()));
        }
        if (!this.busy.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(rejected(null, "another live-patch operation is running"));
        }
        String key = actor + "\u0000rollback\u0000" + id;
        Optional<PatchRecord> record = this.registry.latest(id).filter(r -> r.status() == Status.ACTIVE);
        if (record.isEmpty()) {
            this.busy.set(false);
            return CompletableFuture.completedFuture(rejected(null, "patch " + id + " is not active in this JVM"
                    + " (patches do not survive a restart; after a restart the server runs its jar again)"));
        }
        long version = this.registry.version();
        CompletableFuture<Result> result = onWorker(() -> checkRollback(record.get(), inst)).thenApplyAsync(problems -> {
            if (!problems.isEmpty()) {
                return new Result(Result.Kind.REJECTED, null, "patch " + id + " cannot be rolled back", problems);
            }
            List<String> notes = new ArrayList<>();
            record.get().classes().forEach(c -> notes.add("restore " + c.name() + " to " + PatchPlanner.shortSha(c.previous().sha256())
                    + (c.previous().installedBy() == null ? " (jar version)" : " (version of patch " + c.previous().installedBy() + ")")));
            if (!record.get().definedClasses().isEmpty()) {
                notes.add("classes defined by the patch stay loaded until a restart: " + record.get().definedClasses());
            }
            notes.add("only bytecode is restored; game state, objects and static fields are NOT rolled back");
            if (!confirm) {
                this.pending.put(key, new Pending(record.get().archiveSha256(), record.get().mechanism(),
                        Instant.now().plusSeconds(confirmTimeout())));
                return new Result(Result.Kind.CONFIRMATION_REQUIRED, null, "rollback of " + id + " is ready", notes);
            }
            Pending p = this.pending.remove(key);
            if (p == null || p.expires().isBefore(Instant.now()) || !p.archiveSha256().equals(record.get().archiveSha256())) {
                return rejected(null, "no rollback of " + id + " is waiting for confirmation by " + actor
                        + "; run '/fork live rollback " + id + "' first");
            }
            if (this.registry.version() != version) {
                return rejected(null, "another patch was applied or rolled back meanwhile; try again");
            }
            return executeRollback(actor, record.get(), inst, notes);
        }, this.platform.mainThreadExecutor());
        return result.whenComplete((r, ex) -> this.busy.set(false));
    }

    private List<String> checkRollback(PatchRecord record, Instrumentation inst) throws ClassFormatException {
        List<String> problems = new ArrayList<>();
        Set<Capability> needed = EnumSet.noneOf(Capability.class);
        List<ClassChange> changes = new ArrayList<>();
        if (record.mechanism() == Mechanism.RETRANSFORM) {
            if (!inst.isRetransformClassesSupported()) {
                problems.add("retransformation is not available any more");
            }
            ClassLoader loader = this.platform.serverClassLoader();
            for (ClassRecord c : record.classes()) {
                try {
                    Optional<PatchTransformer.SlotInfo> slot = this.transformer.slot(Class.forName(c.name(), false, loader));
                    List<String> layers = slot.map(PatchTransformer.SlotInfo::patchIds).orElse(List.of());
                    if (layers.isEmpty() || !layers.get(layers.size() - 1).equals(record.id())) {
                        problems.add(c.name() + ": PaperSwap's transformer holds " + layers + ", not patch " + record.id() + " on top");
                    }
                } catch (ClassNotFoundException | LinkageError e) {
                    problems.add(c.name() + ": cannot be resolved: " + e);
                }
            }
        }
        for (ClassRecord c : record.classes()) {
            Optional<ClassVersion> now = this.registry.current(c.name());
            if (now.isEmpty() || !now.get().sha256().equals(c.installed().sha256())) {
                problems.add(c.name() + " was changed again by patch " + now.map(ClassVersion::installedBy).orElse("?")
                        + "; roll that patch back first");
                continue;
            }
            for (ClassChange change : ClassDiff.compare(ClassFileReader.read(c.installed().bytes()),
                    ClassFileReader.read(c.previous().bytes()))) {
                changes.add(change);
                Set<Capability> required = PatchPlanner.required(change.kind(), record.mechanism());
                needed.addAll(required);
                if (required.isEmpty()) {
                    problems.add(c.name() + ": reverting '" + change + "' is never applied by PaperSwap");
                }
            }
        }
        Map<Capability, CapabilityProbe.Result> caps = JvmCapabilities.ensure(inst, needed);
        for (Capability cap : needed) {
            if (!caps.get(cap).supported()) {
                problems.add("this JVM cannot " + cap.description() + ", which the rollback needs: " + caps.get(cap).detail());
            }
        }
        return problems;
    }

    private Result executeRollback(String actor, PatchRecord record, Instrumentation inst, List<String> notes) {
        ClassLoader loader = this.platform.serverClassLoader();
        List<ClassDefinition> definitions = new ArrayList<>();
        try {
            for (ClassRecord c : record.classes()) {
                definitions.add(new ClassDefinition(Class.forName(c.name(), false, loader), c.previous().bytes()));
            }
            long start = System.nanoTime();
            List<String> warnings = new ArrayList<>();
            if (record.mechanism() == Mechanism.RETRANSFORM) {
                String problem = removeRetransformed(record.id(), definitions, inst, warnings);
                if (problem != null) {
                    String message = problem + "; patch " + record.id() + " stays active.";
                    this.audit.record(actor, "patch-rollback", record.id(), false, message);
                    LiveHistory.INSTANCE.record(LiveHistory.Kind.PATCH, record.id(), "rollback", actor, false, message);
                    return new Result(Result.Kind.FAILED, null, message, List.of());
                }
            } else {
                inst.redefineClasses(definitions.toArray(ClassDefinition[]::new));
            }
            long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
            List<AppliedBytecodeCheck.Expectation> expectations = new ArrayList<>();
            for (int i = 0; i < definitions.size(); i++) {
                ClassRecord c = record.classes().get(i);
                expectations.add(new AppliedBytecodeCheck.Expectation(definitions.get(i).getDefinitionClass(), c.previous().bytes(),
                        c.installed().bytes()));
            }
            AppliedBytecodeCheck.Report verification = AppliedBytecodeCheck.check(inst, expectations);
            String detail = "rolled back by " + actor + ": " + definitions.size() + " class(es) restored in " + micros + " µs; "
                    + verification.summary();
            this.registry.rolledBack(record.id(), detail);
            this.audit.record(actor, "patch-rollback", record.id(), true, detail);
            LiveHistory.INSTANCE.record(LiveHistory.Kind.PATCH, record.id(), "rollback", actor, true, detail);
            this.platform.logger().warning("Live patch " + record.id() + " " + detail);
            List<String> details = new ArrayList<>(notes);
            details.add("JVM " + (record.mechanism() == Mechanism.RETRANSFORM ? "retransformation" : "redefinition") + " took "
                    + micros + " µs on the main thread");
            details.addAll(warnings);
            details.add(verification.summary());
            details.addAll(verification.lines());
            return new Result(Result.Kind.ROLLED_BACK, null, "patch " + record.id() + " rolled back"
                    + (verification.outcome() == AppliedBytecodeCheck.Outcome.VERIFIED ? " and verified" : ""), details, verification);
        } catch (ClassNotFoundException | java.lang.instrument.UnmodifiableClassException | LinkageError | RuntimeException e) {
            String message = "the JVM rejected the rollback: " + e + ". Per the JVMTI specification no class was changed;"
                    + " patch " + record.id() + " stays active.";
            this.audit.record(actor, "patch-rollback", record.id(), false, message);
            LiveHistory.INSTANCE.record(LiveHistory.Kind.PATCH, record.id(), "rollback", actor, false, message);
            return new Result(Result.Kind.FAILED, null, message, List.of());
        }
    }

    /**
     * Takes a retransformation patch out of PaperSwap's transformer and retransforms; the layer
     * below (or the class's initial bytes) runs again. Puts the patch back if the JVM refuses.
     *
     * @param warnings receives problems found after a successful retransformation
     * @return {@code null} if the patch was removed, otherwise why nothing was changed
     */
    private String removeRetransformed(String id, List<ClassDefinition> definitions, Instrumentation inst, List<String> warnings) {
        List<Class<?>> classes = definitions.stream().<Class<?>>map(ClassDefinition::getDefinitionClass).toList();
        Map<Class<?>, Long> servedBefore = new LinkedHashMap<>();
        classes.forEach(c -> servedBefore.put(c, this.transformer.served(c)));
        Map<Class<?>, PatchTransformer.Layer> removed = this.transformer.remove(id, classes);
        try {
            inst.retransformClasses(classes.toArray(Class<?>[]::new));
        } catch (java.lang.instrument.UnmodifiableClassException | LinkageError | RuntimeException e) {
            this.transformer.restore(removed);
            return "the JVM rejected the retransformation: " + e + ". Per the JVMTI specification no class was changed";
        }
        List<String> missed = new ArrayList<>();
        for (Class<?> c : classes) {
            // A class with a lower layer must have received that layer again.
            if (this.transformer.slot(c).isPresent() && this.transformer.served(c) <= servedBefore.get(c)) {
                missed.add(c.getName());
            }
        }
        if (!missed.isEmpty()) {
            warnings.add("WARNING: the transformer did not supply the lower patch layer of " + missed + " (see the server log);"
                    + " the read-back below shows what runs now");
        }
        return null;
    }

    // ------------------------------------------------------------------ verify

    /**
     * Reads back the bytecode the JVM runs for every class of an active patch and compares
     * it with the class files of the patch. Changes nothing (the read-back is a
     * retransformation without changes).
     *
     * @param actor who asked
     * @param id    patch id
     * @return {@link Result.Kind#APPLIED} with the report, or a rejection
     */
    public CompletableFuture<Result> verify(String actor, String id) {
        Instrumentation inst;
        try {
            inst = requireUsable(id);
        } catch (PatchException e) {
            return CompletableFuture.completedFuture(rejected(null, e.getMessage()));
        }
        Optional<PatchRecord> record = this.registry.latest(id).filter(r -> r.status() == Status.ACTIVE);
        if (record.isEmpty()) {
            return CompletableFuture.completedFuture(rejected(null, "patch " + id + " is not active in this JVM"));
        }
        if (!this.busy.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(rejected(null, "another live-patch operation is running"));
        }
        CompletableFuture<Result> result = CompletableFuture.supplyAsync(() -> {
            ClassLoader loader = this.platform.serverClassLoader();
            List<AppliedBytecodeCheck.Expectation> expectations = new ArrayList<>();
            List<String> notes = new ArrayList<>();
            for (ClassRecord c : record.get().classes()) {
                Optional<ClassVersion> now = this.registry.current(c.name());
                if (now.isEmpty() || !now.get().sha256().equals(c.installed().sha256())) {
                    notes.add(c.name() + ": replaced again by patch " + now.map(ClassVersion::installedBy).orElse("?")
                            + "; checked against that version");
                }
                ClassVersion expected = now.orElse(c.installed());
                try {
                    Class<?> type = Class.forName(c.name(), false, loader);
                    expectations.add(new AppliedBytecodeCheck.Expectation(type, expected.bytes(), c.previous().bytes()));
                    this.transformer.slot(type).ifPresent(s -> notes.add(c.name() + ": transformer layers " + s.patchIds()
                            + ", supplied " + s.served() + " time(s)" + (s.displaced() == null ? "" : ", DISPLACED: " + s.displaced())));
                } catch (ClassNotFoundException | LinkageError e) {
                    notes.add(c.name() + ": cannot be resolved: " + e);
                }
            }
            AppliedBytecodeCheck.Report report = AppliedBytecodeCheck.check(inst, expectations);
            List<String> details = new ArrayList<>(notes);
            details.add(report.summary());
            details.addAll(report.lines());
            if (!record.get().definedClasses().isEmpty()) {
                details.add("classes added by the patch are not part of this check: " + record.get().definedClasses());
            }
            this.audit.record(actor, "patch-verify", id, report.outcome() == AppliedBytecodeCheck.Outcome.VERIFIED, report.summary());
            return new Result(Result.Kind.APPLIED, null, "patch " + id + ": " + report.summary(), details, report);
        }, this.platform.mainThreadExecutor());
        return result.whenComplete((r, ex) -> this.busy.set(false));
    }

    // ------------------------------------------------------------------ loaded classes

    /**
     * Searches the loaded classes (worker thread; changes nothing).
     *
     * @param query   query
     * @param loaders loader knowledge, collected on the main thread
     * @return a page of results
     */
    public CompletableFuture<ClassCatalog.Page> classes(ClassCatalog.Query query, ClassCatalog.Loaders loaders) {
        InstrumentationAccess.Lookup lookup = agent();
        if (lookup.instrumentation().isEmpty()) {
            return CompletableFuture.failedFuture(new PatchException(lookup.detail()));
        }
        Instrumentation inst = lookup.instrumentation().get();
        return onWorker(() -> ClassCatalog.search(inst, query, loaders, this.registry, this.transformer));
    }

    /**
     * Describes every loaded class with this name (worker thread; loads and changes nothing).
     *
     * @param className binary name
     * @param loaders   loader knowledge, collected on the main thread
     * @return one description per defining class loader; empty if the class is not loaded
     */
    public CompletableFuture<List<ClassCatalog.Description>> describe(String className, ClassCatalog.Loaders loaders) {
        InstrumentationAccess.Lookup lookup = agent();
        if (lookup.instrumentation().isEmpty()) {
            return CompletableFuture.failedFuture(new PatchException(lookup.detail()));
        }
        String problem = ClassCatalog.nameProblem(className);
        if (problem != null) {
            return CompletableFuture.failedFuture(new PatchException(problem));
        }
        Instrumentation inst = lookup.instrumentation().get();
        return onWorker(() -> ClassCatalog.find(inst, className).stream()
                .map(c -> ClassCatalog.describe(inst, c, loaders, this.registry, this.transformer)).toList());
    }

    /**
     * Result of {@link #compareRunning}.
     *
     * @param className   binary name
     * @param loader      defining class loader
     * @param expected    what the running bytecode was compared with
     * @param differences differences; empty = equal
     */
    public record RunningCheck(String className, String loader, String expected, List<ClassChange> differences) {
        public RunningCheck {
            differences = List.copyOf(differences);
        }
    }

    /**
     * Reads back the bytecode the JVM runs for one loaded class and compares it with what
     * PaperSwap expects: the class file of its loader, or the version of PaperSwap's active patch.
     * This is a retransformation (main thread, one live-patch operation at a time), so it
     * needs {@code live-patching.enabled}.
     *
     * @param actor     who asked
     * @param className binary name
     * @param index     1-based index among the loaded classes with that name (sorted by loader)
     * @return the comparison
     */
    public CompletableFuture<RunningCheck> compareRunning(String actor, String className, int index) {
        if (!enabled()) {
            return CompletableFuture.failedFuture(new PatchException("reading back running bytecode is a retransformation and"
                    + " needs live-patching.enabled: true"));
        }
        String problem = ClassCatalog.nameProblem(className);
        if (problem != null) {
            return CompletableFuture.failedFuture(new PatchException(problem));
        }
        InstrumentationAccess.Lookup lookup = agent();
        Optional<Instrumentation> found = lookup.instrumentation();
        if (found.isEmpty()) {
            return CompletableFuture.failedFuture(new PatchException(lookup.detail()));
        }
        Instrumentation inst = found.get();
        if (!inst.isRetransformClassesSupported()) {
            return CompletableFuture.failedFuture(new PatchException("this JVM or agent cannot retransform, so the running"
                    + " bytecode cannot be read back"));
        }
        if (!this.busy.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new PatchException("another live-patch operation is running"));
        }
        CompletableFuture<RunningCheck> result = CompletableFuture.supplyAsync(() -> {
            try {
                List<Class<?>> classes = ClassCatalog.find(inst, className);
                if (index < 1 || index > classes.size()) {
                    throw new PatchException(classes.isEmpty() ? className + " is not loaded"
                            : "there are " + classes.size() + " loaded classes named " + className + "; choose 1-" + classes.size());
                }
                Class<?> c = classes.get(index - 1);
                if (!inst.isModifiableClass(c)) {
                    throw new PatchException(className + " is not modifiable, so the JVM cannot report its running bytecode");
                }
                byte[] expected;
                String source;
                Optional<PatchTransformer.SlotInfo> slot = this.transformer.slot(c);
                Optional<ClassVersion> patched = c.getClassLoader() != null && c.getClassLoader() == this.platform.serverClassLoader()
                        ? this.registry.current(className) : Optional.empty();
                if (patched.isPresent()) {
                    expected = patched.get().bytes();
                    source = "the version of active patch " + patched.get().installedBy()
                            + slot.map(s -> " (retransformation layers " + s.patchIds() + ")").orElse("");
                } else {
                    Optional<byte[]> file = ClassCatalog.classFile(c);
                    if (file.isEmpty()) {
                        throw new PatchException("no class file of " + className + " is visible to its class loader");
                    }
                    expected = file.get();
                    source = "the class file its class loader provides (" + PatchPlanner.shortSha(Hashing.sha256(expected)) + ")";
                }
                List<ClassChange> diff = ClassCatalog.runningBytecode(inst, c, expected);
                String summary = diff.isEmpty() ? "running bytecode equals " + source : diff.size() + " difference(s) to " + source;
                this.audit.record(actor, "class-verify", className, diff.isEmpty(), summary);
                return new RunningCheck(className, PatchTransformer.loaderName(c.getClassLoader()), source, diff);
            } catch (PatchException e) {
                throw new java.util.concurrent.CompletionException(e);
            } catch (Exception | LinkageError e) {
                throw new java.util.concurrent.CompletionException(new PatchException("reading back " + className + " failed: " + e
                        + " (per the JVMTI specification no class was changed)"));
            }
        }, this.platform.mainThreadExecutor());
        return result.whenComplete((r, ex) -> this.busy.set(false));
    }

    // ------------------------------------------------------------------ helpers

    private Instrumentation requireUsable(String id) throws PatchException {
        return requireUsable(id, Mechanism.REDEFINE);
    }

    private Instrumentation requireUsable(String id, Mechanism mechanism) throws PatchException {
        if (!enabled()) {
            throw new PatchException("live patching is disabled (live-patching.enabled: false in "
                    + RuntimePaths.DEFAULT_CONFIG + ")");
        }
        String problem = PatchIds.problem(id);
        if (problem != null) {
            throw new PatchException("invalid patch id '" + PathGuard.sanitize(id) + "': " + problem);
        }
        InstrumentationAccess.Lookup lookup = agent();
        Optional<Instrumentation> inst = lookup.instrumentation();
        if (inst.isEmpty()) {
            throw new PatchException(lookup.detail());
        }
        if (!inst.get().isRedefineClassesSupported()) {
            throw new PatchException("this JVM does not support class redefinition");
        }
        if (mechanism == Mechanism.RETRANSFORM && !inst.get().isRetransformClassesSupported()) {
            throw new PatchException("this JVM or agent does not support retransformation (agent " + lookup.agentVersion()
                    + "; agent version 2 or newer declares Can-Retransform-Classes)");
        }
        return inst.get();
    }

    private PatchPlan plan(PatchArchive archive, Instrumentation inst, Mechanism mechanism) {
        return PatchPlanner.plan(archive, new PatchPlanner.Context(this.platform.serverBuildId(), this.platform.minecraftVersion(),
                this.platform.serverClassLoader(), inst, this.registry, this.profile == LivePatchConfig.Profile.DEVELOPMENT,
                mechanism, this.transformer));
    }

    /**
     * Reads {@code <id>.swappatch} into memory, verifies sidecar and allow-list over exactly
     * those bytes and parses them.
     */
    private PatchArchive loadVerified(String id) throws PatchException {
        Path dir = this.paths.patchDirectory(this.config.get());
        Path file = PathGuard.resolveInside(dir, id + PatchIds.SUFFIX);
        byte[] bytes;
        try {
            if (!Files.exists(file)) {
                throw new PatchException(id + PatchIds.SUFFIX + " not found in " + this.paths.serverDirectory().relativize(dir));
            }
            PathGuard.requireRegularFile(file);
            if (Files.size(file) > PatchArchive.MAX_ARCHIVE_BYTES) {
                throw new PatchException(file.getFileName() + " is larger than " + PatchArchive.MAX_ARCHIVE_BYTES + " bytes");
            }
            bytes = Files.readAllBytes(file);
        } catch (IOException | SecurityException e) {
            throw new PatchException("cannot read " + id + PatchIds.SUFFIX + ": " + e.getMessage());
        }
        String sha = Hashing.sha256(bytes);
        Optional<Path> trusted = this.paths.patchTrustedHashes(this.config.get());
        if (trusted.isEmpty() && this.profile == LivePatchConfig.Profile.PRODUCTION) {
            throw new PatchException("the production profile only applies patches listed in live-patching.trusted-hashes-file,"
                    + " which is not configured");
        }
        try {
            new IntegrityVerifier(true, trusted).verifyDigest(file, sha);
        } catch (IntegrityVerifier.IntegrityException e) {
            throw new PatchException("integrity check failed: " + e.getMessage());
        } catch (IOException e) {
            throw new PatchException("integrity check failed: " + e.getMessage());
        }
        PatchArchive archive = PatchArchive.parse(bytes);
        if (!archive.manifest().id().equals(id)) {
            throw new PatchException("file " + file.getFileName() + " contains patch '" + archive.manifest().id()
                    + "'; the file name must be <id>" + PatchIds.SUFFIX);
        }
        return archive;
    }

    private long confirmTimeout() {
        return this.config.get().livePatching().confirmTimeoutSeconds();
    }

    @FunctionalInterface
    private interface Work<T> {
        T run() throws Exception;
    }

    private <T> CompletableFuture<T> onWorker(Work<T> work) {
        CompletableFuture<T> f = new CompletableFuture<>();
        try {
            this.worker.execute(() -> {
                try {
                    f.complete(work.run());
                } catch (Throwable t) {
                    if (t instanceof VirtualMachineError vme && !(t instanceof StackOverflowError)) {
                        f.completeExceptionally(t);
                        throw vme;
                    }
                    f.completeExceptionally(t);
                }
            });
        } catch (RuntimeException e) {
            f.completeExceptionally(e);
        }
        return f.orTimeout(this.config.get().operationTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }
}
