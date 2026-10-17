package dev.paperswap.runtime.metrics;

import dev.paperswap.runtime.api.ModuleState;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Point-in-time diagnostics of a loaded module instance.
 *
 * @param id                 module id
 * @param version            module version
 * @param state              lifecycle state
 * @param sha256             digest of the loaded jar
 * @param stateHistory       recent transitions, oldest first
 * @param loadMillis         duration of the last load
 * @param enableMillis       duration of the last {@code onEnable}
 * @param disableMillis      duration of the last disable incl. cleanup (-1 if never disabled)
 * @param enabledSince       time of the last successful enable or {@code null}
 * @param resources          live tracked resources by category
 * @param services           service types provided
 * @param inFlightCallbacks  callbacks currently executing
 * @param callbackFailures   callbacks that threw
 * @param droppedCallbacks   callbacks dropped because the module was inactive
 * @param runningAsyncWork   asynchronous work still running after cancellation
 * @param lastError          last error or {@code null}
 * @param cleanupProblems    problems during the last cleanup
 * @param dependencies       ids this module depends on
 * @param dependents         ids of loaded modules depending on this one
 * @param previousInstances  unloaded instances of the same id and whether they were collected
 */
public record ModuleDiagnostics(
        String id,
        String version,
        ModuleState state,
        String sha256,
        List<String> stateHistory,
        long loadMillis,
        long enableMillis,
        long disableMillis,
        Instant enabledSince,
        Map<String, Integer> resources,
        List<String> services,
        int inFlightCallbacks,
        long callbackFailures,
        long droppedCallbacks,
        List<String> runningAsyncWork,
        String lastError,
        List<String> cleanupProblems,
        List<String> dependencies,
        List<String> dependents,
        List<LeakDetector.Entry> previousInstances
) {
}
