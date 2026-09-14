/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.proxy;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The proxy's half of the {@code bucksmpac:bans} contract.
 *
 * <p>Every sample here is also asserted by the encoder test in the
 * {@code common} module, so a field added to one end without the other stops
 * the build. The failure this guards against is invisible at runtime: the
 * proxy simply never learns about a ban, and the only symptom is that banned
 * players keep getting to the login screen.</p>
 */
class BanMessageTest {

    @Test
    void decodesEverySample() throws IOException {
        List<BanChannelSamples.Sample> samples = BanChannelSamples.load();

        for (BanChannelSamples.Sample sample : samples) {
            BanMessage message = BanMessage.parse(sample.payload());
            assertNotNull(message, () -> "did not decode: " + sample.payload());

            assertEquals(sample.kind(), message.kind().name(), sample.payload());
            assertEquals(UUID.fromString(sample.uuid()), message.uuid(), sample.payload());
            assertEquals(sample.name(), message.name(), sample.payload());

            if (message.kind() != BanMessage.Kind.BAN) continue;
            assertEquals(Long.parseLong(sample.when()), message.whenEpochMs(), sample.payload());
            assertEquals(Long.parseLong(sample.expires()), message.expiresEpochMs(), sample.payload());
            assertEquals(sample.actor(), message.actor(), sample.payload());
            assertEquals(sample.reason(), message.reason(), sample.payload());
        }
    }

    @Test
    void banSamplesSurviveARoundTripIntoARecord() throws IOException {
        for (BanChannelSamples.Sample sample : BanChannelSamples.load()) {
            if (!"BAN".equals(sample.kind())) continue;

            BanRecord record = BanMessage.parse(sample.payload()).toRecord();
            assertEquals(sample.name(), record.name());
            assertEquals(sample.reason(), record.reason());
            assertEquals(Long.parseLong(sample.expires()), record.expiresEpochMs());
        }
    }

    @Test
    void expiryOfZeroIsNeverTreatedAsAlreadyOver() throws IOException {
        // The "0 means permanent" encoding is the one place where reading a
        // number literally would silently unban somebody.
        BanRecord permanent = BanMessage.parse(
                "BAN|" + UUID.randomUUID() + "|Steve|1700000000000|0|CONSOLE|Manual").toRecord();

        assertEquals(false, permanent.isExpired());
    }

    @Test
    void rejectsMessagesItCannotTrust() {
        assertNull(BanMessage.parse(null));
        assertNull(BanMessage.parse(""));
        assertNull(BanMessage.parse("BAN"), "no uuid at all");
        assertNull(BanMessage.parse("BAN|not-a-uuid|Steve|1|2|a|b"), "unusable uuid");
        assertNull(BanMessage.parse("KICK|" + UUID.randomUUID() + "|Steve"), "unknown verb");
        assertNull(BanMessage.parse("BAN|" + UUID.randomUUID() + "|Steve|1700000000000"), "truncated ban");
    }

    @Test
    void aReasonKeepsItsSpacesAndBrackets() {
        UUID uuid = UUID.randomUUID();
        BanMessage message = BanMessage.parse(
                "BAN|" + uuid + "|Steve|1700000000000|1701209600000|CONSOLE|Killaura (autoclicker) x3");

        assertNotNull(message);
        assertEquals("Killaura (autoclicker) x3", message.reason());
    }

    @Test
    void anUnbanWithoutANameStillCarriesItsUuid() {
        UUID uuid = UUID.randomUUID();
        BanMessage message = BanMessage.parse("UNBAN|" + uuid + "|");

        assertNotNull(message);
        assertEquals(BanMessage.Kind.UNBAN, message.kind());
        assertEquals(uuid, message.uuid());
        assertEquals("", message.name());
    }
}
