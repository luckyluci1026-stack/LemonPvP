package dev.lemonpvp.runtime.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lemonpvp.runtime.livepatch.tool.LivePatchTool;
import dev.lemonpvp.runtime.testsupport.ModuleJarBuilder;
import dev.lemonpvp.runtime.testsupport.RuntimeFixture;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** /fork instrumentation on the command level: permissions, output, and a full retransformation patch cycle. */
class InstrumentationCommandsTest {

    static final String BUILD = "test:lemon/26.2-DEV-test/1970-01-01T00:00:00Z";

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

    private static void run(RuntimeFixture f, AdminCommand cmd, AdminCommandTest.Actor a, String line) throws Exception {
        f.platform.onMain(() -> {
            cmd.execute(a, line.split(" "));
            return null;
        });
    }

    static Path jar(Path dir, String version) throws Exception {
        return ModuleJarBuilder.create().noDescriptor().resource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n"
                        + "Brand-Id: test:lemon\nImplementation-Version: 26.2-DEV-test\nBuild-Time: 1970-01-01T00:00:00Z\n\n")
                .source("ica.Greeter", "package ica;\npublic class Greeter {\n    public static String text() { return \"" + version
                        + "\"; }\n}\n").writeTo(dir, "server.jar", false);
    }

    @Test
    void readingNeedsLiveViewChangingNeedsLiveUpdate() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            AdminCommand cmd = new AdminCommand(f.runtime);
            AdminCommandTest.Actor viewer = new AdminCommandTest.Actor(RuntimePermissions.BASE, RuntimePermissions.LIVE_VIEW);
            run(f, cmd, viewer, "instrumentation status");
            assertTrue(viewer.saw("SUCCESS live-patch agent 3 loaded via -javaagent"), viewer.lines.toString());
            assertTrue(viewer.saw("SUCCESS retransformClasses: supported"), viewer.lines.toString());
            assertTrue(viewer.saw("Lemon transformer: "), viewer.lines.toString());
            assertTrue(viewer.saw("live-patching.enabled is false"), viewer.lines.toString());

            run(f, cmd, viewer, "instrumentation classes java.lang.Strin jdk");
            viewer.await("Loaded classes matching 'java.lang.Strin' in jdk");
            viewer.await("java.lang.String - jdk - modifiable");
            run(f, cmd, viewer, "instrumentation classes bad;name");
            viewer.await("Usage: /fork instrumentation classes <filter> [component] [page]");
            run(f, cmd, viewer, "instrumentation inspect java.lang.String");
            viewer.await("HEADER Class java.lang.String");
            viewer.await("isModifiableClass: true");
            viewer.await("Lemon: not changed by Lemon in this process");

            run(f, cmd, viewer, "instrumentation inspect java.lang.String verify");
            assertTrue(viewer.saw("do not have permission (" + RuntimePermissions.LIVE_UPDATE + ")"));
            for (String changing : List.of("patch apply x", "patch rollback x", "patch verify x")) {
                viewer.lines.clear();
                run(f, cmd, viewer, "instrumentation " + changing);
                assertTrue(viewer.saw("do not have permission (" + RuntimePermissions.LIVE_UPDATE + ")"), changing);
            }
            viewer.lines.clear();
            run(f, cmd, viewer, "instrumentation patch check nope retransform");
            viewer.await("NOT DONE: live patching is disabled");

            AdminCommandTest.Actor nobody = new AdminCommandTest.Actor(RuntimePermissions.BASE);
            run(f, cmd, nobody, "instrumentation status");
            assertTrue(nobody.saw("do not have permission (" + RuntimePermissions.LIVE_VIEW + ")"));
            run(f, cmd, nobody, "instrumentation");
            assertTrue(nobody.saw("HEADER Lemon JVM instrumentation"));
            assertFalse(nobody.saw("patch apply"), "changing commands are not advertised without permission");

            AdminCommandTest.Actor admin = new AdminCommandTest.Actor(RuntimePermissions.BASE, RuntimePermissions.LIVE_VIEW,
                    RuntimePermissions.LIVE_UPDATE);
            run(f, cmd, admin, "instrumentation patch apply x sideways");
            assertTrue(admin.saw("Usage: /fork instrumentation patch check <id> [redefine|retransform]"), admin.lines.toString());
            run(f, cmd, admin, "instrumentation inspect java.lang.String verify");
            admin.await("needs live-patching.enabled: true");

            assertEquals(List.of("patch"), cmd.complete(admin, new String[] {"instrumentation", "pa"}));
            assertEquals(List.of("retransform", "redefine", "confirm").stream().sorted().toList(),
                    cmd.complete(admin, new String[] {"instrumentation", "patch", "apply", "x", ""}).stream().sorted().toList());
            assertTrue(cmd.complete(admin, new String[] {"instrumentation", "classes", "x", "cr"}).contains("craftbukkit"));
        }
    }

    @Test
    void retransformationPatchCycleThroughTheCommands() throws Exception {
        Path server = jar(this.dir.resolve("v1"), "v1");
        Path next = jar(this.dir.resolve("v2"), "v2");
        try (RuntimeFixture f = RuntimeFixture.start(this.dir.resolve("server"), "live-patching:", "  enabled: true",
                "  profile: development")) {
            f.platform.serverClassLoader(new URLClassLoader(new URL[] {server.toUri().toURL()}, getClass().getClassLoader()));
            f.platform.buildId(BUILD);
            Class<?> greeter = Class.forName("ica.Greeter", true, f.platform.serverClassLoader());
            ByteArrayOutputStream toolOut = new ByteArrayOutputStream();
            assertEquals(0, LivePatchTool.run(new String[] {"create", "--base", server.toString(), "--classes", next.toString(), "--out",
                    f.serverDir.resolve("lemon/patches/inbox").toString(), "--id", "cmd-fix"},
                    new PrintStream(toolOut, true, StandardCharsets.UTF_8)), toolOut.toString(StandardCharsets.UTF_8));
            AdminCommand cmd = new AdminCommand(f.runtime);
            AdminCommandTest.Actor admin = new AdminCommandTest.Actor(RuntimePermissions.BASE, RuntimePermissions.LIVE_VIEW,
                    RuntimePermissions.LIVE_UPDATE);

            run(f, cmd, admin, "instrumentation patch check cmd-fix retransform");
            admin.await("Apply: /fork instrumentation patch apply cmd-fix retransform");
            assertTrue(admin.saw("mechanism retransform"), admin.lines.toString());
            run(f, cmd, admin, "instrumentation patch apply cmd-fix retransform");
            admin.await("Run /fork instrumentation patch apply cmd-fix retransform confirm to change the running server.");
            assertEquals("v1", greeter.getMethod("text").invoke(null), "nothing changes before the confirmation");
            run(f, cmd, admin, "instrumentation patch apply cmd-fix retransform confirm");
            admin.await("SUCCESS OK patch cmd-fix applied and verified");
            assertEquals("v2", greeter.getMethod("text").invoke(null));
            assertTrue(admin.saw("retransformed ica.Greeter"), admin.lines.toString());

            run(f, cmd, admin, "instrumentation classes ica.Greeter");
            admin.await("ica.Greeter - library - modifiable - retransformed by cmd-fix");
            run(f, cmd, admin, "instrumentation inspect ica.Greeter");
            admin.await("Lemon: retransformed by cmd-fix");
            run(f, cmd, admin, "instrumentation status");
            admin.await("ica.Greeter: layers [cmd-fix]");
            run(f, cmd, admin, "instrumentation patch verify cmd-fix");
            admin.await("OK patch cmd-fix: verified: the JVM runs the expected bytecode");
            run(f, cmd, admin, "instrumentation inspect ica.Greeter verify");
            admin.await("the running bytecode equals the version of active patch cmd-fix");
            run(f, cmd, admin, "instrumentation history");
            admin.await("cmd-fix (retransform) active by actor");

            run(f, cmd, admin, "instrumentation patch rollback cmd-fix");
            admin.await("Run /fork instrumentation patch rollback cmd-fix confirm to change the running server.");
            run(f, cmd, admin, "instrumentation patch rollback cmd-fix confirm");
            admin.await("OK patch cmd-fix rolled back and verified");
            assertEquals("v1", greeter.getMethod("text").invoke(null));
            f.awaitAudit("patch-rollback | cmd-fix | OK");
        }
    }
}
