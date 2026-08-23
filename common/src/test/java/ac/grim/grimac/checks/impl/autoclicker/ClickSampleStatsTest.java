package ac.grim.grimac.checks.impl.autoclicker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The arithmetic behind the AutoClicker's consistency signal. These are the
 * numbers that decide whether a player is called a cheater, so they are pinned
 * here rather than trusted to review.
 */
class ClickSampleStatsTest {

    private static final long MS = 1_000_000L;

    /** Builds nanosecond timestamps from a list of millisecond gaps. */
    private static long[] fromGapsMs(long... gapsMs) {
        long[] nanos = new long[gapsMs.length + 1];
        nanos[0] = 1_234_567_890L; // arbitrary origin; only differences matter
        for (int i = 0; i < gapsMs.length; i++) {
            nanos[i + 1] = nanos[i] + gapsMs[i] * MS;
        }
        return nanos;
    }

    @Test
    void tooFewSamplesHasNoIntervals() {
        long[] samples = {5L, 10L};
        assertTrue(Double.isNaN(ClickSampleStats.meanIntervalMillis(samples, 0)));
        assertTrue(Double.isNaN(ClickSampleStats.meanIntervalMillis(samples, 1)));
        assertTrue(Double.isNaN(ClickSampleStats.stdDevIntervalMillis(samples, 0)));
        assertTrue(Double.isNaN(ClickSampleStats.stdDevIntervalMillis(samples, 1)));
    }

    @Test
    void perfectlyConstantClickingHasZeroDeviation() {
        long[] samples = fromGapsMs(100, 100, 100, 100);
        assertEquals(100.0, ClickSampleStats.meanIntervalMillis(samples, samples.length), 1e-9);
        assertEquals(0.0, ClickSampleStats.stdDevIntervalMillis(samples, samples.length), 1e-9);
    }

    @Test
    void unevenClickingHasKnownMeanAndDeviation() {
        // gaps 90/110/100 -> mean 100, deviations -10/+10/0
        long[] samples = fromGapsMs(90, 110, 100);
        assertEquals(100.0, ClickSampleStats.meanIntervalMillis(samples, samples.length), 1e-9);
        assertEquals(Math.sqrt((100.0 + 100.0 + 0.0) / 3.0),
                ClickSampleStats.stdDevIntervalMillis(samples, samples.length), 1e-9);
    }

    @Test
    void onlyTheFirstCountSamplesAreRead() {
        // Stale entries past `count` must not leak into the result - the check
        // reuses one array across windows and only resets the counter.
        long[] samples = new long[8];
        long[] real = fromGapsMs(50, 50, 50);
        System.arraycopy(real, 0, samples, 0, real.length);
        samples[real.length] = Long.MAX_VALUE / 2; // garbage from an older window

        assertEquals(50.0, ClickSampleStats.meanIntervalMillis(samples, real.length), 1e-9);
        assertEquals(0.0, ClickSampleStats.stdDevIntervalMillis(samples, real.length), 1e-9);
    }

    @Test
    void aFlushedBacklogFallsBelowThePlausibleSpacingGate() {
        // 12 packets delivered in one burst, ~200 microseconds apart. This is the
        // shape that made the old wall-clock check flag lagging players: near-zero
        // deviation. The mean gap is what rejects it.
        long[] nanos = new long[12];
        for (int i = 0; i < nanos.length; i++) {
            nanos[i] = i * 200_000L; // 0.2 ms
        }
        double mean = ClickSampleStats.meanIntervalMillis(nanos, nanos.length);
        double deviation = ClickSampleStats.stdDevIntervalMillis(nanos, nanos.length);

        assertEquals(0.2, mean, 1e-9);
        assertEquals(0.0, deviation, 1e-9);
        // Deviation alone would flag; the 20 ms spacing gate is what saves the player.
        assertTrue(mean < 20.0, "a burst must fall under the plausible-spacing gate");
    }

    @Test
    void humanClickingStaysAboveTheRoboticThreshold() {
        // ~10 CPS with the millisecond-scale wobble a hand actually produces.
        long[] samples = fromGapsMs(96, 104, 91, 112, 99, 107, 88, 103, 110, 94);
        double deviation = ClickSampleStats.stdDevIntervalMillis(samples, samples.length);
        assertTrue(deviation > 3.0,
                "human clicking must stay above the default constant-deviation-ms, was " + deviation);
    }
}
