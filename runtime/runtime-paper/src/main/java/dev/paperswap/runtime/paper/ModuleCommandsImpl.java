package dev.paperswap.runtime.paper;

import dev.paperswap.runtime.api.TrackedResource;
import dev.paperswap.runtime.api.paper.CommandSpec;
import dev.paperswap.runtime.api.paper.ModuleCommandHandler;
import dev.paperswap.runtime.api.paper.ModuleCommands;
import dev.paperswap.runtime.platform.ModuleHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

/**
 * Registers module commands in the server command map without ever replacing an existing
 * command: the namespaced form {@code <module-id>:<label>} is always registered, the short
 * label and aliases only if they are still free.
 */
final class ModuleCommandsImpl implements ModuleCommands {

    private final Server server;
    private final ModuleHandle handle;

    ModuleCommandsImpl(Server server, ModuleHandle handle) {
        this.server = server;
        this.handle = handle;
    }

    @Override
    public TrackedResource register(CommandSpec spec, ModuleCommandHandler handler) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(handler, "handler");
        if (!this.handle.resources().isOpen()) {
            throw new IllegalStateException("module '" + this.handle.id() + "' is not enabled");
        }
        CommandMap map = this.server.getCommandMap();
        Map<String, Command> known = map.getKnownCommands();
        String prefix = this.handle.id();
        String namespaced = prefix + ":" + spec.label();
        if (known.containsKey(namespaced)) {
            throw new IllegalStateException("command /" + namespaced + " is already registered");
        }
        List<String> freeAliases = new ArrayList<>();
        for (String alias : spec.aliases()) {
            if (map.getCommand(alias) == null && !known.containsKey(prefix + ":" + alias)) {
                freeAliases.add(alias);
            }
        }
        ModuleCommand command = new ModuleCommand(spec, freeAliases, handler, this.handle.callbacks());
        if (map.getCommand(spec.label()) == null) {
            map.register(spec.label(), prefix, command);
        } else {
            // The short label belongs to someone else: only the namespaced form is added.
            known.put(namespaced, command);
            command.setLabel(namespaced);
            command.register(map);
            this.handle.logger().info("Command /" + spec.label() + " already exists; registered only as /" + namespaced);
        }
        return this.handle.resources().register("command", "/" + namespaced, () -> unregister(map, command));
    }

    private static void unregister(CommandMap map, Command command) {
        Map<String, Command> known = map.getKnownCommands();
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Command> e : known.entrySet()) {
            if (e.getValue() == command) {
                keys.add(e.getKey());
            }
        }
        for (String key : keys) {
            known.remove(key);
        }
        command.unregister(map);
    }
}
