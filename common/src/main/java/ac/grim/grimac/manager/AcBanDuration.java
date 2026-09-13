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

    /** Human-readable time left, e.g. {@code 6 days 3 hours}. */
    public static @NotNull String remaining(long expiresEpochMs) {
        if (expiresEpochMs == PERMANENT) return "never";

        long left = expiresEpochMs - System.currentTimeMillis();
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
