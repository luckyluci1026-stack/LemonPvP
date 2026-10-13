package dev.lemonpvp.runtime.state;

import dev.lemonpvp.runtime.descriptor.ModuleIds;
import dev.lemonpvp.runtime.security.Hashing;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Persists which modules were activated at runtime so they can be restored after a
 * restart ({@code persist-runtime-state}). Each entry pins the SHA-256 digest of the jar
 * that was activated; a changed jar is never re-activated implicitly.
 *
 * <p>Writes are atomic (temporary file + rename). The store is only touched from the
 * runtime worker thread.</p>
 */
public final class RuntimeStateStore {

    /**
     * Persisted activation.
     *
     * @param id      module id
     * @param version module version at activation time
     * @param sha256  jar digest at activation time
     */
    public record Activation(String id, String version, String sha256) {
    }

    private final Path file;

    /** @param file state file */
    public RuntimeStateStore(Path file) {
        this.file = file;
    }

    /**
     * @return persisted activations by id; invalid entries are skipped
     * @throws IOException if the file exists but cannot be read
     */
    public synchronized Map<String, Activation> load() throws IOException {
        if (!Files.exists(this.file)) {
            return Map.of();
        }
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
            props.load(r);
        }
        Map<String, Activation> out = new LinkedHashMap<>();
        for (String key : new TreeMap<>(props).keySet().stream().map(String::valueOf).toList()) {
            if (!key.startsWith("module.") || !key.endsWith(".sha256")) {
                continue;
            }
            String id = key.substring("module.".length(), key.length() - ".sha256".length());
            String sha = props.getProperty(key, "");
            String version = props.getProperty("module." + id + ".version", "");
            if (ModuleIds.isValid(id) && Hashing.isSha256(sha)) {
                out.put(id, new Activation(id, version, sha));
            }
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * Records or removes an activation.
     *
     * @param id         module id
     * @param activation activation to store, or {@code null} to remove the entry
     * @throws IOException if writing fails
     */
    public synchronized void put(String id, Activation activation) throws IOException {
        Map<String, Activation> current = new TreeMap<>(load());
        if (activation == null) {
            current.remove(id);
        } else {
            current.put(id, activation);
        }
        Properties props = new Properties();
        for (Activation a : current.values()) {
            props.setProperty("module." + a.id() + ".sha256", a.sha256());
            props.setProperty("module." + a.id() + ".version", a.version());
        }
        Files.createDirectories(this.file.toAbsolutePath().getParent());
        Path tmp = this.file.resolveSibling(this.file.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            props.store(w, "Lemon runtime state - modules activated at runtime. Managed by the server.");
        }
        Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
