package dev.lemonpvp.runtime.config;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Validated runtime configuration ({@code config/lemon-runtime.yml}). Every field is
 * evaluated by the runtime; see {@code docs/HOT-DEPLOYMENT.md} for the semantics.
 *
 * @param enabled              master switch; when {@code false} the runtime does not start at all
 * @param moduleDirectory      directory scanned for module jars, relative to the server directory
 * @param allowRuntimeDeploy   allow load/enable/disable/reload/unload while the server runs
 * @param startupModules       module ids activated on server start
 * @param persistRuntimeState  re-activate modules enabled at runtime after a restart
 * @param verifyIntegrity      require a matching {@code <jar>.sha256} file next to every module jar
 * @param trustedHashesFile    optional allow-list of trusted SHA-256 digests (empty = disabled)
 * @param logModuleLifecycle   log every lifecycle transition at INFO level
 * @param maxModuleCount       upper bound for simultaneously loaded modules
 * @param operationTimeout     timeout for asynchronous phases; threshold for slow-callback warnings
 * @param allowedApiVersions   module API versions accepted by this server
 * @param allowServerInternals let module class loaders see {@code net.minecraft} / CraftBukkit internals
 */
public record RuntimeConfig(
        boolean enabled,
        String moduleDirectory,
        boolean allowRuntimeDeploy,
        List<String> startupModules,
        boolean persistRuntimeState,
        boolean verifyIntegrity,
        String trustedHashesFile,
        boolean logModuleLifecycle,
        int maxModuleCount,
        Duration operationTimeout,
        Set<Integer> allowedApiVersions,
        boolean allowServerInternals
) {
    public RuntimeConfig {
        startupModules = List.copyOf(startupModules);
        allowedApiVersions = Set.copyOf(allowedApiVersions);
        trustedHashesFile = trustedHashesFile == null ? "" : trustedHashesFile;
    }

    /** @return the documented defaults (runtime disabled) */
    public static RuntimeConfig defaults() {
        return new RuntimeConfig(false, "lemon/modules", false, List.of(), true, true, "", true, 32,
                Duration.ofSeconds(30), Set.of(1), false);
    }

    /** @return a copy with {@code enabled} changed (used by tests and tooling) */
    public RuntimeConfig withEnabled(boolean value) {
        return new RuntimeConfig(value, this.moduleDirectory, this.allowRuntimeDeploy, this.startupModules,
                this.persistRuntimeState, this.verifyIntegrity, this.trustedHashesFile, this.logModuleLifecycle,
                this.maxModuleCount, this.operationTimeout, this.allowedApiVersions, this.allowServerInternals);
    }
}
