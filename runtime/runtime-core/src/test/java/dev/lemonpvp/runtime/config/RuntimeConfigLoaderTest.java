package dev.lemonpvp.runtime.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RuntimeConfigLoaderTest {

    @TempDir
    Path dir;

    @Test
    void defaultFileMatchesDocumentedDefaults() {
        Map<String, Object> raw = YamlSupport.load(new ByteArrayInputStream(
                RuntimeConfigLoader.DEFAULT_FILE.getBytes(StandardCharsets.UTF_8)), "default");
        RuntimeConfigLoader.Result r = RuntimeConfigLoader.parse(raw);
        assertTrue(r.valid(), r.errors().toString());
        assertTrue(r.warnings().isEmpty(), r.warnings().toString());
        assertEquals(RuntimeConfig.defaults(), r.config());
        assertFalse(r.config().enabled(), "hot deployment is disabled by default");
        assertFalse(r.config().allowRuntimeDeploy());
        assertTrue(r.config().verifyIntegrity());
    }

    @Test
    void missingFileIsCreatedWithDefaults() throws Exception {
        Path file = this.dir.resolve("config/lemon-runtime.yml");
        RuntimeConfigLoader.Result r = RuntimeConfigLoader.loadOrCreate(file);
        assertTrue(r.createdDefault());
        assertEquals(RuntimeConfigLoader.DEFAULT_FILE, Files.readString(file));
        assertFalse(RuntimeConfigLoader.loadOrCreate(file).createdDefault());
    }

    @Test
    void invalidValuesAreRejectedAndDefaultsKept() throws Exception {
        Path file = this.dir.resolve("c.yml");
        Files.writeString(file, """
                enabled: "yes"
                module-directory: ../outside
                max-module-count: 0
                operation-timeout-seconds: 99999
                allowed-api-versions: [0]
                startup-modules: [Bad, ok-id, ok-id]
                trusted-hashes-file: /etc/hashes
                surprise: 1
                """);
        RuntimeConfigLoader.Result r = RuntimeConfigLoader.load(file);
        assertFalse(r.valid());
        assertTrue(r.errors().size() >= 7, r.errors().toString());
        assertEquals(1, r.warnings().size());
        assertEquals(RuntimeConfig.defaults(), r.config());
    }

    @Test
    void restartRequiredSettingsAreDetected() {
        RuntimeConfig a = RuntimeConfig.defaults().withEnabled(true);
        RuntimeConfig b = RuntimeConfig.defaults();
        assertEquals(java.util.List.of("enabled"), RuntimeConfigLoader.restartRequiredChanges(a, b));
    }
}
