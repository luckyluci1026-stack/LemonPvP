package dev.lemonpvp.runtime.livepatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The pre-verification Lemon runs on JVMs whose class redefinition does not verify. */
class PreVerifierTest {

    @Test
    void validClassesPassAndInvalidBytecodeIsRejectedWithoutTouchingTheServerLoader() {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        classes.put("pvtest.Good", new ProbeClassWriter("pvtest/Good", "java/lang/Object")
                .staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1).toBytes());
        classes.put("pvtest.Bad", new ProbeClassWriter("pvtest/Bad", "java/lang/Object")
                .staticIntMethod("value", ProbeClassWriter.ACC_PUBLIC, 1).invalidStack().toBytes());
        classes.put("pvtest.Garbage", "not a class file".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ClassLoader server = getClass().getClassLoader();

        Map<String, PreVerifier.Outcome> r = PreVerifier.verify(classes, server);

        assertEquals(PreVerifier.Outcome.Kind.VERIFIED, r.get("pvtest.Good").kind(), String.valueOf(r.get("pvtest.Good")));
        assertEquals(PreVerifier.Outcome.Kind.INVALID, r.get("pvtest.Bad").kind(), String.valueOf(r.get("pvtest.Bad")));
        assertTrue(r.get("pvtest.Bad").detail().startsWith("VerifyError"), r.get("pvtest.Bad").detail());
        assertEquals(PreVerifier.Outcome.Kind.INVALID, r.get("pvtest.Garbage").kind(), String.valueOf(r.get("pvtest.Garbage")));
        for (String name : classes.keySet()) {
            try {
                Class.forName(name, false, server);
                throw new AssertionError(name + " leaked into the server class loader");
            } catch (ClassNotFoundException expected) {
                // the copies only exist in the throw-away loader
            }
        }
    }

    @Test
    void verifierProbeReportsWhetherTheJvmVerifiesRedefinedClasses() {
        var inst = InstrumentationAccess.find().instrumentation().orElseThrow();
        CapabilityProbe.Result r = CapabilityProbe.probe(inst, Capability.VERIFY_REDEFINED_CLASSES);
        boolean enhanced = "true".equals(JvmCapabilities.enhancedRedefinitionFlag());
        if (!enhanced) {
            assertTrue(r.supported(), "standard HotSpot verifies redefined classes: " + r.detail());
        }
        // With enhanced redefinition either answer is possible; the result is only reported.
        System.out.println("VERIFY_REDEFINED_CLASSES on " + JvmCapabilities.vmDescription() + " (enhanced=" + enhanced + "): "
                + r.supported() + " - " + r.detail());
    }
}
