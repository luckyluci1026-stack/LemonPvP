/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.proxy;

import java.util.UUID;

/** One ban held by the proxy. {@code expiresEpochMs} of 0 means it never lifts. */
public record BanRecord(UUID uuid, String name, long whenEpochMs, long expiresEpochMs,
                        String actor, String reason) {

    public boolean isExpired() {
        return expiresEpochMs != 0L && System.currentTimeMillis() >= expiresEpochMs;
    }
}
