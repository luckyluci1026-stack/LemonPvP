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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Durations as typed by a person in a hurry.
 *
 * <p>{@code /acban} reads the first word as a duration and falls back to
 * treating it as the start of the reason, so both a wrong answer and a right
 * one for the wrong string change what an admin actually hands out.</p>
 */
class AcBanDurationTest {

    private static final long DAY = TimeUnit.DAYS.toMillis(1);

    @Test
    void readsTheUnitsTheCommandAdvertises() {
        assertEquals(TimeUnit.DAYS.toMillis(7), AcBanDuration.parse("7d"));
        assertEquals(TimeUnit.DAYS.toMillis(14), AcBanDuration.parse("14d"));
        assertEquals(TimeUnit.HOURS.toMillis(12), AcBanDuration.parse("12h"));
        assertEquals(TimeUnit.MINUTES.toMillis(30), AcBanDuration.parse("30m"));
        assertEquals(AcBanDuration.PERMANENT, AcBanDuration.parse("perm"));
        assertEquals(AcBanDuration.PERMANENT, AcBanDuration.parse("permanent"));
    }

    @Test
    void isNotFussyAboutCaseOrSurroundingSpace() {
        assertEquals(TimeUnit.DAYS.toMillis(7), AcBanDuration.parse("7D"));
        assertEquals(AcBanDuration.PERMANENT, AcBanDuration.parse(" PERM "));
    }

    @Test
    void returnsNullForAnythingThatIsReallyAReason() {
        // Null is not a failure here - it is how the command learns that the
        // first word was the start of the reason, not a length.
        assertNull(AcBanDuration.parse(""));
        assertNull(AcBanDuration.parse("cheating"));
        assertNull(AcBanDuration.parse("d"), "no amount");
        assertNull(AcBanDuration.parse("7"), "no unit");
        assertNull(AcBanDuration.parse("7y"), "unit we do not offer");
        assertNull(AcBanDuration.parse("0d"), "a zero-length ban is not a ban");
        assertNull(AcBanDuration.parse("-5d"));
        assertNull(AcBanDuration.parse("7 d"));
    }

    @Test
    void theCeilingCollapsesPermanentIntoATemporaryBan() {
        // The whole point: BuckSMP hands out days. An admin typing "perm"
        // should still produce a ban that lifts itself.
        long max = 14 * DAY;

        assertEquals(max, AcBanDuration.clamp(AcBanDuration.PERMANENT, max));
        assertEquals(max, AcBanDuration.clamp(365 * DAY, max));
        assertEquals(7 * DAY, AcBanDuration.clamp(7 * DAY, max), "a shorter ban is left alone");
        assertEquals(max, AcBanDuration.clamp(max, max), "exactly the ceiling is allowed");
    }

    @Test
    void aCeilingOfPermanentTurnsTheLimitOff() {
        // acban-max-duration: perm is the documented escape hatch.
        assertEquals(AcBanDuration.PERMANENT,
                AcBanDuration.clamp(AcBanDuration.PERMANENT, AcBanDuration.PERMANENT));
        assertEquals(365 * DAY, AcBanDuration.clamp(365 * DAY, AcBanDuration.PERMANENT));
    }

    @Test
    void describesWhatIsLeftInWordsAPlayerCanRead() {
        // Against a fixed clock. The wall clock made these assertions depend
        // on how long the test took to reach them, which held on a quiet
        // machine and broke on a loaded CI runner.
        long now = 1_700_000_000_000L;

        assertEquals("never", AcBanDuration.remaining(AcBanDuration.PERMANENT, now));
        assertEquals("expired", AcBanDuration.remaining(now - 1000, now));
        assertEquals("7 days 1 hour", AcBanDuration.remaining(now + 7 * DAY + TimeUnit.MINUTES.toMillis(90), now));
        assertEquals("1 day 0 hours", AcBanDuration.remaining(now + DAY + TimeUnit.MINUTES.toMillis(1), now));
        assertEquals("1 hour 30 minutes", AcBanDuration.remaining(now + TimeUnit.MINUTES.toMillis(90), now));
        assertEquals("5 minutes", AcBanDuration.remaining(now + TimeUnit.MINUTES.toMillis(5), now));
    }

    @Test
    void singularAndPluralAreBothSpelledOut() {
        long now = 1_700_000_000_000L;

        assertEquals("1 day 1 hour", AcBanDuration.remaining(now + DAY + TimeUnit.HOURS.toMillis(1), now));
        assertEquals("2 days 2 hours", AcBanDuration.remaining(now + 2 * DAY + TimeUnit.HOURS.toMillis(2), now));
        assertEquals("1 hour 1 minute", AcBanDuration.remaining(now + TimeUnit.MINUTES.toMillis(61), now));
    }

    @Test
    void theLastMinuteNeverReadsAsZero() {
        // "0 minutes" on a ban screen reads as a bug to the player looking at it.
        long now = 1_700_000_000_000L;
        assertEquals("1 minute", AcBanDuration.remaining(now + 5_000, now));
    }

    @Test
    void theWallClockOverloadAgreesWithTheFixedOne() {
        // The one-argument form is what production calls; this is the only
        // assertion loose enough to survive an arbitrarily slow machine.
        assertEquals("never", AcBanDuration.remaining(AcBanDuration.PERMANENT));
        assertEquals("expired", AcBanDuration.remaining(System.currentTimeMillis() - DAY));
        assertTrue(AcBanDuration.remaining(System.currentTimeMillis() + 7 * DAY).startsWith("6 days ")
                        || AcBanDuration.remaining(System.currentTimeMillis() + 7 * DAY).startsWith("7 days "),
                "7 days out should read as 6 or 7 days, depending on the millisecond");
    }

    @Test
    void everyDurationTheShippedConfigsUseIsUnderstood() {
        for (String token : new String[]{"7d", "10d", "14d"}) {
            assertNotNull(AcBanDuration.parse(token), token + " is used by the bundled ladders");
        }
    }
}
