package dev.lemonpvp.runtime.commands;

import dev.lemonpvp.runtime.LemonRuntime;
import dev.lemonpvp.runtime.livepatch.Capability;
import dev.lemonpvp.runtime.livepatch.CapabilityProbe;
import dev.lemonpvp.runtime.livepatch.ClassCatalog;
import dev.lemonpvp.runtime.livepatch.InstrumentationDiagnostics;
import dev.lemonpvp.runtime.livepatch.JvmCapabilities;
import dev.lemonpvp.runtime.livepatch.LivePatchRegistry;
import dev.lemonpvp.runtime.livepatch.LivePatchService;
import dev.lemonpvp.runtime.livepatch.Mechanism;
import dev.lemonpvp.runtime.livepatch.PatchTransformer;
import dev.lemonpvp.runtime.plugins.PluginHost;
import dev.lemonpvp.runtime.security.PathGuard;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/**
 * {@code /fork instrumentation ...}: the JVM instrumentation behind core live patches -
 * what the JVM offers, which classes are loaded, and patches by redefinition or
 * retransformation. Patch operations go through the same {@link LivePatchService} (checks,
 * confirmation, lock, backups, audit log, history) as {@code /fork live patch}.
 *
 * <pre>
 * /fork instrumentation status | capabilities | history
 * /fork instrumentation classes &lt;filter&gt; [component] [page]
 * /fork instrumentation inspect &lt;class&gt; [n] [verify]
 * /fork instrumentation patch check &lt;id&gt; [redefine|retransform]
 * /fork instrumentation patch apply &lt;id&gt; [redefine|retransform] [confirm]
 * /fork instrumentation patch rollback &lt;id&gt; [confirm]
 * /fork instrumentation patch verify &lt;id&gt;
 * </pre>
 *
 * <p>Reading needs {@link RuntimePermissions#LIVE_VIEW}. Everything that makes the JVM
 * redefine or retransform a class (apply, rollback, verify, {@code inspect ... verify},
 * the first capability probe) needs {@link RuntimePermissions#LIVE_UPDATE}, which only the
 * console has by default, and {@code live-patching.enabled: true}.</p>
 */
final class InstrumentationCommands {

    static final List<String> SUBCOMMANDS = List.of("status", "capabilities", "classes", "inspect", "patch", "history");
    private static final List<String> PATCH = List.of("check", "apply", "rollback", "verify");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    private static final String PREFIX = "/fork instrumentation ";

    private final LemonRuntime runtime;

    InstrumentationCommands(LemonRuntime runtime) {
        this.runtime = runtime;
    }

    void execute(CommandActor a, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        if (!SUBCOMMANDS.contains(sub)) {
            usage(a);
            return;
        }
        boolean changes = switch (sub) {
            case "capabilities" -> JvmCapabilities.probed().isEmpty();
            case "inspect" -> args[args.length - 1].equalsIgnoreCase("verify");
            case "patch" -> args.length >= 3 && List.of("apply", "rollback", "verify").contains(args[2].toLowerCase(Locale.ROOT));
            default -> false;
        };
        String node = changes ? RuntimePermissions.LIVE_UPDATE : RuntimePermissions.LIVE_VIEW;
        if (!a.hasPermission(node)) {
            a.send(MessageLevel.ERROR, "You do not have permission (" + node + ").");
            return;
        }
        switch (sub) {
            case "status" -> status(a);
            case "capabilities" -> capabilities(a);
            case "classes" -> classes(a, args);
            case "inspect" -> inspect(a, args);
            case "patch" -> patch(a, args);
            case "history" -> history(a);
            default -> usage(a);
        }
    }

    // ------------------------------------------------------------------ status

    private void status(CommandActor a) {
        LivePatchService service = this.runtime.livePatches();
        LivePatchService.StatusReport s = service.status();
        a.send(MessageLevel.HEADER, "JVM instrumentation");
        for (InstrumentationDiagnostics.Finding f : service.diagnostics(true).findings()) {
            a.send(switch (f.level()) {
                case OK -> MessageLevel.SUCCESS;
                case INFO -> MessageLevel.INFO;
                case MISSING -> MessageLevel.WARNING;
                case WARNING -> MessageLevel.ERROR;
            }, f.text());
        }
        a.send(s.enabled() ? MessageLevel.SUCCESS : MessageLevel.WARNING, "live-patching.enabled: " + s.enabled() + ", profile: "
                + s.profile().yamlName() + (s.profileNote() == null ? "" : " (" + s.profileNote() + ")") + "; " + s.trustNote());
        a.send(MessageLevel.INFO, "Lemon transformer: " + service.transformer().registeredAt()
                .map(t -> "registered (retransformation-capable) since " + TIME.format(t) + " UTC")
                .orElse("not registered (registered with the first retransformation patch)") + "; " + s.retransformedClasses()
                + " class(es) with a retransformation patch" + (s.displacedClasses() == 0 ? "" : ", " + s.displacedClasses() + " DISPLACED"));
        for (PatchTransformer.SlotInfo slot : service.transformer().slots().stream().limit(10).toList()) {
            a.send(slot.displaced() == null ? MessageLevel.DETAIL : MessageLevel.WARNING, slot.className() + ": layers " + slot.patchIds()
                    + ", supplied " + slot.served() + "x" + (slot.displaced() == null ? "" : " - DISPLACED: " + slot.displaced()));
        }
        a.send(MessageLevel.INFO, "Active patches: " + s.active() + " (apply attempts in this process: " + s.attempts() + "), build id "
                + s.buildId());
        a.send(MessageLevel.DETAIL, "Next: " + PREFIX + "classes <filter> | inspect <class> | patch check <id> [retransform]");
    }

    private void capabilities(CommandActor a) {
        Map<Capability, CapabilityProbe.Result> probed = JvmCapabilities.probed();
        if (!probed.isEmpty()) {
            a.send(MessageLevel.HEADER, "What this JVM can change (probed with throw-away classes)");
            LiveCommands.capabilities(a, probed);
            return;
        }
        a.send(MessageLevel.INFO, "Probing redefinition and retransformation with throw-away classes ...");
        reply(a, this.runtime.livePatches().probe(), results -> {
            a.send(MessageLevel.HEADER, "What this JVM can change (probed with throw-away classes)");
            LiveCommands.capabilities(a, results);
        });
    }

    // ------------------------------------------------------------------ classes

    private ClassCatalog.Loaders loaders() {
        Map<ClassLoader, String> plugins = this.runtime.platform().pluginHost().map(PluginHost::classLoaders).orElse(Map.of());
        return new ClassCatalog.Loaders(this.runtime.platform().serverClassLoader(), plugins);
    }

    private void classes(CommandActor a, String[] args) {
        ClassCatalog.Query query;
        try {
            query = ClassCatalog.Query.parse(Arrays.asList(args).subList(2, args.length));
        } catch (IllegalArgumentException e) {
            a.send(MessageLevel.ERROR, "Usage: " + PREFIX + "classes <filter> [component] [page] - " + e.getMessage());
            a.send(MessageLevel.DETAIL, "filter: part of the name (spigotmc.Ticks), a pattern (org.bukkit.craftbukkit.*Command) or *");
            return;
        }
        reply(a, this.runtime.livePatches().classes(query, loaders()), page -> {
            ClassCatalog.Query q = page.query();
            a.send(MessageLevel.HEADER, "Loaded classes matching '" + q.filter() + "'" + (q.component() == null ? ""
                    : " in " + q.component().id()));
            a.send(MessageLevel.INFO, page.matches() + " of " + page.loaded() + " loaded classes match; page " + q.page() + "/"
                    + page.pages() + " (" + Math.max(1, page.micros() / 1000) + " ms)");
            for (ClassCatalog.Row r : page.rows()) {
                a.send(r.lemon() != null ? MessageLevel.WARNING : MessageLevel.DETAIL, r.name() + " - " + r.origin().label() + " - "
                        + (r.modifiable() ? "modifiable" : "NOT modifiable") + (r.hidden() ? ", hidden" : "")
                        + (r.lemon() == null ? "" : " - " + r.lemon()));
            }
            if (page.matches() == 0) {
                a.send(MessageLevel.INFO, "No loaded class matches. Classes are loaded on first use, so a class of the server jar"
                        + " may simply not be loaded yet.");
            } else if (q.page() < page.pages()) {
                a.send(MessageLevel.DETAIL, "Next page: " + PREFIX + "classes " + q.filter() + (q.component() == null ? ""
                        : " " + q.component().id()) + " " + (q.page() + 1));
            }
            a.send(MessageLevel.DETAIL, "Components: " + Arrays.stream(ClassCatalog.Component.values()).map(ClassCatalog.Component::id)
                    .toList() + "; details: " + PREFIX + "inspect <class>");
        });
    }

    private void inspect(CommandActor a, String[] args) {
        List<String> rest = new ArrayList<>(Arrays.asList(args).subList(2, args.length));
        boolean verify = !rest.isEmpty() && rest.get(rest.size() - 1).equalsIgnoreCase("verify");
        if (verify) {
            rest.remove(rest.size() - 1);
        }
        if (rest.isEmpty() || rest.size() > 2 || (rest.size() == 2 && !rest.get(1).matches("[0-9]{1,3}"))) {
            a.send(MessageLevel.ERROR, "Usage: " + PREFIX + "inspect <class> [n] [verify]");
            return;
        }
        String name = rest.get(0);
        int index = rest.size() == 2 ? Integer.parseInt(rest.get(1)) : 0;
        if (verify) {
            a.send(MessageLevel.INFO, "Reading back the bytecode the JVM runs for " + PathGuard.sanitize(name) + " ...");
            reply(a, this.runtime.livePatches().compareRunning(a.name(), name, Math.max(1, index)), r -> {
                if (r.differences().isEmpty()) {
                    a.send(MessageLevel.SUCCESS, "OK " + r.className() + " (" + r.loader() + "): the running bytecode equals "
                            + r.expected());
                } else {
                    a.send(MessageLevel.WARNING, r.className() + " (" + r.loader() + ") runs bytecode that DIFFERS from " + r.expected()
                            + " - changed by an agent, a tool or a patch outside Lemon:");
                    r.differences().stream().limit(8).forEach(d -> a.send(MessageLevel.DETAIL, d.toString()));
                    if (r.differences().size() > 8) {
                        a.send(MessageLevel.DETAIL, "... and " + (r.differences().size() - 8) + " more");
                    }
                }
                a.send(MessageLevel.DETAIL, "The read-back was a retransformation: retransformation-capable transformers ran again.");
            });
            return;
        }
        reply(a, this.runtime.livePatches().describe(name, loaders()), found -> {
            if (found.isEmpty()) {
                a.send(MessageLevel.WARNING, PathGuard.sanitize(name) + " is not loaded. Classes are loaded on first use; Lemon"
                        + " does not load classes just to show them.");
                return;
            }
            if (found.size() > 1 && index == 0) {
                a.send(MessageLevel.HEADER, found.size() + " loaded classes are named " + name + " (one per class loader)");
                for (int i = 0; i < found.size(); i++) {
                    a.send(MessageLevel.DETAIL, (i + 1) + ": " + found.get(i).loader() + " - " + found.get(i).origin().label());
                }
                a.send(MessageLevel.INFO, "Choose one: " + PREFIX + "inspect " + name + " <n>");
                return;
            }
            if (index > found.size()) {
                a.send(MessageLevel.ERROR, "There are only " + found.size() + " loaded classes named " + name + ".");
                return;
            }
            describe(a, found.get(Math.max(1, index) - 1), Math.max(1, index), found.size());
        });
    }

    private void describe(CommandActor a, ClassCatalog.Description d, int index, int count) {
        LivePatchService.StatusReport s = this.runtime.livePatches().status();
        a.send(MessageLevel.HEADER, "Class " + d.name() + (count > 1 ? " (" + index + " of " + count + ")" : ""));
        a.send(MessageLevel.INFO, "kind: " + d.kind() + "; component: " + d.origin().label());
        a.send(MessageLevel.DETAIL, "class loader: " + d.loader());
        a.send(MessageLevel.DETAIL, "module: " + d.module());
        a.send(MessageLevel.DETAIL, "code source (as reported by the class loader): " + d.codeSource());
        if (d.modifiable()) {
            a.send(MessageLevel.SUCCESS, "isModifiableClass: true - the JVM accepts redefinition/retransformation of this class in"
                    + " principle; which changes it accepts is a separate question (" + PREFIX + "capabilities)");
        } else {
            a.send(MessageLevel.WARNING, "isModifiableClass: false - " + d.notModifiable());
        }
        a.send(MessageLevel.DETAIL, "this JVM: redefineClasses " + (s.redefineSupported() ? "supported" : "NOT supported")
                + ", retransformClasses " + (s.retransformSupported() ? "supported" : "NOT supported"));
        a.send(MessageLevel.DETAIL, "class file: " + d.classFile());
        if (d.classFileSha256() != null) {
            a.send(MessageLevel.DETAIL, "class file sha256: " + d.classFileSha256() + " (a patch for this class must be built against"
                    + " exactly this file)");
        }
        if (d.lemon() == null) {
            a.send(MessageLevel.INFO, "Lemon: not changed by Lemon in this process");
        } else {
            a.send(MessageLevel.WARNING, "Lemon: " + d.lemon());
        }
        if (d.transformer() != null) {
            PatchTransformer.SlotInfo t = d.transformer();
            a.send(MessageLevel.DETAIL, "transformer: layers " + t.patchIds() + ", supplied " + t.served() + "x, base "
                    + (t.baseSha256() == null ? "-" : t.baseSha256().substring(0, 12))
                    + (t.displaced() == null ? "" : ", DISPLACED: " + t.displaced()));
        }
        a.send(MessageLevel.DETAIL, "Changes by other agents or tools are not tracked; compare with the running bytecode: " + PREFIX
                + "inspect " + d.name() + (count > 1 ? " " + index : "") + " verify");
    }

    // ------------------------------------------------------------------ patches

    private void patch(CommandActor a, String[] args) {
        String what = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "";
        if (!PATCH.contains(what) || args.length < 4) {
            patchUsage(a);
            return;
        }
        String id = args[3];
        List<String> options = Arrays.asList(args).subList(4, args.length);
        Mechanism mechanism = null;
        boolean confirm = false;
        for (String o : options) {
            Optional<Mechanism> m = Mechanism.parse(o);
            if (m.isPresent() && mechanism == null && (what.equals("check") || what.equals("apply"))) {
                mechanism = m.get();
            } else if (o.equalsIgnoreCase("confirm") && !confirm && (what.equals("apply") || what.equals("rollback"))) {
                confirm = true;
            } else {
                patchUsage(a);
                return;
            }
        }
        Mechanism m = mechanism == null ? Mechanism.REDEFINE : mechanism;
        LivePatchService service = this.runtime.livePatches();
        String shown = PathGuard.sanitize(id);
        switch (what) {
            case "check" -> reply(a, service.inspect(id, m), plan -> {
                LiveCommands.plan(a, plan);
                if (plan.applicable()) {
                    a.send(MessageLevel.INFO, "Apply: " + PREFIX + "patch apply " + shown + " " + m.id());
                }
            });
            case "apply" -> {
                if (confirm) {
                    a.send(MessageLevel.INFO, "Applying core patch " + shown + " (" + m.id() + ") ...");
                }
                reply(a, service.apply(a.name(), id, confirm, m), r -> LiveCommands.patchOutcome(a, r,
                        PREFIX + "patch apply " + shown + " " + m.id() + " confirm", false));
            }
            case "rollback" -> reply(a, service.rollback(a.name(), id, confirm), r -> LiveCommands.patchOutcome(a, r,
                    PREFIX + "patch rollback " + shown + " confirm", false));
            case "verify" -> {
                a.send(MessageLevel.INFO, "Reading back the bytecode the JVM runs for patch " + shown + " ...");
                reply(a, service.verify(a.name(), id), r -> LiveCommands.patchOutcome(a, r, "", true));
            }
            default -> patchUsage(a);
        }
    }

    private static void patchUsage(CommandActor a) {
        a.send(MessageLevel.ERROR, "Usage: " + PREFIX + "patch check <id> [redefine|retransform]");
        a.send(MessageLevel.DETAIL, PREFIX + "patch apply <id> [redefine|retransform] [confirm]");
        a.send(MessageLevel.DETAIL, PREFIX + "patch rollback <id> [confirm] | patch verify <id>");
    }

    private void history(CommandActor a) {
        List<LivePatchRegistry.PatchRecord> records = new ArrayList<>(this.runtime.livePatches().history());
        a.send(MessageLevel.HEADER, "Core patches in this server process (newest first)");
        if (records.isEmpty()) {
            a.send(MessageLevel.INFO, "No patch was applied in this process. The full record is lemon/logs/audit.log.");
            return;
        }
        java.util.Collections.reverse(records);
        for (LivePatchRegistry.PatchRecord r : records.stream().limit(15).toList()) {
            a.send(switch (r.status()) {
                case ACTIVE -> MessageLevel.SUCCESS;
                case ROLLED_BACK -> MessageLevel.INFO;
                case FAILED -> MessageLevel.ERROR;
            }, TIME.format(r.appliedAt()) + " " + r.id() + " (" + r.mechanism().id() + ") "
                    + r.status().name().toLowerCase(Locale.ROOT).replace('_', ' ') + " by " + r.actor() + ": " + r.detail());
        }
        a.send(MessageLevel.DETAIL, "Complete record: lemon/logs/audit.log; patches end with the process.");
    }

    // ------------------------------------------------------------------ plumbing

    void usage(CommandActor a) {
        a.send(MessageLevel.HEADER, "Lemon JVM instrumentation");
        a.send(MessageLevel.DETAIL, PREFIX + "status | capabilities | history");
        a.send(MessageLevel.DETAIL, PREFIX + "classes <filter> [component] [page]  - search loaded classes");
        a.send(MessageLevel.DETAIL, PREFIX + "inspect <class> [n]  - loader, code source, modifiable, Lemon changes");
        a.send(MessageLevel.DETAIL, PREFIX + "patch check <id> [redefine|retransform]");
        if (a.hasPermission(RuntimePermissions.LIVE_UPDATE)) {
            a.send(MessageLevel.DETAIL, PREFIX + "patch apply <id> [redefine|retransform] [confirm] | rollback <id> [confirm]"
                    + " | verify <id>");
            a.send(MessageLevel.DETAIL, PREFIX + "inspect <class> [n] verify  - read back the running bytecode");
        }
    }

    List<String> complete(String[] args) {
        if (args.length == 2) {
            return AdminCommand.filter(SUBCOMMANDS, args[1]);
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        if (sub.equals("classes") && args.length == 4) {
            return AdminCommand.filter(Arrays.stream(ClassCatalog.Component.values()).map(ClassCatalog.Component::id).toList(), args[3]);
        }
        if (sub.equals("inspect") && args.length == 3) {
            List<String> names = new ArrayList<>();
            this.runtime.livePatches().transformer().slots().forEach(s -> names.add(s.className()));
            LivePatchRegistry.INSTANCE.records().forEach(r -> r.classes().forEach(c -> names.add(c.name())));
            return AdminCommand.filter(names.stream().distinct().sorted().toList(), args[2]);
        }
        if (!sub.equals("patch")) {
            return List.of();
        }
        if (args.length == 3) {
            return AdminCommand.filter(PATCH, args[2]);
        }
        if (args.length == 4) {
            return AdminCommand.filter(LivePatchRegistry.INSTANCE.records().stream().map(LivePatchRegistry.PatchRecord::id).distinct()
                    .sorted().toList(), args[3]);
        }
        String what = args[2].toLowerCase(Locale.ROOT);
        List<String> next = new ArrayList<>();
        if (what.equals("check") || what.equals("apply")) {
            next.addAll(List.of("redefine", "retransform"));
        }
        if (what.equals("apply") || what.equals("rollback")) {
            next.add("confirm");
        }
        return AdminCommand.filter(next, args[args.length - 1]);
    }

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
}
