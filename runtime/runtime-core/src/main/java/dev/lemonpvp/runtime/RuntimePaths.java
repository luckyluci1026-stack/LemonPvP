package dev.lemonpvp.runtime;

import dev.lemonpvp.runtime.config.RuntimeConfig;
import dev.lemonpvp.runtime.security.PathGuard;
import java.nio.file.Path;
import java.util.Optional;

/**
 * File layout of the runtime. Everything except the configuration file lives below
 * {@code <server>/lemon/} and is separated by purpose:
 *
 * <pre>
 * config/lemon-runtime.yml          runtime configuration (next to Paper's config files)
 * lemon/modules/                    module jars + .sha256 files   (module-directory, configurable)
 * lemon/module-data/&lt;id&gt;/           configuration and data of each module
 * lemon/staging/                    verified private copies of loaded jars (temporary)
 * lemon/logs/audit.log              audit log of administrative actions
 * lemon/state/runtime-state.properties  modules activated at runtime
 * lemon/patches/inbox/              uploaded live patches (patch-directory, configurable)
 * lemon/patches/backups/&lt;id&gt;/      class files that were replaced by a live patch
 * lemon/plugin-backups/&lt;name&gt;/     plugin jars that were replaced by a live plugin update
 * lemon/modules-removed/            module jars removed with /fork module remove
 * </pre>
 *
 * @param serverDirectory server working directory
 * @param configFile      runtime configuration file
 * @param root            runtime root directory
 */
public record RuntimePaths(Path serverDirectory, Path configFile, Path root) {

    /** Default configuration file, relative to the server directory. */
    public static final String DEFAULT_CONFIG = "config/lemon-runtime.yml";

    /**
     * @param serverDirectory server working directory
     * @return default layout
     */
    public static RuntimePaths of(Path serverDirectory) {
        Path dir = serverDirectory.toAbsolutePath().normalize();
        return new RuntimePaths(dir, dir.resolve(DEFAULT_CONFIG), dir.resolve("lemon"));
    }

    /** @param config configuration @return module jar directory */
    public Path moduleDirectory(RuntimeConfig config) {
        return PathGuard.resolveInside(this.serverDirectory, config.moduleDirectory());
    }

    /** @return parent of all module data directories */
    public Path moduleDataRoot() {
        return this.root.resolve("module-data");
    }

    /** @param moduleId validated module id @return data directory of the module */
    public Path moduleData(String moduleId) {
        return PathGuard.resolveInside(moduleDataRoot(), moduleId);
    }

    /** @return staging directory */
    public Path staging() {
        return this.root.resolve("staging");
    }

    /** @return audit log file */
    public Path auditLog() {
        return this.root.resolve("logs").resolve("audit.log");
    }

    /** @return persisted runtime state file */
    public Path stateFile() {
        return this.root.resolve("state").resolve("runtime-state.properties");
    }

    /** @param config configuration @return directory with uploaded live patches */
    public Path patchDirectory(RuntimeConfig config) {
        return PathGuard.resolveInside(this.serverDirectory, config.livePatching().patchDirectory());
    }

    /** @return directory for backups of replaced plugin jars */
    public Path pluginBackups() {
        return this.root.resolve("plugin-backups");
    }

    /** @param config configuration @return plugin allow-list file if configured */
    public Optional<Path> pluginTrustedHashes(RuntimeConfig config) {
        String file = config.pluginUpdates().trustedHashesFile();
        return file.isEmpty() ? Optional.empty() : Optional.of(PathGuard.resolveInside(this.serverDirectory, file));
    }

    /** @return directory for backups of replaced class files */
    public Path patchBackups() {
        return this.root.resolve("patches").resolve("backups");
    }

    /** @param config configuration @return live-patch allow-list file if configured */
    public Optional<Path> patchTrustedHashes(RuntimeConfig config) {
        String file = config.livePatching().trustedHashesFile();
        return file.isEmpty() ? Optional.empty() : Optional.of(PathGuard.resolveInside(this.serverDirectory, file));
    }

    /** @param config configuration @return allow-list file if configured */
    public Optional<Path> trustedHashes(RuntimeConfig config) {
        if (config.trustedHashesFile().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(PathGuard.resolveInside(this.serverDirectory, config.trustedHashesFile()));
    }
}
