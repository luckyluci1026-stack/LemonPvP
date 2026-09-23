/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.manager;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bytes every ban on the network is stored as.
 *
 * <p>Both directions of failure are silent and slow: a record that will not
 * decode lets a banned player back in, and one that decodes wrongly can keep
 * an innocent player out past their sentence. Neither shows up in a log.</p>
 */
class AcBanRecordTest {

    private static final long NOW = 1_700_000_000_000L;
    private static final long WEEK = TimeUnit.DAYS.toMillis(7);

    @Test
    void aBanSurvivesBeingWrittenAndReadBack() {
        AcBanRecord original = new AcBanRecord(NOW, NOW + WEEK, "CONSOLE", "Reach detected (Reach)");

        AcBanRecord decoded = AcBanRecord.decode(original.encode());

        assertNotNull(decoded);
        assertEquals(original, decoded);
    }

    @Test
    void aReasonKeepsItsSpaces() {
        // The reason is last precisely so this works; it is also the field
        // most likely to be a whole sentence.
        String reason = "BuckSMPAC > 45 flags across 4 checks (Simulation x22)";
        AcBanRecord decoded = AcBanRecord.decode(new AcBanRecord(NOW, NOW + WEEK, "CONSOLE", reason).encode());

        assertNotNull(decoded);
        assertEquals(reason, decoded.reason());
    }

    @Test
    void anActorWithASpaceCannotEatTheReason() {
        // Without collapsing the space, the actor would truncate itself and
        // the rest of its name would appear glued to the front of the reason.
        AcBanRecord decoded = AcBanRecord.decode(
                new AcBanRecord(NOW, NOW + WEEK, "Console Operator", "Reach").encode());

        assertNotNull(decoded);
        assertEquals("Console_Operator", decoded.actor());
        assertEquals("Reach", decoded.reason());
    }

    @Test
    void lineBreaksNeverReachTheStoredValue() {
        String encoded = new AcBanRecord(NOW, NOW + WEEK, "CON\nSOLE", "first\r\nsecond").encode();

        assertFalse(encoded.contains("\n"));
        assertFalse(encoded.contains("\r"));
        assertEquals(1, encoded.split("\n", -1).length);
    }

    @Test
    void nothingStoredMeansNotBanned() {
        // An unban writes an empty value rather than deleting the row.
        assertNull(AcBanRecord.decode(null));
        assertNull(AcBanRecord.decode(""));
    }

    @Test
    void anUnreadableValueIsRefusedRatherThanGuessedAt() {
        assertNull(AcBanRecord.decode("nonsense"), "one field");
        assertNull(AcBanRecord.decode("1700000000000 1701209600000"), "no actor");
    }

    @Test
    void aBanWithNoReasonStillDecodes() {
        AcBanRecord decoded = AcBanRecord.decode("1700000000000 1701209600000 CONSOLE");

        assertNotNull(decoded);
        assertEquals("CONSOLE", decoded.actor());
        assertEquals("", decoded.reason());
    }

    @Test
    void anUnreadableExpiryIsHeldRatherThanLifted() {
        // 0 is the "never lifts" encoding. Reading a corrupt expiry as 0 keeps
        // the player out until somebody looks; reading it as a past timestamp
        // would quietly unban them, which is the worse way to be wrong.
        AcBanRecord decoded = AcBanRecord.decode("1700000000000 garbage CONSOLE Reach");

        assertNotNull(decoded);
        assertEquals(AcBanDuration.PERMANENT, decoded.expiresEpochMs());
        assertFalse(decoded.isExpired(NOW + 100 * WEEK));
    }

    @Test
    void aSentenceEndsWhenItEnds() {
        AcBanRecord week = new AcBanRecord(NOW, NOW + WEEK, "CONSOLE", "Reach");

        assertFalse(week.isExpired(NOW), "just issued");
        assertFalse(week.isExpired(NOW + WEEK - 1), "one millisecond left");
        assertTrue(week.isExpired(NOW + WEEK), "the moment it runs out");
        assertTrue(week.isExpired(NOW + WEEK + 1));
    }

    @Test
    void aPermanentBanNeverLapses() {
        AcBanRecord permanent = new AcBanRecord(NOW, AcBanDuration.PERMANENT, "CONSOLE", "Manual");

        assertFalse(permanent.isExpired(NOW));
        assertFalse(permanent.isExpired(Long.MAX_VALUE));
    }
}
