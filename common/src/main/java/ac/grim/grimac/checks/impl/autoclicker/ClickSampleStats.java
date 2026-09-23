/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.checks.impl.autoclicker;

/**
 * Pure statistics over a run of {@code System.nanoTime()} timestamps.
 *
 * <p>Deliberately free of any Grim/PacketEvents type so the arithmetic can be
 * unit tested without standing up a {@code GrimPlayer}.</p>
 *
 * <p>Both methods read the first {@code count} entries of the array and work on
 * the {@code count - 1} gaps between them, returning {@link Double#NaN} when
 * there are not enough samples to form a gap.</p>
 */
final class ClickSampleStats {

    private static final double NANOS_PER_MILLI = 1_000_000.0;

    private ClickSampleStats() {
    }

    /** Mean gap between consecutive timestamps, in milliseconds. */
    static double meanIntervalMillis(long[] nanos, int count) {
        if (count < 2) return Double.NaN;
        // The samples are recorded in arrival order, so the span between the
        // first and last already equals the sum of every gap.
        return (nanos[count - 1] - nanos[0]) / NANOS_PER_MILLI / (count - 1);
    }

    /** Population standard deviation of the gaps between timestamps, in milliseconds. */
    static double stdDevIntervalMillis(long[] nanos, int count) {
        double mean = meanIntervalMillis(nanos, count);
        if (Double.isNaN(mean)) return Double.NaN;

        double variance = 0.0;
        for (int i = 1; i < count; i++) {
            double diff = (nanos[i] - nanos[i - 1]) / NANOS_PER_MILLI - mean;
            variance += diff * diff;
        }
        return Math.sqrt(variance / (count - 1));
    }
}
