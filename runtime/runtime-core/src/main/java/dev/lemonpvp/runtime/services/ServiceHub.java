package dev.lemonpvp.runtime.services;

import dev.lemonpvp.runtime.api.Capability;
import dev.lemonpvp.runtime.api.ServiceRegistry;
import dev.lemonpvp.runtime.api.TrackedResource;
import dev.lemonpvp.runtime.resources.ResourceTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/** Runtime-wide service registry; each module sees it through {@link #view}. */
public final class ServiceHub {

    private final Map<Class<?>, List<Provider>> providers = new ConcurrentHashMap<>();

    private record Provider(String owner, Object instance) {
    }

    /**
     * Creates the registry view of one module enable cycle.
     *
     * @param owner          module id
     * @param canProvide     whether the module declared {@link Capability#SERVICES}
     * @param trackerSupplier supplies the module's current resource tracker
     * @return the view
     */
    public ServiceRegistry view(String owner, boolean canProvide, Supplier<ResourceTracker> trackerSupplier) {
        return new ServiceRegistry() {
            @Override
            public <T> TrackedResource provide(Class<T> type, T implementation) {
                Objects.requireNonNull(type, "type");
                Objects.requireNonNull(implementation, "implementation");
                if (!canProvide) {
                    throw new IllegalStateException("module '" + owner + "' did not declare the 'services' capability");
                }
                if (!type.isInstance(implementation)) {
                    throw new IllegalArgumentException("implementation does not implement " + type.getName());
                }
                ResourceTracker tracker = trackerSupplier.get();
                if (tracker == null || !tracker.isOpen()) {
                    throw new IllegalStateException("module '" + owner + "' is not enabled");
                }
                Provider p = new Provider(owner, implementation);
                ServiceHub.this.providers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(p);
                // Remove the key together with the last provider: the Class object may belong to
                // the module's class loader and must not stay referenced after an unload.
                return tracker.register("service", type.getName(), () ->
                        ServiceHub.this.providers.computeIfPresent(type, (k, list) -> {
                            list.remove(p);
                            return list.isEmpty() ? null : list;
                        }));
            }

            @Override
            public <T> Optional<T> find(Class<T> type) {
                return ServiceHub.this.find(type);
            }
        };
    }

    /**
     * @param type service type
     * @param <T>  service type
     * @return first provider
     */
    public <T> Optional<T> find(Class<T> type) {
        List<Provider> list = this.providers.get(type);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(type.cast(list.getFirst().instance()));
    }

    /**
     * @param owner module id
     * @return service type names currently provided by the module
     */
    public List<String> providedBy(String owner) {
        List<String> out = new ArrayList<>();
        this.providers.forEach((type, list) -> {
            for (Provider p : list) {
                if (p.owner().equals(owner)) {
                    out.add(type.getName());
                }
            }
        });
        return out;
    }
}
