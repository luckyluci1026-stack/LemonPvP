package dev.lemonpvp.runtime.livepatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lemonpvp.runtime.livepatch.InstrumentationDiagnostics.Finding;
import dev.lemonpvp.runtime.livepatch.InstrumentationDiagnostics.Level;
import dev.lemonpvp.runtime.security.AuditLog;
import dev.lemonpvp.runtime.testsupport.RuntimeFixture;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Startup diagnostics, JDWP check and the agent lookup that picks up a later attach. */
class InstrumentationDiagnosticsTest {

    @TempDir
    Path dir;

    static boolean has(InstrumentationDiagnostics.Report r, Level level, String fragment) {
        return r.findings().stream().anyMatch(f -> f.level() == level && f.text().contains(fragment));
    }

    @Test
    void withTheAgentEverythingIsReportedAvailableAndClassesAreCounted() {
        InstrumentationDiagnostics.Report r = InstrumentationDiagnostics.collect(InstrumentationAccess.find(), true, true);
        assertTrue(has(r, Level.OK, "live-patch agent 3 loaded via -javaagent"), r.findings().toString());
        assertTrue(has(r, Level.OK, "redefineClasses: supported"), r.findings().toString());
        assertTrue(has(r, Level.OK, "retransformClasses: supported"), r.findings().toString());
        InstrumentationDiagnostics.ClassCounts counts = r.classCounts().orElseThrow();
        assertTrue(counts.loaded() > 500 && counts.modifiable() > 0 && counts.modifiable() < counts.loaded(), counts.toString());
        assertTrue(counts.hidden() > 0, "lambdas are hidden classes: " + counts);
        assertFalse(has(r, Level.MISSING, ""), r.findings().toString());
    }

    @Test
    void withoutTheAgentTheFixIsExplained() {
        InstrumentationAccess.Lookup none = new InstrumentationAccess.Lookup(Optional.empty(), 0, "not loaded",
                "live-patch agent not loaded");
        InstrumentationDiagnostics.Report r = InstrumentationDiagnostics.collect(none, false, true);
        assertTrue(has(r, Level.MISSING, "live-patch agent not loaded"), r.findings().toString());
        assertTrue(has(r, Level.INFO, "-javaagent:lemon-livepatch-agent.jar")
                && has(r, Level.INFO, "attach " + ProcessHandle.current().pid()), r.findings().toString());
        assertTrue(has(r, Level.INFO, "live-patching.enabled is false"), r.findings().toString());
        assertTrue(r.classCounts().isEmpty(), "classes cannot be counted without instrumentation");
        assertTrue(r.hasProblems());
    }

    @Test
    void jdwpOnAllInterfacesIsAWarningLoopbackIsNot() {
        Optional<Finding> open = InstrumentationDiagnostics.jdwp(List.of("-Xmx1G",
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"));
        assertEquals(Level.WARNING, open.orElseThrow().level());
        assertTrue(open.get().text().contains("anyone who can reach it can run any code"), open.get().text());
        assertEquals(Level.WARNING, InstrumentationDiagnostics.jdwp(List.of(
                "-agentlib:jdwp=transport=dt_socket,server=y,address=0.0.0.0:5005")).orElseThrow().level());
        assertEquals(Level.INFO, InstrumentationDiagnostics.jdwp(List.of(
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:5005")).orElseThrow().level());
        assertEquals(Level.INFO, InstrumentationDiagnostics.jdwp(List.of(
                "-agentlib:jdwp=transport=dt_socket,server=y,address=5005")).orElseThrow().level(),
                "without a host, JDK 9+ binds to localhost");
        assertEquals(Level.WARNING, InstrumentationDiagnostics.jdwp(List.of(
                "-Xrunjdwp:transport=dt_socket,server=y,address=*:8000")).orElseThrow().level());
        assertTrue(InstrumentationDiagnostics.jdwp(List.of("-Xmx1G", "-javaagent:x.jar")).isEmpty());
    }

    @Test
    void anAgentAttachedAfterTheStartIsPickedUpWithoutARestart() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            AtomicInteger calls = new AtomicInteger();
            Supplier<InstrumentationAccess.Lookup> lookup = () -> calls.getAndIncrement() < 2
                    ? new InstrumentationAccess.Lookup(Optional.empty(), 0, "not loaded", "not attached yet")
                    : InstrumentationAccess.find();
            LivePatchService s = new LivePatchService(f.runtime::config, f.runtime.paths(), f.platform, Runnable::run,
                    new AuditLog(this.dir.resolve("audit.log"), Runnable::run, f.platform.logger()), LivePatchRegistry.INSTANCE,
                    PatchTransformer.INSTANCE, lookup);
            LivePatchService.StatusReport first = s.status();
            assertFalse(first.instrumentation(), "constructor and first status call: no agent yet");
            assertEquals("not attached yet", first.agentDetail());
            assertTrue(s.status().instrumentation(), "looked up again and found");
            int afterFound = calls.get();
            s.status();
            s.status();
            assertEquals(afterFound, calls.get(), "once found, the lookup is not repeated");
            assertTrue(f.platform.logLines().stream().anyMatch(l -> l.contains("Live-patch agent found: agent 3 loaded via -javaagent")),
                    f.platform.logLines().toString());
        }
    }

    @Test
    void startupLogExplainsTheInstrumentationState() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir, "live-patching:", "  enabled: true", "  profile: development")) {
            List<String> log = f.platform.logLines();
            assertTrue(log.stream().anyMatch(l -> l.contains("Instrumentation: live-patch agent 3 loaded via -javaagent")), log.toString());
            assertTrue(log.stream().anyMatch(l -> l.contains("Instrumentation: classes: ") && l.contains("modifiable")), log.toString());
        }
    }
}
