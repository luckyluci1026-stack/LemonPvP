package dev.paperswap.runtime.config;

import dev.paperswap.runtime.api.ConfigSnapshot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the last valid configuration of a module and swaps it atomically.
 *
 * <p>Readers always see a complete snapshot (old or new, never a mix). A candidate that
 * fails to parse or validate never becomes visible.</p>
 */
public final class ModuleConfigHolder {

    /** File name of the module configuration inside the module data directory. */
    public static final String FILE_NAME = "config.yml";

    private final Path file;
    private final AtomicReference<ConfigSnapshot> current = new AtomicReference<>(ConfigSnapshot.empty());

    /** @param file module configuration file */
    public ModuleConfigHolder(Path file) {
        this.file = file;
    }

    /** @return configuration file path */
    public Path file() {
        return this.file;
    }

    /** @return current snapshot */
    public ConfigSnapshot current() {
        return this.current.get();
    }

    /**
     * Parses the configuration file. Performs file I/O - worker thread only.
     *
     * @return parsed candidate; empty if the file does not exist
     * @throws IOException                       if reading fails
     * @throws YamlSupport.YamlFormatException if the YAML is invalid
     */
    public ConfigSnapshot readCandidate() throws IOException {
        if (!Files.exists(this.file)) {
            return ConfigSnapshot.empty();
        }
        return ConfigSnapshot.of(YamlSupport.load(this.file));
    }

    /**
     * Makes a validated candidate the current configuration.
     *
     * @param candidate new snapshot
     * @return the previous snapshot (for rollback)
     */
    public ConfigSnapshot swap(ConfigSnapshot candidate) {
        return this.current.getAndSet(candidate);
    }
}
