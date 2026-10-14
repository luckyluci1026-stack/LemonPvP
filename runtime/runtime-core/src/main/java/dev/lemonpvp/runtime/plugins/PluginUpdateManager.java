package dev.lemonpvp.runtime.plugins;

import dev.lemonpvp.runtime.RuntimePaths;
import dev.lemonpvp.runtime.config.RuntimeConfig;
import dev.lemonpvp.runtime.live.LiveHistory;
import dev.lemonpvp.runtime.platform.RuntimePlatform;
import dev.lemonpvp.runtime.security.AuditLog;
import dev.lemonpvp.runtime.security.PathGuard;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Live installation, update and rollback of normal Paper/Bukkit plugins.
 *
 * <p>Flow of an update: the new jar lies in the server's update folder; {@code check}
 * inspects it and decides ({@link PluginCompatibility}); {@code update} shows that verdict
 * and waits for {@code confirm}; the confirmed update copies the old jar to
 * {@code lemon/plugin-backups/}, unloads the old plugin, moves the new jar into
 * {@code plugins/} and loads it. If the new version does not load or enable, the old jar
 * is put back and loaded again, and the result says whether that worked.</p>
 *
 * <p>Threading: jar inspection and copies run on the worker thread; unloading, file moves
 * inside the plugin directory and loading run on the main thread (Paper requires it).</p>
 */
public final class PluginUpdateManager {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final Supplier<RuntimeConfig> config;
    private final RuntimePaths paths;
    private final RuntimePlatform platform;
    private final Executor worker;
    private final AuditLog audit;
    private final PluginHost host;
    private final AtomicBoolean busy = new AtomicBoolean();
    private final Map<String, String> pending = new ConcurrentHashMap<>();
    private final Map<String, Instant> pendingExpiry = new ConcurrentHashMap<>();

    /**
     * A plugin jar and what Lemon thinks about it.
     *
     * @param jar           inspected jar
     * @param installed     installed plugin with the same name, if any
     * @param installedFile jar of the installed plugin in {@code plugins/}, if found
     * @param verdict       compatibility verdict
     */
    public record Candidate(PluginJar jar, Optional<InstalledPlugin> installed, Optional<Path> installedFile,
                            PluginCompatibility.Verdict verdict) {
    }

    /**
     * Outcome of update or rollback.
     *
     * @param kind      outcome
     * @param message   one line
     * @param details   further lines
     * @param candidate what was checked, or {@code null}
     */
    public record Result(Kind kind, String message, List<String> details, Candidate candidate) {
        public Result {
            details = List.copyOf(details);
        }

        /** Outcome kinds. */
        public enum Kind {
            /** Check passed; repeat with {@code confirm}. */
            CONFIRMATION_REQUIRED,
            /** Refused before anything was changed. */
            REJECTED,
            /** New version runs. */
            UPDATED,
            /** New version failed; the previous version runs again. */
            RESTORED,
            /** Failed and the previous version could not be restored live (restart needed). */
            FAILED
        }
    }

    /**
     * @param config   configuration
     * @param paths    file layout
     * @param platform platform (main thread executor, logger, versions)
     * @param worker   worker executor
     * @param audit    audit log
     * @param host     plugin host
     */
    public PluginUpdateManager(Supplier<RuntimeConfig> config, RuntimePaths paths, RuntimePlatform platform, Executor worker,
                               AuditLog audit, PluginHost host) {
        this.config = config;
        this.paths = paths;
        this.platform = platform;
        this.worker = worker;
        this.audit = audit;
        this.host = host;
    }

    /** @return whether live plugin updates are enabled */
    public boolean enabled() {
        return this.config.get().pluginUpdates().enabled();
    }

    /** @return installed plugins (main thread) */
    public List<InstalledPlugin> installed() {
        return this.host.installed();
    }

    /** @return the update folder */
    public Path updateDirectory() {
        return this.host.updateDirectory();
    }

    /** @return why plugins cannot be unloaded, or {@code null} */
    public String unloadProblem() {
        return this.host.unloadProblem();
    }

    // ------------------------------------------------------------------ check

    /** @return every jar in the update folder with its verdict */
    public CompletableFuture<List<Candidate>> scan() {
        return onMain(this.host::installed).thenCompose(all -> onWorker(() -> {
            List<Candidate> out = new ArrayList<>();
            for (Path jar : jars(this.host.updateDirectory())) {
                out.add(evaluate(jar, all));
            }
            return out;
        }));
    }

    /**
     * @param name plugin name (case-insensitive)
     * @return the candidate in the update folder for that plugin
     */
    public CompletableFuture<Candidate> check(String name) {
        return onMain(this.host::installed).thenCompose(all -> onWorker(() -> findCandidate(name, all)));
    }

    private Candidate findCandidate(String name, List<InstalledPlugin> all) throws IOException, PluginUpdateException {
        for (Path jar : jars(this.host.updateDirectory())) {
            Optional<PluginInfo> info = PluginJarInspector.readInfo(jar);
            if (info.isPresent() && info.get().name().equalsIgnoreCase(name)) {
                return evaluate(jar, all);
            }
        }
        boolean installed = all.stream().anyMatch(p -> p.name().equalsIgnoreCase(name));
        throw new PluginUpdateException("no jar for plugin '" + PathGuard.sanitize(name) + "' in "
                + this.paths.serverDirectory().relativize(this.host.updateDirectory().toAbsolutePath().normalize())
                + (installed ? " (the plugin is installed; put the new version into the update folder)" : ""));
    }

    private Candidate evaluate(Path jar, List<InstalledPlugin> all) throws IOException {
        PluginJar inspected = PluginJarInspector.inspect(jar, Runtime.version().feature(),
                this.paths.pluginTrustedHashes(this.config.get()));
        Optional<InstalledPlugin> installed = inspected.info() == null ? Optional.empty()
                : all.stream().filter(p -> p.name().equalsIgnoreCase(inspected.info().name())).findFirst();
        Optional<Path> installedFile = installed.isEmpty() ? Optional.empty() : findInstalledFile(installed.get().name());
        Optional<PluginJar> installedJar = installedFile.isEmpty() || installedFile.get().equals(jar) ? Optional.empty()
                : Optional.of(PluginJarInspector.inspect(installedFile.get(), Runtime.version().feature(), Optional.empty()));
        PluginCompatibility.Verdict verdict = PluginCompatibility.evaluate(inspected, installed, installedJar, all,
                new PluginCompatibility.Host(this.platform.minecraftVersion(), Runtime.version().feature(), this.host.unloadProblem()));
        if (installed.isPresent() && installedFile.isEmpty() && verdict.status() == PluginCompatibility.Status.LIVE) {
            verdict = new PluginCompatibility.Verdict(PluginCompatibility.Status.RESTART_REQUIRED,
                    List.of("the jar of the installed plugin was not found in plugins/"), verdict.warnings());
        }
        return new Candidate(inspected, installed, installedFile, verdict);
    }

    private Optional<Path> findInstalledFile(String name) throws IOException {
        for (Path jar : jars(this.host.pluginDirectory())) {
            Optional<PluginInfo> info = PluginJarInspector.readInfo(jar);
            if (info.isPresent() && info.get().name().equalsIgnoreCase(name)) {
                return Optional.of(jar);
            }
        }
        return Optional.empty();
    }

    private static List<Path> jars(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".jar") && PathGuard.isSafeFileName(p.getFileName().toString())
                    && Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).sorted().limit(500).toList();
        }
    }

    // ------------------------------------------------------------------ update

    /**
     * Installs or updates a plugin from the update folder.
     *
     * @param actor   who asked
     * @param name    plugin name
     * @param confirm second step
     * @return the outcome
     */
    public CompletableFuture<Result> update(String actor, String name, boolean confirm) {
        if (!enabled()) {
            return CompletableFuture.completedFuture(rejected("live plugin updates are disabled (plugin-updates.enabled: false)", null));
        }
        if (!this.busy.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(rejected("another plugin update is running", null));
        }
        String key = actor + "\u0000update\u0000" + name.toLowerCase(java.util.Locale.ROOT);
        CompletableFuture<Result> f = check(name).thenCompose(c -> {
            if (c.verdict().status() != PluginCompatibility.Status.LIVE) {
                return CompletableFuture.completedFuture(new Result(Result.Kind.REJECTED, verdictLine(c), c.verdict().reasons(), c));
            }
            if (!confirm) {
                this.pending.put(key, c.jar().sha256());
                this.pendingExpiry.put(key, Instant.now().plusSeconds(120));
                this.audit.record(actor, "plugin-check", c.jar().info().name(), true, "update planned, waiting for confirmation; sha256 "
                        + c.jar().sha256());
                return CompletableFuture.completedFuture(new Result(Result.Kind.CONFIRMATION_REQUIRED, verdictLine(c), List.of(), c));
            }
            String sha = this.pending.remove(key);
            Instant expiry = this.pendingExpiry.remove(key);
            if (sha == null || expiry == null || expiry.isBefore(Instant.now()) || !sha.equals(c.jar().sha256())) {
                return CompletableFuture.completedFuture(rejected("no update of " + name + " is waiting for confirmation by "
                        + actor + " (expired, never checked, or the jar changed); run '/fork live update " + name + "' first", c));
            }
            return onWorker(() -> backup(c)).thenCompose(backup -> onMain(() -> swap(actor, c, c.jar().file(), backup, false)));
        });
        return f.whenComplete((r, ex) -> this.busy.set(false));
    }

    /**
     * Restores the newest backup of a plugin (the version before the last update).
     *
     * @param actor   who asked
     * @param name    plugin name
     * @param confirm second step
     * @return the outcome
     */
    public CompletableFuture<Result> rollback(String actor, String name, boolean confirm) {
        if (!enabled()) {
            return CompletableFuture.completedFuture(rejected("live plugin updates are disabled (plugin-updates.enabled: false)", null));
        }
        if (!this.busy.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(rejected("another plugin update is running", null));
        }
        String key = actor + "\u0000rollback\u0000" + name.toLowerCase(java.util.Locale.ROOT);
        CompletableFuture<Result> f = onMain(this.host::installed).thenCompose(all -> onWorker(() -> {
            Optional<Path> jar = latestBackup(name);
            return jar.isEmpty() ? null : evaluate(jar.get(), all);
        })).thenCompose(c -> {
            if (c == null) {
                return CompletableFuture.completedFuture(rejected("no backup of plugin '" + PathGuard.sanitize(name)
                        + "' in lemon/plugin-backups/ (nothing to roll back to; failed updates are not rollback targets)", null));
            }
            if (c.verdict().status() != PluginCompatibility.Status.LIVE) {
                return CompletableFuture.completedFuture(new Result(Result.Kind.REJECTED, "rollback of " + name + ": " + verdictLine(c),
                        c.verdict().reasons(), c));
            }
            if (!confirm) {
                this.pending.put(key, c.jar().sha256());
                this.pendingExpiry.put(key, Instant.now().plusSeconds(120));
                return CompletableFuture.completedFuture(new Result(Result.Kind.CONFIRMATION_REQUIRED, "rollback of "
                        + c.jar().info().name() + " to " + c.jar().info().version() + " is ready", List.of(), c));
            }
            String sha = this.pending.remove(key);
            Instant expiry = this.pendingExpiry.remove(key);
            if (sha == null || expiry == null || expiry.isBefore(Instant.now()) || !sha.equals(c.jar().sha256())) {
                return CompletableFuture.completedFuture(rejected("no rollback of " + name + " is waiting for confirmation by "
                        + actor + "; run '/fork live rollback " + name + "' first", c));
            }
            return onWorker(() -> backup(c)).thenCompose(backup -> onMain(() -> swap(actor, c, c.jar().file(), backup, true)));
        });
        return f.whenComplete((r, ex) -> this.busy.set(false));
    }

    private Optional<Path> latestBackup(String name) throws IOException {
        Path root = this.paths.pluginBackups();
        if (!Files.isDirectory(root)) {
            return Optional.empty();
        }
        List<Path> dirs;
        try (Stream<Path> s = Files.list(root)) {
            dirs = s.filter(Files::isDirectory).filter(d -> d.getFileName().toString().equalsIgnoreCase(name)).toList();
        }
        for (Path dir : dirs) {
            List<Path> stamps;
            try (Stream<Path> s = Files.list(dir)) {
                stamps = s.filter(Files::isDirectory).sorted(Comparator.reverseOrder()).toList();
            }
            for (Path stamp : stamps) {
                if (Files.exists(stamp.resolve(FAILED_MARKER))) {
                    continue;
                }
                for (Path jar : jars(stamp)) {
                    if (!jar.getFileName().toString().startsWith("failed-")) {
                        return Optional.of(jar);
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** Worker: copies the installed jar into a fresh backup directory. */
    private Path backup(Candidate c) throws IOException {
        String name = c.jar().info().name();
        if (!PathGuard.isSafeFileName(name)) {
            throw new IOException("plugin name '" + PathGuard.sanitize(name) + "' cannot be used as a directory name");
        }
        Path dir = this.paths.pluginBackups().resolve(name).resolve(STAMP.format(Instant.now()));
        Files.createDirectories(dir);
        if (c.installedFile().isPresent()) {
            Path old = c.installedFile().get();
            Files.copy(old, dir.resolve(old.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        return dir;
    }

    /** Main thread: replaces the running plugin with the candidate; restores the old one on failure. */
    private Result swap(String actor, Candidate c, Path source, Path backupDir, boolean rollback) {
        String name = c.jar().info().name();
        String action = rollback ? "rollback" : c.installed().isPresent() ? "update" : "install";
        String from = c.installed().map(p -> p.info().version()).orElse("-");
        String to = c.jar().info().version();
        List<String> details = new ArrayList<>();
        Path pluginDir = this.host.pluginDirectory();
        Path target = pluginDir.resolve(source.getFileName());
        boolean placed = false;
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && !c.installedFile().map(target::equals).orElse(false)) {
            return rejected("plugins/" + target.getFileName() + " already exists and belongs to another plugin; rename the new jar", c);
        }
        try {
            if (c.installed().isPresent()) {
                this.host.unload(c.installed().get().name());
                details.add("unloaded " + name + " " + from + " (commands, permissions and plugin-manager entry removed)");
                Files.delete(c.installedFile().orElseThrow());
            }
            if (rollback) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                placed = true;
            } else {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                placed = true;
                Path sidecar = source.resolveSibling(source.getFileName() + ".sha256");
                if (Files.isRegularFile(sidecar, LinkOption.NOFOLLOW_LINKS)) {
                    Files.move(sidecar, backupDir.resolve("new-" + sidecar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException | RuntimeException e) {
            String message = action + " of " + name + " stopped before the new version was loaded: " + e.getMessage();
            return recover(actor, c, action, message, placed, target, backupDir, details);
        }
        PluginHost.LoadResult loaded = this.host.load(target);
        if (loaded.enabled() && name.equalsIgnoreCase(loaded.name())) {
            this.host.commandsChanged();
            details.add("loaded and enabled " + loaded.name() + " " + to + " from plugins/" + target.getFileName());
            details.add("backup of the previous jar: " + this.paths.serverDirectory().relativize(backupDir));
            details.add("check the plugin now; Lemon verifies loading and enabling, not the plugin's behaviour");
            String summary = name + " " + from + " -> " + to;
            this.audit.record(actor, "plugin-" + action, name, true, summary + "; sha256 " + c.jar().sha256());
            LiveHistory.INSTANCE.record(LiveHistory.Kind.PLUGIN, name, action, actor, true, summary);
            this.platform.logger().warning("Plugin " + action + " by " + actor + ": " + summary);
            return new Result(Result.Kind.UPDATED, action + " of " + name + " done: " + from + " -> " + to, details, c);
        }
        String reason = loaded.error() != null ? loaded.error() : loaded.name() == null ? "the jar did not load"
                : "the plugin did not enable (see the server log for its exception)";
        if (loaded.name() != null) {
            try {
                this.host.unload(loaded.name());
            } catch (RuntimeException e) {
                details.add("could not unload the failed version: " + e.getMessage());
            }
        }
        return recover(actor, c, action, action + " of " + name + " to " + to + " failed: " + reason, true, target, backupDir, details);
    }

    /** Marker in a backup directory whose update failed; rollback ignores such directories. */
    static final String FAILED_MARKER = "UPDATE-FAILED.txt";

    /** Main thread: puts the previous jar back and loads it. */
    private Result recover(String actor, Candidate c, String action, String message, boolean newJarPlaced, Path newJar,
                           Path backupDir, List<String> details) {
        String name = c.jar().info().name();
        try {
            Files.writeString(backupDir.resolve(FAILED_MARKER), message + System.lineSeparator());
        } catch (IOException e) {
            details.add("could not mark the backup directory as failed: " + e.getMessage());
        }
        try {
            if (newJarPlaced && Files.exists(newJar)) {
                Files.move(newJar, backupDir.resolve("failed-" + newJar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                details.add("moved the failed jar to " + this.paths.serverDirectory().relativize(backupDir) + "/failed-"
                        + newJar.getFileName());
            }
        } catch (IOException e) {
            details.add("could not move the failed jar out of plugins/: " + e.getMessage());
        }
        if (c.installed().isEmpty() || c.installedFile().isEmpty()) {
            this.audit.record(actor, "plugin-" + action, name, false, message);
            LiveHistory.INSTANCE.record(LiveHistory.Kind.PLUGIN, name, action, actor, false, message);
            return new Result(Result.Kind.FAILED, message, details, c);
        }
        Path oldFile = c.installedFile().get();
        Path saved = backupDir.resolve(oldFile.getFileName());
        try {
            if (!Files.exists(oldFile)) {
                Files.copy(saved, oldFile);
            }
        } catch (IOException e) {
            String m = message + "; the previous jar could not be restored (" + e.getMessage() + "): copy "
                    + this.paths.serverDirectory().relativize(saved) + " back into plugins/ and restart";
            this.audit.record(actor, "plugin-" + action, name, false, m);
            LiveHistory.INSTANCE.record(LiveHistory.Kind.PLUGIN, name, action, actor, false, m);
            return new Result(Result.Kind.FAILED, m, details, c);
        }
        Optional<InstalledPlugin> still = this.host.installed().stream().filter(p -> p.name().equalsIgnoreCase(name)).findFirst();
        PluginHost.LoadResult restored = still.isPresent()
                ? new PluginHost.LoadResult(name, still.get().enabled(), still.get().enabled() ? null : "the old version is still"
                + " registered but disabled")
                : this.host.load(oldFile);
        this.host.commandsChanged();
        if (restored.enabled()) {
            String m = message + "; the previous version " + c.installed().get().info().version() + " was restored and is enabled";
            this.audit.record(actor, "plugin-" + action, name, false, m);
            LiveHistory.INSTANCE.record(LiveHistory.Kind.PLUGIN, name, action, actor, false, m);
            this.platform.logger().warning("Plugin " + action + " FAILED: " + m);
            return new Result(Result.Kind.RESTORED, m, details, c);
        }
        String m = message + "; restoring the previous version also failed (" + (restored.error() == null ? "not enabled"
                : restored.error()) + "). The previous jar is back in plugins/; restart the server";
        this.audit.record(actor, "plugin-" + action, name, false, m);
        LiveHistory.INSTANCE.record(LiveHistory.Kind.PLUGIN, name, action, actor, false, m);
        this.platform.logger().severe("Plugin " + action + " FAILED: " + m);
        return new Result(Result.Kind.FAILED, m, details, c);
    }

    // ------------------------------------------------------------------ helpers

    /** @param c candidate @return one-line verdict */
    public static String verdictLine(Candidate c) {
        PluginInfo i = c.jar().info();
        String what = i == null ? c.jar().file().getFileName().toString()
                : i.name() + " " + c.installed().map(p -> p.info().version() + " -> ").orElse("(new) ") + i.version();
        return what + ": " + switch (c.verdict().status()) {
            case LIVE -> "can be " + (c.installed().isPresent() ? "updated" : "installed") + " live";
            case RESTART_REQUIRED -> "needs a server restart"
                    + (c.installed().isPresent() ? " (Paper installs the jar from the update folder on the next start)" : "");
            case INCOMPATIBLE -> "incompatible";
        };
    }

    private static Result rejected(String message, Candidate c) {
        return new Result(Result.Kind.REJECTED, message, List.of(), c);
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
                    f.completeExceptionally(t);
                    if (t instanceof VirtualMachineError vme && !(t instanceof StackOverflowError)) {
                        throw vme;
                    }
                }
            });
        } catch (RuntimeException e) {
            f.completeExceptionally(e);
        }
        return f.orTimeout(this.config.get().operationTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }

    private <T> CompletableFuture<T> onMain(Work<T> work) {
        CompletableFuture<T> f = new CompletableFuture<>();
        try {
            this.platform.mainThreadExecutor().execute(() -> {
                try {
                    f.complete(work.run());
                } catch (Throwable t) {
                    f.completeExceptionally(t);
                    if (t instanceof VirtualMachineError vme && !(t instanceof StackOverflowError)) {
                        throw vme;
                    }
                }
            });
        } catch (RuntimeException e) {
            f.completeExceptionally(e);
        }
        return f;
    }

    /** A plugin update request was refused. */
    public static final class PluginUpdateException extends Exception {
        /** @param message reason */
        public PluginUpdateException(String message) {
            super(message);
        }
    }
}
