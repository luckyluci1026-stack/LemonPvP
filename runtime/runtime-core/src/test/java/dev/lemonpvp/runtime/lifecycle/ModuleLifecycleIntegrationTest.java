package dev.lemonpvp.runtime.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lemonpvp.runtime.api.ModuleState;
import dev.lemonpvp.runtime.metrics.ModuleDiagnostics;
import dev.lemonpvp.runtime.testsupport.ModuleJarBuilder;
import dev.lemonpvp.runtime.testsupport.RuntimeFixture;
import dev.lemonpvp.runtime.testsupport.TestModules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import testplatform.Gate;
import testplatform.Probe;

/**
 * End-to-end lifecycle tests with real module jars compiled at test time, a real class
 * loader per module and a simulated server main thread.
 */
class ModuleLifecycleIntegrationTest {

    private static final String A = RuntimeFixture.ADMIN;

    @TempDir
    Path dir;

    @BeforeEach
    void reset() {
        Probe.clear();
        Gate.reset();
    }

    private static Path basic(Path modules, String id, String version, String tag, String... extra) throws Exception {
        String pkg = "mod." + id.replace('-', '_') + ".v" + version.replace('.', '_');
        return ModuleJarBuilder.create()
                .descriptor(id, version, pkg + ".Main", extra)
                .source(pkg + ".Main", TestModules.basic(pkg, tag))
                .writeTo(modules, id + ".jar");
    }

    private static ModuleState state(RuntimeFixture f, String id) {
        return f.runtime.modules().modules().stream().filter(m -> m.id().equals(id)).map(ModuleSummary::state)
                .findFirst().orElse(null);
    }

    @Test
    void validModuleGoesThroughTheWholeLifecycleAndReleasesEverything() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            basic(f.modules, "alpha", "1.0.0", "alpha");

            OperationResult load = f.await(f.runtime.modules().load(A, "alpha"));
            assertTrue(load.success(), load.toString());
            assertEquals(ModuleState.LOADED, state(f, "alpha"));
            assertTrue(Probe.events().contains("alpha:load"));
            assertEquals(0, f.platform.listeners.size(), "onLoad must not register anything");

            OperationResult enable = f.await(f.runtime.modules().enable(A, "alpha"));
            assertTrue(enable.success(), enable.toString());
            assertEquals(ModuleState.ENABLED, state(f, "alpha"));
            assertEquals(1, f.platform.listeners.size());
            assertEquals(1, f.platform.tasks.size());
            assertEquals("alpha", f.platform.commands.get("alpha-cmd"));
            assertTrue(f.platform.commandSyncs() >= 1, "client command trees must be refreshed");

            f.platform.fire("join", "steve");
            f.platform.tick();
            assertTrue(Probe.events().contains("alpha:join:steve"));
            assertTrue(Probe.events().contains("alpha:tick"));

            OperationResult disable = f.await(f.runtime.modules().disable(A, "alpha"));
            assertTrue(disable.success(), disable.toString());
            assertEquals(ModuleState.DISABLED, state(f, "alpha"));
            assertEquals(0, f.platform.listeners.size(), "listeners must be unregistered");
            assertEquals(0, f.platform.tasks.size(), "tasks must be cancelled");
            assertTrue(f.platform.commands.isEmpty(), "commands must be removed");
            List<String> ev = Probe.events();
            assertTrue(ev.indexOf("alpha:disable") < ev.indexOf("alpha:cleanup"), "onDisable runs before resource cleanup");

            int before = Probe.events().size();
            f.platform.fire("join", "alex");
            assertEquals(before, Probe.events().size(), "a disabled module must not receive events");

            OperationResult unload = f.await(f.runtime.modules().unload(A, "alpha"));
            assertTrue(unload.success(), unload.toString());
            assertTrue(f.runtime.modules().modules().isEmpty());
            ModuleDiagnostics d = f.runtime.modules().diagnostics("alpha").orElseThrow();
            assertEquals(1, d.previousInstances().size());
            f.awaitAudit("| tester | unload | alpha | OK |");
        }
    }

    @Test
    void repeatedAndConcurrentActivationIsPrevented() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            basic(f.modules, "twice", "1.0.0", "twice");
            CompletableFuture<OperationResult> first = f.runtime.modules().enable(A, "twice");
            CompletableFuture<OperationResult> second = f.runtime.modules().enable(A, "twice");
            OperationResult r1 = f.await(first);
            OperationResult r2 = f.await(second);
            assertTrue(r1.success() ^ r2.success(), r1 + " / " + r2);
            assertTrue((r1.success() ? r2 : r1).message().contains("in progress"));

            OperationResult again = f.await(f.runtime.modules().enable(A, "twice"));
            assertFalse(again.success());
            assertTrue(again.message().contains("already enabled"), again.message());
            assertEquals(1, f.platform.listeners.size(), "no duplicate registrations");
        }
    }

    @Test
    void invalidModulesAreRejectedWithReasons() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            ModuleJarBuilder.create().noDescriptor().resource("readme.txt", "x").writeTo(f.modules, "nodesc.jar");
            Files.writeString(f.modules.resolve("garbage.jar"), "this is not a zip");
            ModuleJarBuilder.create().rawDescriptor("id: Bad Id\nversion: one\nmain: X\n").writeTo(f.modules, "bad.jar");
            ModuleJarBuilder.create().descriptor("notmodule", "1.0.0", "mod.nm.Main")
                    .source("mod.nm.Main", TestModules.broken("mod.nm", "not-a-module")).writeTo(f.modules, "notmodule.jar");

            var scan = f.runtime.modules().scan().get();
            assertEquals(1, scan.artifacts().size(), "only the syntactically valid module is discovered");
            assertTrue(scan.problems().stream().anyMatch(p -> p.message().contains("missing module.yml (or lemon-module.yml)")));
            assertTrue(scan.problems().stream().anyMatch(p -> p.file().equals("garbage.jar")));
            assertTrue(scan.problems().stream().anyMatch(p -> p.message().contains("not a semantic version")));

            OperationResult r = f.await(f.runtime.modules().enable(A, "notmodule"));
            assertFalse(r.success());
            assertTrue(r.message().contains("does not implement"), r.message());
            assertTrue(f.runtime.modules().modules().isEmpty(), "failed loads leave nothing registered");
        }
    }

    @Test
    void incompatibleApiAndMinecraftVersionsAreRejected() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            ModuleJarBuilder.create().rawDescriptor("id: future\nversion: 1.0.0\nmain: a.b.Main\napi-version: 2\n"
                    + "minecraft-versions: [\"*\"]\n").writeTo(f.modules, "future.jar");
            OperationResult api = f.await(f.runtime.modules().validate(A, "future"));
            assertFalse(api.success());
            assertTrue(api.details().stream().anyMatch(d -> d.contains("requires module API 2")), api.toString());

            basic(f.modules, "old", "1.0.0", "old", "minecraft-versions: [\"1.21.11\"]");
            OperationResult mc = f.await(f.runtime.modules().enable(A, "old"));
            assertFalse(mc.success());
            assertTrue(mc.details().stream().anyMatch(d -> d.contains("server runs 26.2")), mc.toString());

            basic(f.modules, "wild", "1.0.0", "wild", "minecraft-versions: [\"26.*\"]");
            assertTrue(f.await(f.runtime.modules().enable(A, "wild")).success());
        }
    }

    @Test
    void versionPatternsFollowTheRunningServer() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            f.platform.minecraftVersion("1.21.11");
            basic(f.modules, "legacy", "1.0.0", "legacy", "minecraft-versions: [\"1.21.*\"]");
            basic(f.modules, "modern", "1.0.0", "modern", "minecraft-versions: [\"26.*\"]");
            assertTrue(f.await(f.runtime.modules().validate(A, "legacy")).success());
            assertFalse(f.await(f.runtime.modules().validate(A, "modern")).success());
        }
    }

    @Test
    void dependenciesAreResolvedAndProtected() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            Path provider = ModuleJarBuilder.create().descriptor("provider", "1.2.0", "prov.Main")
                    .source("prov.Api", "package prov; public final class Api { public static String value() { return \"from-provider\"; } }")
                    .source("prov.Main", "package prov; public class Main implements dev.lemonpvp.runtime.api.RuntimeModule {"
                            + " public void onEnable(dev.lemonpvp.runtime.api.ModuleContext c) { } }")
                    .writeTo(f.modules, "provider.jar");
            ModuleJarBuilder.create().descriptor("consumer", "1.0.0", "cons.Main", "dependencies:",
                            "  - id: provider", "    version: \">=1.0.0 <2.0.0\"")
                    .compileAgainst(provider)
                    .source("cons.Main", "package cons; public class Main implements dev.lemonpvp.runtime.api.RuntimeModule {"
                            + " public void onEnable(dev.lemonpvp.runtime.api.ModuleContext c) { testplatform.Probe.record(\"consumer:\" + prov.Api.value()); } }")
                    .writeTo(f.modules, "consumer.jar");

            OperationResult missing = f.await(f.runtime.modules().enable(A, "consumer"));
            assertFalse(missing.success());
            assertTrue(missing.details().stream().anyMatch(d -> d.contains("requires module 'provider'")), missing.toString());

            assertTrue(f.await(f.runtime.modules().enable(A, "provider")).success());
            OperationResult ok = f.await(f.runtime.modules().enable(A, "consumer"));
            assertTrue(ok.success(), ok.toString());
            assertTrue(Probe.events().contains("consumer:from-provider"), "consumer uses the provider's classes");

            OperationResult refused = f.await(f.runtime.modules().disable(A, "provider"));
            assertFalse(refused.success());
            assertTrue(refused.message().contains("depend on it"), refused.message());
            assertFalse(f.await(f.runtime.modules().unload(A, "provider")).success());
            assertFalse(f.await(f.runtime.modules().reload(A, "provider")).success());

            assertTrue(f.await(f.runtime.modules().unload(A, "consumer")).success());
            assertTrue(f.await(f.runtime.modules().unload(A, "provider")).success());
        }
    }

    @Test
    void duplicateModuleIdsAreRefused() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            basic(f.modules, "dup", "1.0.0", "dup");
            Files.copy(f.modules.resolve("dup.jar"), f.modules.resolve("dup-copy.jar"));
            ModuleJarBuilder.writeSidecar(f.modules.resolve("dup-copy.jar"));
            OperationResult r = f.await(f.runtime.modules().enable(A, "dup"));
            assertFalse(r.success());
            assertTrue(r.details().stream().anyMatch(d -> d.contains("duplicate module id 'dup'")), r.toString());
        }
    }

    @Test
    void failingEnableReleasesPartialRegistrationsAndServerKeepsRunning() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            ModuleJarBuilder.create().descriptor("faulty", "1.0.0", "mod.faulty.Main")
                    .source("mod.faulty.Main", TestModules.failingEnable("mod.faulty", "faulty")).writeTo(f.modules, "faulty.jar");
            OperationResult r = f.await(f.runtime.modules().enable(A, "faulty"));
            assertFalse(r.success());
            assertTrue(r.message().contains("boom from faulty"), r.message());
            assertEquals(ModuleState.FAILED, state(f, "faulty"), "failure stays visible");
            assertEquals(0, f.platform.listeners.size());
            assertEquals(0, f.platform.tasks.size());
            assertTrue(f.platform.commands.isEmpty());
            assertFalse(f.runtime.health().restartRecommended(), "cleanup was complete");

            for (String how : List.of("constructor", "static", "onload")) {
                String id = "broken-" + how;
                String pkg = "mod.broken_" + how;
                ModuleJarBuilder.create().descriptor(id, "1.0.0", pkg + ".Main")
                        .source(pkg + ".Main", TestModules.broken(pkg, how)).writeTo(f.modules, id + ".jar");
                OperationResult b = f.await(f.runtime.modules().enable(A, id));
                assertFalse(b.success(), how);
                assertTrue(state(f, id) == null, how + " must not stay registered");
            }

            basic(f.modules, "healthy", "1.0.0", "healthy");
            assertTrue(f.await(f.runtime.modules().enable(A, "healthy")).success(), "other modules still work");
            assertTrue(f.await(f.runtime.modules().unload(A, "faulty")).success(), "failed instance can be unloaded");
        }
    }

    @Test
    void failedLoadDuringAReloadOfTheSameJarKeepsTheRunningVersionsStagedCopy() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            ModuleJarBuilder.create().descriptor("loadfail", "1.0.0", "mod.loadfail.Main")
                    .source("mod.loadfail.Main", TestModules.failingLoadOnConfig("mod.loadfail", "lf"))
                    .writeTo(f.modules, "loadfail.jar");
            assertTrue(f.await(f.runtime.modules().enable(A, "loadfail")).success());
            List<Path> staged;
            try (var files = Files.list(f.runtime.paths().staging())) {
                staged = files.filter(p -> p.getFileName().toString().endsWith(".jar")).toList();
            }
            assertEquals(1, staged.size(), staged.toString());

            // Same jar, so the new instance is staged at the same path as the running one.
            Files.writeString(f.runtime.paths().moduleData("loadfail").resolve("config.yml"), "fail-on-load: true\n");
            OperationResult r = f.await(f.runtime.modules().reload(A, "loadfail"));
            assertFalse(r.success());
            assertTrue(r.message().contains("loading failed"), r.message());
            assertEquals(ModuleState.ENABLED, state(f, "loadfail"), "the running version is untouched");
            assertTrue(Files.exists(staged.getFirst()), "the staged jar of the running version must not be deleted");

            assertTrue(f.await(f.runtime.modules().unload(A, "loadfail")).success());
            assertFalse(Files.exists(staged.getFirst()), "deleted once no instance uses it");
        }
    }

    @Test
    void hotSwapRollsBackWhenTheNewVersionFailsAndSwitchesWhenItWorks() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            basic(f.modules, "swap", "1.0.0", "v1");
            assertTrue(f.await(f.runtime.modules().enable(A, "swap")).success());

            ModuleJarBuilder.create().descriptor("swap", "2.0.0", "mod.swap.v2.Main")
                    .source("mod.swap.v2.Main", TestModules.failingEnable("mod.swap.v2", "v2")).writeTo(f.modules, "swap.jar");
            OperationResult failed = f.await(f.runtime.modules().reload(A, "swap"));
            assertFalse(failed.success());
            assertTrue(failed.message().contains("rolled back to swap@1.0.0"), failed.message());
            assertTrue(failed.details().stream().anyMatch(d -> d.contains("boom from v2")),
                    "the administrator sees why the new version failed: " + failed.details());
            assertEquals(ModuleState.ENABLED, state(f, "swap"));
            assertEquals("1.0.0", f.runtime.modules().descriptor("swap").orElseThrow().version());
            assertEquals(1, f.platform.listeners.size(), "old version re-registered exactly once");
            assertEquals(2, Probe.events().stream().filter(e -> e.startsWith("v1:enable")).count());

            Probe.clear();
            basic(f.modules, "swap", "3.0.0", "v3");
            OperationResult ok = f.await(f.runtime.modules().reload(A, "swap"));
            assertTrue(ok.success(), ok.toString());
            assertEquals("3.0.0", f.runtime.modules().descriptor("swap").orElseThrow().version());
            List<String> ev = Probe.events();
            assertTrue(ev.indexOf("v3:load") < ev.indexOf("v1:disable"), "new version is loaded before the old one stops");
            assertTrue(ev.indexOf("v1:disable") < ev.indexOf("v3:enable:none"), "old version stops before the new one starts");
            assertEquals(1, f.platform.listeners.size());
            f.platform.fire("join", "x");
            assertTrue(Probe.events().contains("v3:join:x"));
            assertFalse(Probe.events().contains("v1:join:x"));
        }
    }

    @Test
    void hotSwapKeepsTheOldVersionUntouchedWhenTheNewOneCannotLoad() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            basic(f.modules, "keep", "1.0.0", "k1");
            assertTrue(f.await(f.runtime.modules().enable(A, "keep")).success());
            ModuleJarBuilder.create().descriptor("keep", "2.0.0", "mod.keep2.Main")
                    .source("mod.keep2.Main", TestModules.broken("mod.keep2", "onload")).writeTo(f.modules, "keep.jar");
            OperationResult r = f.await(f.runtime.modules().reload(A, "keep"));
            assertFalse(r.success());
            assertEquals(ModuleState.ENABLED, state(f, "keep"));
            assertFalse(Probe.events().contains("k1:disable"), "old version was never stopped");
        }
    }

    @Test
    void runningAsyncWorkPreventsUnloadUntilItFinishes() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            ModuleJarBuilder.create().descriptor("worker", "1.0.0", "mod.worker.Main")
                    .source("mod.worker.Main", """
                            package mod.worker;
                            public class Main implements dev.lemonpvp.runtime.api.RuntimeModule {
                                public void onEnable(dev.lemonpvp.runtime.api.ModuleContext c) {
                                    c.platform(testplatform.FakeServerApi.class).scheduleAsync(testplatform.Gate::await);
                                }
                            }
                            """).writeTo(f.modules, "worker.jar");
            assertTrue(f.await(f.runtime.modules().enable(A, "worker")).success());
            assertTrue(Gate.awaitEntered(), "the async task started");
            OperationResult disable = f.await(f.runtime.modules().disable(A, "worker"));
            assertTrue(disable.success(), disable.toString());
            assertTrue(disable.details().stream().anyMatch(d -> d.contains("still running")), disable.toString());

            OperationResult blocked = f.await(f.runtime.modules().unload(A, "worker"));
            assertFalse(blocked.success());
            assertTrue(blocked.message().contains("running asynchronous work"), blocked.message());

            Gate.open();
            for (int i = 0; i < 50 && !f.runtime.modules().diagnostics("worker").orElseThrow().runningAsyncWork().isEmpty(); i++) {
                Thread.sleep(100);
            }
            assertTrue(f.await(f.runtime.modules().unload(A, "worker")).success());
        }
    }

    @Test
    void refusedUnloadOfAnEnabledModuleSaysItWasDisabledAndPersistsThat() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            ModuleJarBuilder.create().descriptor("worker", "1.0.0", "mod.worker.Main")
                    .source("mod.worker.Main", """
                            package mod.worker;
                            public class Main implements dev.lemonpvp.runtime.api.RuntimeModule {
                                public void onEnable(dev.lemonpvp.runtime.api.ModuleContext c) {
                                    c.platform(testplatform.FakeServerApi.class).scheduleAsync(testplatform.Gate::await);
                                }
                            }
                            """).writeTo(f.modules, "worker.jar");
            assertTrue(f.await(f.runtime.modules().enable(A, "worker")).success());
            assertTrue(Gate.awaitEntered(), "the async task started");
            assertTrue(Files.readString(f.runtime.paths().stateFile()).contains("module.worker.sha256"));

            OperationResult blocked = f.await(f.runtime.modules().unload(A, "worker"));
            assertFalse(blocked.success());
            assertTrue(blocked.message().contains("running asynchronous work"), blocked.message());
            assertTrue(blocked.details().stream().anyMatch(d -> d.contains("still running")), blocked.toString());
            assertTrue(blocked.details().stream().anyMatch(d -> d.contains("was disabled")), blocked.toString());
            assertEquals(ModuleState.DISABLED, state(f, "worker"));
            for (int i = 0; i < 50 && Files.readString(f.runtime.paths().stateFile()).contains("module.worker.sha256"); i++) {
                Thread.sleep(100);
            }
            assertFalse(Files.readString(f.runtime.paths().stateFile()).contains("module.worker.sha256"),
                    "a restart must not re-activate a module the administrator tried to unload");

            Gate.open();
            for (int i = 0; i < 50 && !f.runtime.modules().diagnostics("worker").orElseThrow().runningAsyncWork().isEmpty(); i++) {
                Thread.sleep(100);
            }
            assertTrue(f.await(f.runtime.modules().unload(A, "worker")).success());
        }
    }

    @Test
    void runtimeActivationsAreRestoredAfterRestartOnlyForUnchangedJars() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            basic(f.modules, "persist", "1.0.0", "p1");
            assertTrue(f.await(f.runtime.modules().enable(A, "persist")).success());
            assertTrue(Files.readString(f.runtime.paths().stateFile()).contains("module.persist.sha256"));

            f.restart();
            f.runtime.activateStartupModules().get();
            assertEquals(ModuleState.ENABLED, state(f, "persist"), "restored after restart");

            f.restart();
            basic(f.modules, "persist", "1.0.1", "p2"); // jar changed while the server was down
            f.runtime.activateStartupModules().get();
            assertEquals(null, state(f, "persist"), "a changed jar is never activated implicitly");
            assertTrue(f.runtime.modules().lastFailures().get("persist").message().contains("changed since it was activated"));

            assertTrue(f.await(f.runtime.modules().enable(A, "persist")).success(), "explicit activation works");
            assertTrue(f.await(f.runtime.modules().disable(A, "persist")).success());
            assertFalse(Files.readString(f.runtime.paths().stateFile()).contains("module.persist"), "disable removes persistence");
        }
    }

    @Test
    void withoutRuntimeDeployOnlyStartupModulesAreActivated() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir, "allow-runtime-deploy: false", "startup-modules: [boot]")) {
            basic(f.modules, "boot", "1.0.0", "boot");
            basic(f.modules, "other", "1.0.0", "other");
            f.runtime.activateStartupModules().get();
            assertEquals(ModuleState.ENABLED, state(f, "boot"));
            OperationResult r = f.await(f.runtime.modules().enable(A, "other"));
            assertFalse(r.success());
            assertTrue(r.message().contains("allow-runtime-deploy"), r.message());
            assertFalse(f.await(f.runtime.modules().disable(A, "boot")).success());
            assertTrue(f.await(f.runtime.modules().reloadConfig(A, "boot")).success(), "configuration reload stays possible");
        }
    }

    @Test
    void integrityChecksRejectTamperedOrUnlistedJars() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir, "trusted-hashes-file: lemon/trusted.txt")) {
            Path jar = basic(f.modules, "signed", "1.0.0", "signed");
            Files.writeString(this.dir.resolve("lemon/trusted.txt"), "# none yet\n");
            OperationResult notListed = f.await(f.runtime.modules().enable(A, "signed"));
            assertFalse(notListed.success());
            assertTrue(notListed.message().contains("is not listed"), notListed.message());

            String sha = dev.lemonpvp.runtime.security.Hashing.sha256(jar);
            Files.writeString(this.dir.resolve("lemon/trusted.txt"), sha + " signed 1.0.0\n");
            Files.writeString(f.modules.resolve("signed.jar.sha256"), "0".repeat(64) + "  signed.jar\n");
            OperationResult mismatch = f.await(f.runtime.modules().enable(A, "signed"));
            assertFalse(mismatch.success());
            assertTrue(mismatch.message().contains("SHA-256 mismatch"), mismatch.message());

            Files.delete(f.modules.resolve("signed.jar.sha256"));
            OperationResult missing = f.await(f.runtime.modules().enable(A, "signed"));
            assertTrue(missing.message().contains("missing signed.jar.sha256"), missing.message());

            ModuleJarBuilder.writeSidecar(jar);
            assertTrue(f.await(f.runtime.modules().enable(A, "signed")).success());
        }
    }

    @Test
    void stagedCopyIsolatesTheRunningModuleFromFileChanges() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            Path jar = basic(f.modules, "iso", "1.0.0", "iso");
            assertTrue(f.await(f.runtime.modules().enable(A, "iso")).success());
            String sha = f.runtime.modules().diagnostics("iso").orElseThrow().sha256();
            Files.writeString(jar, "overwritten while running");
            f.platform.fire("join", "p");
            assertTrue(Probe.events().contains("iso:join:p"));
            assertEquals(sha, f.runtime.modules().diagnostics("iso").orElseThrow().sha256());
            assertTrue(f.await(f.runtime.modules().disable(A, "iso")).success());
        }
    }

    @Test
    void manipulatedIdsAndPathsAreRejected() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            for (String bad : List.of("../evil", "a/b", "..", "ALPHA", "x", "minecraft", "a\nb")) {
                OperationResult r = f.await(f.runtime.modules().enable(A, bad));
                assertFalse(r.success(), bad);
            }
            ModuleJarBuilder.create().descriptor("escape", "1.0.0", "mod.esc.Main", "resources: [\"../../escape.yml\"]")
                    .source("mod.esc.Main", TestModules.broken("mod.esc", "onload")).writeTo(f.modules, "escape.jar");
            var scan = f.runtime.modules().scan().get();
            assertTrue(scan.problems().stream().anyMatch(p -> p.message().contains("not a safe relative path")));

            Path target = basic(this.dir.resolve("elsewhere"), "linked", "1.0.0", "linked");
            Files.createSymbolicLink(f.modules.resolve("linked.jar"), target);
            scan = f.runtime.modules().scan().get();
            assertTrue(scan.problems().stream().anyMatch(p -> p.file().equals("linked.jar")
                    && (p.message().contains("symbolic links") || p.message().contains("via a link"))), scan.problems().toString());
            assertTrue(scan.find("linked").isEmpty(), "a symlinked jar is never discovered");
            assertFalse(Files.exists(this.dir.resolve("escape.yml")));
        }
    }

    @Test
    void optionalNetworkAdapterMayBeAbsent() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            String lobby = """
                    package mod.lobby;
                    public class Main implements dev.lemonpvp.runtime.api.RuntimeModule {
                        public void onEnable(dev.lemonpvp.runtime.api.ModuleContext c) {
                            String net = c.services().find(testplatform.ProxyApi.class)
                                    .map(testplatform.ProxyApi::networkName).orElse("standalone");
                            testplatform.Probe.record("lobby:network:" + net);
                        }
                    }
                    """;
            ModuleJarBuilder.create().descriptor("lobby", "1.0.0", "mod.lobby.Main", "dependencies:", "  - id: proxy-adapter",
                    "    optional: true").source("mod.lobby.Main", lobby).writeTo(f.modules, "lobby.jar");
            OperationResult r = f.await(f.runtime.modules().enable(A, "lobby"));
            assertTrue(r.success(), r.toString());
            assertTrue(r.details().stream().anyMatch(d -> d.contains("optional dependency 'proxy-adapter' is not loaded")));
            assertTrue(Probe.events().contains("lobby:network:standalone"));

            ModuleJarBuilder.create().descriptor("proxy-adapter", "1.0.0", "mod.proxy.Main", "capabilities: [services]")
                    .source("mod.proxy.Main", """
                            package mod.proxy;
                            public class Main implements dev.lemonpvp.runtime.api.RuntimeModule {
                                public void onEnable(dev.lemonpvp.runtime.api.ModuleContext c) {
                                    c.services().provide(testplatform.ProxyApi.class, () -> "test-network");
                                }
                            }
                            """).writeTo(f.modules, "proxy-adapter.jar");
            assertTrue(f.await(f.runtime.modules().enable(A, "proxy-adapter")).success());
            assertTrue(f.await(f.runtime.modules().reload(A, "lobby")).success());
            assertTrue(Probe.events().contains("lobby:network:test-network"));

            assertTrue(f.await(f.runtime.modules().unload(A, "lobby")).success());
            assertTrue(f.await(f.runtime.modules().disable(A, "proxy-adapter")).success());
            assertTrue(f.runtime.modules().diagnostics("proxy-adapter").orElseThrow().services().isEmpty(),
                    "services are unregistered on disable");
        }
    }

    @Test
    void configurationReloadIsAtomicAndFallsBackToTheLastValidConfig() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            String pkg = "mod.cfg";
            ModuleJarBuilder.create().descriptor("cfg", "1.0.0", pkg + ".Main", "resources: [config.yml]")
                    .resource("config.yml", "greeting: hello\n")
                    .source(pkg + ".Main", TestModules.basic(pkg, "cfg")).writeTo(f.modules, "cfg.jar");
            assertTrue(f.await(f.runtime.modules().enable(A, "cfg")).success());
            assertTrue(Probe.events().contains("cfg:enable:hello"), "default resource copied and read");
            Path config = this.dir.resolve("lemon/module-data/cfg/config.yml");

            Files.writeString(config, "greeting: world\n");
            assertTrue(f.await(f.runtime.modules().reloadConfig(A, "cfg")).success());
            assertTrue(Probe.events().contains("cfg:config:world"));

            Files.writeString(config, "greeting: invalid\n");
            OperationResult rejected = f.await(f.runtime.modules().reloadConfig(A, "cfg"));
            assertFalse(rejected.success());
            assertTrue(rejected.details().contains("greeting must not be 'invalid'"));

            Files.writeString(config, "greeting: [unclosed\n");
            assertFalse(f.await(f.runtime.modules().reloadConfig(A, "cfg")).success(), "broken YAML is rejected");

            assertTrue(f.await(f.runtime.modules().disable(A, "cfg")).success());
            Files.writeString(config, "greeting: again\n");
            assertTrue(f.await(f.runtime.modules().enable(A, "cfg")).success());
            assertTrue(Probe.events().contains("cfg:enable:world"), "the last valid configuration was kept");
        }
    }

    @Test
    void runtimeInternalsAreHiddenFromModules() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            ModuleJarBuilder.create().descriptor("snoop", "1.0.0", "mod.snoop.Main")
                    .source("mod.snoop.Main", TestModules.broken("mod.snoop", "internals")).writeTo(f.modules, "snoop.jar");
            OperationResult r = f.await(f.runtime.modules().enable(A, "snoop"));
            assertFalse(r.success());
            assertTrue(r.message().contains("runtime internals are not part of the module API"), r.message());
        }
    }

    @Test
    void unloadedClassLoadersBecomeCollectable() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            basic(f.modules, "gc", "1.0.0", "gc");
            assertTrue(f.await(f.runtime.modules().enable(A, "gc")).success());
            f.platform.fire("join", "p");
            assertTrue(f.await(f.runtime.modules().unload(A, "gc")).success());
            boolean collected = false;
            for (int i = 0; i < 20 && !collected; i++) {
                System.gc();
                Thread.sleep(100);
                collected = f.runtime.modules().diagnostics("gc").orElseThrow().previousInstances().getFirst().collected();
            }
            assertTrue(collected, "the runtime must not keep references to an unloaded module");
        }
    }

    @Test
    void shutdownStopsModulesInReverseDependencyOrder() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            basic(f.modules, "base", "1.0.0", "base");
            basic(f.modules, "top", "1.0.0", "top", "dependencies: [base]");
            assertTrue(f.await(f.runtime.modules().enable(A, "base")).success());
            assertTrue(f.await(f.runtime.modules().enable(A, "top")).success());
            f.stopRuntime();
            List<String> ev = Probe.events();
            assertTrue(ev.indexOf("top:disable") < ev.indexOf("base:disable"), ev.toString());
            assertEquals(0, f.platform.listeners.size());
            assertTrue(f.runtime.modules().isStopped());
            assertFalse(f.await(f.runtime.modules().enable(A, "base")).success(), "no operations after shutdown");
        }
    }
}
