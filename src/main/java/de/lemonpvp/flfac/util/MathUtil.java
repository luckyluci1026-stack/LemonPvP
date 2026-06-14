package de.lemonpvp.flfac.util;

import java.util.Collection;

/** Small statistical helpers used by several checks. */
public final class MathUtil {

    private MathUtil() {
    }

    /** Wraps an angle difference into the range [-180, 180]. */
    public static float wrapDegrees(float delta) {
        delta %= 360.0F;
        if (delta >= 180.0F) {
            delta -= 360.0F;
        }
        if (delta < -180.0F) {
            delta += 360.0F;
        }
        return delta;
    }

    public static double average(Collection<? extends Number> values) {
        if (values.isEmpty()) {
            return 0.0D;
        }
        double sum = 0.0D;
        for (Number n : values) {
            sum += n.doubleValue();
        }
        return sum / values.size();
    }

    /** Population standard deviation. */
    public static double standardDeviation(Collection<? extends Number> values) {
        if (values.size() < 2) {
            return 0.0D;
        }
        double mean = average(values);
        double variance = 0.0D;
        for (Number n : values) {
            double diff = n.doubleValue() - mean;
            variance += diff * diff;
        }
        return Math.sqrt(variance / values.size());
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
