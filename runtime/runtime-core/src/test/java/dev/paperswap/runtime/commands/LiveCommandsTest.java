package dev.paperswap.runtime.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.paperswap.runtime.testsupport.ModuleJarBuilder;
import dev.paperswap.runtime.testsupport.RuntimeFixture;
import dev.paperswap.runtime.testsupport.TestModules;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** /fork live on the command level (permissions, routing, wording). */
class LiveCommandsTest {

    @TempDir
    Path dir;

    private static void run(RuntimeFixture f, AdminCommand cmd, AdminCommandTest.Actor a, String line) throws Exception {
        f.platform.onMain(() -> {
            cmd.execute(a, line.split(" "));
            return null;
        });
    }

    @Test
    void viewAndUpdatePermissionsAreSeparate() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            AdminCommand cmd = new AdminCommand(f.runtime);
            AdminCommandTest.Actor viewer = new AdminCommandTest.Actor(RuntimePermissions.BASE, RuntimePermissions.LIVE_VIEW);
            run(f, cmd, viewer, "live status");
            viewer.await("Core patches:");
            assertTrue(viewer.saw("agent loaded"), viewer.lines.toString());
            run(f, cmd, viewer, "live update something");
            viewer.await("do not have permission (" + RuntimePermissions.LIVE_UPDATE + ")");
            run(f, cmd, viewer, "live patch probe");
            assertTrue(viewer.saw("do not have permission (" + RuntimePermissions.LIVE_UPDATE + ")"));

            AdminCommandTest.Actor nobody = new AdminCommandTest.Actor(RuntimePermissions.BASE);
            run(f, cmd, nobody, "live scan");
            assertTrue(nobody.saw("do not have permission (" + RuntimePermissions.LIVE_VIEW + ")"));
            run(f, cmd, nobody, "live");
            assertFalse(nobody.saw("/fork live update"), "update is not advertised without permission");
        }
    }

    @Test
    void moduleUpdateThroughLiveNeedsConfirmationAndUsesTheModuleManager() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            ModuleJarBuilder.create().descriptor("livemod", "1.0.0", "mod.livemod.Main")
                    .source("mod.livemod.Main", TestModules.basic("mod.livemod", "livemod")).writeTo(f.modules, "livemod.jar");
            AdminCommand cmd = new AdminCommand(f.runtime);
            AdminCommandTest.Actor admin = new AdminCommandTest.Actor(RuntimePermissions.BASE, RuntimePermissions.LIVE_VIEW,
                    RuntimePermissions.LIVE_UPDATE);
            run(f, cmd, admin, "live check livemod");
            admin.await("OK validate livemod");
            run(f, cmd, admin, "live update livemod confirm");
            admin.await("No update of module livemod is waiting for confirmation");
            run(f, cmd, admin, "live update livemod");
            admin.await("Run /fork live update livemod confirm");
            run(f, cmd, admin, "live update livemod confirm");
            admin.await("OK enable livemod");
            run(f, cmd, admin, "live history");
            admin.await("module livemod enable OK");
            run(f, cmd, admin, "live rollback module:livemod");
            admin.await("Modules have no separate rollback");
            run(f, cmd, admin, "live check nothing-like-this");
            admin.await("No module, plugin or patch named 'nothing-like-this'");
            run(f, cmd, admin, "live inspect livemod");
            admin.await("Diagnostics livemod 1.0.0 - ENABLED");
            run(f, cmd, admin, "live inspect patch:no-such-patch");
            admin.await("No patch file no-such-patch.swappatch and no record in this process.");
            run(f, cmd, admin, "live patch verify no-such-patch");
            admin.await("NOT DONE live patching is disabled");
        }
    }

    @Test
    void patchStatusExplainsWhatIsMissing() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            AdminCommand cmd = new AdminCommand(f.runtime);
            AdminCommandTest.Actor admin = new AdminCommandTest.Actor(RuntimePermissions.BASE, RuntimePermissions.LIVE_VIEW,
                    RuntimePermissions.LIVE_UPDATE);
            run(f, cmd, admin, "live patch status");
            admin.await("live-patching.enabled: false");
            assertTrue(admin.saw("Build id: unknown"), admin.lines.toString());
            run(f, cmd, admin, "live patch probe");
            admin.await("yes change existing method bodies");
            run(f, cmd, admin, "live update patch:some-fix");
            admin.await("live patching is disabled");
        }
    }
}
