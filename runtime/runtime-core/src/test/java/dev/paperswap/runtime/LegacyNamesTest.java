package dev.paperswap.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.paperswap.runtime.commands.AdminCommand;
import dev.paperswap.runtime.commands.CommandActor;
import dev.paperswap.runtime.commands.MessageLevel;
import dev.paperswap.runtime.commands.RuntimePermissions;
import dev.paperswap.runtime.descriptor.DescriptorException;
import dev.paperswap.runtime.loader.ModuleArtifact;
import dev.paperswap.runtime.loader.ModuleScanner;
import dev.paperswap.runtime.testsupport.ModuleJarBuilder;
import dev.paperswap.runtime.testsupport.RuntimeFixture;
import dev.paperswap.runtime.testsupport.TestModules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Servers and modules from before the rename to PaperSwap (project name "Lemon") keep working. */
class LegacyNamesTest {

    @TempDir
    Path dir;

    @Test
    void aNewServerUsesTheNewNames() {
        RuntimePaths p = RuntimePaths.of(this.dir);
        assertEquals(this.dir.resolve("config/paperswap.yml"), p.configFile());
        assertEquals(this.dir.resolve("paperswap"), p.root());
        assertTrue(p.legacyNotes().isEmpty());
    }

    @Test
    void aServerSetUpBeforeTheRenameKeepsItsConfigurationAndData() throws Exception {
        Files.createDirectories(this.dir.resolve("config"));
        Files.writeString(this.dir.resolve("config/lemon-runtime.yml"), "config-version: 1\nenabled: false\n");
        Files.createDirectories(this.dir.resolve("lemon/modules"));
        RuntimePaths old = RuntimePaths.of(this.dir);
        assertEquals(this.dir.resolve("config/lemon-runtime.yml"), old.configFile());
        assertEquals(this.dir.resolve("lemon"), old.root());
        List<String> notes = old.legacyNotes();
        assertEquals(2, notes.size(), notes.toString());
        assertTrue(notes.get(0).contains("rename it to config/paperswap.yml"), notes.get(0));
        assertTrue(notes.get(1).contains("rename it to paperswap/"), notes.get(1));

        // Once the files are moved, the new names win.
        Files.move(this.dir.resolve("config/lemon-runtime.yml"), this.dir.resolve("config/paperswap.yml"));
        Files.move(this.dir.resolve("lemon"), this.dir.resolve("paperswap"));
        RuntimePaths moved = RuntimePaths.of(this.dir);
        assertEquals(this.dir.resolve("config/paperswap.yml"), moved.configFile());
        assertEquals(this.dir.resolve("paperswap"), moved.root());
        assertTrue(moved.legacyNotes().isEmpty());
    }

    @Test
    void aRuntimeStartedFromTheOldNamesSaysSoInTheLogAndInStatus() throws Exception {
        Files.createDirectories(this.dir.resolve("config"));
        Files.writeString(this.dir.resolve("config/lemon-runtime.yml"), "enabled: true\nmodule-directory: lemon/modules\n");
        Files.createDirectories(this.dir.resolve("lemon/modules"));
        try (RuntimeFixture f = RuntimeFixture.startExisting(this.dir)) {
            assertEquals(this.dir.resolve("lemon/modules"), f.runtime.paths().moduleDirectory(f.runtime.config()));
            assertTrue(f.platform.logLines().stream().anyMatch(l -> l.contains("PaperSwap (formerly Lemon)")
                    && l.contains("rename it to config/paperswap.yml")), f.platform.logLines().toString());
            List<String> lines = new CopyOnWriteArrayList<>();
            CommandActor console = new CommandActor() {
                @Override
                public String name() {
                    return "console";
                }

                @Override
                public boolean hasPermission(String node) {
                    return RuntimePermissions.BASE.equals(node);
                }

                @Override
                public void send(MessageLevel level, String text) {
                    lines.add(level + " " + text);
                }
            };
            f.platform.onMain(() -> {
                new AdminCommand(f.runtime).execute(console, new String[] {"status"});
                return null;
            });
            assertTrue(lines.stream().anyMatch(l -> l.startsWith("WARNING Old name (Lemon): ") && l.contains("rename it to paperswap/")),
                    lines.toString());
        }
    }

    @Test
    void aModuleWithTheOldDescriptorNameStillLoadsButNotTwoDescriptors() throws Exception {
        Path modules = Files.createDirectories(this.dir.resolve("modules"));
        String descriptor = "id: oldmod\nversion: 1.0.0\nmain: mod.oldmod.Main\napi-version: 1\n";
        Path old = ModuleJarBuilder.create().noDescriptor().resource("lemon-module.yml", descriptor)
                .source("mod.oldmod.Main", TestModules.basic("mod.oldmod", "oldmod")).writeTo(modules, "oldmod.jar", false);
        ModuleArtifact artifact = new ModuleScanner(modules).inspect(old);
        assertEquals("oldmod", artifact.id());

        Path both = ModuleJarBuilder.create().descriptor("twomod", "1.0.0", "mod.twomod.Main")
                .resource("lemon-module.yml", descriptor.replace("oldmod", "twomod"))
                .source("mod.twomod.Main", TestModules.basic("mod.twomod", "twomod")).writeTo(modules, "twomod.jar", false);
        DescriptorException e = assertThrows(DescriptorException.class, () -> new ModuleScanner(modules).inspect(both));
        assertTrue(e.getMessage().contains("keep only one descriptor"), e.getMessage());
    }
}
