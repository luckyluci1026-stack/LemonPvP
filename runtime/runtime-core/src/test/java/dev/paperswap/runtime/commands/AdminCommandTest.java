package dev.paperswap.runtime.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.paperswap.runtime.api.ModuleState;
import dev.paperswap.runtime.testsupport.ModuleJarBuilder;
import dev.paperswap.runtime.testsupport.RuntimeFixture;
import dev.paperswap.runtime.testsupport.TestModules;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AdminCommandTest {

    @TempDir
    Path dir;

    /** Command sender with a fixed permission set that records output. */
    static final class Actor implements CommandActor {
        final Set<String> permissions;
        final List<String> lines = new CopyOnWriteArrayList<>();

        Actor(String... permissions) {
            this.permissions = Set.of(permissions);
        }

        @Override
        public String name() {
            return "actor";
        }

        @Override
        public boolean hasPermission(String node) {
            return this.permissions.contains(node);
        }

        @Override
        public void send(MessageLevel level, String text) {
            this.lines.add(level + " " + text);
        }

        boolean saw(String fragment) {
            return this.lines.stream().anyMatch(l -> l.contains(fragment));
        }

        void await(String fragment) throws InterruptedException {
            for (int i = 0; i < 100 && !saw(fragment); i++) {
                Thread.sleep(50);
            }
            assertTrue(saw(fragment), "expected output containing '" + fragment + "' but got " + this.lines);
        }
    }

    private static void run(RuntimeFixture f, AdminCommand cmd, Actor a, String line) throws Exception {
        f.platform.onMain(() -> {
            cmd.execute(a, line.isEmpty() ? new String[0] : line.split(" "));
            return null;
        });
    }

    @Test
    void permissionsAreCheckedForEverySubcommand() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            ModuleJarBuilder.create().descriptor("perm", "1.0.0", "mod.perm.Main")
                    .source("mod.perm.Main", TestModules.basic("mod.perm", "perm")).writeTo(f.modules, "perm.jar");
            AdminCommand cmd = new AdminCommand(f.runtime);

            Actor nobody = new Actor();
            run(f, cmd, nobody, "status");
            assertTrue(nobody.saw("do not have permission (" + RuntimePermissions.BASE + ")"));

            Actor viewer = new Actor(RuntimePermissions.BASE, RuntimePermissions.MODULE_VIEW);
            run(f, cmd, viewer, "module enable perm");
            assertTrue(viewer.saw("do not have permission (" + RuntimePermissions.MODULE_MANAGE + ")"));
            run(f, cmd, viewer, "reload");
            assertTrue(viewer.saw("do not have permission (" + RuntimePermissions.RELOAD + ")"));
            run(f, cmd, viewer, "module list");
            viewer.await("perm 1.0.0 - not loaded");
            assertTrue(f.runtime.modules().modules().isEmpty(), "denied commands changed nothing");

            Actor admin = new Actor(RuntimePermissions.BASE, RuntimePermissions.MODULE_MANAGE);
            run(f, cmd, admin, "module list");
            assertTrue(admin.saw("do not have permission (" + RuntimePermissions.MODULE_VIEW + ")"));
            run(f, cmd, admin, "module enable perm");
            admin.await("OK enable perm");
            assertEquals(ModuleState.ENABLED, f.runtime.modules().modules().getFirst().state());
        }
    }

    @Test
    void argumentsAreValidated() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            AdminCommand cmd = new AdminCommand(f.runtime);
            Actor admin = new Actor(RuntimePermissions.BASE, RuntimePermissions.MODULE_VIEW, RuntimePermissions.MODULE_MANAGE,
                    RuntimePermissions.RELOAD);
            run(f, cmd, admin, "module enable ../../plugins/x");
            assertTrue(admin.saw("Invalid module id"));
            run(f, cmd, admin, "module enable");
            assertTrue(admin.saw("Usage: /fork module enable <id>"));
            run(f, cmd, admin, "module enable a b");
            run(f, cmd, admin, "module explode x");
            assertTrue(admin.saw("PaperSwap runtime commands"), "unknown subcommands show usage");
            run(f, cmd, admin, "module enable <red>evil");
            assertTrue(admin.saw("Invalid module id '<red>evil'"), "input is echoed as plain text only");
            run(f, cmd, admin, "info");
            assertTrue(admin.saw("Hot deployment: enabled"));
            run(f, cmd, admin, "status");
            assertTrue(admin.saw("No unresolved problems"));
            run(f, cmd, admin, "reload");
            admin.await("Runtime configuration applied");
        }
    }

    @Test
    void tabCompletionRespectsPermissions() throws Exception {
        try (RuntimeFixture f = RuntimeFixture.start(this.dir)) {
            AdminCommand cmd = new AdminCommand(f.runtime);
            assertTrue(cmd.complete(new Actor(), new String[]{""}).isEmpty());
            List<String> viewer = cmd.complete(new Actor(RuntimePermissions.BASE, RuntimePermissions.MODULE_VIEW),
                    new String[]{"module", ""});
            assertTrue(viewer.contains("list"));
            assertFalse(viewer.contains("enable"));
            assertEquals(List.of("reload"), cmd.complete(new Actor(RuntimePermissions.BASE), new String[]{"rel"}));
        }
    }
}
