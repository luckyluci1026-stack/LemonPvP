package dev.lemonpvp.runtime.livepatch;

import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.ACTOR;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.await;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.fixture;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.greeter;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.inbox;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.jar;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.tool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lemonpvp.runtime.livepatch.ClassCatalog.Component;
import dev.lemonpvp.runtime.security.Hashing;
import dev.lemonpvp.runtime.testsupport.ModuleJarBuilder;
import dev.lemonpvp.runtime.testsupport.RuntimeFixture;
import dev.lemonpvp.runtime.testsupport.TestModules;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarFile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Class search and description over the JVM's loaded classes (unit tests plus real {@code getAllLoadedClasses}). */
class ClassCatalogTest {

    @TempDir
    Path dir;

    @BeforeAll
    static void developmentProfile() {
        System.setProperty("lemon.livepatch.development", "true");
    }

    @AfterAll
    static void reset() {
        System.clearProperty("lemon.livepatch.development");
    }

    @BeforeEach
    void clean() {
        LivePatchRegistry.INSTANCE.clearForTests();
        PatchTransformer.INSTANCE.clearForTests();
    }

    static Instrumentation inst() {
        return InstrumentationAccess.find().instrumentation().orElseThrow();
    }

    // ------------------------------------------------------------------ unit tests (no JVM state)

    @Test
    void componentsFollowPackageAndClassLoader() {
        assertEquals(Component.MINECRAFT, ClassCatalog.classify("net.minecraft.server.MinecraftServer", false, true));
        assertEquals(Component.MINECRAFT, ClassCatalog.classify("com.mojang.brigadier.CommandDispatcher", false, true));
        assertEquals(Component.CRAFTBUKKIT, ClassCatalog.classify("org.bukkit.craftbukkit.CraftServer", false, true));
        assertEquals(Component.PAPER, ClassCatalog.classify("io.papermc.paper.PaperBootstrap", false, true));
        assertEquals(Component.PAPER, ClassCatalog.classify("org.spigotmc.TicksPerSecondCommand", false, true));
        assertEquals(Component.BUKKIT, ClassCatalog.classify("org.bukkit.Bukkit", false, true));
        assertEquals(Component.LEMON, ClassCatalog.classify("dev.lemonpvp.runtime.LemonRuntime", false, true));
        assertEquals(Component.OTHER, ClassCatalog.classify("dev.lemonpvp.runtime.LemonRuntime", false, false),
                "Lemon classes outside the server class loader are not the server's");
        assertEquals(Component.AGENT, ClassCatalog.classify("dev.lemonpvp.livepatch.agent.LemonLivePatchAgent", false, false));
        assertEquals(Component.JDK, ClassCatalog.classify("java.lang.String", true, false));
        assertEquals(Component.LIBRARY, ClassCatalog.classify("io.netty.channel.Channel", false, true));
        assertEquals(Component.OTHER, ClassCatalog.classify("io.papermc.paperclip.Paperclip", false, false),
                "the launcher: io.papermc.paperclip is not io.papermc.paper, and it is not in the server class loader");
    }

    @Test
    void queriesAreValidatedAndParsed() {
        ClassCatalog.Query q = ClassCatalog.Query.parse(List.of("org.bukkit.craftbukkit.*Command", "craftbukkit", "3"));
        assertEquals(Component.CRAFTBUKKIT, q.component());
        assertEquals(3, q.page());
        assertTrue(q.nameMatcher().test("org.bukkit.craftbukkit.command.VanillaCommand"));
        assertFalse(q.nameMatcher().test("org.bukkit.command.Command"), "the pattern matches the whole name");
        assertTrue(ClassCatalog.Query.parse(List.of("spigotmc.ticks")).nameMatcher().test("org.spigotmc.TicksPerSecondCommand"),
                "plain text: case-insensitive part of the name");
        assertThrows(IllegalArgumentException.class, () -> ClassCatalog.Query.parse(List.of("a;rm -rf")));
        assertThrows(IllegalArgumentException.class, () -> ClassCatalog.Query.parse(List.of("org", "kernel")));
        assertThrows(IllegalArgumentException.class, () -> ClassCatalog.Query.parse(List.of()));
        assertNull(ClassCatalog.nameProblem("org.spigotmc.TicksPerSecondCommand"));
        assertNull(ClassCatalog.nameProblem("a.Outer$Inner"));
        assertNotNull(ClassCatalog.nameProblem("../etc/passwd"));
        assertNotNull(ClassCatalog.nameProblem("org..Bad"));
    }

    // ------------------------------------------------------------------ real loaded classes

    @Test
    void searchFindsServerPluginModuleAndJdkClassesAndPages() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("cat.Greeter", greeter("cat", "v1")));
        Path pluginJar = jar(this.dir.resolve("plugin"), "plugin.jar", Map.of("catplug.Main", greeter("catplug", "p")
                .replace("Greeter", "Main")));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class.forName("cat.Greeter", true, f.platform.serverClassLoader());
            URLClassLoader pluginLoader = new URLClassLoader(new URL[] {pluginJar.toUri().toURL()}, getClass().getClassLoader());
            Class.forName("catplug.Main", true, pluginLoader);
            ModuleJarBuilder.create().descriptor("catmod", "1.0.0", "mod.catmod.Main")
                    .source("mod.catmod.Main", TestModules.basic("mod.catmod", "catmod")).writeTo(f.modules, "catmod.jar");
            assertTrue(f.await(f.runtime.modules().enable(RuntimeFixture.ADMIN, "catmod")).success());
            Map<ClassLoader, String> plugins = new IdentityHashMap<>();
            plugins.put(pluginLoader, "CatPlugin");
            ClassCatalog.Loaders loaders = new ClassCatalog.Loaders(f.platform.serverClassLoader(), plugins);
            LivePatchService s = f.runtime.livePatches();

            ClassCatalog.Page server1 = await(s.classes(ClassCatalog.Query.parse(List.of("cat.Greet")), loaders));
            assertEquals(1, server1.matches(), server1.rows().toString());
            ClassCatalog.Row row = server1.rows().get(0);
            assertEquals("cat.Greeter", row.name());
            assertEquals(Component.LIBRARY, row.origin().component(), "server class loader, no known package");
            assertTrue(row.modifiable());
            assertNull(row.lemon());

            ClassCatalog.Row plugin = await(s.classes(ClassCatalog.Query.parse(List.of("catplug.")), loaders)).rows().get(0);
            assertEquals(new ClassCatalog.Origin(Component.PLUGIN, "CatPlugin"), plugin.origin());
            ClassCatalog.Page module = await(s.classes(ClassCatalog.Query.parse(List.of("mod.catmod.Main", "module")), loaders));
            assertTrue(module.rows().stream().anyMatch(r -> r.name().equals("mod.catmod.Main")), module.rows().toString());
            assertTrue(module.rows().stream().allMatch(r -> r.origin().label().equals("module catmod")),
                    "the main class and its nested/lambda classes belong to the module: " + module.rows());

            ClassCatalog.Page jdk1 = await(s.classes(ClassCatalog.Query.parse(List.of("java.lang.", "jdk", "1")), loaders));
            ClassCatalog.Page jdk2 = await(s.classes(ClassCatalog.Query.parse(List.of("java.lang.", "jdk", "2")), loaders));
            assertTrue(jdk1.matches() > ClassCatalog.PAGE_SIZE, "java.lang has many loaded classes: " + jdk1.matches());
            assertEquals(ClassCatalog.PAGE_SIZE, jdk1.rows().size());
            assertNotEquals(jdk1.rows().get(0).name(), jdk2.rows().get(0).name());
            assertTrue(jdk1.rows().stream().allMatch(r -> r.origin().component() == Component.JDK));
            ClassCatalog.Page beyond = await(s.classes(ClassCatalog.Query.parse(List.of("java.lang.", "jdk", "99999")), loaders));
            assertEquals(beyond.pages(), beyond.query().page(), "a page beyond the end shows the last page");

            ClassCatalog.Page all = await(s.classes(ClassCatalog.Query.parse(List.of("*")), loaders));
            long nonArrays = java.util.Arrays.stream(inst().getAllLoadedClasses()).filter(c -> !c.isArray()).count();
            assertTrue(Math.abs(all.loaded() - nonArrays) < 500, "array classes are not counted: " + all.loaded() + " vs " + nonArrays);
            assertEquals(all.loaded(), all.matches());
        }
    }

    @Test
    void describeShowsLoaderCodeSourceDigestModifiabilityAndLemonsChange() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("catd.Greeter", greeter("catd", "v1")));
        Path next = jar(this.dir.resolve("v2"), "classes.jar", Map.of("catd.Greeter", greeter("catd", "v2")));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> greeter = Class.forName("catd.Greeter", true, f.platform.serverClassLoader());
            ClassCatalog.Loaders loaders = new ClassCatalog.Loaders(f.platform.serverClassLoader(), Map.of());
            LivePatchService s = f.runtime.livePatches();
            List<ClassCatalog.Description> found = await(s.describe("catd.Greeter", loaders));
            assertEquals(1, found.size());
            ClassCatalog.Description d = found.get(0);
            assertEquals("class", d.kind());
            assertTrue(d.codeSource().endsWith("server.jar"), d.codeSource());
            assertTrue(d.modifiable());
            assertNull(d.notModifiable());
            byte[] file;
            try (JarFile j = new JarFile(server.toFile()); InputStream in = j.getInputStream(j.getEntry("catd/Greeter.class"))) {
                file = in.readAllBytes();
            }
            assertEquals(Hashing.sha256(file), d.classFileSha256(), "the digest a patch must be built against");
            assertTrue(d.classFile().contains("3 methods, 0 fields, super java.lang.Object"), d.classFile());
            assertNull(d.lemon());

            tool(server, next, inbox(f), "cat-fix");
            // JVMs with enhanced redefinition refuse retransformation patches (JvmCapabilities.ENHANCED_RETRANSFORM_PROBLEM).
            boolean retransform = !JvmCapabilities.enhancedRedefinition();
            Mechanism mechanism = retransform ? Mechanism.RETRANSFORM : Mechanism.REDEFINE;
            assertEquals(LivePatchService.Result.Kind.APPLIED, RetransformPatchTest.apply(s, "cat-fix", mechanism).kind());
            ClassCatalog.Description patched = await(s.describe("catd.Greeter", loaders)).get(0);
            if (retransform) {
                assertEquals("retransformed by cat-fix", patched.lemon());
                assertEquals(List.of("cat-fix"), patched.transformer().patchIds());
            } else {
                assertEquals("redefined by cat-fix", patched.lemon());
                assertNull(patched.transformer());
            }

            Runnable lambda = () -> { };
            ClassCatalog.Description hidden = ClassCatalog.describe(inst(), lambda.getClass(), loaders, LivePatchRegistry.INSTANCE,
                    PatchTransformer.INSTANCE);
            assertFalse(hidden.modifiable(), "hidden classes are never modifiable");
            assertTrue(hidden.kind().contains("hidden") && hidden.notModifiable().contains("hidden"), hidden.toString());
            assertNull(hidden.classFileSha256());

            assertTrue(await(s.describe("catd.NotLoadedYet", loaders)).isEmpty(), "nothing is loaded just to describe it");
            ExecutionException bad = assertThrows(ExecutionException.class, () -> await(s.describe("../x", loaders)));
            assertTrue(bad.getCause().getMessage().contains("not a binary class name"), bad.getCause().getMessage());
            assertEquals(greeter, ClassCatalog.find(inst(), "catd.Greeter").get(0));
        }
    }

    @Test
    void runningBytecodeIsComparedOnRequestAndShowsChangesMadeOutsideLemon() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("catr.Greeter", greeter("catr", "v1")));
        Path foreign = jar(this.dir.resolve("v3"), "classes.jar", Map.of("catr.Greeter", greeter("catr", "foreign")));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> greeter = Class.forName("catr.Greeter", true, f.platform.serverClassLoader());
            LivePatchService s = f.runtime.livePatches();
            LivePatchService.RunningCheck same = await(s.compareRunning(ACTOR, "catr.Greeter", 1));
            assertTrue(same.differences().isEmpty(), same.toString());
            assertTrue(same.expected().startsWith("the class file its class loader provides"), same.expected());
            byte[] other;
            try (JarFile j = new JarFile(foreign.toFile()); InputStream in = j.getInputStream(j.getEntry("catr/Greeter.class"))) {
                other = in.readAllBytes();
            }
            inst().redefineClasses(new ClassDefinition(greeter, other));
            LivePatchService.RunningCheck changed = await(s.compareRunning(ACTOR, "catr.Greeter", 1));
            assertFalse(changed.differences().isEmpty(), "a redefinition behind Lemon's back is visible");
            assertTrue(changed.differences().toString().contains("method body changed"), changed.differences().toString());
            ExecutionException none = assertThrows(ExecutionException.class, () -> await(s.compareRunning(ACTOR, "catr.Missing", 1)));
            assertTrue(none.getCause().getMessage().contains("is not loaded"), none.getCause().getMessage());
            f.awaitAudit("class-verify | catr.Greeter | FAILED");
        }
        try (RuntimeFixture off = RuntimeFixture.start(this.dir.resolve("off"))) {
            ExecutionException disabled = assertThrows(ExecutionException.class,
                    () -> await(off.runtime.livePatches().compareRunning(ACTOR, "java.lang.String", 1)));
            assertTrue(disabled.getCause().getMessage().contains("needs live-patching.enabled: true"), disabled.getCause().getMessage());
        }
    }
}
