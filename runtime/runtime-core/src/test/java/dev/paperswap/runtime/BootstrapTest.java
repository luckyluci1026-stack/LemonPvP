package dev.paperswap.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.paperswap.runtime.testsupport.TestPlatform;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** "Hot deployment disabled" must mean: no threads, no scans, no directories, nothing. */
class BootstrapTest {

    @TempDir
    Path dir;

    private static boolean workerThreadExists() {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(t -> t.getName().equals(PaperSwapRuntime.WORKER_THREAD_NAME) && t.isAlive());
    }

    @Test
    void disabledByDefaultAndCompletelyInactive() throws Exception {
        TestPlatform platform = new TestPlatform();
        try {
            Files.createDirectories(this.dir.resolve("paperswap/modules"));
            assertTrue(PaperSwapRuntime.bootstrap(platform, this.dir).isEmpty());
            assertTrue(Files.exists(this.dir.resolve("config/paperswap.yml")), "default config is written once");
            assertFalse(workerThreadExists(), "no background thread when disabled");
            assertFalse(Files.exists(this.dir.resolve("paperswap/staging")), "nothing is staged");
            assertFalse(Files.exists(this.dir.resolve("paperswap/logs")), "no audit log");
            assertTrue(platform.logLines().stream().anyMatch(l -> l.contains("Hot deployment is disabled")));
        } finally {
            platform.shutdown();
        }
    }

    @Test
    void invalidConfigurationKeepsTheRuntimeDisabled() throws Exception {
        TestPlatform platform = new TestPlatform();
        try {
            Files.createDirectories(this.dir.resolve("config"));
            Files.writeString(this.dir.resolve("config/paperswap.yml"), "enabled: true\nmodule-directory: /etc\n");
            assertTrue(PaperSwapRuntime.bootstrap(platform, this.dir).isEmpty(), "fail safe: invalid config never enables");
            assertTrue(platform.logLines().stream().anyMatch(l -> l.startsWith("SEVERE") && l.contains("stays disabled")));
        } finally {
            platform.shutdown();
        }
    }

    @Test
    void workerThreadOnlyExistsWhileWorkIsQueued() throws Exception {
        TestPlatform platform = new TestPlatform();
        Files.createDirectories(this.dir.resolve("config"));
        Files.writeString(this.dir.resolve("config/paperswap.yml"), "enabled: true\n");
        PaperSwapRuntime runtime = PaperSwapRuntime.bootstrap(platform, this.dir).orElseThrow();
        try {
            runtime.modules().scan().get();
            platform.onMain(() -> {
                runtime.stop();
                return null;
            });
            for (int i = 0; i < 50 && workerThreadExists(); i++) {
                Thread.sleep(100);
            }
            assertFalse(workerThreadExists(), "worker thread ends with the runtime");
        } finally {
            platform.shutdown();
        }
    }
}
