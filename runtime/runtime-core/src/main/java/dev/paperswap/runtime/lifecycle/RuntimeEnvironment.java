package dev.paperswap.runtime.lifecycle;

import dev.paperswap.runtime.RuntimePaths;
import dev.paperswap.runtime.api.RuntimeInfo;
import dev.paperswap.runtime.config.RuntimeConfig;
import dev.paperswap.runtime.metrics.LeakDetector;
import dev.paperswap.runtime.metrics.RuntimeHealth;
import dev.paperswap.runtime.metrics.RuntimeMetrics;
import dev.paperswap.runtime.platform.RuntimePlatform;
import dev.paperswap.runtime.security.AuditLog;
import dev.paperswap.runtime.services.ServiceHub;
import dev.paperswap.runtime.state.RuntimeStateStore;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Shared collaborators of the module manager.
 *
 * @param config      supplies the current runtime configuration (may change on /fork reload)
 * @param paths       file layout
 * @param platform    hosting platform
 * @param worker      executor for file I/O and hashing (never the main thread)
 * @param audit       audit log
 * @param state       persisted runtime state
 * @param metrics     operation counters
 * @param health      restart recommendations
 * @param leaks       unloaded class loader tracking
 * @param services    shared service registry
 * @param runtimeInfo runtime information exposed to modules
 */
public record RuntimeEnvironment(
        Supplier<RuntimeConfig> config,
        RuntimePaths paths,
        RuntimePlatform platform,
        ExecutorService worker,
        AuditLog audit,
        RuntimeStateStore state,
        RuntimeMetrics metrics,
        RuntimeHealth health,
        LeakDetector leaks,
        ServiceHub services,
        RuntimeInfo runtimeInfo
) {
}
