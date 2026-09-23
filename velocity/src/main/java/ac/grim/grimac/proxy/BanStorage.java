/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.proxy;

import java.util.Collection;
import java.util.UUID;

/**
 * Where the proxy keeps its bans between restarts.
 *
 * <p>Only ever touched on startup and on a write. Lookups are served from the
 * in-memory index in {@link ProxyBanList}, so a slow or unreachable store can
 * never delay a login — which matters because the answer is needed during
 * {@code PreLoginEvent}.</p>
 */
public interface BanStorage {

    /** Everything currently stored. Called once at startup. */
    Collection<BanRecord> loadAll();

    /** Stores a ban, replacing any existing one for that UUID. */
    void save(BanRecord record);

    void delete(UUID uuid);

    /** Human-readable description of where bans are going, for the startup log. */
    String describe();
}
