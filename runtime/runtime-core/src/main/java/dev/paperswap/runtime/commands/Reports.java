package dev.paperswap.runtime.commands;

import dev.paperswap.runtime.PaperSwapRuntime;
import dev.paperswap.runtime.api.ModuleDependency;
import dev.paperswap.runtime.api.ModuleDescriptor;
import dev.paperswap.runtime.api.ModuleState;
import dev.paperswap.runtime.api.PermissionDeclaration;
import dev.paperswap.runtime.config.RuntimeConfig;
import dev.paperswap.runtime.lifecycle.ModuleSummary;
import dev.paperswap.runtime.lifecycle.OperationResult;
import dev.paperswap.runtime.loader.ModuleArtifact;
import dev.paperswap.runtime.loader.ScanReport;
import dev.paperswap.runtime.metrics.LeakDetector;
import dev.paperswap.runtime.metrics.ModuleDiagnostics;
import dev.paperswap.runtime.metrics.RuntimeHealth;
import dev.paperswap.runtime.security.PathGuard;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Renders the read-only reports of {@code /fork}. All values are emitted as plain text. */
final class Reports {

    private final PaperSwapRuntime runtime;

    Reports(PaperSwapRuntime runtime) {
        this.runtime = runtime;
    }

    void version(CommandActor a) {
        var info = this.runtime.info();
        a.send(MessageLevel.HEADER, info.serverBrand() + " " + info.serverVersion());
        a.send(MessageLevel.INFO, "Minecraft " + info.minecraftVersion() + " | runtime " + info.runtimeVersion()
                + " | module API " + info.apiVersion());
    }

    void info(CommandActor a) {
        version(a);
        RuntimeConfig c = this.runtime.config();
        a.send(MessageLevel.INFO, "Hot deployment: enabled | runtime deploy: " + (c.allowRuntimeDeploy() ? "allowed" : "disabled")
                + " | integrity sidecars: " + (c.verifyIntegrity() ? "required" : "not required")
                + " | trusted hashes: " + (c.trustedHashesFile().isEmpty() ? "off" : c.trustedHashesFile()));
        a.send(MessageLevel.INFO, "Module directory: " + c.moduleDirectory() + " | max modules: " + c.maxModuleCount()
                + " | API versions: " + c.allowedApiVersions() + " | server internals visible: " + c.allowServerInternals());
        a.send(MessageLevel.DETAIL, "Configuration: " + this.runtime.paths().configFile());
        a.send(MessageLevel.DETAIL, "Audit log: " + this.runtime.paths().auditLog());
    }

    void status(CommandActor a) {
        a.send(MessageLevel.HEADER, "PaperSwap runtime status (uptime " + format(this.runtime.uptime()) + ")");
        for (String note : this.runtime.paths().legacyNotes()) {
            a.send(MessageLevel.WARNING, "Old name (Lemon): " + note);
        }
        List<ModuleSummary> modules = this.runtime.modules().modules();
        Map<ModuleState, Integer> counts = new EnumMap<>(ModuleState.class);
        modules.forEach(m -> counts.merge(m.state(), 1, Integer::sum));
        a.send(MessageLevel.INFO, "Loaded instances: " + modules.size() + (counts.isEmpty() ? "" : " " + counts));
        Map<String, String> ops = this.runtime.modules().operationsInProgress();
        if (!ops.isEmpty()) {
            a.send(MessageLevel.INFO, "Operations in progress: " + ops);
        }
        int retiring = this.runtime.modules().retiringCount();
        if (retiring > 0) {
            a.send(MessageLevel.WARNING, retiring + " old module instance(s) wait for asynchronous work before unloading");
        }
        RuntimeHealth health = this.runtime.health();
        if (health.restartRecommended()) {
            a.send(MessageLevel.WARNING, "A controlled server restart is recommended:");
            for (RuntimeHealth.Issue i : health.issues()) {
                a.send(MessageLevel.DETAIL, i.at() + " " + (i.moduleId() == null ? "" : i.moduleId() + ": ") + i.reason());
            }
        } else {
            a.send(MessageLevel.SUCCESS, "No unresolved problems.");
        }
        Map<String, OperationResult> failures = this.runtime.modules().lastFailures();
        failures.forEach((id, r) -> a.send(MessageLevel.WARNING, "Last " + r.action() + " of " + id + " failed: " + r.message()));
        Map<String, String> metrics = this.runtime.metrics().summary();
        if (!metrics.isEmpty()) {
            a.send(MessageLevel.DETAIL, "Operations: " + metrics);
        }
    }

    void list(CommandActor a, ScanReport scan) {
        List<ModuleSummary> loaded = this.runtime.modules().modules();
        a.send(MessageLevel.HEADER, "Modules (" + loaded.size() + " loaded, " + scan.artifacts().size() + " found in "
                + this.runtime.config().moduleDirectory() + ")");
        Set<String> loadedIds = new HashSet<>();
        for (ModuleSummary m : loaded) {
            loadedIds.add(m.id());
            MessageLevel level = switch (m.state()) {
                case ENABLED -> MessageLevel.SUCCESS;
                case FAILED -> MessageLevel.ERROR;
                default -> MessageLevel.INFO;
            };
            a.send(level, m.id() + " " + m.version() + " - " + m.state() + (m.lastError() == null ? "" : " (" + m.lastError() + ")"));
        }
        for (ModuleArtifact art : scan.artifacts()) {
            if (!loadedIds.contains(art.id())) {
                a.send(MessageLevel.DETAIL, art.id() + " " + art.descriptor().version() + " - not loaded ("
                        + art.file().getFileName() + ")");
            }
        }
        for (ScanReport.Problem p : scan.problems()) {
            a.send(MessageLevel.WARNING, "rejected " + p.file() + ": " + p.message());
        }
    }

    void moduleInfo(CommandActor a, String id, ScanReport scan) {
        Optional<ModuleDescriptor> loaded = this.runtime.modules().descriptor(id);
        Optional<ModuleArtifact> found = scan.find(id);
        ModuleDescriptor d = loaded.orElse(found.map(ModuleArtifact::descriptor).orElse(null));
        if (d == null) {
            a.send(MessageLevel.ERROR, "Unknown module '" + id + "'.");
            scan.problemsFor(id).forEach(p -> a.send(MessageLevel.DETAIL, p.file() + ": " + p.message()));
            return;
        }
        a.send(MessageLevel.HEADER, d.name() + " (" + d.coordinates() + ")" + (loaded.isPresent() ? " [loaded]" : " [not loaded]"));
        if (!d.description().isEmpty()) {
            a.send(MessageLevel.INFO, d.description());
        }
        a.send(MessageLevel.DETAIL, "Authors: " + (d.authors().isEmpty() ? "-" : String.join(", ", d.authors())));
        a.send(MessageLevel.DETAIL, "Main class: " + d.mainClass() + " | API " + d.apiVersion() + " | load order " + d.loadOrder());
        a.send(MessageLevel.DETAIL, "Minecraft versions: " + d.minecraftVersions());
        a.send(MessageLevel.DETAIL, "Capabilities: " + d.capabilities().stream().map(c -> c.key()).sorted().toList());
        a.send(MessageLevel.DETAIL, "Dependencies: " + (d.dependencies().isEmpty() ? "-" : d.dependencies().stream()
                .map(Reports::dependency).collect(Collectors.joining(", "))));
        if (!d.pluginDependencies().isEmpty()) {
            a.send(MessageLevel.DETAIL, "Plugin dependencies: " + d.pluginDependencies());
        }
        for (PermissionDeclaration p : d.permissions()) {
            a.send(MessageLevel.DETAIL, "Permission " + p.node() + " (default " + p.defaultValue() + ") " + p.description());
        }
        found.ifPresent(f -> a.send(MessageLevel.DETAIL, "File: " + f.file().getFileName() + " (" + f.size() + " bytes)"));
    }

    private static String dependency(ModuleDependency d) {
        return d.id() + " " + d.versionRange() + (d.optional() ? " (optional)" : "");
    }

    void diagnostics(CommandActor a, String id) {
        Optional<ModuleDiagnostics> opt = this.runtime.modules().diagnostics(id);
        if (opt.isEmpty()) {
            a.send(MessageLevel.ERROR, "No diagnostics for '" + id + "' (not loaded and never unloaded).");
            return;
        }
        ModuleDiagnostics d = opt.get();
        a.send(MessageLevel.HEADER, "Diagnostics " + d.id() + " " + d.version() + " - " + d.state());
        a.send(MessageLevel.DETAIL, "sha256: " + d.sha256());
        a.send(MessageLevel.DETAIL, "Timings: load " + ms(d.loadMillis()) + ", enable " + ms(d.enableMillis()) + ", disable "
                + ms(d.disableMillis()) + (d.enabledSince() == null ? "" : ", enabled since " + d.enabledSince()));
        a.send(MessageLevel.DETAIL, "Tracked resources: " + (d.resources().isEmpty() ? "none" : d.resources()));
        if (!d.services().isEmpty()) {
            a.send(MessageLevel.DETAIL, "Provided services: " + d.services());
        }
        a.send(MessageLevel.DETAIL, "Callbacks: " + d.inFlightCallbacks() + " in flight, " + d.callbackFailures() + " failed, "
                + d.droppedCallbacks() + " dropped after disable");
        d.runningAsyncWork().forEach(w -> a.send(MessageLevel.WARNING, "Still running: " + w));
        if (d.lastError() != null) {
            a.send(MessageLevel.ERROR, "Last error: " + PathGuard.sanitize(d.lastError()));
        }
        d.cleanupProblems().forEach(p -> a.send(MessageLevel.WARNING, "Cleanup: " + p));
        a.send(MessageLevel.DETAIL, "Dependencies: " + d.dependencies() + " | dependents: " + d.dependents());
        d.stateHistory().forEach(h -> a.send(MessageLevel.DETAIL, "  " + h));
        for (LeakDetector.Entry e : d.previousInstances()) {
            a.send(e.collected() ? MessageLevel.DETAIL : MessageLevel.WARNING, "Unloaded " + e.version() + " at " + e.unloadedAt() + ": "
                    + (e.collected() ? "class loader collected" : "class loader still reachable (not yet collected or leaked)"
                    + (e.liveThreads().isEmpty() ? "" : ", threads: " + e.liveThreads())));
        }
    }

    private static String ms(long v) {
        return v < 0 ? "-" : v + " ms";
    }

    private static String format(Duration d) {
        long s = d.toSeconds();
        return String.format("%dh %02dm %02ds", s / 3600, (s % 3600) / 60, s % 60);
    }
}
