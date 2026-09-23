/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.manager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One stored ban, and the exact bytes it is kept as.
 *
 * <p>Separate from {@link AcBanStore} so the format can be tested without a
 * datastore behind it. This is what every ban on the network is written as;
 * a fault here either lets a banned player back in or keeps an innocent one
 * out past their sentence, and neither shows up until it has already
 * happened.</p>
 *
 * <h2>The format</h2>
 *
 * <pre>{@code <when> <expires> <actor> <reason>}</pre>
 *
 * <p>Space-separated, reason last. Reasons contain spaces — that is why the
 * split is bounded, so everything after the third space belongs to the
 * reason whatever it holds. The actor cannot afford spaces for the same
 * reason and is stripped of them on the way in: a name with one would eat
 * the front of the reason and truncate itself.</p>
 *
 * <p>{@code expiresEpochMs} of 0 means the ban never lifts.</p>
 */
public record AcBanRecord(long whenEpochMs, long expiresEpochMs,
                          @NotNull String actor, @NotNull String reason) {

    private static final char SEP = ' ';
    /** when, expires, actor, and then all of the reason. */
    private static final int FIELDS = 4;

    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    /** @param nowEpochMs the clock to judge against, so this can be tested. */
    public boolean isExpired(long nowEpochMs) {
        return expiresEpochMs != AcBanDuration.PERMANENT && nowEpochMs >= expiresEpochMs;
    }

    public @NotNull String encode() {
        return whenEpochMs + String.valueOf(SEP) + expiresEpochMs + SEP + oneWord(actor) + SEP + oneLine(reason);
    }

    /**
     * @return the stored ban, or null when there is nothing to read. An
     * unban writes an empty value rather than deleting the row, so an empty
     * string means "not banned" rather than "corrupt" — both answer null,
     * because the caller does the same thing either way.
     */
    public static @Nullable AcBanRecord decode(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return null;

        String[] parts = raw.split(String.valueOf(SEP), FIELDS);
        if (parts.length < 3) return null;

        return new AcBanRecord(
                parseLong(parts[0]),
                parseLong(parts[1]),
                parts[2],
                parts.length >= FIELDS ? parts[3] : "");
    }

    /** Keeps the actor from spilling into the reason. */
    private static String oneWord(String value) {
        return oneLine(value).replace(' ', '_');
    }

    private static String oneLine(String value) {
        return value.replace('\n', ' ').replace('\r', ' ');
    }

    private static long parseLong(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
