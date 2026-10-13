package dev.lemonpvp.runtime.platform;

import java.util.List;

/** Platform-specific state of one module enable cycle. */
public interface PlatformBinding {

    /**
     * @param type requested facade type
     * @param <T>  facade type
     * @return the facade or {@code null} if the platform does not offer it
     */
    <T> T facade(Class<T> type);

    /**
     * @return descriptions of asynchronous work of the module that is still running even
     * though its tasks were cancelled (it cannot be interrupted safely)
     */
    List<String> runningAsyncWork();

    /**
     * Releases platform registrations that are not tracked as individual resources (for
     * example declared permissions). Called on the main thread after the resource tracker
     * was closed.
     *
     * @return problems that occurred
     */
    List<String> close();

    /** Binding for platforms without additional facades (tests, headless use). */
    PlatformBinding NONE = new PlatformBinding() {
        @Override
        public <T> T facade(Class<T> type) {
            return null;
        }

        @Override
        public List<String> runningAsyncWork() {
            return List.of();
        }

        @Override
        public List<String> close() {
            return List.of();
        }
    };
}
