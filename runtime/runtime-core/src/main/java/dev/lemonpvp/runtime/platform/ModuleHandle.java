package dev.lemonpvp.runtime.platform;

import dev.lemonpvp.runtime.api.ModuleDescriptor;
import dev.lemonpvp.runtime.resources.ResourceTracker;
import java.util.logging.Logger;

/**
 * View of a module enable cycle handed to the platform when creating a binding.
 *
 * @param descriptor  module descriptor
 * @param resources   resource tracker of this enable cycle
 * @param callbacks   guard to wrap every callback into module code
 * @param logger      module logger
 * @param classLoader module class loader
 */
public record ModuleHandle(
        ModuleDescriptor descriptor,
        ResourceTracker resources,
        CallbackGuard callbacks,
        Logger logger,
        ClassLoader classLoader
) {
    /** @return module id */
    public String id() {
        return this.descriptor.id();
    }
}
