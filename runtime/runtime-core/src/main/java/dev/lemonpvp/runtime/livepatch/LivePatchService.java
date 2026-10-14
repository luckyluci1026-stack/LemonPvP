package dev.lemonpvp.runtime.livepatch;

import dev.lemonpvp.runtime.RuntimePaths;
import dev.lemonpvp.runtime.config.LivePatchConfig;
import dev.lemonpvp.runtime.config.RuntimeConfig;
import dev.lemonpvp.runtime.live.LiveHistory;
import dev.lemonpvp.runtime.livepatch.LivePatchRegistry.ClassRecord;
import dev.lemonpvp.runtime.livepatch.LivePatchRegistry.ClassVersion;
import dev.lemonpvp.runtime.livepatch.LivePatchRegistry.PatchRecord;
import dev.lemonpvp.runtime.livepatch.LivePatchRegistry.Status;
import dev.lemonpvp.runtime.livepatch.classfile.ClassFileReader;
import dev.lemonpvp.runtime.livepatch.classfile.ClassFormatException;
import dev.lemonpvp.runtime.platform.RuntimePlatform;
import dev.lemonpvp.runtime.security.AuditLog;
import dev.lemonpvp.runtime.security.Hashing;
import dev.lemonpvp.runtime.security.IntegrityVerifier;
import dev.lemonpvp.runtime.security.PathGuard;
import java.io.IOException;
import java.io.InputStream;
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
 * Core live patching: inspects, applies and rolls back patches that redefine classes of
 * the running server through {@link Instrumentation#redefineClasses}.
 *
 * <p>Threading: file I/O, hashing, parsing and planning run on the runtime worker thread.
 * The redefinition itself runs on the server main thread, between two scheduled tasks, so
 * no game logic of the main thread is half-way through a patched method of its own tick
 * code (other threads may still be inside old method versions; the JVM lets them finish
 * with the old code). One live-patch operation runs at a time.</p>
 *
 * <p>Atomicity: all classes of a patch are passed to one {@code redefineClasses} call. The
 * JVMTI specification guarantees that either all of them get the new definition or none.
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
    private final InstrumentationAccess.Lookup agent;
    private final LivePatchConfig.Profile profile;
    private final String profileNote;
    private final AtomicBoolean busy = new AtomicBoolean();
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();

    private record Pending(String archiveSha256, Instant expires) {
    }

    /**
     * Outcome of an apply or rollback request.
     *
     * @param kind    outcome
     * @param plan    the plan (apply only), or {@code null}
     * @param message one-line summary
     * @param details further lines
     */
    public record Result(Kind kind, PatchPlan plan, String message, List<String> details) {
        public Result {
            details = List.copyOf(details);
        }

        /** Outcome kinds. */
        public enum Kind {
            /** Plan is valid; repeat the command with {@code confirm}. */
            CONFIRMATION_REQUIRED,
            /** Refused before anything was changed. */
            REJECTED,
            /** Redefinition succeeded. */
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
     * @param instrumentation  whether the instrumentation instance is available
     * @param redefineSupported whether the JVM supports redefinition at all
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
                               boolean instrumentation, boolean redefineSupported, String vm, String enhancedFlag, String buildId,
                               Path patchDirectory, String trustNote, long active, int attempts, long heapUsedMiB, long heapMaxMiB) {
    }

    /**
     * @param config   configuration supplier (re-read for reloadable settings)
     * @param paths    file layout
     * @param platform hosting platform
     * @param worker   worker executor for I/O
     * @param audit    audit log
     * @param registry registry of this JVM
     * @param agent    agent lookup result
     */
    public LivePatchService(Supplier<RuntimeConfig> config, RuntimePaths paths, RuntimePlatform platform, Executor worker,
                            AuditLog audit, LivePatchRegistry registry, InstrumentationAccess.Lookup agent) {
        this.config = config;
        this.paths = paths;
        this.platform = platform;
        this.worker = worker;
        this.audit = audit;
        this.registry = registry;
        this.agent = agent;
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

    // ------------------------------------------------------------------ read-only views

    /** @return current status (no I/O) */
    public StatusReport status() {
        Optional<Instrumentation> inst = this.agent.instrumentation();
        Runtime rt = Runtime.getRuntime();
        return new StatusReport(enabled(), this.profile, this.profileNote, this.agent.detail(), inst.isPresent(),
                inst.map(Instrumentation::isRedefineClassesSupported).orElse(false), JvmCapabilities.vmDescription(),
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
        Optional<Instrumentation> inst = this.agent.instrumentation();
        if (inst.isEmpty()) {
            return CompletableFuture.failedFuture(new PatchException(this.agent.detail()));
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
     * Creates the plan for a patch without changing anything.
     *
     * @param id patch id
     * @return the plan
     */
    public CompletableFuture<PatchPlan> inspect(String id) {
        try {
            Instrumentation inst = requireUsable(id);
            return onWorker(() -> plan(loadVerified(id), inst));
        } catch (PatchException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    // ------------------------------------------------------------------ apply

    /**
     * Applies a patch. Without {@code confirm} only the plan is created and remembered;
     * with {@code confirm} the remembered plan is re-checked and executed.
     *
     * @param actor   who asked
     * @param id      patch id
     * @param confirm second step
     * @return the outcome
     */
    public CompletableFuture<Result> apply(String actor, String id, boolean confirm) {
        Instrumentation inst;
        try {
            inst = requireUsable(id);
        } catch (PatchException e) {
            return CompletableFuture.completedFuture(rejected(null, e.getMessage()));
        }
        if (!this.busy.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(rejected(null, "another live-patch operation is running"));
        }
        String key = actor + "\u0000apply\u0000" + id;
        CompletableFuture<Result> result;
        if (!confirm) {
            result = onWorker(() -> plan(loadVerified(id), inst)).thenApplyAsync(plan -> {
                if (!plan.applicable()) {
                    this.audit.record(actor, "patch-plan", id, false, String.join("; ", plan.errors()));
                    return new Result(Result.Kind.REJECTED, plan, "patch " + id + " cannot be applied", plan.errors());
                }
                this.pending.put(key, new Pending(plan.archiveSha256(), Instant.now().plusSeconds(confirmTimeout())));
                this.audit.record(actor, "patch-plan", id, true, "plan created, waiting for confirmation; archive " + plan.archiveSha256());
                return new Result(Result.Kind.CONFIRMATION_REQUIRED, plan, "plan for " + id + " is ready", List.of());
            }, this.platform.mainThreadExecutor());
        } else {
            Pending p = this.pending.remove(key);
            if (p == null || p.expires().isBefore(Instant.now())) {
                this.busy.set(false);
                return CompletableFuture.completedFuture(rejected(null, "no plan of " + id + " is waiting for confirmation by "
                        + actor + " (it expired or was never created); run '/fork live update " + id + "' first"));
            }
            result = onWorker(() -> prepare(id, inst, p.archiveSha256())).thenApplyAsync(prepared -> {
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

    private Prepared prepare(String id, Instrumentation inst, String confirmedSha) throws PatchException, IOException {
        PatchArchive archive = loadVerified(id);
        if (!archive.sha256().equals(confirmedSha)) {
            return new Prepared(archive, null, Map.of(), null, rejected(null, id + PatchIds.SUFFIX
                    + " changed after the plan was shown (was " + PatchPlanner.shortSha(confirmedSha) + ", now "
                    + PatchPlanner.shortSha(archive.sha256()) + "); inspect it again"));
        }
        PatchPlan plan = plan(archive, inst);
        if (!plan.applicable()) {
            return new Prepared(archive, plan, Map.of(), null, new Result(Result.Kind.REJECTED, plan,
                    "patch " + id + " cannot be applied any more", plan.errors()));
        }
        Map<String, byte[]> previous = new LinkedHashMap<>();
        ClassLoader loader = this.platform.serverClassLoader();
        for (PatchPlan.Entry e : plan.redefinitions()) {
            Optional<ClassVersion> patched = this.registry.current(e.className());
            byte[] bytes = patched.isPresent() ? patched.get().bytes() : readResource(loader, e.className());
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

    /** Main thread: performs the redefinition. */
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
        try {
            if (!definitions.isEmpty()) {
                inst.redefineClasses(definitions.toArray(ClassDefinition[]::new));
            }
        } catch (ClassNotFoundException | java.lang.instrument.UnmodifiableClassException | LinkageError | RuntimeException e) {
            return fail(actor, plan, defined, "the JVM rejected the redefinition: " + e
                    + ". Per the JVMTI specification no class of this patch was changed.", prepared);
        }
        long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
        PatchRecord record = new PatchRecord(id, plan.archiveSha256(), actor, Instant.now(), Status.ACTIVE,
                definitions.size() + " class(es) redefined, " + defined.size() + " defined in " + micros + " µs",
                records, defined, prepared.backupDirectory().toString(), Instant.now());
        this.registry.applied(record);
        List<String> details = new ArrayList<>();
        for (ClassRecord r : records) {
            details.add("redefined " + r.name() + " (" + PatchPlanner.shortSha(r.previous().sha256()) + " -> "
                    + PatchPlanner.shortSha(r.installed().sha256()) + ")");
        }
        defined.forEach(n -> details.add("defined new class " + n));
        details.add("JVM redefinition took " + micros + " µs on the main thread");
        details.add("backup of the replaced class files: " + this.paths.serverDirectory().relativize(prepared.backupDirectory()));
        details.add("status: technically applied; behaviour NOT validated by Lemon - test the changed code path now");
        this.audit.record(actor, "patch-apply", id, true, record.detail() + "; archive " + plan.archiveSha256());
        LiveHistory.INSTANCE.record(LiveHistory.Kind.PATCH, id, "apply", actor, true, record.detail());
        this.platform.logger().warning("Live patch " + id + " applied by " + actor + ": " + record.detail());
        return new Result(Result.Kind.APPLIED, plan, "patch " + id + " applied", details);
    }

    private Result fail(String actor, PatchPlan plan, List<String> defined, String message, Prepared prepared) {
        PatchRecord record = new PatchRecord(plan.patchId(), plan.archiveSha256(), actor, Instant.now(), Status.FAILED, message,
                List.of(), defined, prepared.backupDirectory() == null ? "" : prepared.backupDirectory().toString(), Instant.now());
        this.registry.failed(record);
        List<String> details = new ArrayList<>();
        if (!defined.isEmpty()) {
            details.add("new classes defined before the failure stay loaded until a restart (nothing references them): " + defined);
        }
        details.add("the server keeps running the previous code; see the server log for the full exception");
        this.audit.record(actor, "patch-apply", plan.patchId(), false, message);
        LiveHistory.INSTANCE.record(LiveHistory.Kind.PATCH, plan.patchId(), "apply", actor, false, message);
        this.platform.logger().warning("Live patch " + plan.patchId() + " FAILED: " + message);
        return new Result(Result.Kind.FAILED, plan, message, details);
    }

    // ------------------------------------------------------------------ rollback

    /**
     * Restores the bytecode that ran before a patch. Only the most recent version of each
     * class can be rolled back (a later patch of the same class must be rolled back first).
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
                this.pending.put(key, new Pending(record.get().archiveSha256(), Instant.now().plusSeconds(confirmTimeout())));
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
                needed.addAll(change.kind().required());
                if (change.kind().required().isEmpty()) {
                    problems.add(c.name() + ": reverting '" + change + "' is never applied by Lemon");
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
            inst.redefineClasses(definitions.toArray(ClassDefinition[]::new));
            long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
            String detail = "rolled back by " + actor + ": " + definitions.size() + " class(es) restored in " + micros + " µs";
            this.registry.rolledBack(record.id(), detail);
            this.audit.record(actor, "patch-rollback", record.id(), true, detail);
            LiveHistory.INSTANCE.record(LiveHistory.Kind.PATCH, record.id(), "rollback", actor, true, detail);
            this.platform.logger().warning("Live patch " + record.id() + " " + detail);
            List<String> details = new ArrayList<>(notes);
            details.add("JVM redefinition took " + micros + " µs on the main thread");
            return new Result(Result.Kind.ROLLED_BACK, null, "patch " + record.id() + " rolled back", details);
        } catch (ClassNotFoundException | java.lang.instrument.UnmodifiableClassException | LinkageError | RuntimeException e) {
            String message = "the JVM rejected the rollback: " + e + ". Per the JVMTI specification no class was changed;"
                    + " patch " + record.id() + " stays active.";
            this.audit.record(actor, "patch-rollback", record.id(), false, message);
            LiveHistory.INSTANCE.record(LiveHistory.Kind.PATCH, record.id(), "rollback", actor, false, message);
            return new Result(Result.Kind.FAILED, null, message, List.of());
        }
    }

    // ------------------------------------------------------------------ helpers

    private Instrumentation requireUsable(String id) throws PatchException {
        if (!enabled()) {
            throw new PatchException("live patching is disabled (live-patching.enabled: false in "
                    + RuntimePaths.DEFAULT_CONFIG + ")");
        }
        String problem = PatchIds.problem(id);
        if (problem != null) {
            throw new PatchException("invalid patch id '" + PathGuard.sanitize(id) + "': " + problem);
        }
        Optional<Instrumentation> inst = this.agent.instrumentation();
        if (inst.isEmpty()) {
            throw new PatchException(this.agent.detail());
        }
        if (!inst.get().isRedefineClassesSupported()) {
            throw new PatchException("this JVM does not support class redefinition");
        }
        return inst.get();
    }

    private PatchPlan plan(PatchArchive archive, Instrumentation inst) {
        return PatchPlanner.plan(archive, new PatchPlanner.Context(this.platform.serverBuildId(), this.platform.minecraftVersion(),
                this.platform.serverClassLoader(), inst, this.registry, this.profile == LivePatchConfig.Profile.DEVELOPMENT));
    }

    /**
     * Reads {@code <id>.lemonpatch} into memory, verifies sidecar and allow-list over exactly
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

    private static byte[] readResource(ClassLoader loader, String className) throws IOException {
        String path = className.replace('.', '/') + ".class";
        try (InputStream in = loader.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException(path + " not found");
            }
            return in.readNBytes(PatchArchive.MAX_CLASS_BYTES + 1);
        }
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
