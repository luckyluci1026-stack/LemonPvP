/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.config;

import ac.grim.grimac.manager.config.update.ConfigUpdater;
import ac.grim.grimac.manager.config.update.GrimConfigSpecs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The upgrade path an existing server takes on its next restart.
 *
 * <p>This is the change nobody sees coming. Bumping {@code config-version}
 * rewrites the operator's file in place: it keeps the bundled default's
 * structure and comments and lifts the operator's own values across. Getting
 * it wrong silently resets somebody's tuned thresholds back to stock, or
 * leaves the file unreadable and the plugin refusing to load — and the only
 * way to find out by hand is to have an old config lying around and restart a
 * server, which is exactly what nobody does before shipping.</p>
 */
class ConfigMigrationTest {

    private static final Path FIXTURE = Path.of("src/test/resources/config-v14-en.yml");

    /** Same anchor the plugin uses: the bundled defaults live next to this class. */
    private static ConfigUpdater updater() {
        return new ConfigUpdater(ConfigUpdater.class, Logger.getLogger("config-migration-test"));
    }

    private static Path stageFixture(Path dir) throws IOException {
        assertTrue(Files.isRegularFile(FIXTURE), "missing fixture " + FIXTURE.toAbsolutePath());
        Path configFile = dir.resolve("config.yml");
        Files.copy(FIXTURE, configFile, StandardCopyOption.REPLACE_EXISTING);
        return configFile;
    }

    @Test
    void anOldConfigIsLiftedToTheCurrentVersion(@TempDir Path dir) throws IOException {
        Path configFile = stageFixture(dir);

        ConfigUpdater.Result result = updater().update(configFile.toFile(), GrimConfigSpecs.mainConfig());

        assertTrue(result.migrated, "a version-14 file should have been migrated");
        assertEquals(14, result.oldVersion);
        assertTrue(result.newVersion > 14, "should have been stamped forward, got " + result.newVersion);
        assertEquals(result.newVersion, asInt(read(configFile).get("config-version")));
    }

    @Test
    void theOperatorsOwnSettingsSurvive(@TempDir Path dir) throws IOException {
        // The whole point of migrating rather than overwriting. Somebody who
        // tuned these numbers must not find them back at stock after a
        // restart they did not ask for.
        Path configFile = stageFixture(dir);

        updater().update(configFile.toFile(), GrimConfigSpecs.mainConfig());
        Map<String, Object> migrated = read(configFile);

        assertEquals(200, asInt(migrated.get("update-permission-ticks")));
        assertEquals(Boolean.FALSE, migrated.get("experimental-checks"));
        assertEquals(25, asInt(nested(migrated, "Autoclicker", "max-attack-cps")));
    }

    @Test
    void theNewKeysArrive(@TempDir Path dir) throws IOException {
        // Without this the bump is pointless: an existing server would never
        // gain the switch, and the operator has no way to rehearse the
        // punishment ladders before arming them.
        Path configFile = stageFixture(dir);
        assertFalse(read(configFile).containsKey("punishment-dry-run"), "fixture should predate the key");

        updater().update(configFile.toFile(), GrimConfigSpecs.mainConfig());

        Map<String, Object> migrated = read(configFile);
        assertTrue(migrated.containsKey("punishment-dry-run"), "punishment-dry-run never reached the file");
        assertEquals(Boolean.FALSE, migrated.get("punishment-dry-run"), "must default to off");
    }

    @Test
    void theOldFileIsKeptAsABackup(@TempDir Path dir) throws IOException {
        Path configFile = stageFixture(dir);

        updater().update(configFile.toFile(), GrimConfigSpecs.mainConfig());

        try (var entries = Files.list(dir)) {
            List<String> names = entries.map(p -> p.getFileName().toString()).sorted().toList();
            assertTrue(names.stream().anyMatch(n -> n.endsWith(".bak")),
                    "no backup written; a bad migration would be unrecoverable. Files: " + names);
        }
    }

    @Test
    void migratingTwiceChangesNothingTheSecondTime(@TempDir Path dir) throws IOException {
        // Every restart runs this. If it were not idempotent, the file would
        // be rewritten and re-backed-up on every single boot.
        Path configFile = stageFixture(dir);

        updater().update(configFile.toFile(), GrimConfigSpecs.mainConfig());
        String afterFirst = Files.readString(configFile, StandardCharsets.UTF_8);

        ConfigUpdater.Result second = updater().update(configFile.toFile(), GrimConfigSpecs.mainConfig());

        assertFalse(second.migrated, "an already-current file should not be migrated again");
        assertEquals(afterFirst, Files.readString(configFile, StandardCharsets.UTF_8),
                "the file changed on a no-op run");
    }

    @Test
    void theMigratedFileStillParsesAndKeepsItsComments(@TempDir Path dir) throws IOException {
        Path configFile = stageFixture(dir);

        updater().update(configFile.toFile(), GrimConfigSpecs.mainConfig());
        String text = Files.readString(configFile, StandardCharsets.UTF_8);

        assertNotNull(read(configFile), "migrated file does not parse as YAML");
        assertTrue(text.contains("#"), "all commentary was stripped; the file is the documentation");
        assertTrue(text.contains("config-flavor: V2"), "the flavor marker must survive");
    }

    // ------------------------------------------------------------ messages

    private static final Path MESSAGES_FIXTURE = Path.of("src/test/resources/messages-v2-en.yml");

    private static Path stageMessages(Path dir) throws IOException {
        assertTrue(Files.isRegularFile(MESSAGES_FIXTURE), "missing " + MESSAGES_FIXTURE.toAbsolutePath());
        Path file = dir.resolve("messages.yml");
        Files.copy(MESSAGES_FIXTURE, file, StandardCopyOption.REPLACE_EXISTING);
        return file;
    }

    @Test
    void theBanLengthCeilingReachesAnExistingMessagesFile(@TempDir Path dir) throws IOException {
        // Without this key, /acban perm still produces a permanent ban on a
        // server that has been running since before the ceiling existed.
        Path file = stageMessages(dir);

        ConfigUpdater.Result result = updater().update(file.toFile(), GrimConfigSpecs.messages());
        Map<String, Object> migrated = read(file);

        assertTrue(result.migrated);
        assertEquals("14d", migrated.get("acban-max-duration"));
        assertEquals("3d", migrated.get("acban-default-duration"), "the operator's own length was reset");
    }

    @Test
    void theBanScreenSurvivesTheRewriteCharacterForCharacter(@TempDir Path dir) throws IOException {
        // One very long quoted line full of gradients, box-drawing characters
        // and angle brackets. If the line-mapped patcher mangles anything, it
        // mangles this - and the result is a ban screen that throws while a
        // banned player is being disconnected.
        Path file = stageMessages(dir);
        String before = (String) read(file).get("acban-screen");
        assertNotNull(before, "fixture should carry a ban screen");

        updater().update(file.toFile(), GrimConfigSpecs.messages());

        assertEquals(before, read(file).get("acban-screen"));
    }

    // ------------------------------------------------------------- helpers

    @SuppressWarnings("unchecked")
    private static Map<String, Object> read(Path file) throws IOException {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object root = yaml.load(reader);
            return root instanceof Map ? (Map<String, Object>) root : Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private static Object nested(Map<String, Object> root, String outer, String inner) {
        Object branch = root.get(outer);
        assertTrue(branch instanceof Map, outer + " is missing or not a section");
        return ((Map<String, Object>) branch).get(inner);
    }

    private static int asInt(Object value) {
        assertTrue(value instanceof Number, "expected a number, got " + value);
        return ((Number) value).intValue();
    }
}
