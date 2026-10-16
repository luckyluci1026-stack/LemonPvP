package dev.lemonpvp.runtime.commands;

import dev.lemonpvp.runtime.LemonRuntime;
import dev.lemonpvp.runtime.descriptor.ModuleIds;
import dev.lemonpvp.runtime.live.LiveHistory;
import dev.lemonpvp.runtime.lifecycle.ModuleSummary;
import dev.lemonpvp.runtime.lifecycle.OperationResult;
import dev.lemonpvp.runtime.livepatch.AppliedBytecodeCheck;
import dev.lemonpvp.runtime.livepatch.Capability;
import dev.lemonpvp.runtime.livepatch.CapabilityProbe;
import dev.lemonpvp.runtime.livepatch.ClassChange;
import dev.lemonpvp.runtime.livepatch.JvmCapabilities;
import dev.lemonpvp.runtime.livepatch.LivePatchRegistry;
import dev.lemonpvp.runtime.livepatch.LivePatchService;
import dev.lemonpvp.runtime.livepatch.PatchIds;
import dev.lemonpvp.runtime.livepatch.PatchPlan;
import dev.lemonpvp.runtime.loader.ModuleArtifact;
import dev.lemonpvp.runtime.loader.ScanReport;
import dev.lemonpvp.runtime.plugins.InstalledPlugin;
import dev.lemonpvp.runtime.plugins.PluginCompatibility;
import dev.lemonpvp.runtime.plugins.PluginInfo;
import dev.lemonpvp.runtime.plugins.PluginUpdateManager;
import dev.lemonpvp.runtime.security.PathGuard;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/**
 * {@code /fork live ...}: one entry point for everything that changes a running server
 * without a restart. It distinguishes three kinds of targets and never mixes them up:
 *
 * <ul>
 *     <li><b>module</b> - Lemon runtime modules (own class loader, swapped with rollback);</li>
 *     <li><b>plugin</b> - normal Paper/Bukkit plugins (unload + load, only when safe);</li>
 *     <li><b>patch</b>  - core live patches (JVM class redefinition of server classes).</li>
 * </ul>
 *
 * <pre>
 * /fork live status | scan | plugins | modules | history
 * /fork live check &lt;name&gt;
 * /fork live update &lt;name&gt; [confirm]
 * /fork live rollback &lt;name&gt; [confirm]
 * /fork live patch status | probe | verify &lt;id&gt;
 * </pre>
 *
 * <p>A name may carry a prefix ({@code plugin:}, {@code module:}, {@code patch:}); without
 * one, Lemon looks it up in all three areas and asks for a prefix if it is ambiguous.</p>
 */
final class LiveCommands {

    static final List<String> SUBCOMMANDS = List.of("status", "scan", "plugins", "modules", "check", "update", "rollback",
            "history", "patch");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final LemonRuntime runtime;
    private final Reports reports;
    private final Map<String, PendingModule> moduleConfirmations = new ConcurrentHashMap<>();

    /** A validated module jar waiting for {@code confirm}: only exactly this jar is activated. */
    private record PendingModule(Instant until, String sha256) {
    }

    /** Kind of target. */
    enum Kind {
        MODULE, PLUGIN, PATCH
    }

    private record Target(Kind kind, String name) {
    }

    LiveCommands(LemonRuntime runtime, Reports reports) {
        this.runtime = runtime;
        this.reports = reports;
    }

    void execute(CommandActor actor, String[] args) {
        if (args.length < 2) {
            usage(actor);
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        boolean changes = sub.equals("update") || sub.equals("rollback")
                || (sub.equals("patch") && args.length >= 3 && (args[2].equalsIgnoreCase("probe") || args[2].equalsIgnoreCase("verify")));
        String node = changes ? RuntimePermissions.LIVE_UPDATE : RuntimePermissions.LIVE_VIEW;
        if (!SUBCOMMANDS.contains(sub)) {
            usage(actor);
            return;
        }
        if (!actor.hasPermission(node)) {
            actor.send(MessageLevel.ERROR, "You do not have permission (" + node + ").");
            return;
        }
        switch (sub) {
            case "status" -> status(actor);
            case "scan" -> scan(actor);
            case "plugins" -> plugins(actor);
            case "modules" -> reply(actor, this.runtime.modules().scan(), scan -> this.reports.list(actor, scan));
            case "history" -> history(actor);
            case "patch" -> patch(actor, args);
            case "check", "update", "rollback" -> {
                boolean confirm = args.length == 4 && args[3].equalsIgnoreCase("confirm");
                if (args.length != 3 && !(args.length == 4 && confirm && !sub.equals("check"))) {
                    actor.send(MessageLevel.ERROR, "Usage: /fork live " + sub + " <name>" + (sub.equals("check") ? "" : " [confirm]"));
                    return;
                }
                resolve(actor, args[2], target -> {
                    switch (sub) {
                        case "check" -> check(actor, target);
                        case "update" -> update(actor, target, confirm);
                        default -> rollback(actor, target, confirm);
                    }
                });
            }
            default -> usage(actor);
        }
    }

    // ------------------------------------------------------------------ overview

    private void status(CommandActor a) {
        a.send(MessageLevel.HEADER, "Lemon live updates");
        List<ModuleSummary> modules = this.runtime.modules().modules();
        long enabledModules = modules.stream().filter(m -> m.state() == dev.lemonpvp.runtime.api.ModuleState.ENABLED).count();
        a.send(MessageLevel.INFO, "Modules: " + modules.size() + " loaded, " + enabledModules + " enabled; live changes "
                + (this.runtime.config().allowRuntimeDeploy() ? "allowed" : "OFF (allow-runtime-deploy: false)"));
        Optional<PluginUpdateManager> plugins = this.runtime.pluginUpdates();
        if (plugins.isPresent()) {
            List<InstalledPlugin> installed = plugins.get().installed();
            long enabled = installed.stream().filter(InstalledPlugin::enabled).count();
            a.send(MessageLevel.INFO, "Plugins: " + installed.size() + " installed, " + enabled + " enabled; live updates "
                    + (plugins.get().enabled() ? "allowed" : "OFF (plugin-updates.enabled: false)")
                    + (plugins.get().unloadProblem() == null ? "" : "; unloading not possible: " + plugins.get().unloadProblem()));
        } else {
            a.send(MessageLevel.INFO, "Plugins: live updates are not supported on this platform");
        }
        LivePatchService.StatusReport p = this.runtime.livePatches().status();
        a.send(MessageLevel.INFO, "Core patches: " + (p.enabled() ? "allowed" : "OFF (live-patching.enabled: false)") + ", profile "
                + p.profile().yamlName() + ", " + (p.instrumentation() ? "agent loaded" : "agent NOT loaded") + ", " + p.active()
                + " active");
        List<LiveHistory.Entry> last = LiveHistory.INSTANCE.latest(3);
        if (!last.isEmpty()) {
            a.send(MessageLevel.INFO, "Recent:");
            last.forEach(e -> a.send(MessageLevel.DETAIL, line(e)));
        }
        a.send(MessageLevel.DETAIL, "Next: /fork live scan  - what is ready to be updated");
    }

    private void scan(CommandActor a) {
        CompletableFuture<ScanReport> modules = this.runtime.modules().scan();
        CompletableFuture<List<PluginUpdateManager.Candidate>> plugins = this.runtime.pluginUpdates()
                .map(PluginUpdateManager::scan).orElse(CompletableFuture.completedFuture(List.of()));
        CompletableFuture<List<LivePatchService.InboxEntry>> patches = this.runtime.config().livePatching().enabled()
                ? this.runtime.livePatches().list() : CompletableFuture.completedFuture(List.of());
        a.send(MessageLevel.INFO, "Scanning module directory, plugin update folder and patch directory ...");
        reply(a, CompletableFuture.allOf(modules, plugins, patches).thenApply(v -> null), ignored -> {
            a.send(MessageLevel.HEADER, "Ready for live update");
            ScanReport scan = modules.join();
            int shown = 0;
            for (ModuleArtifact art : scan.artifacts()) {
                Optional<ModuleSummary> loaded = this.runtime.modules().modules().stream().filter(m -> m.id().equals(art.id())).findFirst();
                String file = art.descriptor().version();
                if (loaded.isEmpty()) {
                    a.send(MessageLevel.INFO, "module " + art.id() + " " + file + ": not loaded - /fork live update " + art.id() + " enables it");
                    shown++;
                } else if (!loaded.get().version().equals(file)) {
                    a.send(MessageLevel.SUCCESS, "module " + art.id() + " " + loaded.get().version() + " -> " + file
                            + ": live (hot swap with automatic rollback)");
                    shown++;
                }
            }
            for (ScanReport.Problem p : scan.problems()) {
                a.send(MessageLevel.WARNING, "module file " + p.file() + ": " + p.message());
                shown++;
            }
            for (PluginUpdateManager.Candidate c : plugins.join()) {
                a.send(level(c.verdict().status()), "plugin " + PluginUpdateManager.verdictLine(c));
                c.verdict().reasons().stream().limit(2).forEach(r -> a.send(MessageLevel.DETAIL, r));
                shown++;
            }
            for (LivePatchService.InboxEntry e : patches.join()) {
                if (e.status() == LivePatchRegistry.Status.ACTIVE) {
                    continue;
                }
                a.send(e.problem() == null ? MessageLevel.INFO : MessageLevel.WARNING, "patch " + e.id() + ": "
                        + (e.problem() == null ? e.classes() + " class(es) - /fork live check " + e.id() + " shows whether it applies"
                        : e.problem()));
                shown++;
            }
            if (shown == 0) {
                a.send(MessageLevel.INFO, "Nothing new. Put files into " + this.runtime.config().moduleDirectory() + " (modules), "
                        + this.runtime.pluginUpdates().map(m -> relative(m.updateDirectory())).orElse("plugins/update")
                        + " (plugins) or " + this.runtime.config().livePatching().patchDirectory() + " (core patches).");
            }
        });
    }

    private void plugins(CommandActor a) {
        Optional<PluginUpdateManager> m = this.runtime.pluginUpdates();
        if (m.isEmpty()) {
            a.send(MessageLevel.ERROR, "Plugin updates are not supported on this platform.");
            return;
        }
        List<InstalledPlugin> all = m.get().installed();
        a.send(MessageLevel.HEADER, "Plugins (" + all.size() + ")");
        for (InstalledPlugin p : all) {
            List<String> dependents = all.stream().filter(o -> o != p && o.enabled()
                    && (o.info().depend().stream().anyMatch(p.info()::answersTo) || o.info().softDepend().stream().anyMatch(p.info()::answersTo)))
                    .map(InstalledPlugin::name).toList();
            String live;
            if (p.info().kind() == PluginInfo.Kind.PAPER) {
                live = "restart only (Paper plugin)";
            } else if (!dependents.isEmpty()) {
                live = "restart only (used by " + dependents + ")";
            } else if (m.get().unloadProblem() != null) {
                live = "restart only";
            } else {
                live = "live update possible";
            }
            a.send(p.enabled() ? MessageLevel.INFO : MessageLevel.WARNING, p.name() + " " + p.info().version() + " - "
                    + (p.enabled() ? "enabled" : "DISABLED") + " - " + live);
        }
        a.send(MessageLevel.DETAIL, "Update: put the new jar into " + relative(m.get().updateDirectory())
                + ", then /fork live check <plugin>. Whether a jar can go live is decided per jar.");
    }

    private void history(CommandActor a) {
        List<LiveHistory.Entry> entries = LiveHistory.INSTANCE.latest(15);
        a.send(MessageLevel.HEADER, "Live update history (this server process, newest first)");
        if (entries.isEmpty()) {
            a.send(MessageLevel.INFO, "Nothing happened yet. The full record is lemon/logs/audit.log.");
            return;
        }
        entries.forEach(e -> a.send(e.success() ? MessageLevel.INFO : MessageLevel.WARNING, line(e)));
        a.send(MessageLevel.DETAIL, "Complete record: lemon/logs/audit.log");
    }

    private static String line(LiveHistory.Entry e) {
        return TIME.format(e.time()) + " " + e.kind().name().toLowerCase(Locale.ROOT) + " " + e.name() + " " + e.action() + " "
                + (e.success() ? "OK" : "FAILED") + " (" + e.actor() + "): " + e.summary();
    }

    // ------------------------------------------------------------------ core patches

    private void patch(CommandActor a, String[] args) {
        String what = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "";
        LivePatchService service = this.runtime.livePatches();
        if (what.equals("verify") && args.length == 4) {
            a.send(MessageLevel.INFO, "Reading back the bytecode the JVM runs for patch " + PathGuard.sanitize(args[3]) + " ...");
            reply(a, service.verify(a.name(), args[3]), r -> patchResult(a, r, "verify", args[3]));
            return;
        }
        if (args.length != 3 || !(what.equals("status") || what.equals("probe"))) {
            a.send(MessageLevel.ERROR, "Usage: /fork live patch status | probe | verify <id>");
            return;
        }
        if (what.equals("probe")) {
            a.send(MessageLevel.INFO, "Probing class redefinition with throw-away classes ...");
            reply(a, service.probe(), results -> {
                a.send(MessageLevel.HEADER, "Class redefinition support of this JVM (probed)");
                capabilities(a, results);
            });
            return;
        }
        LivePatchService.StatusReport s = service.status();
        a.send(MessageLevel.HEADER, "Core live patching");
        a.send(s.enabled() ? MessageLevel.SUCCESS : MessageLevel.WARNING, "live-patching.enabled: " + s.enabled()
                + ", profile: " + s.profile().yamlName() + (s.profileNote() == null ? "" : " (" + s.profileNote() + ")"));
        a.send(s.instrumentation() ? MessageLevel.INFO : MessageLevel.WARNING, "Agent: " + s.agentDetail());
        a.send(MessageLevel.INFO, "JVM: " + s.vm());
        a.send(MessageLevel.DETAIL, "class redefinition supported: " + s.redefineSupported() + ", AllowEnhancedClassRedefinition: "
                + s.enhancedFlag());
        a.send(MessageLevel.INFO, "Build id: " + s.buildId());
        a.send(MessageLevel.INFO, "Patch directory: " + relative(s.patchDirectory()) + " - " + s.trustNote());
        a.send(MessageLevel.INFO, "Active patches: " + s.active() + " (apply attempts in this process: " + s.attempts() + ")");
        a.send(MessageLevel.INFO, "Heap: " + s.heapUsedMiB() + " / " + s.heapMaxMiB() + " MiB used");
        Map<Capability, CapabilityProbe.Result> probed = JvmCapabilities.probed();
        if (probed.isEmpty()) {
            a.send(MessageLevel.DETAIL, "Capabilities not probed yet: /fork live patch probe");
        } else {
            capabilities(a, probed);
        }
    }

    private static void capabilities(CommandActor a, Map<Capability, CapabilityProbe.Result> results) {
        for (Map.Entry<Capability, CapabilityProbe.Result> e : new EnumMap<>(results).entrySet()) {
            a.send(e.getValue().supported() ? MessageLevel.SUCCESS : MessageLevel.WARNING, (e.getValue().supported() ? "yes " : "no  ")
                    + e.getKey().description());
            a.send(MessageLevel.DETAIL, e.getValue().detail());
        }
    }

    // ------------------------------------------------------------------ check / update / rollback

    private void resolve(CommandActor a, String raw, Consumer<Target> action) {
        String lower = raw.toLowerCase(Locale.ROOT);
        for (Kind k : Kind.values()) {
            String prefix = k.name().toLowerCase(Locale.ROOT) + ":";
            if (lower.startsWith(prefix)) {
                String name = raw.substring(prefix.length());
                if (name.isEmpty()) {
                    a.send(MessageLevel.ERROR, "Missing name after '" + prefix + "'");
                    return;
                }
                action.accept(new Target(k, name));
                return;
            }
        }
        CompletableFuture<ScanReport> modules = this.runtime.modules().scan().exceptionally(ex -> null);
        CompletableFuture<List<PluginUpdateManager.Candidate>> plugins = this.runtime.pluginUpdates()
                .map(m -> m.scan().exceptionally(ex -> List.of())).orElse(CompletableFuture.completedFuture(List.of()));
        reply(a, CompletableFuture.allOf(modules, plugins).thenApply(v -> null), ignored -> {
            List<Kind> kinds = new ArrayList<>();
            if (PatchIds.problem(raw) == null && (LivePatchRegistry.INSTANCE.latest(raw).isPresent() || patchFileExists(raw))) {
                kinds.add(Kind.PATCH);
            }
            ScanReport scan = modules.join();
            if (ModuleIds.problem(raw) == null && (this.runtime.modules().modules().stream().anyMatch(m -> m.id().equals(raw))
                    || (scan != null && scan.artifacts().stream().anyMatch(art -> art.id().equals(raw))))) {
                kinds.add(Kind.MODULE);
            }
            boolean plugin = plugins.join().stream().anyMatch(c -> c.jar().info() != null && c.jar().info().name().equalsIgnoreCase(raw))
                    || this.runtime.pluginUpdates().map(m -> m.installed().stream().anyMatch(p -> p.name().equalsIgnoreCase(raw)))
                    .orElse(false);
            if (plugin) {
                kinds.add(Kind.PLUGIN);
            }
            if (kinds.isEmpty()) {
                a.send(MessageLevel.ERROR, "No module, plugin or patch named '" + PathGuard.sanitize(raw) + "'. /fork live scan lists"
                        + " what is available.");
            } else if (kinds.size() > 1) {
                a.send(MessageLevel.ERROR, "'" + PathGuard.sanitize(raw) + "' is ambiguous (" + kinds + "); use a prefix, e.g. "
                        + kinds.get(0).name().toLowerCase(Locale.ROOT) + ":" + PathGuard.sanitize(raw));
            } else {
                action.accept(new Target(kinds.get(0), raw));
            }
        });
    }

    private boolean patchFileExists(String id) {
        try {
            return Files.isRegularFile(this.runtime.paths().patchDirectory(this.runtime.config()).resolve(id + PatchIds.SUFFIX));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void check(CommandActor a, Target t) {
        switch (t.kind()) {
            case PLUGIN -> {
                Optional<PluginUpdateManager> m = this.runtime.pluginUpdates();
                if (m.isEmpty()) {
                    a.send(MessageLevel.ERROR, "Plugin updates are not supported on this platform.");
                    return;
                }
                reply(a, m.get().check(t.name()), c -> verdict(a, c));
            }
            case MODULE -> {
                if (ModuleIds.problem(t.name()) != null) {
                    a.send(MessageLevel.ERROR, "Invalid module id: " + ModuleIds.problem(t.name()));
                    return;
                }
                reply(a, this.runtime.modules().validate(a.name(), t.name()), r -> {
                    result(a, r);
                    if (r.success()) {
                        boolean loaded = this.runtime.modules().modules().stream().anyMatch(m -> m.id().equals(t.name()));
                        a.send(MessageLevel.INFO, loaded ? "Live: /fork live update " + t.name() + " swaps the running version"
                                + " (automatic rollback if the new version fails)" : "Live: /fork live update " + t.name() + " enables it");
                    }
                });
            }
            case PATCH -> {
                if (PatchIds.problem(t.name()) != null) {
                    a.send(MessageLevel.ERROR, "Invalid patch id: " + PatchIds.problem(t.name()));
                    return;
                }
                reply(a, this.runtime.livePatches().inspect(t.name()), plan -> plan(a, plan));
            }
            default -> throw new IllegalStateException();
        }
    }

    private void update(CommandActor a, Target t, boolean confirm) {
        switch (t.kind()) {
            case PLUGIN -> {
                Optional<PluginUpdateManager> m = this.runtime.pluginUpdates();
                if (m.isEmpty()) {
                    a.send(MessageLevel.ERROR, "Plugin updates are not supported on this platform.");
                    return;
                }
                if (confirm) {
                    a.send(MessageLevel.INFO, "Updating plugin " + t.name() + " ...");
                }
                reply(a, m.get().update(a.name(), t.name(), confirm), r -> pluginResult(a, r, "update", t.name()));
            }
            case MODULE -> moduleUpdate(a, t.name(), confirm);
            case PATCH -> {
                if (confirm) {
                    a.send(MessageLevel.INFO, "Applying core patch " + t.name() + " ...");
                }
                reply(a, this.runtime.livePatches().apply(a.name(), t.name(), confirm), r -> patchResult(a, r, "update", t.name()));
            }
            default -> throw new IllegalStateException();
        }
    }

    private void rollback(CommandActor a, Target t, boolean confirm) {
        switch (t.kind()) {
            case PLUGIN -> {
                Optional<PluginUpdateManager> m = this.runtime.pluginUpdates();
                if (m.isEmpty()) {
                    a.send(MessageLevel.ERROR, "Plugin updates are not supported on this platform.");
                    return;
                }
                reply(a, m.get().rollback(a.name(), t.name(), confirm), r -> pluginResult(a, r, "rollback", t.name()));
            }
            case PATCH -> reply(a, this.runtime.livePatches().rollback(a.name(), t.name(), confirm),
                    r -> patchResult(a, r, "rollback", t.name()));
            case MODULE -> a.send(MessageLevel.ERROR, "Modules have no separate rollback: a failed update already switches back"
                    + " automatically. To go back to an older version, put its jar into the module directory and run"
                    + " /fork live update " + t.name() + ".");
            default -> throw new IllegalStateException();
        }
    }

    private void moduleUpdate(CommandActor a, String id, boolean confirm) {
        if (ModuleIds.problem(id) != null) {
            a.send(MessageLevel.ERROR, "Invalid module id: " + ModuleIds.problem(id));
            return;
        }
        String key = a.name() + "\u0000" + id;
        boolean loaded = this.runtime.modules().modules().stream().anyMatch(m -> m.id().equals(id));
        if (!confirm) {
            reply(a, this.runtime.modules().validateJar(a.name(), id), v -> {
                result(a, v.result());
                if (v.result().success() && v.sha256() != null) {
                    this.moduleConfirmations.put(key, new PendingModule(Instant.now().plusSeconds(120), v.sha256()));
                    a.send(MessageLevel.DETAIL, "sha256 " + v.sha256());
                    a.send(MessageLevel.WARNING, (loaded ? "Swap the running module to this version" : "Enable this module")
                            + "? Run /fork live update " + id + " confirm within 120 s.");
                }
            });
            return;
        }
        PendingModule pending = this.moduleConfirmations.remove(key);
        if (pending == null || pending.until().isBefore(Instant.now())) {
            a.send(MessageLevel.ERROR, "No update of module " + id + " is waiting for confirmation; run /fork live update " + id + " first.");
            return;
        }
        a.send(MessageLevel.INFO, "Working ...");
        // Only the jar that was validated: a file replaced in the meantime is refused.
        CompletableFuture<OperationResult> f = loaded ? this.runtime.modules().reload(a.name(), id, pending.sha256())
                : this.runtime.modules().enable(a.name(), id, pending.sha256());
        reply(a, f, r -> result(a, r));
    }

    // ------------------------------------------------------------------ rendering

    private static MessageLevel level(PluginCompatibility.Status s) {
        return switch (s) {
            case LIVE -> MessageLevel.SUCCESS;
            case RESTART_REQUIRED -> MessageLevel.WARNING;
            case INCOMPATIBLE -> MessageLevel.ERROR;
        };
    }

    private void verdict(CommandActor a, PluginUpdateManager.Candidate c) {
        a.send(MessageLevel.HEADER, "Plugin check: " + c.jar().file().getFileName());
        a.send(level(c.verdict().status()), PluginUpdateManager.verdictLine(c));
        a.send(MessageLevel.DETAIL, "sha256 " + c.jar().sha256() + ", " + c.jar().classCount() + " classes, " + c.jar().size() + " bytes");
        if (c.jar().info() != null) {
            PluginInfo i = c.jar().info();
            a.send(MessageLevel.DETAIL, "type " + i.kind().name().toLowerCase(Locale.ROOT) + ", main " + i.main()
                    + (i.apiVersion().isEmpty() ? "" : ", api-version " + i.apiVersion())
                    + (i.depend().isEmpty() ? "" : ", depends on " + i.depend()));
        }
        c.verdict().reasons().forEach(r -> a.send(MessageLevel.WARNING, r));
        c.verdict().warnings().forEach(w -> a.send(MessageLevel.DETAIL, "note: " + w));
        if (c.verdict().status() == PluginCompatibility.Status.LIVE) {
            a.send(MessageLevel.INFO, "Next: /fork live update " + c.jar().info().name());
        }
    }

    private static void pluginResult(CommandActor a, PluginUpdateManager.Result r, String verb, String name) {
        switch (r.kind()) {
            case CONFIRMATION_REQUIRED -> {
                a.send(MessageLevel.SUCCESS, r.message());
                if (r.candidate() != null) {
                    r.candidate().verdict().warnings().forEach(w -> a.send(MessageLevel.DETAIL, "note: " + w));
                }
                a.send(MessageLevel.WARNING, "Run /fork live " + verb + " " + name + " confirm within 120 s to do it.");
            }
            case UPDATED -> a.send(MessageLevel.SUCCESS, "OK " + r.message());
            case RESTORED -> a.send(MessageLevel.WARNING, "FAILED " + r.message());
            case FAILED, REJECTED -> a.send(MessageLevel.ERROR, (r.kind() == PluginUpdateManager.Result.Kind.REJECTED ? "NOT DONE " : "FAILED ")
                    + r.message());
            default -> throw new IllegalStateException();
        }
        r.details().forEach(d -> a.send(MessageLevel.DETAIL, d));
    }

    private static void patchResult(CommandActor a, LivePatchService.Result r, String verb, String id) {
        if (r.plan() != null && (r.kind() == LivePatchService.Result.Kind.CONFIRMATION_REQUIRED
                || r.kind() == LivePatchService.Result.Kind.REJECTED)) {
            plan(a, r.plan());
        }
        switch (r.kind()) {
            case CONFIRMATION_REQUIRED -> {
                a.send(MessageLevel.SUCCESS, r.message());
                r.details().forEach(d -> a.send(MessageLevel.DETAIL, d));
                a.send(MessageLevel.WARNING, "Run /fork live " + verb + " " + id + " confirm to change the running server.");
                return;
            }
            case APPLIED, ROLLED_BACK -> {
                if (r.verification() == null || r.verification().outcome() == AppliedBytecodeCheck.Outcome.VERIFIED) {
                    a.send(MessageLevel.SUCCESS, "OK " + r.message());
                } else {
                    // The JVM accepted the change, but Lemon could not confirm the running bytecode.
                    a.send(r.verification().outcome() == AppliedBytecodeCheck.Outcome.DIFFERS ? MessageLevel.ERROR : MessageLevel.WARNING,
                            (verb.equals("verify") ? "" : "DONE, ") + r.message() + " - " + r.verification().summary());
                }
            }
            case REJECTED -> a.send(MessageLevel.ERROR, "NOT DONE " + r.message());
            case FAILED -> a.send(MessageLevel.ERROR, "FAILED " + r.message());
            default -> throw new IllegalStateException();
        }
        r.details().forEach(d -> a.send(MessageLevel.DETAIL, d));
    }

    private static void plan(CommandActor a, PatchPlan plan) {
        a.send(MessageLevel.HEADER, "Core patch " + plan.patchId() + (plan.description().isEmpty() ? "" : " - " + plan.description()));
        a.send(MessageLevel.DETAIL, "archive sha256 " + plan.archiveSha256());
        for (PatchPlan.Entry e : plan.entries()) {
            MessageLevel level = switch (e.action()) {
                case REDEFINE, DEFINE -> MessageLevel.SUCCESS;
                case UNCHANGED -> MessageLevel.INFO;
                case REJECT -> MessageLevel.ERROR;
            };
            a.send(level, e.action().name().toLowerCase(Locale.ROOT) + " " + e.className()
                    + (e.replaces() == null ? "" : " (replaces the version of patch " + e.replaces() + ")"));
            for (ClassChange c : e.changes()) {
                a.send(MessageLevel.DETAIL, c.toString());
            }
            if (e.problem() != null) {
                a.send(MessageLevel.ERROR, e.problem());
            }
        }
        plan.errors().stream().filter(err -> plan.entries().stream().noneMatch(e -> err.endsWith(String.valueOf(e.problem()))))
                .forEach(err -> a.send(MessageLevel.ERROR, err));
        plan.warnings().forEach(w -> a.send(MessageLevel.WARNING, w));
        a.send(plan.applicable() ? MessageLevel.SUCCESS : MessageLevel.ERROR, plan.applicable()
                ? "Applicable live without restart." : "Cannot be applied live - a restart with a rebuilt server jar is required.");
    }

    private static void result(CommandActor a, OperationResult r) {
        a.send(r.success() ? MessageLevel.SUCCESS : MessageLevel.ERROR, (r.success() ? "OK " : "FAILED ") + r.action() + " "
                + r.moduleId() + ": " + r.message() + " (" + r.duration().toMillis() + " ms)");
        r.details().forEach(d -> a.send(MessageLevel.DETAIL, d));
    }

    private String relative(java.nio.file.Path p) {
        try {
            return this.runtime.paths().serverDirectory().relativize(p.toAbsolutePath().normalize()).toString();
        } catch (IllegalArgumentException e) {
            return p.toString();
        }
    }

    // ------------------------------------------------------------------ plumbing

    private <T> void reply(CommandActor actor, CompletableFuture<T> future, Consumer<T> render) {
        future.whenComplete((value, ex) -> deliver(() -> {
            if (ex != null) {
                Throwable t = ex;
                while ((t instanceof java.util.concurrent.CompletionException || t instanceof java.util.concurrent.ExecutionException)
                        && t.getCause() != null) {
                    t = t.getCause();
                }
                actor.send(MessageLevel.ERROR, "NOT DONE: " + PathGuard.sanitize(t.getMessage() == null ? t.toString() : t.getMessage()));
            } else {
                render.accept(value);
            }
        }));
    }

    private void deliver(Runnable r) {
        try {
            this.runtime.platform().mainThreadExecutor().execute(r);
        } catch (RejectedExecutionException e) {
            r.run();
        }
    }

    void usage(CommandActor a) {
        a.send(MessageLevel.HEADER, "Lemon live updates - modules, plugins and core patches");
        a.send(MessageLevel.DETAIL, "/fork live status | scan | plugins | modules | history");
        a.send(MessageLevel.DETAIL, "/fork live check <name>             - can it go live? (prefix plugin:, module: or patch: if ambiguous)");
        if (a.hasPermission(RuntimePermissions.LIVE_UPDATE)) {
            a.send(MessageLevel.DETAIL, "/fork live update <name> [confirm]  - install/update a plugin or module, apply a core patch");
            a.send(MessageLevel.DETAIL, "/fork live rollback <name> [confirm] - previous plugin jar or bytecode before a core patch");
        }
        a.send(MessageLevel.DETAIL, "/fork live patch status" + (a.hasPermission(RuntimePermissions.LIVE_UPDATE)
                ? " | probe | verify <id>  - JVM support; read back the running bytecode of an active patch" : ""));
    }

    List<String> complete(CommandActor actor, String[] args) {
        if (args.length == 2) {
            return AdminCommand.filter(SUBCOMMANDS, args[1]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("patch")) {
            return AdminCommand.filter(List.of("status", "probe", "verify"), args[2]);
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("patch") && args[2].equalsIgnoreCase("verify")) {
            return AdminCommand.filter(LivePatchRegistry.INSTANCE.records().stream()
                    .filter(r -> r.status() == LivePatchRegistry.Status.ACTIVE).map(LivePatchRegistry.PatchRecord::id).distinct().toList(), args[3]);
        }
        if (args.length == 3 && List.of("check", "update", "rollback").contains(args[1].toLowerCase(Locale.ROOT))) {
            List<String> names = new ArrayList<>();
            this.runtime.modules().modules().forEach(m -> names.add(m.id()));
            this.runtime.pluginUpdates().ifPresent(m -> m.installed().forEach(p -> names.add(p.name())));
            LivePatchRegistry.INSTANCE.records().forEach(r -> names.add(r.id()));
            return AdminCommand.filter(names.stream().distinct().sorted().toList(), args[2]);
        }
        if (args.length == 4 && List.of("update", "rollback").contains(args[1].toLowerCase(Locale.ROOT))) {
            return AdminCommand.filter(List.of("confirm"), args[3]);
        }
        return List.of();
    }
}
