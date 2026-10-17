package dev.paperswap.runtime;

import dev.paperswap.runtime.config.RuntimeConfig;
import dev.paperswap.runtime.security.PathGuard;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * File layout of the runtime. Everything except the configuration file lives below
 * {@code <server>/paperswap/} and is separated by purpose:
 *
 * <pre>
 * config/paperswap.yml          runtime configuration (next to Paper's config files)
 * paperswap/modules/                    module jars + .sha256 files   (module-directory, configurable)
 * paperswap/module-data/&lt;id&gt;/           configuration and data of each module
 * paperswap/staging/                    verified private copies of loaded jars (temporary)
 * paperswap/logs/audit.log              audit log of administrative actions
 * paperswap/state/runtime-state.properties  modules activated at runtime
 * paperswap/patches/inbox/              uploaded live patches (patch-directory, configurable)
 * paperswap/patches/backups/&lt;id&gt;/      class files that were replaced by a live patch
 * paperswap/plugin-backups/&lt;name&gt;/     plugin jars that were replaced by a live plugin update
 * paperswap/modules-removed/            module jars removed with /fork module remove
 * </pre>
 *
 * @param serverDirectory server working directory
 * @param configFile      runtime configuration file
 * @param root            runtime root directory
 */
public record RuntimePaths(Path serverDirectory, Path configFile, Path root) {

    /** Default configuration file, relative to the server directory. */
    public static final String DEFAULT_CONFIG = "config/paperswap.yml";
    /** Runtime root directory, relative to the server directory. */
    public static final String DEFAULT_ROOT = "paperswap";
    /** Configuration file of servers set up before the project was renamed to PaperSwap. */
    public static final String LEGACY_CONFIG = "config/lemon-runtime.yml";
    /** Runtime root directory of servers set up before the project was renamed to PaperSwap. */
    public static final String LEGACY_ROOT = "lemon";

    /**
     * Default layout. A server set up before the rename keeps working: if the new
     * configuration file or root directory does not exist but the old one does, the old one
     * is used ({@link #legacyNotes} says which and how to move it).
     *
     * @param serverDirectory server working directory
     * @return the layout
     */
    public static RuntimePaths of(Path serverDirectory) {
        Path dir = serverDirectory.toAbsolutePath().normalize();
        Path config = dir.resolve(DEFAULT_CONFIG);
        if (!Files.exists(config) && Files.isRegularFile(dir.resolve(LEGACY_CONFIG))) {
            config = dir.resolve(LEGACY_CONFIG);
        }
        Path root = dir.resolve(DEFAULT_ROOT);
        if (!Files.exists(root) && Files.isDirectory(dir.resolve(LEGACY_ROOT))) {
            root = dir.resolve(LEGACY_ROOT);
        }
        return new RuntimePaths(dir, config, root);
    }

    /** @return one line per old (pre-PaperSwap) location in use, with the command that moves it */
    public List<String> legacyNotes() {
        List<String> notes = new ArrayList<>();
        if (this.configFile.equals(this.serverDirectory.resolve(LEGACY_CONFIG))) {
            notes.add("using the old configuration file " + LEGACY_CONFIG + "; rename it to " + DEFAULT_CONFIG
                    + " while the server is stopped");
        }
        if (this.root.equals(this.serverDirectory.resolve(LEGACY_ROOT))) {
            notes.add("using the old data directory " + LEGACY_ROOT + "/; rename it to " + DEFAULT_ROOT + "/ while the server is"
                    + " stopped (and adjust module-directory / patch-directory in the configuration if they point into it)");
        }
        return notes;
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

    /** @return private copies of plugin jars between their check and their installation */
    public Path pluginStaging() {
        return this.root.resolve("plugin-staging");
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
