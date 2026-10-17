package dev.paperswap.runtime.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.paperswap.runtime.api.Capability;
import dev.paperswap.runtime.api.ModuleDescriptor;
import dev.paperswap.runtime.api.RuntimeApi;
import dev.paperswap.runtime.descriptor.DescriptorParser;
import dev.paperswap.runtime.live.LiveHistory;
import dev.paperswap.runtime.testsupport.ModuleJarBuilder;
import dev.paperswap.runtime.testsupport.RuntimeFixture;
import dev.paperswap.runtime.testsupport.TestModules;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The simple module workflow: module.yml, minimal descriptors, remove. */
class ModuleSimplicityTest {

    private static final String A = RuntimeFixture.ADMIN;

    @TempDir
    Path dir;

    @Test
    void minimalDescriptorNeedsOnlyIdVersionAndMain() throws Exception {
        ModuleDescriptor d = DescriptorParser.parse(new ByteArrayInputStream(
                "id: tiny\nversion: 1.0.0\nmain: mod.tiny.Main\n".getBytes(StandardCharsets.UTF_8)), "module.yml");
        assertEquals(RuntimeApi.API_VERSION, d.apiVersion());
        assertEquals(List.of("*"), d.minecraftVersions());
        assertEquals(EnumSet.allOf(Capability.class), d.capabilities(), "omitted capabilities = all");
        assertEquals("tiny", d.name());
        ModuleDescriptor restricted = DescriptorParser.parse(new ByteArrayInputStream(
                "id: tiny\nversion: 1.0.0\nmain: mod.tiny.Main\ncapabilities: [events]\n".getBytes(StandardCharsets.UTF_8)), "module.yml");
        assertEquals(EnumSet.of(Capability.EVENTS), restricted.capabilities(), "a listed set restricts");
    }

    @Test
    void moduleYmlWorksAndTwoDescriptorsAreRejected() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            ModuleJarBuilder.create().noDescriptor().resource("module.yml", "id: short\nversion: 1.0.0\nmain: mod.short1.Main\n")
                    .source("mod.short1.Main", TestModules.basic("mod.short1", "short")).writeTo(f.modules, "short.jar");
            ModuleJarBuilder.create().descriptor("both", "1.0.0", "mod.both.Main").resource("module.yml", "id: both\n")
                    .source("mod.both.Main", TestModules.basic("mod.both", "both")).writeTo(f.modules, "both.jar");
            var scan = f.runtime.modules().scan().get();
            assertTrue(scan.artifacts().stream().anyMatch(a -> a.id().equals("short")));
            assertTrue(scan.problems().stream().anyMatch(p -> p.message().contains("keep only one")), scan.problems().toString());
            OperationResult r = f.await(f.runtime.modules().enable(A, "short"));
            assertTrue(r.success(), r.toString());
        }
    }

    @Test
    void removeUnloadsAndMovesTheFilesOutOfTheModuleDirectory() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            ModuleJarBuilder.create().descriptor("gone", "1.0.0", "mod.gone.Main")
                    .source("mod.gone.Main", TestModules.basic("mod.gone", "gone")).writeTo(f.modules, "gone.jar");
            assertTrue(f.await(f.runtime.modules().enable(A, "gone")).success());
            OperationResult r = f.await(f.runtime.modules().remove(A, "gone"));
            assertTrue(r.success(), r.toString());
            assertTrue(f.runtime.modules().modules().isEmpty());
            assertFalse(Files.exists(f.modules.resolve("gone.jar")));
            assertFalse(Files.exists(f.modules.resolve("gone.jar.sha256")));
            try (Stream<Path> s = Files.walk(f.serverDir.resolve("paperswap/modules-removed"))) {
                assertEquals(2, s.filter(p -> p.getFileName().toString().startsWith("gone.jar")).count(), "jar and .sha256 kept");
            }
            assertTrue(LiveHistory.INSTANCE.latest(5).stream().anyMatch(e -> e.name().equals("gone") && e.action().equals("remove")));
            assertFalse(f.await(f.runtime.modules().remove(A, "gone")).success(), "nothing left to remove");
        }
    }

    @Test
    void removeWithoutAJarChangesNothing() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            ModuleJarBuilder.create().descriptor("kept", "1.0.0", "mod.kept.Main")
                    .source("mod.kept.Main", TestModules.basic("mod.kept", "kept")).writeTo(f.modules, "kept.jar");
            assertTrue(f.await(f.runtime.modules().enable(A, "kept")).success());
            Files.delete(f.modules.resolve("kept.jar"));
            OperationResult r = f.await(f.runtime.modules().remove(A, "kept"));
            assertFalse(r.success(), r.toString());
            assertTrue(r.message().contains("nothing was changed"), r.message());
            assertEquals(dev.paperswap.runtime.api.ModuleState.ENABLED, f.runtime.modules().modules().get(0).state(),
                    "the module still runs");
        }
    }

    @Test
    void aConfirmedUpdateOnlyActivatesTheJarThatWasValidated() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            ModuleJarBuilder.create().descriptor("swapme", "1.0.0", "mod.swapme.Main")
                    .source("mod.swapme.Main", TestModules.basic("mod.swapme", "v1")).writeTo(f.modules, "swapme.jar");
            ModuleManager.Validated v = f.runtime.modules().validateJar(A, "swapme").get(60, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(v.result().success(), v.result().toString());
            // The file is replaced between the check and the confirmation.
            ModuleJarBuilder.create().descriptor("swapme", "1.0.1", "mod.swapme.Main")
                    .source("mod.swapme.Main", TestModules.basic("mod.swapme", "v2")).writeTo(f.modules, "swapme.jar");
            OperationResult r = f.await(f.runtime.modules().enable(A, "swapme", v.sha256()));
            assertFalse(r.success(), "a jar that was not validated must not be activated: " + r);
            assertTrue(f.runtime.modules().modules().isEmpty(), "nothing was loaded");
            assertTrue(f.await(f.runtime.modules().enable(A, "swapme")).success(), "an explicit enable still works");
        }
    }
}
