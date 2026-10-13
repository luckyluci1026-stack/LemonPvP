package dev.lemonpvp.runtime.api.paper;

import dev.lemonpvp.runtime.api.TrackedResource;

/**
 * Command registrations of a module.
 *
 * <p>A module command is always reachable as {@code /<module-id>:<label>}. The short form
 * {@code /<label>} (and each alias) is only registered if no other command uses it yet:
 * module commands never replace existing server or plugin commands. Commands are removed
 * when the module is disabled and connected players receive an updated command tree.</p>
 *
 * <p>Implementation note: Paper's Brigadier lifecycle API only allows registrations during
 * server start and reload, so module commands use the Bukkit {@code CommandMap}, which
 * Paper marks obsolete (not deprecated for removal) since 26.3.</p>
 */
public interface ModuleCommands {

    /**
     * @param spec    label, description, usage, aliases and permission
     * @param handler executor and tab completer
     * @return handle that removes the command when closed
     * @throws IllegalStateException if the namespaced label is already taken
     */
    TrackedResource register(CommandSpec spec, ModuleCommandHandler handler);
}
