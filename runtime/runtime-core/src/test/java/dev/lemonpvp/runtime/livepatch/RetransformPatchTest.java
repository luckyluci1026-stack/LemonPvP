package dev.lemonpvp.runtime.livepatch;

import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.ACTOR;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.await;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.call;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.fixture;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.greeter;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.inbox;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.jar;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.tool;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.unzipClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lemonpvp.runtime.livepatch.LivePatchService.Result.Kind;
import dev.lemonpvp.runtime.livepatch.classfile.ClassFileReader;
import dev.lemonpvp.runtime.testsupport.RuntimeFixture;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;
import java.util.jar.JarFile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests of {@link Mechanism#RETRANSFORM} patches with real JVM retransformation
 * (the test JVM runs with {@code -javaagent:lemon-livepatch-agent.jar}). They prove the
 * mechanism on this JVM, not on a Paper server (see the CI server test for that).
 */
class RetransformPatchTest {

    @TempDir
    Path dir;

    @BeforeAll
    static void developmentProfile() {
        System.setProperty("lemon.livepatch.development", "true");
    }

    @AfterAll
    static void reset() {
        System.clearProperty("lemon.livepatch.development");
        PatchTransformer.INSTANCE.clearForTests();
    }

    @BeforeEach
    void clean() {
        LivePatchRegistry.INSTANCE.clearForTests();
        PatchTransformer.INSTANCE.clearForTests();
    }

    static Instrumentation inst() {
        return InstrumentationAccess.find().instrumentation().orElseThrow();
    }

    static LivePatchService.Result apply(LivePatchService s, String id, Mechanism m) throws Exception {
        LivePatchService.Result plan = await(s.apply(ACTOR, id, false, m));
        if (plan.kind() != Kind.CONFIRMATION_REQUIRED) {
            return plan;
        }
        assertEquals(m, plan.plan().mechanism());
        return await(s.apply(ACTOR, id, true, m));
    }

    static LivePatchService.Result rollback(LivePatchService s, String id) throws Exception {
        LivePatchService.Result plan = await(s.rollback(ACTOR, id, false));
        if (plan.kind() != Kind.CONFIRMATION_REQUIRED) {
            return plan;
        }
        return await(s.rollback(ACTOR, id, true));
    }

    static byte[] entry(Path jar, String path) throws Exception {
        try (JarFile f = new JarFile(jar.toFile()); InputStream in = f.getInputStream(f.getEntry(path))) {
            return in.readAllBytes();
        }
    }

    @Test
    void retransformationPatchRunsNewCodeSurvivesLaterRetransformationsAndRollsBack() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("rta.Greeter", greeter("rta", "v1")));
        Path next = jar(this.dir.resolve("v2"), "classes.jar", Map.of("rta.Greeter", greeter("rta", "v2")));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> greeter = Class.forName("rta.Greeter", true, f.platform.serverClassLoader());
            Object existing = greeter.getConstructor().newInstance();
            tool(server, next, inbox(f), "rt-fix");
            LivePatchService s = f.runtime.livePatches();
            assertFalse(PatchTransformer.INSTANCE.registeredAt().isPresent(), "registered only when first needed");

            PatchPlan plan = await(s.inspect("rt-fix", Mechanism.RETRANSFORM));
            assertTrue(plan.applicable(), plan.errors().toString());
            assertEquals(Mechanism.RETRANSFORM, plan.mechanism());
            assertTrue(plan.capabilities().get(Capability.RETRANSFORM_METHOD_BODY).supported(), plan.capabilities().toString());

            LivePatchService.Result done = apply(s, "rt-fix", Mechanism.RETRANSFORM);
            assertEquals(Kind.APPLIED, done.kind(), done.message() + " " + done.details());
            assertEquals(AppliedBytecodeCheck.Outcome.VERIFIED, done.verification().outcome(), done.details().toString());
            assertTrue(done.details().stream().anyMatch(d -> d.startsWith("retransformed rta.Greeter")), done.details().toString());
            assertEquals("v2", call(greeter, "text"), "new invocations run the new code");
            assertEquals("hello v2", greeter.getMethod("hello").invoke(existing), "existing objects run the new code too");
            assertTrue(PatchTransformer.INSTANCE.registeredAt().isPresent());
            LivePatchRegistry.PatchRecord record = LivePatchRegistry.INSTANCE.latest("rt-fix").orElseThrow();
            assertEquals(Mechanism.RETRANSFORM, record.mechanism());
            assertEquals(LivePatchRegistry.Status.ACTIVE, record.status());

            PatchTransformer.SlotInfo slot = PatchTransformer.INSTANCE.slot(greeter).orElseThrow();
            assertEquals(java.util.List.of("rt-fix"), slot.patchIds());
            long served = slot.served();
            assertTrue(served >= 2, "applied and read back: " + served);

            // Another tool retransforms the class (the JVM starts from the initial bytes again): the
            // transformer supplies the patch again, so the class keeps running it.
            inst().retransformClasses(greeter);
            inst().retransformClasses(greeter);
            assertEquals("v2", call(greeter, "text"));
            assertEquals(served + 2, PatchTransformer.INSTANCE.served(greeter));
            assertEquals(AppliedBytecodeCheck.Outcome.VERIFIED, await(s.verify(ACTOR, "rt-fix")).verification().outcome());

            LivePatchService.Result rb = rollback(s, "rt-fix");
            assertEquals(Kind.ROLLED_BACK, rb.kind(), rb.message() + " " + rb.details());
            assertEquals(AppliedBytecodeCheck.Outcome.VERIFIED, rb.verification().outcome(), rb.details().toString());
            assertEquals("v1", call(greeter, "text"), "without the transformer's bytes the class file of the jar runs again");
            assertTrue(PatchTransformer.INSTANCE.slot(greeter).isEmpty());
            inst().retransformClasses(greeter);
            assertEquals("v1", call(greeter, "text"), "a later retransformation does not bring the patch back");
            f.awaitAudit("patch-rollback | rt-fix | OK");
        }
    }

    @Test
    void redefinitionByAnotherToolDisplacesTheRetransformationPatchAndIsReported() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("rtb.Greeter", greeter("rtb", "v1")));
        Path next = jar(this.dir.resolve("v2"), "classes.jar", Map.of("rtb.Greeter", greeter("rtb", "v2")));
        Path foreign = jar(this.dir.resolve("v3"), "classes.jar", Map.of("rtb.Greeter", greeter("rtb", "foreign")));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> greeter = Class.forName("rtb.Greeter", true, f.platform.serverClassLoader());
            tool(server, next, inbox(f), "rt-displaced");
            LivePatchService s = f.runtime.livePatches();
            assertEquals(Kind.APPLIED, apply(s, "rt-displaced", Mechanism.RETRANSFORM).kind());
            assertEquals("v2", call(greeter, "text"));

            inst().redefineClasses(new ClassDefinition(greeter, entry(foreign, "rtb/Greeter.class")));
            assertEquals("foreign", call(greeter, "text"), "the other tool's redefinition wins; Lemon does not overwrite it");
            PatchTransformer.SlotInfo slot = PatchTransformer.INSTANCE.slot(greeter).orElseThrow();
            assertNotNull(slot.displaced(), "the transformer noticed that the class was redefined underneath");
            assertEquals(1, s.status().displacedClasses());
            inst().retransformClasses(greeter);
            assertEquals("foreign", call(greeter, "text"), "a displaced patch is not supplied any more");

            LivePatchService.Result check = await(s.verify(ACTOR, "rt-displaced"));
            assertEquals(AppliedBytecodeCheck.Outcome.DIFFERS, check.verification().outcome(), "never reported as verified");
            assertTrue(check.details().toString().contains("DISPLACED"), check.details().toString());
            PatchPlan again = await(s.inspect("rt-displaced", Mechanism.RETRANSFORM));
            assertFalse(again.applicable());

            LivePatchService.Result rb = rollback(s, "rt-displaced");
            assertEquals(Kind.ROLLED_BACK, rb.kind(), rb.message());
            assertEquals(AppliedBytecodeCheck.Outcome.DIFFERS, rb.verification().outcome(),
                    "Lemon removed its patch, but the class runs the other tool's version, not the jar's: " + rb.details());
            assertEquals("foreign", call(greeter, "text"));
            assertTrue(PatchTransformer.INSTANCE.slot(greeter).isEmpty());
        }
    }

    @Test
    void retransformationPatchesStackAndRedefinitionIsRefusedOnTop() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("rtc.Greeter", greeter("rtc", "v1")));
        Path v2 = jar(this.dir.resolve("v2"), "classes.jar", Map.of("rtc.Greeter", greeter("rtc", "v2")));
        Path v3 = jar(this.dir.resolve("v3"), "classes.jar", Map.of("rtc.Greeter", greeter("rtc", "v3")));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> greeter = Class.forName("rtc.Greeter", true, f.platform.serverClassLoader());
            tool(server, v2, inbox(f), "layer-one");
            tool(server, v3, inbox(f), "layer-two");
            LivePatchService s = f.runtime.livePatches();
            assertEquals(Kind.APPLIED, apply(s, "layer-one", Mechanism.RETRANSFORM).kind());

            PatchPlan redefine = await(s.inspect("layer-two", Mechanism.REDEFINE));
            assertFalse(redefine.applicable(), "a redefinition would push the retransformation patch out");
            assertTrue(redefine.errors().toString().contains("runs retransformation patch [layer-one]"), redefine.errors().toString());

            LivePatchService.Result two = apply(s, "layer-two", Mechanism.RETRANSFORM);
            assertEquals(Kind.APPLIED, two.kind(), two.message() + " " + two.details());
            assertEquals("layer-one", two.plan().entries().get(0).replaces());
            assertEquals("v3", call(greeter, "text"));
            assertEquals(java.util.List.of("layer-one", "layer-two"), PatchTransformer.INSTANCE.slot(greeter).orElseThrow().patchIds());

            LivePatchService.Result blocked = await(s.rollback(ACTOR, "layer-one", false));
            assertEquals(Kind.REJECTED, blocked.kind(), "last in, first out");
            assertEquals(Kind.ROLLED_BACK, rollback(s, "layer-two").kind());
            assertEquals("v2", call(greeter, "text"), "the lower layer runs again");
            assertEquals(Kind.ROLLED_BACK, rollback(s, "layer-one").kind());
            assertEquals("v1", call(greeter, "text"));
        }
    }

    @Test
    void retransformationOnTopOfARedefinitionPatchFallsBackToThatPatch() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("rtd.Greeter", greeter("rtd", "v1")));
        Path v2 = jar(this.dir.resolve("v2"), "classes.jar", Map.of("rtd.Greeter", greeter("rtd", "v2")));
        Path v3 = jar(this.dir.resolve("v3"), "classes.jar", Map.of("rtd.Greeter", greeter("rtd", "v3")));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> greeter = Class.forName("rtd.Greeter", true, f.platform.serverClassLoader());
            tool(server, v2, inbox(f), "redefined");
            tool(server, v3, inbox(f), "retransformed");
            LivePatchService s = f.runtime.livePatches();
            assertEquals(Kind.APPLIED, apply(s, "redefined", Mechanism.REDEFINE).kind());
            assertEquals(Kind.APPLIED, apply(s, "retransformed", Mechanism.RETRANSFORM).kind());
            assertEquals("v3", call(greeter, "text"));
            LivePatchService.Result rb = rollback(s, "retransformed");
            assertEquals(Kind.ROLLED_BACK, rb.kind());
            assertEquals(AppliedBytecodeCheck.Outcome.VERIFIED, rb.verification().outcome(), rb.details().toString());
            assertEquals("v2", call(greeter, "text"), "the redefined version is the class's initial bytes now");
            assertEquals(Kind.ROLLED_BACK, rollback(s, "redefined").kind());
            assertEquals("v1", call(greeter, "text"));
        }
    }

    @Test
    void structuralChangesAreNeverAppliedByRetransformation() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("rte.Greeter", greeter("rte", "v1")));
        Path next = jar(this.dir.resolve("v2"), "classes.jar", Map.of("rte.Greeter", greeter("rte", "v2").replace(
                "public String hello()", "public static int added() { return 7; }\n    public String hello()")));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> greeter = Class.forName("rte.Greeter", true, f.platform.serverClassLoader());
            tool(server, next, inbox(f), "rt-structural");
            LivePatchService s = f.runtime.livePatches();
            PatchPlan plan = await(s.inspect("rt-structural", Mechanism.RETRANSFORM));
            assertFalse(plan.applicable(), "independent of the JVM: Lemon applies only method bodies by retransformation");
            assertTrue(plan.errors().toString().contains("only method-body changes by retransformation"), plan.errors().toString());
            LivePatchService.Result r = apply(s, "rt-structural", Mechanism.RETRANSFORM);
            assertEquals(Kind.REJECTED, r.kind());
            assertEquals("v1", call(greeter, "text"));
            assertTrue(PatchTransformer.INSTANCE.slots().isEmpty(), "nothing was handed to the transformer");
        }
    }

    @Test
    void multiClassRetransformationIsAllOrNothing() throws Exception {
        Map<String, String> v1 = Map.of("rtf.A", greeter("rtf", "v1").replace("Greeter", "A"),
                "rtf.B", greeter("rtf", "v1").replace("Greeter", "B"));
        Map<String, String> v2 = Map.of("rtf.A", greeter("rtf", "v2").replace("Greeter", "A"),
                "rtf.B", greeter("rtf", "v2").replace("Greeter", "B"));
        Path server = jar(this.dir.resolve("v1"), "server.jar", v1);
        Path classes = unzipClasses(jar(this.dir.resolve("v2"), "classes.jar", v2), this.dir.resolve("v2classes"));
        Path b = classes.resolve("rtf/B.class");
        byte[] bytes = Files.readAllBytes(b);
        int code = ClassFileReader.read(bytes).method("text", "()Ljava/lang/String;").orElseThrow().codeOffset();
        bytes[code] = 0; // max_stack 0: parses, but fails verification
        bytes[code + 1] = 0;
        Files.write(b, bytes);
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> a = Class.forName("rtf.A", true, f.platform.serverClassLoader());
            Class<?> bc = Class.forName("rtf.B", true, f.platform.serverClassLoader());
            tool(server, classes, inbox(f), "rt-two");
            LivePatchService s = f.runtime.livePatches();
            boolean jvmVerifies = JvmCapabilities.ensure(inst(), EnumSet.of(Capability.VERIFY_REDEFINED_CLASSES))
                    .get(Capability.VERIFY_REDEFINED_CLASSES).supported();
            LivePatchService.Result r = apply(s, "rt-two", Mechanism.RETRANSFORM);
            if (jvmVerifies) {
                assertEquals(Kind.FAILED, r.kind(), r.message());
                assertTrue(r.message().contains("the JVM rejected the retransformation"), r.message());
                assertTrue(r.message().contains("VerifyError"), r.message());
            } else {
                assertEquals(Kind.REJECTED, r.kind(), "pre-verification must stop invalid bytecode: " + r.message());
            }
            assertEquals("v1", call(a, "text"), "A was valid but is not changed: all or nothing");
            assertEquals("v1", call(bc, "text"));
            assertTrue(PatchTransformer.INSTANCE.slots().isEmpty(), "a failed patch is removed from the transformer");
            inst().retransformClasses(a, bc);
            assertEquals("v1", call(a, "text"), "and does not come back with a later retransformation");
            assertEquals(0, LivePatchRegistry.INSTANCE.activeCount());
        }
    }

    @Test
    void theConfirmationMustUseTheMechanismOfThePlan() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "server.jar", Map.of("rtg.Greeter", greeter("rtg", "v1")));
        Path next = jar(this.dir.resolve("v2"), "classes.jar", Map.of("rtg.Greeter", greeter("rtg", "v2")));
        try (RuntimeFixture f = fixture(this.dir, server)) {
            Class<?> greeter = Class.forName("rtg.Greeter", true, f.platform.serverClassLoader());
            tool(server, next, inbox(f), "rt-mixed");
            LivePatchService s = f.runtime.livePatches();
            assertEquals(Kind.CONFIRMATION_REQUIRED, await(s.apply(ACTOR, "rt-mixed", false, Mechanism.RETRANSFORM)).kind());
            LivePatchService.Result wrong = await(s.apply(ACTOR, "rt-mixed", true, Mechanism.REDEFINE));
            assertEquals(Kind.REJECTED, wrong.kind());
            assertTrue(wrong.message().contains("was made for 'retransform'"), wrong.message());
            assertEquals("v1", call(greeter, "text"));
            assertNull(LivePatchRegistry.INSTANCE.latest("rt-mixed").orElse(null), "nothing was attempted");
        }
    }

    @Test
    void retransformationCapabilityIsProbedWithARealRetransformation() {
        CapabilityProbe.Result r = CapabilityProbe.probe(inst(), Capability.RETRANSFORM_METHOD_BODY);
        assertTrue(r.supported(), r.detail());
        assertTrue(r.detail().contains("(1 -> 2)") && r.detail().contains("(2 -> 1)"), r.detail());
    }
}
