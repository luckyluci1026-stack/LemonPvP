package dev.lemonpvp.runtime.lifecycle;

import dev.lemonpvp.runtime.RuntimePaths;
import dev.lemonpvp.runtime.api.RuntimeInfo;
import dev.lemonpvp.runtime.config.RuntimeConfig;
import dev.lemonpvp.runtime.metrics.LeakDetector;
import dev.lemonpvp.runtime.metrics.RuntimeHealth;
import dev.lemonpvp.runtime.metrics.RuntimeMetrics;
import dev.lemonpvp.runtime.platform.RuntimePlatform;
import dev.lemonpvp.runtime.security.AuditLog;
import dev.lemonpvp.runtime.services.ServiceHub;
import dev.lemonpvp.runtime.state.RuntimeStateStore;
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
