package dev.lemonpvp.runtime.livepatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lemonpvp.runtime.live.LiveHistory;
import dev.lemonpvp.runtime.livepatch.LivePatchService.Result.Kind;
import dev.lemonpvp.runtime.livepatch.classfile.ClassFileReader;
import dev.lemonpvp.runtime.security.Hashing;
import dev.lemonpvp.runtime.testsupport.ModuleJarBuilder;
import dev.lemonpvp.runtime.testsupport.RuntimeFixture;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end tests of core live patching with real JVM class redefinition: the test JVM
 * runs with {@code -javaagent:lemon-livepatch-agent.jar}, "server" classes live in their
 * own class loader (like Paperclip's), patches are built with {@link
 * dev.lemonpvp.runtime.livepatch.tool.LivePatchTool} from compiled sources.
 *
 * <p>These tests prove the mechanism on this JVM; they are not a test on a Paper server
 * (see the CI server test for that).</p>
 */
class LivePatchIntegrationTest {

    static final String BUILD = "test:lemon/26.2-DEV-test/1970-01-01T00:00:00Z";
    static final String MANIFEST = "Manifest-Version: 1.0\nBrand-Id: test:lemon\nImplementation-Version: 26.2-DEV-test\n"
            + "Build-Time: 1970-01-01T00:00:00Z\n\n";
    static final String ACTOR = "console";

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
    void cleanRegistry() {
        LivePatchRegistry.INSTANCE.clearForTests();
        assertTrue(InstrumentationAccess.find().instrumentation().isPresent(), "tests must run with the live-patch agent");
    }

    // ------------------------------------------------------------------ helpers

    /** Compiles sources into a jar with a build manifest. */
    static Path jar(Path dir, String name, Map<String, String> sources) throws Exception {
        ModuleJarBuilder b = ModuleJarBuilder.create().noDescriptor().resource("META-INF/MANIFEST.MF", MANIFEST);
        sources.forEach(b::source);
        return b.writeTo(dir, name, false);
    }

    static String greeter(String pkg, String version) {
        return "package " + pkg + ";\npublic class Greeter {\n"
                + "    public static String text() { return \"" + version + "\"; }\n"
                + "    public String hello() { return \"hello " + version + "\"; }\n}\n";
    }

    /** Starts a runtime whose "server class loader" loads {@code server}. */
    static RuntimeFixture fixture(Path dir, Path server, String... extraConfig) throws Exception {
        String[] lines = new String[extraConfig.length + 3];
        lines[0] = "live-patching:";
        lines[1] = "  enabled: true";
        lines[2] = "  profile: development";
        System.arraycopy(extraConfig, 0, lines, 3, extraConfig.length);
        RuntimeFixture f = RuntimeFixture.start(dir.resolve("server"), lines);
        f.platform.serverClassLoader(new URLClassLoader(new URL[] {server.toUri().toURL()}, LivePatchIntegrationTest.class.getClassLoader()));
        f.platform.buildId(BUILD);
        return f;
    }

    static Path inbox(RuntimeFixture f) {
        return f.serverDir.resolve("lemon/patches/inbox");
    }

    static String tool(Path base, Path classes, Path out, String id, String... extra) throws Exception {
        String[] args = new String[9 + extra.length];
        String[] fixed = {"create", "--base", base.toString(), "--classes", classes.toString(), "--out", out.toString(), "--id", id};
        System.arraycopy(fixed, 0, args, 0, fixed.length);
        System.arraycopy(extra, 0, args, fixed.length, extra.length);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int rc = dev.lemonpvp.runtime.livepatch.tool.LivePatchTool.run(args, new PrintStream(bytes, true, StandardCharsets.UTF_8));
        assertEquals(0, rc, bytes.toString(StandardCharsets.UTF_8));
        return bytes.toString(StandardCharsets.UTF_8);
    }

    static Object call(Class<?> c, String method) throws Exception {
        return c.getMethod(method).invoke(null);
    }

    static <T> T await(java.util.concurrent.CompletableFuture<T> f) throws Exception {
        return f.get(60, TimeUnit.SECONDS);
    }

    static LivePatchService.Result applyConfirmed(LivePatchService s, String id) throws Exception {
        LivePatchService.Result plan = await(s.apply(ACTOR, id, false));
        if (plan.kind() != Kind.CONFIRMATION_REQUIRED) {
            return plan;
        }
        return await(s.apply(ACTOR, id, true));
    }

    // ------------------------------------------------------------------ tests

    @Test
    void methodBodyChangeIsAppliedToTheRunningClassAndRolledBack() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("lpa.Greeter", greeter("lpa", "v1")));
        Path next = jar(this.dir.resolve("v2"), "classes.jar", Map.of("lpa.Greeter", greeter("lpa", "v2")));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> greeter = Class.forName("lpa.Greeter", true, f.platform.serverClassLoader());
            Object existing = greeter.getConstructor().newInstance();
            assertEquals("v1", call(greeter, "text"));
            String toolOutput = tool(server, next, inbox(f), "fix-a", "--description", "test fix");
            assertTrue(toolOutput.contains("method body changed: text() : String"), toolOutput);

            LivePatchService s = f.runtime.livePatches();
            PatchPlan plan = await(s.inspect("fix-a"));
            assertTrue(plan.applicable(), plan.errors().toString());
            assertEquals(List.of(ChangeKind.METHOD_BODY, ChangeKind.METHOD_BODY),
                    plan.entries().get(0).changes().stream().map(ClassChange::kind).toList());

            LivePatchService.Result first = await(s.apply(ACTOR, "fix-a", false));
            assertEquals(Kind.CONFIRMATION_REQUIRED, first.kind(), first.message());
            assertEquals("v1", call(greeter, "text"), "nothing changes before the confirmation");

            LivePatchService.Result done = await(s.apply(ACTOR, "fix-a", true));
            assertEquals(Kind.APPLIED, done.kind(), done.message() + " " + done.details());
            assertEquals("v2", call(greeter, "text"), "the same Class object runs the new code");
            assertEquals("hello v2", greeter.getMethod("hello").invoke(existing), "existing objects run the new code too");
            assertEquals(LivePatchRegistry.Status.ACTIVE, LivePatchRegistry.INSTANCE.latest("fix-a").orElseThrow().status());

            Path backupDir = Path.of(LivePatchRegistry.INSTANCE.latest("fix-a").orElseThrow().backupDirectory());
            byte[] original;
            try (JarFile jar = new JarFile(server.toFile()); InputStream in = jar.getInputStream(jar.getEntry("lpa/Greeter.class"))) {
                original = in.readAllBytes();
            }
            assertEquals(Hashing.sha256(original), Hashing.sha256(backupDir.resolve("lpa/Greeter.class")), "backup holds the replaced bytecode");

            assertEquals(Kind.REJECTED, await(s.apply(ACTOR, "fix-a", true)).kind(), "a confirmation cannot be used twice");

            LivePatchService.Result rbPlan = await(s.rollback(ACTOR, "fix-a", false));
            assertEquals(Kind.CONFIRMATION_REQUIRED, rbPlan.kind(), rbPlan.message());
            LivePatchService.Result rb = await(s.rollback(ACTOR, "fix-a", true));
            assertEquals(Kind.ROLLED_BACK, rb.kind(), rb.message() + " " + rb.details());
            assertEquals("v1", call(greeter, "text"));
            assertTrue(LiveHistory.INSTANCE.latest(5).stream().anyMatch(e -> e.name().equals("fix-a") && e.action().equals("rollback")));
            String audit = Files.readString(f.serverDir.resolve("lemon/logs/audit.log"));
            assertTrue(audit.contains("patch-apply | fix-a | OK"), audit);
        }
    }

    /**
     * Copies a jar and changes every class file without changing its meaning (the SourceFile
     * name keeps its length), like the 1.21.11 paperclip, whose server jar has the same classes
     * as the Gradle jar but a different class file layout.
     */
    static Path rewritten(Path jar, Path target) throws Exception {
        Files.createDirectories(target.getParent());
        try (JarFile in = new JarFile(jar.toFile());
             java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(Files.newOutputStream(target))) {
            for (var e : java.util.Collections.list(in.entries())) {
                byte[] data;
                try (InputStream is = in.getInputStream(e)) {
                    data = is.readAllBytes();
                }
                if (e.getName().endsWith(".class")) {
                    String simple = e.getName().substring(e.getName().lastIndexOf('/') + 1).replace(".class", "");
                    String text = new String(data, StandardCharsets.ISO_8859_1);
                    String from = simple + ".java";
                    assertEquals(text.indexOf(from), text.lastIndexOf(from), "SourceFile name occurs once in " + e.getName());
                    data = text.replace(from, simple + ".jxva").getBytes(StandardCharsets.ISO_8859_1);
                }
                out.putNextEntry(new java.util.zip.ZipEntry(e.getName()));
                out.write(data);
                out.closeEntry();
            }
        }
        return target;
    }

    @Test
    void rewrittenServerJarsArePatchedWithTheGradleJarAsReference() throws Exception {
        Map<String, String> v1 = Map.of("lpr.A", greeter("lpr", "v1").replace("Greeter", "A"),
                "lpr.B", greeter("lpr", "v1").replace("Greeter", "B"));
        Map<String, String> v2 = Map.of("lpr.A", greeter("lpr", "v2").replace("Greeter", "A"),
                "lpr.B", greeter("lpr", "v1").replace("Greeter", "B"));
        Path reference = jar(this.dir.resolve("ref"), "reference.jar", v1);
        Path running = rewritten(reference, this.dir.resolve("run/server.jar"));
        Path classes = unzipClasses(jar(this.dir.resolve("v2"), "classes.jar", v2), this.dir.resolve("v2classes"));
        try (RuntimeFixture f = fixture(this.dir, running)) {
            Class<?> a = Class.forName("lpr.A", true, f.platform.serverClassLoader());
            LivePatchService s = f.runtime.livePatches();

            // Built against the Gradle jar only: the server refuses, its bytecode differs.
            tool(reference, classes, inbox(f), "gradle-base");
            PatchPlan wrong = await(s.inspect("gradle-base"));
            assertFalse(wrong.applicable());
            assertTrue(wrong.errors().stream().anyMatch(e -> e.contains("patch was built against")), wrong.errors().toString());

            // Built against the running jar only: every rewritten class looks changed.
            String noRef = tool(running, classes, inbox(f), "no-reference");
            assertTrue(noRef.contains("changed  lpr.A") && noRef.contains("changed  lpr.B"), noRef);

            // Running jar as base, Gradle jar as reference: only the real change, and it applies.
            String withRef = tool(running, classes, inbox(f), "with-reference", "--reference", reference.toString());
            assertTrue(withRef.contains("changed  lpr.A") && !withRef.contains("lpr.B"), withRef);
            LivePatchService.Result r = applyConfirmed(s, "with-reference");
            assertEquals(Kind.APPLIED, r.kind(), r.message() + " " + r.details());
            assertEquals("v2", call(a, "text"));
        }

        // A reference from other sources is detected instead of producing a wrong patch.
        Path other = jar(this.dir.resolve("other"), "other.jar", Map.of("lpr.A", greeter("lpr", "v0").replace("Greeter", "A"),
                "lpr.B", greeter("lpr", "v1").replace("Greeter", "B")));
        dev.lemonpvp.runtime.livepatch.tool.LivePatchTool.ToolException ex = org.junit.jupiter.api.Assertions.assertThrows(
                dev.lemonpvp.runtime.livepatch.tool.LivePatchTool.ToolException.class,
                () -> dev.lemonpvp.runtime.livepatch.tool.LivePatchTool.run(new String[] {"create", "--base", running.toString(),
                        "--classes", classes.toString(), "--reference", other.toString(), "--out", this.dir.resolve("o").toString(),
                        "--id", "drift"}, new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)));
        assertTrue(ex.getMessage().contains("not the build that runs on the server"), ex.getMessage());
    }

    @Test
    void structuralChangeIsRejectedUnlessTheJvmProvablySupportsIt() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("lpb.Greeter", greeter("lpb", "v1")));
        Path next = jar(this.dir.resolve("v2"), "classes.jar", Map.of("lpb.Greeter", greeter("lpb", "v2").replace(
                "public String hello()", "public static int added() { return 7; }\n    public String hello()")));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> greeter = Class.forName("lpb.Greeter", true, f.platform.serverClassLoader());
            String out = tool(server, next, inbox(f), "add-method");
            assertTrue(out.contains("[structural] method added: added() : int"), out);
            LivePatchService s = f.runtime.livePatches();
            PatchPlan plan = await(s.inspect("add-method"));
            boolean enhanced = JvmCapabilities.ensure(InstrumentationAccess.find().instrumentation().orElseThrow(),
                    EnumSet.of(Capability.ADD_METHOD)).get(Capability.ADD_METHOD).supported();
            if (!enhanced) {
                assertFalse(plan.applicable());
                assertTrue(plan.errors().toString().contains("this JVM cannot add methods"), plan.errors().toString());
                LivePatchService.Result r = await(s.apply(ACTOR, "add-method", false));
                assertEquals(Kind.REJECTED, r.kind());
                assertEquals("v1", call(greeter, "text"), "a rejected patch changes nothing");
            } else {
                assertTrue(plan.applicable(), plan.errors().toString());
                LivePatchService.Result r = applyConfirmed(s, "add-method");
                assertEquals(Kind.APPLIED, r.kind(), r.message());
                assertEquals("v2", call(greeter, "text"));
                assertEquals(7, call(greeter, "added"), "enhanced redefinition added a callable method");
            }
        }
    }

    @Test
    void multiClassPatchIsAllOrNothingWhenTheJvmRejectsOneClass() throws Exception {
        Map<String, String> v1 = Map.of("lpc.A", greeter("lpc", "v1").replace("Greeter", "A"),
                "lpc.B", greeter("lpc", "v1").replace("Greeter", "B"));
        Map<String, String> v2 = Map.of("lpc.A", greeter("lpc", "v2").replace("Greeter", "A"),
                "lpc.B", greeter("lpc", "v2").replace("Greeter", "B"));
        Path server = jar(this.dir.resolve("v1"), "server.jar", v1);
        Path nextJar = jar(this.dir.resolve("v2"), "classes.jar", v2);
        Path classes = unzipClasses(nextJar, this.dir.resolve("v2classes"));
        // Corrupt B: max_stack 0 for text(); the class still parses, but the JVM verifier rejects it.
        Path b = classes.resolve("lpc/B.class");
        byte[] bytes = Files.readAllBytes(b);
        int code = ClassFileReader.read(bytes).method("text", "()Ljava/lang/String;").orElseThrow().codeOffset();
        bytes[code] = 0;
        bytes[code + 1] = 0;
        Files.write(b, bytes);
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> a = Class.forName("lpc.A", true, f.platform.serverClassLoader());
            Class<?> bc = Class.forName("lpc.B", true, f.platform.serverClassLoader());
            tool(server, classes, inbox(f), "two-classes");
            LivePatchService s = f.runtime.livePatches();
            Instrumentation inst = InstrumentationAccess.find().instrumentation().orElseThrow();
            boolean jvmVerifies = JvmCapabilities.ensure(inst, EnumSet.of(Capability.VERIFY_REDEFINED_CLASSES))
                    .get(Capability.VERIFY_REDEFINED_CLASSES).supported();
            PatchPlan plan = await(s.inspect("two-classes"));
            if (jvmVerifies) {
                // Standard HotSpot: Lemon's checks pass; the JVM verifier rejects B inside redefineClasses.
                assertTrue(plan.applicable(), "only the JVM verifier can see the defect: " + plan.errors());
                LivePatchService.Result r = applyConfirmed(s, "two-classes");
                assertEquals(Kind.FAILED, r.kind(), r.message());
                assertTrue(r.message().contains("the JVM rejected the redefinition"), r.message());
                assertTrue(r.message().contains("VerifyError"), r.message());
                assertEquals(LivePatchRegistry.Status.FAILED, LivePatchRegistry.INSTANCE.latest("two-classes").orElseThrow().status());
            } else {
                // JVMs whose redefinition does not verify (seen on JetBrains Runtime with enhanced
                // redefinition): Lemon's own pre-verification must reject the patch before applying it.
                assertFalse(plan.applicable(), "invalid bytecode must never reach redefineClasses");
                assertTrue(plan.errors().stream().anyMatch(e -> e.startsWith("lpc.B") && e.contains("fails bytecode verification")),
                        plan.errors().toString());
                LivePatchService.Result r = applyConfirmed(s, "two-classes");
                assertEquals(Kind.REJECTED, r.kind(), r.message());
            }
            assertEquals("v1", call(a, "text"), "A was not redefined although it was valid: all or nothing");
            assertEquals("v1", call(bc, "text"));
            assertEquals(0, LivePatchRegistry.INSTANCE.activeCount());
        }
    }

    @Test
    void wrongBuildIdAndWrongBaseAreRejected() throws Exception {
        Path v0 = jar(this.dir.resolve("v0"), "server.jar", Map.of("lpd.Greeter", greeter("lpd", "v0")));
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("lpd.Greeter", greeter("lpd", "v1")));
        Path next = jar(this.dir.resolve("v2"), "classes.jar", Map.of("lpd.Greeter", greeter("lpd", "v2")));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> greeter = Class.forName("lpd.Greeter", true, f.platform.serverClassLoader());
            tool(server, next, inbox(f), "wrong-build", "--target-build", "test:lemon/26.2-DEV-other/x");
            PatchPlan plan = await(f.runtime.livePatches().inspect("wrong-build"));
            assertFalse(plan.applicable());
            assertTrue(plan.errors().toString().contains("wrong build id"), plan.errors().toString());

            tool(v0, next, inbox(f), "wrong-base");
            plan = await(f.runtime.livePatches().inspect("wrong-base"));
            assertFalse(plan.applicable());
            assertTrue(plan.errors().toString().contains("but the server runs"), plan.errors().toString());
            assertEquals(Kind.REJECTED, await(f.runtime.livePatches().apply(ACTOR, "wrong-base", false)).kind());
            assertEquals("v1", call(greeter, "text"));
        }
    }

    @Test
    void tamperedArchivesAndChecksumsAreRejected() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("lpe.Greeter", greeter("lpe", "v1")));
        Path next = jar(this.dir.resolve("v2"), "classes.jar", Map.of("lpe.Greeter", greeter("lpe", "v2")));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            tool(server, next, inbox(f), "tampered");
            Path sidecar = inbox(f).resolve("tampered.lemonpatch.sha256");
            Files.writeString(sidecar, "0".repeat(64) + "  tampered.lemonpatch\n");
            Throwable e = failure(f.runtime.livePatches().inspect("tampered"));
            assertTrue(e.getMessage().contains("SHA-256 mismatch"), e.getMessage());

            Files.delete(sidecar);
            e = failure(f.runtime.livePatches().inspect("tampered"));
            assertTrue(e.getMessage().contains("missing tampered.lemonpatch.sha256"), e.getMessage());

            Files.writeString(inbox(f).resolve("garbage.lemonpatch"), "not a zip");
            ModuleJarBuilder.writeSidecar(inbox(f).resolve("garbage.lemonpatch"));
            e = failure(f.runtime.livePatches().inspect("garbage"));
            assertTrue(e.getMessage().contains(PatchManifest.FILE_NAME) || e.getMessage().contains("zip"), e.getMessage());

            e = failure(f.runtime.livePatches().inspect("../escape"));
            assertTrue(e.getMessage().contains("invalid patch id"), e.getMessage());
        }
    }

    @Test
    void productionProfileOnlyAppliesPatchesOnTheAllowList() throws Exception {
        System.clearProperty("lemon.livepatch.development");
        try {
            Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("lpf.Greeter", greeter("lpf", "v1")));
            Path next = jar(this.dir.resolve("v2"), "classes.jar", Map.of("lpf.Greeter", greeter("lpf", "v2")));
            try (RuntimeFixture f = fixture(this.dir, server, "  trusted-hashes-file: lemon/patches/trusted.txt")) {
                Class<?> greeter = Class.forName("lpf.Greeter", true, f.platform.serverClassLoader());
                assertEquals(dev.lemonpvp.runtime.config.LivePatchConfig.Profile.PRODUCTION, f.runtime.livePatches().profile(),
                        "development needs -Dlemon.livepatch.development=true");
                assertNotNull(f.runtime.livePatches().profileNote());
                tool(server, next, inbox(f), "approved");
                Throwable e = failure(f.runtime.livePatches().inspect("approved"));
                assertTrue(e.getMessage().contains("trusted.txt does not exist"), e.getMessage());

                Files.writeString(f.serverDir.resolve("lemon/patches/trusted.txt"), "# approved patches\n");
                e = failure(f.runtime.livePatches().inspect("approved"));
                assertTrue(e.getMessage().contains("is not listed"), e.getMessage());

                Files.writeString(f.serverDir.resolve("lemon/patches/trusted.txt"),
                        Hashing.sha256(inbox(f).resolve("approved.lemonpatch")) + "  approved.lemonpatch\n");
                LivePatchService.Result r = applyConfirmed(f.runtime.livePatches(), "approved");
                assertEquals(Kind.APPLIED, r.kind(), r.message());
                assertEquals("v2", call(greeter, "text"));
            }
        } finally {
            System.setProperty("lemon.livepatch.development", "true");
        }
    }

    @Test
    void newClassesAreDefinedInTheServerClassLoaderBeforeTheRedefinition() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("lpg.Greeter", greeter("lpg", "v1")));
        Path next = jar(this.dir.resolve("v2"), "classes.jar", Map.of(
                "lpg.Greeter", greeter("lpg", "v2").replace("return \"v2\";", "return Helper.value();"),
                "lpg.Helper", "package lpg;\nclass Helper { static String value() { return \"from new class\"; } }\n"));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> greeter = Class.forName("lpg.Greeter", true, f.platform.serverClassLoader());
            String out = tool(server, next, inbox(f), "with-helper");
            assertTrue(out.contains("new      lpg.Helper"), out);
            PatchPlan plan = await(f.runtime.livePatches().inspect("with-helper"));
            assertTrue(plan.applicable(), plan.errors().toString());
            assertEquals(1, plan.definitions().size());
            LivePatchService.Result r = applyConfirmed(f.runtime.livePatches(), "with-helper");
            assertEquals(Kind.APPLIED, r.kind(), r.message());
            assertEquals("from new class", call(greeter, "text"));
            Class<?> helper = Class.forName("lpg.Helper", false, f.platform.serverClassLoader());
            assertTrue(helper.getClassLoader() == f.platform.serverClassLoader(), "defined in the server class loader");
        }
    }

    @Test
    void laterPatchReplacesEarlierVersionAndRollbackIsLastInFirstOut() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("lph.Greeter", greeter("lph", "v1")));
        Path v2 = jar(this.dir.resolve("v2"), "classes.jar", Map.of("lph.Greeter", greeter("lph", "v2")));
        Path v3 = jar(this.dir.resolve("v3"), "classes.jar", Map.of("lph.Greeter", greeter("lph", "v3")));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> greeter = Class.forName("lph.Greeter", true, f.platform.serverClassLoader());
            LivePatchService s = f.runtime.livePatches();
            tool(server, v2, inbox(f), "first");
            tool(server, v3, inbox(f), "second");
            assertEquals(Kind.APPLIED, applyConfirmed(s, "first").kind());
            PatchPlan plan = await(s.inspect("second"));
            assertTrue(plan.applicable(), plan.errors().toString());
            assertEquals("first", plan.entries().get(0).replaces(), "built against the jar, replaces the active version");
            assertEquals(Kind.APPLIED, applyConfirmed(s, "second").kind());
            assertEquals("v3", call(greeter, "text"));

            LivePatchService.Result blocked = await(s.rollback(ACTOR, "first", false));
            assertEquals(Kind.REJECTED, blocked.kind());
            assertTrue(blocked.details().toString().contains("changed again by patch second"), blocked.details().toString());

            assertEquals(Kind.CONFIRMATION_REQUIRED, await(s.rollback(ACTOR, "second", false)).kind());
            assertEquals(Kind.ROLLED_BACK, await(s.rollback(ACTOR, "second", true)).kind());
            assertEquals("v2", call(greeter, "text"), "back to the version of the first patch");
            assertEquals(Kind.CONFIRMATION_REQUIRED, await(s.rollback(ACTOR, "first", false)).kind());
            assertEquals(Kind.ROLLED_BACK, await(s.rollback(ACTOR, "first", true)).kind());
            assertEquals("v1", call(greeter, "text"));
        }
    }

    @Test
    void disabledLivePatchingRefusesToApply() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("lpi.Greeter", greeter("lpi", "v1")));
        try (RuntimeFixture f = RuntimeFixture.start(this.dir.resolve("server"))) {
            f.platform.serverClassLoader(new URLClassLoader(new URL[] {server.toUri().toURL()}, getClass().getClassLoader()));
            LivePatchService.Result r = await(f.runtime.livePatches().apply(ACTOR, "anything", false));
            assertEquals(Kind.REJECTED, r.kind());
            assertTrue(r.message().contains("live patching is disabled"), r.message());
            assertFalse(f.runtime.livePatches().status().enabled());
        }
    }

    @Test
    void probesMatchTheJvmFlagAndBodyRedefinitionWorks() {
        var inst = InstrumentationAccess.find().instrumentation().orElseThrow();
        Map<Capability, CapabilityProbe.Result> r = JvmCapabilities.probeAll(inst);
        assertTrue(r.get(Capability.REDEFINE_METHOD_BODY).supported(), r.toString());
        assertTrue(r.get(Capability.DEFINE_NEW_CLASS).supported(), r.toString());
        boolean enhanced = "true".equals(JvmCapabilities.enhancedRedefinitionFlag());
        assertEquals(enhanced, r.get(Capability.ADD_METHOD).supported(),
                "adding methods must work exactly when AllowEnhancedClassRedefinition is on: " + r);
    }

    // ------------------------------------------------------------------ utilities

    static Throwable failure(java.util.concurrent.CompletableFuture<?> f) throws Exception {
        try {
            f.get(60, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            return e.getCause();
        }
        throw new AssertionError("expected a failure");
    }

    static Path unzipClasses(Path jar, Path target) throws Exception {
        try (JarFile file = new JarFile(jar.toFile())) {
            for (var e : java.util.Collections.list(file.entries())) {
                if (e.getName().endsWith(".class")) {
                    Path out = target.resolve(e.getName());
                    Files.createDirectories(out.getParent());
                    try (InputStream in = file.getInputStream(e)) {
                        Files.write(out, in.readAllBytes());
                    }
                }
            }
        }
        try (Stream<Path> s = Files.walk(target)) {
            assertTrue(s.anyMatch(p -> p.toString().endsWith(".class")));
        }
        return target;
    }
}
