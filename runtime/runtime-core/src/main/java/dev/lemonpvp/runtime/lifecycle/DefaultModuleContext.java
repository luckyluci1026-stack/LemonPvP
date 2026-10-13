package dev.lemonpvp.runtime.lifecycle;

import dev.lemonpvp.runtime.api.ConfigSnapshot;
import dev.lemonpvp.runtime.api.ModuleContext;
import dev.lemonpvp.runtime.api.ModuleDescriptor;
import dev.lemonpvp.runtime.api.ResourceScope;
import dev.lemonpvp.runtime.api.RuntimeInfo;
import dev.lemonpvp.runtime.api.ServiceRegistry;
import dev.lemonpvp.runtime.api.TrackedResource;
import dev.lemonpvp.runtime.config.ModuleConfigHolder;
import dev.lemonpvp.runtime.platform.PlatformBinding;
import dev.lemonpvp.runtime.resources.ResourceTracker;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.logging.Logger;

/** Context of one module instance. Delegates to the state of the current enable cycle. */
final class DefaultModuleContext implements ModuleContext {

    private final ModuleDescriptor descriptor;
    private final Logger logger;
    private final Path dataDirectory;
    private final ModuleConfigHolder config;
    private final ServiceRegistry services;
    private final RuntimeInfo runtimeInfo;
    private final Supplier<ResourceTracker> tracker;
    private final Supplier<PlatformBinding> binding;
    private final Supplier<Boolean> active;

    DefaultModuleContext(ModuleDescriptor descriptor, Logger logger, Path dataDirectory, ModuleConfigHolder config,
                         ServiceRegistry services, RuntimeInfo runtimeInfo, Supplier<ResourceTracker> tracker,
                         Supplier<PlatformBinding> binding, Supplier<Boolean> active) {
        this.descriptor = descriptor;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.config = config;
        this.services = services;
        this.runtimeInfo = runtimeInfo;
        this.tracker = tracker;
        this.binding = binding;
        this.active = active;
    }

    @Override
    public ModuleDescriptor descriptor() {
        return this.descriptor;
    }

    @Override
    public Logger logger() {
        return this.logger;
    }

    @Override
    public Path dataDirectory() {
        return this.dataDirectory;
    }

    @Override
    public ConfigSnapshot config() {
        return this.config.current();
    }

    @Override
    public ResourceScope resources() {
        return new ResourceScope() {
            @Override
            public <T extends AutoCloseable> T track(String description, T resource) {
                return current().track(description, resource);
            }

            @Override
            public TrackedResource onCleanup(String description, Runnable cleanup) {
                return current().onCleanup(description, cleanup);
            }

            @Override
            public int size() {
                ResourceTracker t = DefaultModuleContext.this.tracker.get();
                return t == null ? 0 : t.size();
            }

            private ResourceTracker current() {
                ResourceTracker t = DefaultModuleContext.this.tracker.get();
                if (t == null) {
                    throw new IllegalStateException("resources can only be registered while module '"
                            + DefaultModuleContext.this.descriptor.id() + "' is enabling or enabled");
                }
                return t;
            }
        };
    }

    @Override
    public ServiceRegistry services() {
        return this.services;
    }

    @Override
    public RuntimeInfo runtime() {
        return this.runtimeInfo;
    }

    @Override
    public <T> T platform(Class<T> type) {
        PlatformBinding b = this.binding.get();
        if (b == null) {
            throw new IllegalStateException("the platform is only available while module '" + this.descriptor.id()
                    + "' is enabling or enabled (not in onLoad)");
        }
        T facade = b.facade(type);
        if (facade == null) {
            throw new UnsupportedOperationException("platform facade " + type.getName() + " is not available");
        }
        return facade;
    }

    @Override
    public boolean isActive() {
        return this.active.get();
    }
}
