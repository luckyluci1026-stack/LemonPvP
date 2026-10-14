package dev.lemonpvp.runtime.commands;

import dev.lemonpvp.runtime.LemonRuntime;
import dev.lemonpvp.runtime.descriptor.ModuleIds;
import dev.lemonpvp.runtime.lifecycle.ModuleSummary;
import dev.lemonpvp.runtime.lifecycle.OperationResult;
import dev.lemonpvp.runtime.loader.ModuleArtifact;
import dev.lemonpvp.runtime.security.PathGuard;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiFunction;

/**
 * Platform-neutral implementation of {@code /fork}. The platform adapter only translates
 * its command sender into a {@link CommandActor}.
 *
 * <pre>
 * /fork info | version | status | reload
 * /fork live ...                      (see {@link LiveCommands}: modules, plugins, core patches)
 * /fork module list
 * /fork module info|validate|diagnostics &lt;id&gt;
 * /fork module load|enable|disable|reload|unload|config|remove &lt;id&gt;
 * </pre>
 *
 * <p>Every argument is validated before use; module ids must match
 * {@link ModuleIds#PATTERN}, so no command can reference a file path. Output is plain text.
 * Long-running operations reply asynchronously on the main thread.</p>
 */
public final class AdminCommand {

    private static final List<String> TOP = List.of("info", "version", "status", "reload", "live", "module");
    private static final List<String> MODULE_VIEW = List.of("list", "info", "validate", "diagnostics");
    private static final List<String> MODULE_MANAGE = List.of("load", "enable", "disable", "reload", "unload", "config", "remove");

    private final LemonRuntime runtime;
    private final Reports reports;
    private final LiveCommands live;

    /** @param runtime running runtime */
    public AdminCommand(LemonRuntime runtime) {
        this.runtime = runtime;
        this.reports = new Reports(runtime);
        this.live = new LiveCommands(runtime, this.reports);
    }

    /**
     * Executes the command.
     *
     * @param actor sender
     * @param args  arguments after the label
     */
    public void execute(CommandActor actor, String[] args) {
        if (!actor.hasPermission(RuntimePermissions.BASE)) {
            deny(actor, RuntimePermissions.BASE);
            return;
        }
        if (args.length == 0) {
            usage(actor);
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "info" -> this.reports.info(actor);
            case "version" -> this.reports.version(actor);
            case "status" -> this.reports.status(actor);
            case "reload" -> reloadRuntimeConfig(actor);
            case "live" -> this.live.execute(actor, args);
            case "module" -> module(actor, args);
            default -> usage(actor);
        }
    }

    private void module(CommandActor actor, String[] args) {
        if (args.length < 2) {
            usage(actor);
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        boolean manage = MODULE_MANAGE.contains(action);
        if (!manage && !MODULE_VIEW.contains(action)) {
            usage(actor);
            return;
        }
        String node = manage ? RuntimePermissions.MODULE_MANAGE : RuntimePermissions.MODULE_VIEW;
        if (!actor.hasPermission(node)) {
            deny(actor, node);
            return;
        }
        if (action.equals("list")) {
            if (args.length != 2) {
                actor.send(MessageLevel.ERROR, "Usage: /fork module list");
                return;
            }
            reply(actor, this.runtime.modules().scan(), (a, scan) -> {
                this.reports.list(a, scan);
                return null;
            });
            return;
        }
        if (args.length != 3) {
            actor.send(MessageLevel.ERROR, "Usage: /fork module " + action + " <id>");
            return;
        }
        String id = args[2];
        String problem = ModuleIds.problem(id);
        if (problem != null) {
            actor.send(MessageLevel.ERROR, "Invalid module id '" + PathGuard.sanitize(id) + "': " + problem);
            return;
        }
        String who = actor.name();
        switch (action) {
            case "info" -> reply(actor, this.runtime.modules().scan(), (a, scan) -> {
                this.reports.moduleInfo(a, id, scan);
                return null;
            });
            case "diagnostics" -> this.reports.diagnostics(actor, id);
            case "validate" -> result(actor, this.runtime.modules().validate(who, id));
            case "load" -> result(actor, this.runtime.modules().load(who, id));
            case "enable" -> result(actor, this.runtime.modules().enable(who, id));
            case "disable" -> result(actor, this.runtime.modules().disable(who, id));
            case "reload" -> result(actor, this.runtime.modules().reload(who, id));
            case "unload" -> result(actor, this.runtime.modules().unload(who, id));
            case "config" -> result(actor, this.runtime.modules().reloadConfig(who, id));
            case "remove" -> result(actor, this.runtime.modules().remove(who, id));
            default -> usage(actor);
        }
    }

    private void reloadRuntimeConfig(CommandActor actor) {
        if (!actor.hasPermission(RuntimePermissions.RELOAD)) {
            deny(actor, RuntimePermissions.RELOAD);
            return;
        }
        actor.send(MessageLevel.INFO, "Reloading " + this.runtime.paths().configFile().getFileName() + " ...");
        reply(actor, this.runtime.reloadConfig(actor.name()), (a, r) -> {
            if (!r.applied()) {
                a.send(MessageLevel.ERROR, "Configuration NOT applied; the previous configuration stays active:");
                r.errors().forEach(e -> a.send(MessageLevel.DETAIL, e));
                return null;
            }
            a.send(MessageLevel.SUCCESS, "Runtime configuration applied.");
            r.warnings().forEach(w -> a.send(MessageLevel.WARNING, w));
            if (!r.restartRequired().isEmpty()) {
                a.send(MessageLevel.WARNING, "Changed settings that need a server restart: " + r.restartRequired());
            }
            return null;
        });
    }

    private void result(CommandActor actor, CompletableFuture<OperationResult> future) {
        actor.send(MessageLevel.INFO, "Working ...");
        reply(actor, future, (a, r) -> {
            a.send(r.success() ? MessageLevel.SUCCESS : MessageLevel.ERROR,
                    (r.success() ? "OK " : "FAILED ") + r.action() + " " + r.moduleId() + ": " + r.message()
                            + " (" + r.duration().toMillis() + " ms)");
            r.details().forEach(d -> a.send(MessageLevel.DETAIL, d));
            return null;
        });
    }

    private <T> void reply(CommandActor actor, CompletableFuture<T> future, BiFunction<CommandActor, T, Void> render) {
        future.whenComplete((value, ex) -> deliver(() -> {
            if (ex != null) {
                Throwable t = ex.getCause() != null ? ex.getCause() : ex;
                actor.send(MessageLevel.ERROR, "Failed: " + PathGuard.sanitize(String.valueOf(t.getMessage() == null ? t : t.getMessage())));
            } else {
                render.apply(actor, value);
            }
        }));
    }

    private void deliver(Runnable r) {
        try {
            this.runtime.platform().mainThreadExecutor().execute(r);
        } catch (RejectedExecutionException e) {
            r.run(); // runtime stopping: deliver directly
        }
    }

    private static void deny(CommandActor actor, String node) {
        actor.send(MessageLevel.ERROR, "You do not have permission (" + node + ").");
    }

    private void usage(CommandActor actor) {
        actor.send(MessageLevel.HEADER, "Lemon runtime commands");
        actor.send(MessageLevel.DETAIL, "/fork info | version | status");
        if (actor.hasPermission(RuntimePermissions.LIVE_VIEW)) {
            actor.send(MessageLevel.DETAIL, "/fork live ...  - live updates of modules, plugins and core patches (/fork live for help)");
        }
        if (actor.hasPermission(RuntimePermissions.RELOAD)) {
            actor.send(MessageLevel.DETAIL, "/fork reload  - reload lemon-runtime.yml");
        }
        if (actor.hasPermission(RuntimePermissions.MODULE_VIEW)) {
            actor.send(MessageLevel.DETAIL, "/fork module list | info <id> | validate <id> | diagnostics <id>");
        }
        if (actor.hasPermission(RuntimePermissions.MODULE_MANAGE)) {
            actor.send(MessageLevel.DETAIL, "/fork module load | enable | disable | reload | unload | config | remove <id>");
        }
    }

    /**
     * Tab completion.
     *
     * @param actor sender
     * @param args  arguments typed so far (last one may be partial)
     * @return suggestions
     */
    public List<String> complete(CommandActor actor, String[] args) {
        if (!actor.hasPermission(RuntimePermissions.BASE)) {
            return List.of();
        }
        if (args.length <= 1) {
            return filter(TOP, args.length == 0 ? "" : args[0]);
        }
        if (args[0].equalsIgnoreCase("live")) {
            return actor.hasPermission(RuntimePermissions.LIVE_VIEW) ? this.live.complete(actor, args) : List.of();
        }
        if (!args[0].equalsIgnoreCase("module")) {
            return List.of();
        }
        if (args.length == 2) {
            List<String> subs = new ArrayList<>();
            if (actor.hasPermission(RuntimePermissions.MODULE_VIEW)) {
                subs.addAll(MODULE_VIEW);
            }
            if (actor.hasPermission(RuntimePermissions.MODULE_MANAGE)) {
                subs.addAll(MODULE_MANAGE);
            }
            return filter(subs, args[1]);
        }
        if (args.length == 3 && !args[1].equalsIgnoreCase("list")
                && (actor.hasPermission(RuntimePermissions.MODULE_VIEW) || actor.hasPermission(RuntimePermissions.MODULE_MANAGE))) {
            TreeSet<String> ids = new TreeSet<>();
            for (ModuleSummary s : this.runtime.modules().modules()) {
                ids.add(s.id());
            }
            this.runtime.modules().lastScan().ifPresent(scan -> {
                for (ModuleArtifact a : scan.artifacts()) {
                    ids.add(a.id());
                }
            });
            return filter(new ArrayList<>(ids), args[2]);
        }
        return List.of();
    }

    static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.startsWith(p)).toList();
    }
}
