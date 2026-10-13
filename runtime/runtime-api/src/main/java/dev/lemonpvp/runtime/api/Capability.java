package dev.lemonpvp.runtime.api;

import java.util.Locale;
import java.util.Optional;

/**
 * Runtime capabilities a module must request in its descriptor before it can use the
 * corresponding part of the platform facade.
 *
 * <p>Capabilities make a module's footprint visible to administrators and keep accidental
 * registrations out. They are <strong>not</strong> a security boundary: module code runs
 * with the full rights of the server process.</p>
 */
public enum Capability {
    /** Register event listeners. */
    EVENTS,
    /** Register commands. */
    COMMANDS,
    /** Schedule synchronous or asynchronous tasks. */
    SCHEDULER,
    /** Provide services to other modules. */
    SERVICES;

    /** @return descriptor spelling, e.g. {@code "events"} */
    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /**
     * @param key descriptor spelling
     * @return the matching capability
     */
    public static Optional<Capability> byKey(String key) {
        for (Capability c : values()) {
            if (c.key().equals(key)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
