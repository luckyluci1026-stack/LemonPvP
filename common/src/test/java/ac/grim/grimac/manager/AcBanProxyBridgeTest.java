/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.manager;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The backend's half of the {@code bucksmpac:bans} contract.
 *
 * <p>Every sample here is also asserted by the decoder test in the
 * {@code velocity} module. The failure this guards against is invisible at
 * runtime: the proxy simply never learns about a ban, and the only symptom is
 * that banned players keep reaching the login screen.</p>
 */
class AcBanProxyBridgeTest {

    @Test
    void encodesEverySample() throws IOException {
        for (BanChannelSamples.Sample sample : BanChannelSamples.load()) {
            UUID uuid = UUID.fromString(sample.uuid());

            String encoded = switch (sample.kind()) {
                case "BAN" -> AcBanProxyBridge.encodeBan(uuid, sample.name(),
                        Long.parseLong(sample.when()), Long.parseLong(sample.expires()),
                        sample.actor(), sample.reason());
                // An empty name in the file stands for "no name known", which
                // is what the unban-by-uuid path actually passes.
                case "UNBAN" -> AcBanProxyBridge.encodeUnban(uuid,
                        sample.name().isEmpty() ? null : sample.name());
                default -> throw new IllegalStateException("unknown kind " + sample.kind());
            };

            assertEquals(sample.payload(), encoded);
        }
    }

    @Test
    void aSeparatorInsideAReasonCannotShiftTheFields() {
        // An admin typing "/acban Steve 7d x-ray | fly" must not be able to
        // make the proxy read half the reason as a seventh field.
        String payload = AcBanProxyBridge.encodeBan(UUID.randomUUID(), "Steve",
                1700000000000L, 1701209600000L, "CONSOLE", "x-ray | fly");

        assertEquals(7, payload.split("\\|", -1).length);
        assertTrue(payload.endsWith("|x-ray / fly"));
    }

    @Test
    void lineBreaksNeverReachTheWire() {
        String payload = AcBanProxyBridge.encodeBan(UUID.randomUUID(), "Steve",
                1L, 2L, "CON\r\nSOLE", "first\nsecond");

        assertFalse(payload.contains("\n"));
        assertFalse(payload.contains("\r"));
    }

    @Test
    void aNameIsScrubbedLikeEveryOtherField() {
        // Java-edition names cannot contain a pipe, but a Bedrock gamertag
        // arriving through Floodgate is not bound by those rules.
        String payload = AcBanProxyBridge.encodeBan(UUID.randomUUID(), "Weird|Name",
                1L, 2L, "CONSOLE", "Reach");

        assertEquals(7, payload.split("\\|", -1).length);
        assertTrue(payload.contains("|Weird/Name|"));
    }

    @Test
    void anUnbanWithoutANameStillEndsWithAnEmptyField() {
        UUID uuid = UUID.randomUUID();

        // The decoder splits on the separator, so the trailing one has to be
        // there for the name to read as empty rather than missing.
        assertEquals("UNBAN|" + uuid + "|", AcBanProxyBridge.encodeUnban(uuid, null));
    }
}
