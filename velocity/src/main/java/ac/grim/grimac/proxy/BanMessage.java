/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.proxy;

import java.util.UUID;

/**
 * One message off the {@code bucksmpac:bans} channel, decoded.
 *
 * <p>The encoder is {@code AcBanProxyBridge} in the anticheat itself. Neither
 * side can see the other's code, so both are pinned to the samples in
 * {@code protocol/ban-channel-samples.tsv} by a test — a field added on one
 * end without the other fails the build rather than quietly costing the
 * network its proxy-side bans.</p>
 *
 * <p>Deliberately free of Velocity types so it can be tested on its own.</p>
 */
public record BanMessage(Kind kind, UUID uuid, String name, long whenEpochMs,
                         long expiresEpochMs, String actor, String reason) {

    public enum Kind { BAN, UNBAN }

    /** Fields, in the order they appear on the wire. */
    private static final int MAX_FIELDS = 7;

    /**
     * @return the decoded message, or null when it is not one we understand —
     * a truncated payload, an unknown verb, or an unusable UUID. Returning
     * null rather than throwing keeps a malformed message from taking down
     * the channel handler for every later one.
     */
    public static BanMessage parse(String payload) {
        if (payload == null) return null;
        String[] parts = payload.split("\\|", MAX_FIELDS);
        if (parts.length < 2) return null;

        UUID uuid = tryUuid(parts[1]);
        if (uuid == null) return null;

        switch (parts[0]) {
            case "BAN" -> {
                if (parts.length < MAX_FIELDS) return null;
                return new BanMessage(Kind.BAN, uuid, parts[2],
                        parseWhen(parts[3]), parseExpiry(parts[4]), parts[5], parts[6]);
            }
            case "UNBAN" -> {
                String name = parts.length > 2 ? parts[2] : "";
                return new BanMessage(Kind.UNBAN, uuid, name, 0L, 0L, "", "");
            }
            default -> {
                return null;
            }
        }
    }

    /** The ban this message describes. Only meaningful for {@link Kind#BAN}. */
    public BanRecord toRecord() {
        return new BanRecord(uuid, name, whenEpochMs, expiresEpochMs, actor, reason);
    }

    /** An unparseable expiry is safer read as "never" than as "already over". */
    private static long parseExpiry(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static long parseWhen(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }

    private static UUID tryUuid(String input) {
        try {
            return UUID.fromString(input.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
