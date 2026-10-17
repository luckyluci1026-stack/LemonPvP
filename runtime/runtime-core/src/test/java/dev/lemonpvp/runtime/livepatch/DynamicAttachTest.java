package dev.lemonpvp.runtime.livepatch;

import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.greeter;
import static dev.lemonpvp.runtime.livepatch.LivePatchIntegrationTest.jar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test of the dynamic attach path with two real JVM processes: a JVM started
 * WITHOUT {@code -javaagent} gets the agent through {@code java -jar lemon-livepatch-agent.jar
 * attach <pid>} (the launcher in the agent jar, {@code VirtualMachine.loadAgent}), the
 * JVM calls {@code agentmain}, and the instrumentation is then used to retransform a class
 * that was loaded before the attach.
 */
class DynamicAttachTest {

    @TempDir
    Path dir;

    /** Collects the lines of a process stream on a daemon thread. */
    static List<String> drain(InputStream in) {
        List<String> lines = new CopyOnWriteArrayList<>();
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                for (String line; (line = r.readLine()) != null; ) {
                    lines.add(line);
                }
            } catch (java.io.IOException ignored) {
                // process ended
            }
        });
        t.setDaemon(true);
        t.start();
        return lines;
    }

    /** Waits until the stream reader has seen a line starting with {@code prefix} (the process may have ended already). */
    static void awaitLine(List<String> lines, String prefix) throws InterruptedException {
        for (int i = 0; i < 200 && lines.stream().noneMatch(l -> l.startsWith(prefix)); i++) {
            Thread.sleep(25);
        }
    }

    static String java() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    static Path codeSource(Class<?> c) throws Exception {
        return Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    @Test
    void agentAttachedToARunningJvmProvidesInstrumentationThatRetransformsAlreadyLoadedClasses() throws Exception {
        Path agent = Path.of(System.getProperty("lemon.test.agentJar"));
        assertTrue(Files.isRegularFile(agent), agent.toString());
        Path v1 = jar(this.dir.resolve("v1"), "server.jar", Map.of("att.Greeter", greeter("att", "v1")));
        Path v2 = jar(this.dir.resolve("v2"), "classes.jar", Map.of("att.Greeter", greeter("att", "v2")));
        Path v2Class = this.dir.resolve("Greeter-v2.class");
        try (JarFile j = new JarFile(v2.toFile()); InputStream in = j.getInputStream(j.getEntry("att/Greeter.class"))) {
            Files.write(v2Class, in.readAllBytes());
        }
        // Only Lemon's own classes: test classes (AttachTarget) and runtime-core (InstrumentationAccess, PatchTransformer).
        String cp = codeSource(AttachTarget.class) + java.io.File.pathSeparator + codeSource(PatchTransformer.class);
        Process target = new ProcessBuilder(java(), "-cp", cp, AttachTarget.class.getName(), v1.toString(), v2Class.toString())
                .start();
        List<String> out = drain(target.getInputStream());
        List<String> err = drain(target.getErrorStream());
        try {
            for (int i = 0; i < 400 && out.stream().noneMatch(l -> l.startsWith("loaded before attach")); i++) {
                Thread.sleep(50);
            }
            Thread.sleep(200); // the target is now polling for the agent
            assertTrue(out.contains("agent before attach: false"), "the target starts without the agent: " + out + err);
            assertTrue(out.contains("loaded before attach: v1"), out + " " + err);

            Process launcher = new ProcessBuilder(java(), "-jar", agent.toString(), "attach", Long.toString(target.pid()))
                    .redirectErrorStream(true).start();
            List<String> launched = drain(launcher.getInputStream());
            assertTrue(launcher.waitFor(60, TimeUnit.SECONDS), "launcher finished");
            awaitLine(launched, "agent loaded");
            assertEquals(0, launcher.exitValue(), "launcher output: " + launched);
            assertTrue(launched.stream().anyMatch(l -> l.startsWith("agent loaded")), launched.toString());

            assertTrue(target.waitFor(60, TimeUnit.SECONDS), "target finished: " + out + err);
            awaitLine(out, "removed:");
            awaitLine(err, "[Lemon]");
            assertEquals(0, target.exitValue(), out + " " + err);
        } finally {
            target.destroyForcibly();
        }
        // Kept in the test report as evidence of what the second JVM printed.
        out.forEach(l -> System.out.println("target stdout: " + l));
        err.forEach(l -> System.out.println("target stderr: " + l));
        assertTrue(out.contains("agent: agent 3 loaded via agentmain (dynamic attach)"), out.toString());
        assertTrue(out.contains("redefine true, retransform true, modifiable true"), out.toString());
        assertTrue(out.contains("patched: v2"), "the class loaded before the attach runs the patch: " + out);
        assertTrue(out.stream().anyMatch(l -> l.startsWith("after another retransformation: v2, supplied 2, displaced null")),
                "the reconstituted initial bytes are stable, so the patch is supplied again: " + out);
        assertTrue(out.contains("removed: v1"), out.toString());
        assertTrue(err.stream().anyMatch(l -> l.contains("[Lemon] live-patch agent 3 loaded (agentmain (dynamic attach))")),
                err.toString());
    }

    @Test
    void launcherRejectsBadInputWithoutAttaching() throws Exception {
        Path agent = Path.of(System.getProperty("lemon.test.agentJar"));
        Process usage = new ProcessBuilder(java(), "-jar", agent.toString()).redirectErrorStream(true).start();
        List<String> usageOut = drain(usage.getInputStream());
        assertTrue(usage.waitFor(60, TimeUnit.SECONDS));
        assertEquals(2, usage.exitValue());
        Process bad = new ProcessBuilder(java(), "-jar", agent.toString(), "attach", "12ab").redirectErrorStream(true).start();
        List<String> badOut = drain(bad.getInputStream());
        assertTrue(bad.waitFor(60, TimeUnit.SECONDS));
        assertEquals(2, bad.exitValue());
        awaitLine(usageOut, "Recommended");
        awaitLine(badOut, "not a process id");
        assertTrue(usageOut.stream().anyMatch(l -> l.contains("java -jar lemon-livepatch-agent.jar attach <pid>")), usageOut.toString());
        assertTrue(badOut.stream().anyMatch(l -> l.contains("not a process id: 12ab")), badOut.toString());
    }
}
