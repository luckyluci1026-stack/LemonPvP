package dev.lemonpvp.runtime.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lemonpvp.runtime.plugins.PluginUpdateManager.Result.Kind;
import dev.lemonpvp.runtime.testsupport.ModuleJarBuilder;
import dev.lemonpvp.runtime.testsupport.RuntimeFixture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Plugin update flows against a fake plugin host. The host simulates Paper: versions that
 * contain {@code fail-load} do not load, {@code fail-enable} load but stay disabled, and a
 * jar in the update folder with the same plugin name replaces the jar being loaded (Paper's
 * FileProviderSource does that). What Paper does with a real plugin is tested by the CI
 * server test.
 */
class PluginUpdateTest {

    @TempDir
    Path dir;

    /** In-memory stand-in for Paper's plugin manager. */
    static final class FakeHost implements PluginHost {
        final Path plugins;
        final Path update;
        final Map<String, InstalledPlugin> loaded = new LinkedHashMap<>();
        final List<String> calls = new ArrayList<>();
        String unloadProblem;

        FakeHost(Path serverDir) throws Exception {
            this.plugins = Files.createDirectories(serverDir.resolve("plugins"));
            this.update = Files.createDirectories(this.plugins.resolve("update"));
        }

        void install(Path jar) {
            PluginInfo info = PluginJarInspector.readInfo(jar).orElseThrow();
            this.loaded.put(info.name().toLowerCase(), new InstalledPlugin(info, true));
        }

        @Override
        public List<InstalledPlugin> installed() {
            return List.copyOf(this.loaded.values());
        }

        @Override
        public Path pluginDirectory() {
            return this.plugins;
        }

        @Override
        public Path updateDirectory() {
            return this.update;
        }

        @Override
        public String unloadProblem() {
            return this.unloadProblem;
        }

        /** Resources each loaded plugin reports, and what stays registered after unloading (default: nothing). */
        final Map<String, PluginResources> resources = new LinkedHashMap<>();
        final Map<String, PluginResources> leftovers = new LinkedHashMap<>();
        /** Class loader reported for the next unload of a plugin (to test thread and leak tracking). */
        final Map<String, ClassLoader> loaders = new LinkedHashMap<>();

        @Override
        public PluginResources resources(String name) {
            return this.loaded.containsKey(name.toLowerCase())
                    ? this.resources.getOrDefault(name.toLowerCase(), PluginResources.NONE) : null;
        }

        @Override
        public Unloaded unload(String name) {
            this.calls.add("unload " + name);
            if (this.loaded.remove(name.toLowerCase()) == null) {
                throw new IllegalStateException(name + " not loaded");
            }
            ClassLoader loader = this.loaders.remove(name.toLowerCase());
            return new Unloaded(this.resources.getOrDefault(name.toLowerCase(), PluginResources.NONE),
                    this.leftovers.getOrDefault(name.toLowerCase(), PluginResources.NONE),
                    loader == null ? null : new java.lang.ref.WeakReference<>(loader));
        }

        @Override
        public LoadResult load(Path jar) {
            this.calls.add("load " + jar.getFileName());
            PluginInfo info = PluginJarInspector.readInfo(jar).orElse(null);
            if (info != null) {
                try (Stream<Path> s = Files.list(this.update)) {
                    for (Path u : s.filter(p -> p.toString().endsWith(".jar")).toList()) {
                        if (PluginJarInspector.readInfo(u).map(i -> i.name().equalsIgnoreCase(info.name())).orElse(false)) {
                            Files.move(u, jar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            return load(jar);
                        }
                    }
                } catch (java.io.IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            }
            if (info == null || info.version().contains("fail-load")) {
                return new LoadResult(null, null, false, "simulated load failure");
            }
            boolean enabled = !info.version().contains("fail-enable");
            this.loaded.put(info.name().toLowerCase(), new InstalledPlugin(info, enabled));
            return new LoadResult(info.name(), info.version(), enabled, enabled ? null : "simulated onEnable failure");
        }

        @Override
        public void commandsChanged() {
            this.calls.add("commandsChanged");
        }
    }

    static Path pluginJar(Path dir, String file, String yaml) throws Exception {
        return pluginJar(dir, file, "plugin.yml", yaml, "package tp; public class Main { }");
    }

    static Path pluginJar(Path dir, String file, String descriptorName, String yaml, String mainSource) throws Exception {
        return ModuleJarBuilder.create().noDescriptor().resource(descriptorName, yaml).source("tp.Main", mainSource)
                .writeTo(dir, file, false);
    }

    static String yml(String name, String version, String... extra) {
        return "name: " + name + "\nversion: \"" + version + "\"\nmain: tp.Main\napi-version: \"1.21\"\n" + String.join("\n", extra) + "\n";
    }

    static <T> T await(CompletableFuture<T> f) throws Exception {
        return f.get(60, TimeUnit.SECONDS);
    }

    private RuntimeFixture start(FakeHost[] holder, boolean enabled) throws Exception {
        Path server = this.dir.resolve("server");
        holder[0] = new FakeHost(server);
        return RuntimeFixture.start(server, p -> p.pluginHost(holder[0]), "plugin-updates:", "  enabled: " + enabled);
    }

    private static PluginUpdateManager.Result updateConfirmed(PluginUpdateManager m, String name) throws Exception {
        PluginUpdateManager.Result first = await(m.update("console", name, false));
        if (first.kind() != Kind.CONFIRMATION_REQUIRED) {
            return first;
        }
        return await(m.update("console", name, true));
    }

    private static List<String> names(Path dir) throws Exception {
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".jar")).sorted().toList();
        }
    }

    @Test
    void updateReplacesTheJarKeepsABackupAndCanBeRolledBack() throws Exception {
        FakeHost[] h = new FakeHost[1];
        try (RuntimeFixture f = start(h, true)) {
            FakeHost host = h[0];
            host.install(pluginJar(host.plugins, "TestPlugin-1.0.jar", yml("TestPlugin", "1.0")));
            pluginJar(host.update, "TestPlugin-1.1.jar", yml("TestPlugin", "1.1"));
            PluginUpdateManager m = f.runtime.pluginUpdates().orElseThrow();

            PluginUpdateManager.Candidate c = await(m.check("testplugin"));
            assertEquals(PluginCompatibility.Status.LIVE, c.verdict().status(), c.verdict().reasons().toString());

            PluginUpdateManager.Result plan = await(m.update("console", "TestPlugin", false));
            assertEquals(Kind.CONFIRMATION_REQUIRED, plan.kind(), plan.message());
            assertTrue(host.calls.isEmpty(), "nothing happens before the confirmation");

            PluginUpdateManager.Result r = await(m.update("console", "TestPlugin", true));
            assertEquals(Kind.UPDATED, r.kind(), r.message() + " " + r.details());
            assertEquals(List.of("TestPlugin-1.1.jar"), names(host.plugins));
            assertEquals(List.of(), names(host.update), "the update folder is empty again");
            assertEquals("1.1", host.loaded.get("testplugin").info().version());
            assertEquals(List.of("unload TestPlugin", "load TestPlugin-1.1.jar", "commandsChanged"), host.calls);
            Path backups = f.serverDir.resolve("lemon/plugin-backups/TestPlugin");
            try (Stream<Path> s = Files.walk(backups)) {
                assertTrue(s.anyMatch(p -> p.getFileName().toString().equals("TestPlugin-1.0.jar")), "previous jar is kept");
            }

            assertEquals(Kind.CONFIRMATION_REQUIRED, await(m.rollback("console", "TestPlugin", false)).kind());
            PluginUpdateManager.Result rb = await(m.rollback("console", "TestPlugin", true));
            assertEquals(Kind.UPDATED, rb.kind(), rb.message());
            assertEquals("1.0", host.loaded.get("testplugin").info().version());
            assertEquals(List.of("TestPlugin-1.0.jar"), names(host.plugins));
            f.awaitAudit("plugin-update | TestPlugin | OK");
        }
    }

    @Test
    void leftoversAndRunningThreadsOfTheOldVersionAreReported() throws Exception {
        FakeHost[] h = new FakeHost[1];
        try (RuntimeFixture f = start(h, true)) {
            FakeHost host = h[0];
            host.install(pluginJar(host.plugins, "TestPlugin-1.0.jar", yml("TestPlugin", "1.0")));
            pluginJar(host.update, "TestPlugin-1.1.jar", yml("TestPlugin", "1.1"));
            host.resources.put("testplugin", new PluginResources(3, 1, 2, 0, 1));
            host.leftovers.put("testplugin", new PluginResources(0, 1, 0, 0, 0));
            // A thread whose context class loader is the old plugin's loader, as if onDisable forgot to stop it.
            ClassLoader oldLoader = new java.net.URLClassLoader(new java.net.URL[0], getClass().getClassLoader());
            host.loaders.put("testplugin", oldLoader);
            java.util.concurrent.CountDownLatch stop = new java.util.concurrent.CountDownLatch(1);
            Thread worker = new Thread(() -> {
                try {
                    stop.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "TestPlugin-worker");
            worker.setContextClassLoader(oldLoader);
            worker.start();
            try {
                PluginUpdateManager m = f.runtime.pluginUpdates().orElseThrow();
                assertEquals("3 listeners, 1 scheduler task, 2 commands, 1 messaging channel",
                        m.resources("TestPlugin").orElseThrow().describe());
                PluginUpdateManager.Result r = updateConfirmed(m, "TestPlugin");
                assertEquals(Kind.UPDATED, r.kind(), r.message() + " " + r.details());
                String details = String.join("\n", r.details());
                assertTrue(details.contains("it had 3 listeners, 1 scheduler task, 2 commands, 1 messaging channel"), details);
                assertTrue(details.contains("WARNING: still registered for the unloaded version 1.0: 1 scheduler task"), details);
                assertTrue(details.contains("WARNING: threads of the unloaded version 1.0 are still running [TestPlugin-worker]"), details);
                List<dev.lemonpvp.runtime.metrics.LeakDetector.Entry> previous = m.previousVersions("TestPlugin");
                assertEquals(1, previous.size());
                assertFalse(previous.get(0).collected(), "the thread keeps the old class loader alive");
                assertEquals(List.of("TestPlugin-worker"), previous.get(0).liveThreads());
            } finally {
                stop.countDown();
                worker.join(5000);
            }
        }
    }

    /** Collects command output. */
    static final class Actor implements dev.lemonpvp.runtime.commands.CommandActor {
        final List<String> lines = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public String name() {
            return "admin";
        }

        @Override
        public boolean hasPermission(String node) {
            return true;
        }

        @Override
        public void send(dev.lemonpvp.runtime.commands.MessageLevel level, String text) {
            this.lines.add(level + " " + text);
        }

        void await(String fragment) throws InterruptedException {
            for (int i = 0; i < 200 && this.lines.stream().noneMatch(l -> l.contains(fragment)); i++) {
                Thread.sleep(50);
            }
            assertTrue(this.lines.stream().anyMatch(l -> l.contains(fragment)), "missing '" + fragment + "' in " + this.lines);
        }
    }

    @Test
    void inspectUpdateAllAndDiagnosticsOnTheCommandLevel() throws Exception {
        FakeHost[] h = new FakeHost[1];
        try (RuntimeFixture f = start(h, true)) {
            FakeHost host = h[0];
            host.install(pluginJar(host.plugins, "TestPlugin-1.0.jar", yml("TestPlugin", "1.0")));
            host.install(pluginJar(host.plugins, "Other-2.0.jar", yml("Other", "2.0")));
            pluginJar(host.update, "TestPlugin-1.1.jar", yml("TestPlugin", "1.1"));
            pluginJar(host.update, "Other-2.1.jar", yml("Other", "2.1"));
            host.resources.put("testplugin", new PluginResources(2, 1, 1, 0, 0));
            ClassLoader oldLoader = new java.net.URLClassLoader(new java.net.URL[0], getClass().getClassLoader());
            host.loaders.put("testplugin", oldLoader);
            dev.lemonpvp.runtime.commands.AdminCommand cmd = new dev.lemonpvp.runtime.commands.AdminCommand(f.runtime);
            Actor a = new Actor();
            java.util.function.Consumer<String> run = line -> {
                try {
                    f.platform.onMain(() -> {
                        cmd.execute(a, line.split(" "));
                        return null;
                    });
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            };

            run.accept("live inspect TestPlugin");
            a.await("Registered now: 2 listeners, 1 scheduler task, 1 command");
            a.await("Live update: possible");

            run.accept("live update all confirm");
            a.await("No 'update all' is waiting for confirmation");
            run.accept("live update all");
            a.await("Run /fork live update all confirm within 120 s");
            assertTrue(host.calls.isEmpty(), "nothing happens before the confirmation: " + host.calls);
            run.accept("live update all confirm");
            a.await("All 2 plugin update(s) done.");
            assertEquals("1.1", host.loaded.get("testplugin").info().version());
            assertEquals("2.1", host.loaded.get("other").info().version());

            run.accept("live inspect plugin:TestPlugin");
            a.await("Unloaded version 1.0");
            a.await("class loader still in memory");
            assertTrue(oldLoader != null, "kept reachable on purpose until here");
            run.accept("live diagnostics");
            a.await("Lemon live update diagnostics");
            a.await("Plugins: live updates allowed");
            run.accept("live capabilities");
            a.await("Plugins: Bukkit plugins without dependents can be replaced live");
        }
    }

    @Test
    void anotherJarOfThePluginInTheUpdateFolderBlocksUpdateAndRollback() throws Exception {
        FakeHost[] h = new FakeHost[1];
        try (RuntimeFixture f = start(h, true)) {
            FakeHost host = h[0];
            host.install(pluginJar(host.plugins, "TestPlugin.jar", yml("TestPlugin", "1.0")));
            PluginUpdateManager m = f.runtime.pluginUpdates().orElseThrow();
            pluginJar(host.update, "TestPlugin-1.1.jar", yml("TestPlugin", "1.1"));
            pluginJar(host.update, "TestPlugin-1.2.jar", yml("TestPlugin", "1.2"));

            PluginUpdateManager.Result r = await(m.update("console", "TestPlugin", false));
            assertEquals(Kind.REJECTED, r.kind(), r.message());
            assertTrue(r.details().stream().anyMatch(d -> d.contains("Paper would load that instead")), r.details().toString());
            assertTrue(host.calls.isEmpty(), "nothing was unloaded");

            Files.delete(host.update.resolve("TestPlugin-1.2.jar"));
            assertEquals(Kind.UPDATED, updateConfirmed(m, "TestPlugin").kind());
            assertEquals("1.1", host.loaded.get("testplugin").info().version());

            // A pending jar of the same plugin would be loaded by Paper instead of the backup.
            pluginJar(host.update, "TestPlugin-1.3.jar", yml("TestPlugin", "1.3"));
            PluginUpdateManager.Result rb = await(m.rollback("console", "TestPlugin", false));
            assertEquals(Kind.REJECTED, rb.kind(), rb.message());
            assertTrue(rb.details().stream().anyMatch(d -> d.contains("TestPlugin-1.3.jar")), rb.details().toString());
            assertEquals("1.1", host.loaded.get("testplugin").info().version(), "nothing changed");
        }
    }

    @Test
    void failedEnableOrLoadRestoresThePreviousVersion() throws Exception {
        FakeHost[] h = new FakeHost[1];
        try (RuntimeFixture f = start(h, true)) {
            FakeHost host = h[0];
            host.install(pluginJar(host.plugins, "TestPlugin-1.0.jar", yml("TestPlugin", "1.0")));
            PluginUpdateManager m = f.runtime.pluginUpdates().orElseThrow();

            pluginJar(host.update, "TestPlugin-1.1.jar", yml("TestPlugin", "1.1-fail-enable"));
            PluginUpdateManager.Result r = updateConfirmed(m, "TestPlugin");
            assertEquals(Kind.RESTORED, r.kind(), r.message());
            assertTrue(r.message().contains("simulated onEnable failure") && r.message().contains("was restored and is enabled"), r.message());
            assertEquals("1.0", host.loaded.get("testplugin").info().version());
            assertTrue(host.loaded.get("testplugin").enabled());
            assertEquals(List.of("TestPlugin-1.0.jar"), names(host.plugins), "the failed jar left plugins/");

            pluginJar(host.update, "TestPlugin-1.2.jar", yml("TestPlugin", "1.2-fail-load"));
            r = updateConfirmed(m, "TestPlugin");
            assertEquals(Kind.RESTORED, r.kind(), r.message());
            assertEquals("1.0", host.loaded.get("testplugin").info().version());

            PluginUpdateManager.Result rb = await(m.rollback("console", "TestPlugin", false));
            assertEquals(Kind.REJECTED, rb.kind(), "failed attempts are no rollback target: " + rb.message());
            assertTrue(rb.message().contains("no backup"), rb.message());
        }
    }

    @Test
    void sameFileNameAsTheOldJarIsHandled() throws Exception {
        FakeHost[] h = new FakeHost[1];
        try (RuntimeFixture f = start(h, true)) {
            FakeHost host = h[0];
            host.install(pluginJar(host.plugins, "TestPlugin.jar", yml("TestPlugin", "1.0")));
            pluginJar(host.update, "TestPlugin.jar", yml("TestPlugin", "1.1-fail-enable"));
            PluginUpdateManager.Result r = updateConfirmed(f.runtime.pluginUpdates().orElseThrow(), "TestPlugin");
            assertEquals(Kind.RESTORED, r.kind(), r.message());
            assertEquals("1.0", PluginJarInspector.readInfo(host.plugins.resolve("TestPlugin.jar")).orElseThrow().version(),
                    "the restored file is the old version, not the failed one");
        }
    }

    @Test
    void unsafeUpdatesNeedARestartOrAreIncompatible() throws Exception {
        FakeHost[] h = new FakeHost[1];
        try (RuntimeFixture f = start(h, true)) {
            FakeHost host = h[0];
            host.install(pluginJar(host.plugins, "Core-1.0.jar", yml("Core", "1.0")));
            host.install(pluginJar(host.plugins, "Addon-1.0.jar", yml("Addon", "1.0", "depend: [Core]")));
            PluginUpdateManager m = f.runtime.pluginUpdates().orElseThrow();

            pluginJar(host.update, "Core-1.1.jar", yml("Core", "1.1"));
            PluginUpdateManager.Candidate c = await(m.check("Core"));
            assertEquals(PluginCompatibility.Status.RESTART_REQUIRED, c.verdict().status());
            assertTrue(c.verdict().reasons().toString().contains("[Addon]"), c.verdict().reasons().toString());
            PluginUpdateManager.Result r = await(m.update("console", "Core", false));
            assertEquals(Kind.REJECTED, r.kind());
            assertTrue(host.calls.isEmpty());

            pluginJar(host.update, "Needy-1.0.jar", yml("Needy", "1.0", "depend: [Vault]"));
            assertEquals(PluginCompatibility.Status.INCOMPATIBLE, await(m.check("Needy")).verdict().status());

            pluginJar(host.update, "Future-1.0.jar", yml("Future", "1.0").replace("api-version: \"1.21\"", "api-version: \"99.0\""));
            c = await(m.check("Future"));
            assertEquals(PluginCompatibility.Status.INCOMPATIBLE, c.verdict().status());
            assertTrue(c.verdict().reasons().toString().contains("api-version 99.0"), c.verdict().reasons().toString());

            pluginJar(host.update, "PaperOne-1.0.jar", "paper-plugin.yml", yml("PaperOne", "1.0"), "package tp; public class Main { }");
            c = await(m.check("PaperOne"));
            assertEquals(PluginCompatibility.Status.RESTART_REQUIRED, c.verdict().status());
            assertTrue(c.verdict().reasons().toString().contains("Paper plugin"), c.verdict().reasons().toString());

            pluginJar(host.update, "Brig-1.0.jar", "plugin.yml", yml("Brig", "1.0"),
                    "package tp; public class Main { String api = \"io/papermc/paper/plugin/lifecycle/event/LifecycleEventManager\"; }");
            c = await(m.check("Brig"));
            assertEquals(PluginCompatibility.Status.RESTART_REQUIRED, c.verdict().status());
            assertTrue(c.verdict().reasons().toString().contains("lifecycle API"), c.verdict().reasons().toString());

            pluginJar(host.update, "NoMain-1.0.jar", yml("NoMain", "1.0").replace("main: tp.Main", "main: tp.Missing"));
            c = await(m.check("NoMain"));
            assertEquals(PluginCompatibility.Status.INCOMPATIBLE, c.verdict().status());
            assertTrue(c.verdict().reasons().toString().contains("main class tp.Missing is not in the jar"));
        }
    }

    @Test
    void installsNewPluginsAndRejectsBrokenFiles() throws Exception {
        FakeHost[] h = new FakeHost[1];
        try (RuntimeFixture f = start(h, true)) {
            FakeHost host = h[0];
            PluginUpdateManager m = f.runtime.pluginUpdates().orElseThrow();
            pluginJar(host.update, "Fresh-2.0.jar", yml("Fresh", "2.0"));
            PluginUpdateManager.Result r = updateConfirmed(m, "Fresh");
            assertEquals(Kind.UPDATED, r.kind(), r.message());
            assertTrue(r.message().startsWith("install of Fresh"), r.message());
            assertEquals(List.of("Fresh-2.0.jar"), names(host.plugins));

            Files.writeString(host.update.resolve("broken.jar"), "not a zip");
            ModuleJarBuilder.create().descriptor("amodule", "1.0.0", "mod.a.Main").writeTo(host.update, "amodule.jar", false);
            List<PluginUpdateManager.Candidate> scan = await(m.scan());
            assertEquals(2, scan.size());
            assertTrue(scan.stream().allMatch(c -> c.verdict().status() == PluginCompatibility.Status.INCOMPATIBLE));
            assertTrue(scan.stream().anyMatch(c -> c.verdict().reasons().toString().contains("this is a Lemon module")), scan.toString());
        }
    }

    @Test
    void disabledOrUnsupportedHostsChangeNothing() throws Exception {
        FakeHost[] h = new FakeHost[1];
        try (RuntimeFixture f = start(h, false)) {
            FakeHost host = h[0];
            host.install(pluginJar(host.plugins, "TestPlugin-1.0.jar", yml("TestPlugin", "1.0")));
            pluginJar(host.update, "TestPlugin-1.1.jar", yml("TestPlugin", "1.1"));
            PluginUpdateManager m = f.runtime.pluginUpdates().orElseThrow();
            assertEquals(PluginCompatibility.Status.LIVE, await(m.check("TestPlugin")).verdict().status(), "checking works while disabled");
            PluginUpdateManager.Result r = await(m.update("console", "TestPlugin", false));
            assertEquals(Kind.REJECTED, r.kind());
            assertTrue(r.message().contains("plugin-updates.enabled: false"));

            host.unloadProblem = "simulated: internals changed";
            PluginUpdateManager.Candidate c = await(m.check("TestPlugin"));
            assertEquals(PluginCompatibility.Status.RESTART_REQUIRED, c.verdict().status());
            assertTrue(host.calls.isEmpty());
            assertTrue(c.installedFile().isPresent(), "the installed jar was found in plugins/");
        }
    }

    @Test
    void versionsCompareNumerically() {
        assertTrue(PluginCompatibility.compareVersions("1.21", "1.21.11") < 0);
        assertTrue(PluginCompatibility.compareVersions("1.21.11", "26.1") < 0);
        assertTrue(PluginCompatibility.compareVersions("26.2", "26.10") < 0);
        assertEquals(0, PluginCompatibility.compareVersions("1.0", "1.0.0"));
    }

    @Test
    void repeatedRollbacksGoFurtherBackAndDoNotNeedTheAllowList() throws Exception {
        Path server = this.dir.resolve("server");
        FakeHost host = new FakeHost(server);
        host.install(pluginJar(host.plugins, "TestPlugin.jar", yml("TestPlugin", "1.0")));
        Path staging = Files.createDirectories(this.dir.resolve("staging"));
        Path v11 = pluginJar(staging.resolve("11"), "TestPlugin.jar", yml("TestPlugin", "1.1"));
        Path v12 = pluginJar(staging.resolve("12"), "TestPlugin.jar", yml("TestPlugin", "1.2"));
        // Only the two new versions are approved; 1.0 was installed by hand long ago.
        Files.writeString(server.resolve("trusted-plugins.txt"), dev.lemonpvp.runtime.security.Hashing.sha256(v11) + "\n"
                + dev.lemonpvp.runtime.security.Hashing.sha256(v12) + "\n");
        try (RuntimeFixture f = RuntimeFixture.start(server, p -> p.pluginHost(host), "plugin-updates:", "  enabled: true",
                "  trusted-hashes-file: trusted-plugins.txt")) {
            PluginUpdateManager m = f.runtime.pluginUpdates().orElseThrow();
            Files.copy(v11, host.update.resolve("TestPlugin.jar"));
            assertEquals(Kind.UPDATED, updateConfirmed(m, "TestPlugin").kind());
            Files.copy(v12, host.update.resolve("TestPlugin.jar"));
            assertEquals(Kind.UPDATED, updateConfirmed(m, "TestPlugin").kind());
            assertEquals("1.2", host.loaded.get("testplugin").info().version());

            assertEquals(Kind.CONFIRMATION_REQUIRED, await(m.rollback("console", "TestPlugin", false)).kind());
            PluginUpdateManager.Result first = await(m.rollback("console", "TestPlugin", true));
            assertEquals(Kind.UPDATED, first.kind(), first.message() + " " + first.details());
            assertEquals("1.1", host.loaded.get("testplugin").info().version());

            PluginUpdateManager.Result plan = await(m.rollback("console", "TestPlugin", false));
            assertEquals(Kind.CONFIRMATION_REQUIRED, plan.kind(), "1.0 is not on the allow-list but was backed up with its digest: "
                    + plan.message() + " " + plan.details());
            PluginUpdateManager.Result second = await(m.rollback("console", "TestPlugin", true));
            assertEquals(Kind.UPDATED, second.kind(), second.message());
            assertEquals("1.0", host.loaded.get("testplugin").info().version(), "the second rollback goes further back");

            PluginUpdateManager.Result none = await(m.rollback("console", "TestPlugin", false));
            assertEquals(Kind.REJECTED, none.kind(), none.message());
            assertTrue(names(host.update).isEmpty());
            try (Stream<Path> s = Files.list(f.serverDir.resolve("lemon/plugin-staging"))) {
                assertEquals(0, s.count(), "staged copies are removed");
            }
        }
    }
}
