package dev.paperswap.runtime.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** live-patching and plugin-updates sections. */
class LiveConfigTest {

    private static RuntimeConfigLoader.Result parse(String yaml) {
        return RuntimeConfigLoader.parse(YamlSupport.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "test"));
    }

    @Test
    void oldConfigFilesWithoutTheNewSectionsStayValidAndDisabled() {
        RuntimeConfigLoader.Result r = parse("config-version: 1\nenabled: true\nallow-runtime-deploy: true\n");
        assertTrue(r.valid(), r.errors().toString());
        assertEquals(LivePatchConfig.defaults(), r.config().livePatching());
        assertEquals(PluginUpdateConfig.defaults(), r.config().pluginUpdates());
        assertFalse(r.config().livePatching().enabled());
        assertFalse(r.config().pluginUpdates().enabled());
    }

    @Test
    void sectionsAreParsedAndValidated() {
        RuntimeConfigLoader.Result r = parse("""
                enabled: true
                plugin-updates:
                  enabled: true
                  trusted-hashes-file: paperswap/plugins-trusted.txt
                live-patching:
                  enabled: true
                  profile: development
                  patch-directory: paperswap/patches/inbox
                  confirm-timeout-seconds: 60
                """);
        assertTrue(r.valid(), r.errors().toString());
        assertTrue(r.config().pluginUpdates().enabled());
        assertEquals("paperswap/plugins-trusted.txt", r.config().pluginUpdates().trustedHashesFile());
        assertEquals(LivePatchConfig.Profile.DEVELOPMENT, r.config().livePatching().profile());
        assertEquals(60, r.config().livePatching().confirmTimeoutSeconds());

        RuntimeConfigLoader.Result bad = parse("""
                live-patching:
                  profile: yolo
                  patch-directory: ../outside
                  confirm-timeout-seconds: 1
                  extra: 1
                plugin-updates: yes-please
                """);
        assertFalse(bad.valid());
        String e = bad.errors().toString();
        assertTrue(e.contains("profile must be") && e.contains("patch-directory") && e.contains("confirm-timeout-seconds")
                && e.contains("plugin-updates must be a section"), e);
        assertTrue(bad.warnings().toString().contains("live-patching.extra"), bad.warnings().toString());
    }

    @Test
    void productionWithoutAllowListIsReported() {
        RuntimeConfigLoader.Result r = parse("live-patching:\n  enabled: true\n");
        assertTrue(r.valid());
        assertTrue(r.warnings().toString().contains("every apply will be refused"), r.warnings().toString());
    }

    @Test
    void liveSettingsThatNeedARestartAreNamed() {
        RuntimeConfig a = RuntimeConfig.defaults();
        RuntimeConfig b = parse("live-patching:\n  enabled: true\n  profile: development\n").config();
        assertTrue(RuntimeConfigLoader.restartRequiredChanges(a, b).containsAll(java.util.List.of("live-patching.enabled",
                "live-patching.profile")));
    }
}
