package dev.paperswap.runtime.paper;

import dev.paperswap.runtime.PaperSwapRuntime;
import dev.paperswap.runtime.commands.AdminCommand;
import dev.paperswap.runtime.commands.RuntimePermissions;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

/** The runtime as it runs inside the Paper server: host, platform, command, permissions. */
final class PaperRuntime {

    private static final List<Permission> PERMISSIONS = List.of(
            new Permission(RuntimePermissions.BASE, "Use /fork info, version and status", PermissionDefault.OP),
            new Permission(RuntimePermissions.MODULE_VIEW, "List and inspect runtime modules", PermissionDefault.OP),
            new Permission(RuntimePermissions.RELOAD, "Reload paperswap.yml", PermissionDefault.FALSE),
            new Permission(RuntimePermissions.MODULE_MANAGE, "Load, enable, disable, reload and unload modules (executes code)",
                    PermissionDefault.FALSE),
            new Permission(RuntimePermissions.LIVE_VIEW, "Show live update status, scan and compatibility checks", PermissionDefault.OP),
            new Permission(RuntimePermissions.LIVE_UPDATE, "Live-update plugins and modules, apply core patches (changes running code)",
                    PermissionDefault.FALSE));

    private final Server server;
    private final RuntimeHostPlugin host;
    private final PaperSwapRuntime runtime;
    private final Logger logger;
    private ForkCommand command;

    private PaperRuntime(Server server, RuntimeHostPlugin host, PaperSwapRuntime runtime, Logger logger) {
        this.server = server;
        this.host = host;
        this.runtime = runtime;
        this.logger = logger;
    }

    /**
     * Reads the configuration and starts the runtime if it is enabled.
     *
     * @param server server
     * @return the running runtime, or empty if hot deployment is disabled
     */
    static Optional<PaperRuntime> start(Server server) {
        Logger logger = new PrefixedLogger("[PaperSwap] ", server.getLogger());
        RuntimeHostPlugin host = new RuntimeHostPlugin(server, logger);
        PaperPlatform platform = new PaperPlatform(server, host, logger);
        Optional<PaperSwapRuntime> runtime = PaperSwapRuntime.bootstrap(platform, Path.of("").toAbsolutePath());
        if (runtime.isEmpty()) {
            return Optional.empty();
        }
        host.setEnabled(true);
        PaperRuntime paper = new PaperRuntime(server, host, runtime.get(), logger);
        paper.registerPermissions();
        paper.registerCommand();
        runtime.get().activateStartupModules();
        return Optional.of(paper);
    }

    private void registerPermissions() {
        for (Permission p : PERMISSIONS) {
            if (this.server.getPluginManager().getPermission(p.getName()) == null) {
                this.server.getPluginManager().addPermission(p);
            }
        }
    }

    /** Registers /fork, /paperswap (and paperswap:fork) unless another component already owns them. */
    void registerCommand() {
        CommandMap map = this.server.getCommandMap();
        if (this.command != null && map.getKnownCommands().get(ForkCommand.NAMESPACE + ":" + ForkCommand.LABEL) == this.command) {
            return;
        }
        this.command = new ForkCommand(new AdminCommand(this.runtime));
        boolean primary = map.register(ForkCommand.LABEL, ForkCommand.NAMESPACE, this.command);
        if (!primary) {
            this.logger.warning("/fork is already used by another command; use /paperswap:fork instead");
        }
    }

    /** Stops all modules and removes every registration of the runtime. */
    void stop() {
        try {
            this.runtime.stop();
        } catch (RuntimeException e) {
            this.logger.log(Level.SEVERE, "Error while stopping the PaperSwap runtime", e);
        }
        if (this.command != null) {
            CommandMap map = this.server.getCommandMap();
            map.getKnownCommands().entrySet().stream().filter(e -> e.getValue() == this.command).map(java.util.Map.Entry::getKey)
                    .toList().forEach(map.getKnownCommands()::remove);
            Command c = this.command;
            c.unregister(map);
            this.command = null;
        }
        for (Permission p : PERMISSIONS) {
            this.server.getPluginManager().removePermission(p.getName());
        }
        // Safety net: nothing owned by the runtime host may survive the runtime.
        this.server.getScheduler().cancelTasks(this.host);
        HandlerList.unregisterAll(this.host);
        this.host.setEnabled(false);
    }
}
