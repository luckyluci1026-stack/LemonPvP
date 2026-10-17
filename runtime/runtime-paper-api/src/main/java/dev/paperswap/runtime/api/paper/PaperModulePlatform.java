package dev.paperswap.runtime.api.paper;

import org.bukkit.Server;

/**
 * Paper facade of a module, obtained with
 * {@code context.platform(PaperModulePlatform.class)} while the module is enabling or enabled.
 *
 * <p>Use these registries instead of the Bukkit API directly: everything registered here
 * is tracked and removed when the module is disabled or replaced. Registrations made
 * directly through {@code Bukkit.getPluginManager()} or {@code Bukkit.getScheduler()} are
 * invisible to the runtime and would leak.</p>
 */
public interface PaperModulePlatform {

    /** @return the server (read-only use is always safe; registrations go through the registries) */
    Server server();

    /**
     * @return event registry
     * @throws IllegalStateException if the module did not declare the {@code events} capability
     */
    ModuleEvents events();

    /**
     * @return command registry
     * @throws IllegalStateException if the module did not declare the {@code commands} capability
     */
    ModuleCommands commands();

    /**
     * @return scheduler
     * @throws IllegalStateException if the module did not declare the {@code scheduler} capability
     */
    ModuleScheduler scheduler();
}
