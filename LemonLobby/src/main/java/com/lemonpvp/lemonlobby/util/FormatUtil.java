package com.lemonpvp.lemonlobby.util;

public final class FormatUtil {

    private FormatUtil() {}

    public static String formatAmount(long amount) {
        if (amount < 0) return "-" + formatAmount(-amount);
        if (amount >= 1_000_000_000_000_000L) return "999T+";
        long[] thresholds = {1_000_000_000_000L, 1_000_000_000L, 1_000_000L, 1_000L};
        String[] suffixes = {"T", "B", "M", "k"};
        for (int i = 0; i < thresholds.length; i++) {
            if (amount >= thresholds[i]) {
                long whole = amount / thresholds[i];
                long dec   = (amount % thresholds[i]) / (thresholds[i] / 10);
                return dec > 0 ? whole + "." + dec + suffixes[i] : whole + suffixes[i];
            }
        }
        return String.valueOf(amount);
    }
}
