/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.manager;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Parsing and rendering of ban durations.
 *
 * <p>Accepts the shape people actually type — {@code 7d}, {@code 14d},
 * {@code 12h}, {@code 30m} — plus {@code perm} for a ban that never lifts.</p>
 */
@UtilityClass
public class AcBanDuration {

    /** An expiry of 0 means the ban never lifts. */
    public static final long PERMANENT = 0L;

    /**
     * @return milliseconds, {@link #PERMANENT} for "perm", or null when this is
     * not a duration at all (so a caller can treat the token as reason text).
     */
    public static @Nullable Long parse(@NotNull String token) {
        String s = token.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return null;
        if (s.equals("perm") || s.equals("permanent")) return PERMANENT;

        char unit = s.charAt(s.length() - 1);
        String digits = s.substring(0, s.length() - 1);
        if (digits.isEmpty()) return null;

        long amount;
        try {
            amount = Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return null;
        }
        if (amount <= 0) return null;

        return switch (unit) {
            case 'd' -> TimeUnit.DAYS.toMillis(amount);
            case 'h' -> TimeUnit.HOURS.toMillis(amount);
            case 'm' -> TimeUnit.MINUTES.toMillis(amount);
            default -> null;
        };
    }

    /**
     * Holds a duration inside what the network actually hands out.
     *
     * <p>BuckSMP bans for days, not forever, and that is a rule rather than a
     * default: an admin typing {@code perm} or {@code 365d} in the heat of the
     * moment should still produce a ban that lifts itself. {@link #PERMANENT}
     * therefore collapses to the maximum instead of escaping it.</p>
     *
     * @param durationMs a parsed duration, possibly {@link #PERMANENT}
     * @param maxMs      the longest ban this server hands out
     * @return a duration in {@code (0, maxMs]}, never {@link #PERMANENT}
     */
    public static long clamp(long durationMs, long maxMs) {
        if (maxMs <= 0) return durationMs; // no ceiling configured
        if (durationMs == PERMANENT) return maxMs;
        return Math.min(durationMs, maxMs);
    }

    /** Human-readable time left, e.g. {@code 6 days 3 hours}. */
    public static @NotNull String remaining(long expiresEpochMs) {
        return remaining(expiresEpochMs, System.currentTimeMillis());
    }

    /**
     * The same wording measured against a clock the caller supplies.
     *
     * <p>Exists so the wording can be pinned by a test. Reading
     * {@link System#currentTimeMillis()} inside meant the answer depended on
     * how long the test itself took to reach the assertion, which is a race
     * that only shows up on a loaded build machine.</p>
     */
    public static @NotNull String remaining(long expiresEpochMs, long nowEpochMs) {
        if (expiresEpochMs == PERMANENT) return "never";

        long left = expiresEpochMs - nowEpochMs;
        if (left <= 0) return "expired";

        long days = TimeUnit.MILLISECONDS.toDays(left);
        long hours = TimeUnit.MILLISECONDS.toHours(left) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(left) % 60;

        if (days > 0) return plural(days, "day") + " " + plural(hours, "hour");
        if (hours > 0) return plural(hours, "hour") + " " + plural(minutes, "minute");
        return plural(Math.max(1, minutes), "minute");
    }

    private static String plural(long value, String unit) {
        return value + " " + unit + (value == 1 ? "" : "s");
    }
}
