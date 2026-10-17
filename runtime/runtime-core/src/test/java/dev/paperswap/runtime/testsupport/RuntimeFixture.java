package dev.paperswap.runtime.testsupport;

import dev.paperswap.runtime.PaperSwapRuntime;
import dev.paperswap.runtime.lifecycle.OperationResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** A server directory, a test platform and a started runtime. */
public final class RuntimeFixture implements AutoCloseable {

    public static final String ADMIN = "tester";

    public final Path serverDir;
    public final Path modules;
    public final TestPlatform platform;
    public PaperSwapRuntime runtime;

    private RuntimeFixture(Path serverDir, TestPlatform platform) {
        this.serverDir = serverDir;
        this.modules = serverDir.resolve("paperswap/modules");
        this.platform = platform;
    }

    /**
     * @param serverDir   empty directory
     * @param configLines extra lines for paperswap.yml
     */
    public static RuntimeFixture start(Path serverDir, String... configLines) throws Exception {
        return start(serverDir, p -> { }, configLines);
    }

    /** Like {@link #start(Path, String...)}, but lets the test configure the platform before the runtime starts. */
    public static RuntimeFixture start(Path serverDir, java.util.function.Consumer<TestPlatform> setup, String... configLines)
            throws Exception {
        writeConfig(serverDir, configLines);
        TestPlatform platform = new TestPlatform();
        setup.accept(platform);
        RuntimeFixture f = new RuntimeFixture(serverDir, platform);
        f.runtime = PaperSwapRuntime.bootstrap(f.platform, serverDir).orElseThrow();
        return f;
    }

    /** Starts the runtime on a server directory that is already set up; no configuration is written. */
    public static RuntimeFixture startExisting(Path serverDir) throws Exception {
        RuntimeFixture f = new RuntimeFixture(serverDir, new TestPlatform());
        f.runtime = PaperSwapRuntime.bootstrap(f.platform, serverDir).orElseThrow();
        return f;
    }

    public static void writeConfig(Path serverDir, String... configLines) throws Exception {
        Files.createDirectories(serverDir.resolve("config"));
        StringBuilder sb = new StringBuilder("enabled: true\n");
        boolean deploySet = false;
        for (String l : configLines) {
            if (l.startsWith("allow-runtime-deploy")) {
                deploySet = true;
            }
        }
        if (!deploySet) {
            sb.append("allow-runtime-deploy: true\n");
        }
        for (String l : configLines) {
            sb.append(l).append('\n');
        }
        Files.writeString(serverDir.resolve("config/paperswap.yml"), sb.toString());
    }

    /**
     * Waits until the audit log (written asynchronously) contains {@code expected}.
     *
     * @return the audit log contents
     */
    public String awaitAudit(String expected) throws Exception {
        java.nio.file.Path file = this.serverDir.resolve("paperswap/logs/audit.log");
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        String text = "";
        while (System.nanoTime() < deadline) {
            text = java.nio.file.Files.exists(file) ? java.nio.file.Files.readString(file) : "";
            if (text.contains(expected)) {
                return text;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("audit log does not contain '" + expected + "':\n" + text);
    }

    public OperationResult await(CompletableFuture<OperationResult> f) throws Exception {
        return f.get(60, TimeUnit.SECONDS);
    }

    /** Stops the runtime (on the main thread) and starts a fresh one on the same directory. */
    public void restart() throws Exception {
        stopRuntime();
        this.runtime = PaperSwapRuntime.bootstrap(this.platform, this.serverDir).orElseThrow();
    }

    public void stopRuntime() throws Exception {
        PaperSwapRuntime r = this.runtime;
        this.platform.onMain(() -> {
            r.stop();
            return null;
        });
    }

    @Override
    public void close() throws Exception {
        try {
            if (!this.runtime.modules().isStopped()) {
                stopRuntime();
            }
        } finally {
            this.platform.shutdown();
        }
    }
}
