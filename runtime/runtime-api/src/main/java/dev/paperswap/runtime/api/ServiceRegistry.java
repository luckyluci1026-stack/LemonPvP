package dev.paperswap.runtime.api;

import java.util.Optional;

/**
 * Registry for services shared between modules.
 *
 * <p>A service type must be visible to both provider and consumer, i.e. it is defined in
 * the provider module (consumer declares a dependency on it), in the runtime API or in the
 * server API. Services are removed automatically when the providing module is disabled.
 * Consumers must not cache service instances beyond the lifetime of the provider; the
 * runtime prevents disabling a provider while enabled modules depend on it.</p>
 */
public interface ServiceRegistry {

    /**
     * Publishes a service. Requires the {@link Capability#SERVICES} capability.
     *
     * @param type           service interface
     * @param implementation implementation instance
     * @param <T>            service type
     * @return handle that unregisters the service when closed
     */
    <T> TrackedResource provide(Class<T> type, T implementation);

    /**
     * Looks up the first provider of a service.
     *
     * @param type service interface
     * @param <T>  service type
     * @return the provider, or empty when no enabled module provides it
     */
    <T> Optional<T> find(Class<T> type);
}
