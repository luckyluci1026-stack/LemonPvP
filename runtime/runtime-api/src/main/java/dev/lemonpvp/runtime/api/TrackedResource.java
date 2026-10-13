package dev.lemonpvp.runtime.api;

/**
 * Handle of a tracked resource. Closing it releases the resource early and removes it
 * from the module's resource scope. Closing twice has no effect.
 */
public interface TrackedResource extends AutoCloseable {

    /** @return description used in diagnostics */
    String description();

    /** @return whether the resource was already released */
    boolean isReleased();

    /** Releases the resource now. Never throws; failures are recorded in diagnostics. */
    @Override
    void close();
}
