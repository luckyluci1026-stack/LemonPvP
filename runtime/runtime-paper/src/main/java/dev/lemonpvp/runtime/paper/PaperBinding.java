package dev.lemonpvp.runtime.paper;

import dev.lemonpvp.runtime.api.Capability;
import dev.lemonpvp.runtime.api.PermissionDeclaration;
import dev.lemonpvp.runtime.api.paper.ModuleCommands;
import dev.lemonpvp.runtime.api.paper.ModuleEvents;
import dev.lemonpvp.runtime.api.paper.ModuleScheduler;
import dev.lemonpvp.runtime.api.paper.PaperModulePlatform;
import dev.lemonpvp.runtime.platform.ModuleHandle;
import dev.lemonpvp.runtime.platform.PlatformBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

/**
 * Paper side of one module enable cycle: registers declared permissions and hands out the
 * {@link PaperModulePlatform} facade. Every listener, command and task registered through
 * the facade is tracked in the module's resource tracker and released on disable.
 */
final class PaperBinding implements PlatformBinding, PaperModulePlatform {

    private final Server server;
    private final ModuleHandle handle;
    private final Logger logger;
    private final ModuleEventsImpl events;
    private final ModuleCommandsImpl commands;
    private final ModuleSchedulerImpl scheduler;
    private final List<String> registeredPermissions = new ArrayList<>();

    PaperBinding(Server server, RuntimeHostPlugin host, ModuleHandle handle, Logger logger) {
        this.server = server;
        this.handle = handle;
        this.logger = logger;
        this.events = new ModuleEventsImpl(server, host, handle);
        this.commands = new ModuleCommandsImpl(server, handle);
        this.scheduler = new ModuleSchedulerImpl(server, host, handle);
        registerPermissions();
    }

    private void registerPermissions() {
        for (PermissionDeclaration p : this.handle.descriptor().permissions()) {
            if (this.server.getPluginManager().getPermission(p.node()) != null) {
                this.logger.warning("Module " + this.handle.id() + " declares permission " + p.node()
                        + " which is already registered; keeping the existing definition");
                continue;
            }
            this.server.getPluginManager().addPermission(new Permission(p.node(), p.description(), map(p.defaultValue())));
            this.registeredPermissions.add(p.node());
        }
    }

    private static PermissionDefault map(PermissionDeclaration.PermissionDefault d) {
        return switch (d) {
            case TRUE -> PermissionDefault.TRUE;
            case FALSE -> PermissionDefault.FALSE;
            case OP -> PermissionDefault.OP;
            case NOT_OP -> PermissionDefault.NOT_OP;
        };
    }

    @Override
    public <T> T facade(Class<T> type) {
        return type == PaperModulePlatform.class ? type.cast(this) : null;
    }

    @Override
    public List<String> runningAsyncWork() {
        return this.scheduler.runningAsync();
    }

    @Override
    public List<String> close() {
        List<String> problems = new ArrayList<>();
        for (String node : this.registeredPermissions) {
            try {
                this.server.getPluginManager().removePermission(node);
            } catch (RuntimeException e) {
                problems.add("permission " + node + ": " + e);
            }
        }
        this.registeredPermissions.clear();
        return problems;
    }

    @Override
    public Server server() {
        return this.server;
    }

    @Override
    public ModuleEvents events() {
        require(Capability.EVENTS);
        return this.events;
    }

    @Override
    public ModuleCommands commands() {
        require(Capability.COMMANDS);
        return this.commands;
    }

    @Override
    public ModuleScheduler scheduler() {
        require(Capability.SCHEDULER);
        return this.scheduler;
    }

    private void require(Capability capability) {
        if (!this.handle.descriptor().has(capability)) {
            throw new IllegalStateException("module '" + this.handle.id() + "' must declare the '" + capability.key()
                    + "' capability in lemon-module.yml");
        }
    }
}
