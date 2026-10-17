package dev.paperswap.runtime.commands;

import dev.paperswap.runtime.PaperSwapRuntime;
import dev.paperswap.runtime.live.LiveHistory;
import dev.paperswap.runtime.livepatch.Capability;
import dev.paperswap.runtime.livepatch.CapabilityProbe;
import dev.paperswap.runtime.livepatch.JvmCapabilities;
import dev.paperswap.runtime.livepatch.LivePatchRegistry;
import dev.paperswap.runtime.livepatch.LivePatchService;
import dev.paperswap.runtime.metrics.LeakDetector;
import dev.paperswap.runtime.plugins.InstalledPlugin;
import dev.paperswap.runtime.plugins.PluginInfo;
import dev.paperswap.runtime.plugins.PluginResources;
import dev.paperswap.runtime.plugins.PluginUpdateManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Read-only reports of {@code /fork live}: {@code inspect} for plugins and core patches,
 * and {@code diagnostics}. Runs on the main thread (it reads the server's plugin manager).
 */
final class LiveInspect {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final PaperSwapRuntime runtime;

    LiveInspect(PaperSwapRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Whether an installed plugin can be updated live at all (each new jar is checked again).
     *
     * @return {@code null} if live updates are possible, else the reason for "restart only"
     */
    static String restartOnlyReason(InstalledPlugin p, List<InstalledPlugin> all, PluginUpdateManager m) {
        List<String> dependents = dependents(p, all);
        if (p.info().kind() == PluginInfo.Kind.PAPER) {
            return "Paper plugin (paper-plugin.yml)";
        }
        if (!dependents.isEmpty()) {
            return "used by " + dependents;
        }
        if (m.unloadProblem() != null) {
            return m.unloadProblem();
        }
        return null;
    }

    static List<String> dependents(InstalledPlugin p, List<InstalledPlugin> all) {
        return all.stream().filter(o -> o != p && o.enabled()
                && (o.info().depend().stream().anyMatch(p.info()::answersTo) || o.info().softDepend().stream().anyMatch(p.info()::answersTo)))
                .map(InstalledPlugin::name).toList();
    }

    void plugin(CommandActor a, PluginUpdateManager m, String name) {
        List<InstalledPlugin> all = m.installed();
        Optional<InstalledPlugin> found = all.stream().filter(p -> p.name().equalsIgnoreCase(name)).findFirst();
        List<LeakDetector.Entry> previous = m.previousVersions(name);
        if (found.isEmpty() && previous.isEmpty()) {
            a.send(MessageLevel.ERROR, "Plugin " + name + " is not installed. A new jar in the update folder: /fork live check " + name);
            return;
        }
        if (found.isPresent()) {
            InstalledPlugin p = found.get();
            PluginInfo i = p.info();
            a.send(MessageLevel.HEADER, "Plugin " + i.name() + " " + i.version());
            a.send(p.enabled() ? MessageLevel.INFO : MessageLevel.WARNING, (p.enabled() ? "enabled" : "DISABLED") + ", "
                    + i.kind().name().toLowerCase(Locale.ROOT) + " plugin, main " + i.main()
                    + (i.apiVersion().isEmpty() ? "" : ", api-version " + i.apiVersion()));
            a.send(MessageLevel.DETAIL, "depends on " + i.depend() + ", soft-depends on " + i.softDepend() + ", used by "
                    + dependents(p, all));
            Optional<PluginResources> r = m.resources(i.name());
            a.send(MessageLevel.INFO, "Registered now: " + r.map(PluginResources::describe).orElse("unknown"));
            a.send(MessageLevel.DETAIL, "(listed through the server API: listeners, Bukkit scheduler tasks, commands, services,"
                    + " messaging channels; threads the plugin starts itself and registrations in other plugins' APIs are not listed)");
            String restart = restartOnlyReason(p, all, m);
            a.send(restart == null ? MessageLevel.SUCCESS : MessageLevel.WARNING, restart == null
                    ? "Live update: possible - every new jar is checked again (/fork live check " + i.name() + ")"
                    : "Live update: restart only (" + restart + ")");
        } else {
            a.send(MessageLevel.HEADER, "Plugin " + name + " (not installed now)");
        }
        for (LeakDetector.Entry e : previous) {
            a.send(e.collected() ? MessageLevel.DETAIL : MessageLevel.WARNING, "Unloaded version " + e.version() + " ("
                    + TIME.format(e.unloadedAt()) + " UTC): " + (e.collected() ? "class loader collected"
                    : "class loader still in memory" + (e.liveThreads().isEmpty() ? " (not collected yet, or referenced from elsewhere)"
                    : ", threads still running: " + e.liveThreads())));
        }
        if (previous.stream().anyMatch(e -> !e.collected())) {
            a.send(MessageLevel.DETAIL, "PaperSwap never forces a garbage collection; right after an update 'still in memory' is normal."
                    + " If it stays after the next full GC and threads are listed, the plugin leaks; restart the server when convenient.");
        }
    }

    void patch(CommandActor a, String id) {
        Optional<LivePatchRegistry.PatchRecord> record = LivePatchRegistry.INSTANCE.latest(id);
        if (record.isEmpty()) {
            return;
        }
        LivePatchRegistry.PatchRecord r = record.get();
        a.send(MessageLevel.HEADER, "Core patch " + r.id() + " in this server process");
        a.send(r.status() == LivePatchRegistry.Status.ACTIVE ? MessageLevel.SUCCESS : r.status() == LivePatchRegistry.Status.FAILED
                ? MessageLevel.ERROR : MessageLevel.INFO, r.status().name().toLowerCase(Locale.ROOT).replace('_', ' ') + " - " + r.detail());
        a.send(MessageLevel.DETAIL, "applied by " + r.actor() + " at " + TIME.format(r.appliedAt()) + " UTC ("
                + r.mechanism().id() + "), archive " + shortSha(r.archiveSha256()));
        for (LivePatchRegistry.ClassRecord c : r.classes()) {
            a.send(MessageLevel.DETAIL, c.name() + ": " + shortSha(c.previous().sha256()) + " -> "
                    + shortSha(c.installed().sha256()));
        }
        if (!r.definedClasses().isEmpty()) {
            a.send(MessageLevel.DETAIL, "new classes (stay loaded until a restart): " + r.definedClasses());
        }
        if (!r.backupDirectory().isEmpty()) {
            a.send(MessageLevel.DETAIL, "backup of the replaced class files: " + r.backupDirectory());
        }
        if (r.status() == LivePatchRegistry.Status.ACTIVE) {
            a.send(MessageLevel.INFO, "Read back the running bytecode: /fork live patch verify " + r.id()
                    + " | undo: /fork live rollback " + r.id());
        }
        a.send(MessageLevel.DETAIL, "Patches end with the process: after a restart the server runs its jar again.");
    }

    void diagnostics(CommandActor a) {
        a.send(MessageLevel.HEADER, "PaperSwap live update diagnostics");
        LivePatchService.StatusReport s = this.runtime.livePatches().status();
        a.send(MessageLevel.INFO, "JVM: " + s.vm());
        a.send(s.instrumentation() ? MessageLevel.INFO : MessageLevel.WARNING, "Agent: " + s.agentDetail());
        a.send(MessageLevel.DETAIL, "redefine: " + s.redefineSupported() + ", read back: " + s.retransformSupported()
                + ", AllowEnhancedClassRedefinition: " + s.enhancedFlag() + ", build id: " + s.buildId());
        Map<Capability, CapabilityProbe.Result> probed = JvmCapabilities.probed();
        a.send(MessageLevel.DETAIL, probed.isEmpty() ? "Capabilities: not probed yet (/fork live capabilities)"
                : "Capabilities: " + probed.entrySet().stream().filter(e -> e.getValue().supported()).map(e -> e.getKey().name()
                .toLowerCase(Locale.ROOT)).toList() + " supported");
        List<LivePatchRegistry.PatchRecord> records = LivePatchRegistry.INSTANCE.records();
        a.send(MessageLevel.INFO, "Core patches: " + s.active() + " active, " + records.stream()
                .filter(r -> r.status() == LivePatchRegistry.Status.FAILED).count() + " failed attempts in this process");
        records.stream().filter(r -> r.status() == LivePatchRegistry.Status.ACTIVE)
                .forEach(r -> a.send(MessageLevel.DETAIL, "  " + r.id() + ": " + r.detail()));

        Optional<PluginUpdateManager> plugins = this.runtime.pluginUpdates();
        if (plugins.isPresent()) {
            PluginUpdateManager m = plugins.get();
            a.send(MessageLevel.INFO, "Plugins: live updates " + (m.enabled() ? "allowed" : "off")
                    + (m.unloadProblem() == null ? "" : "; unloading not possible: " + m.unloadProblem()));
            for (LeakDetector.Entry e : m.previousVersions(null)) {
                if (!e.collected()) {
                    a.send(MessageLevel.WARNING, "  unloaded plugin " + e.moduleId() + " " + e.version() + " still in memory"
                            + (e.liveThreads().isEmpty() ? "" : ", threads: " + e.liveThreads()));
                }
            }
            long staged = count(this.runtime.paths().pluginStaging());
            if (staged > 0) {
                a.send(MessageLevel.WARNING, "  " + staged + " file(s) left in " + this.runtime.paths().pluginStaging()
                        + " (an update was interrupted; safe to delete while no update runs)");
            }
        }

        a.send(MessageLevel.INFO, "Modules: " + this.runtime.modules().modules().size() + " loaded, operations in progress: "
                + this.runtime.modules().operationsInProgress());
        for (LeakDetector.Entry e : this.runtime.modules().unloadedInstances()) {
            if (!e.collected()) {
                a.send(MessageLevel.WARNING, "  unloaded module " + e.moduleId() + " " + e.version() + " still in memory"
                        + (e.liveThreads().isEmpty() ? "" : ", threads: " + e.liveThreads()));
            }
        }
        this.runtime.health().issues().forEach(i -> a.send(MessageLevel.WARNING, "  restart recommended (" + i.moduleId() + "): "
                + i.reason()));
        a.send(MessageLevel.INFO, "Heap: " + s.heapUsedMiB() + " / " + s.heapMaxMiB() + " MiB used");

        List<LiveHistory.Entry> failures = LiveHistory.INSTANCE.latest(50).stream().filter(e -> !e.success()).limit(5).toList();
        if (!failures.isEmpty()) {
            a.send(MessageLevel.INFO, "Recent failures:");
            failures.forEach(e -> a.send(MessageLevel.WARNING, "  " + TIME.format(e.time()) + " " + e.kind().name().toLowerCase(Locale.ROOT)
                    + " " + e.name() + " " + e.action() + ": " + e.summary()));
        }
        a.send(MessageLevel.DETAIL, "Full record: paperswap/logs/audit.log; per target: /fork live inspect <name>");
    }

    private static String shortSha(String sha) {
        return sha == null || sha.length() < 12 ? String.valueOf(sha) : sha.substring(0, 12);
    }

    private static long count(Path dir) {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.count();
        } catch (IOException e) {
            return 0;
        }
    }
}
