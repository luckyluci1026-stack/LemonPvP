/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.proxy;

import org.slf4j.Logger;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The proxy's in-memory view of who is banned, backed by a {@link BanStorage}.
 *
 * <p>Everything is held in memory because the answer is needed during
 * {@code PreLoginEvent}: a database round-trip there would put its latency in
 * front of every login on the network. The store is read once at startup and
 * written when something changes.</p>
 *
 * <p>Indexed by UUID and by lowercased name. The name index is what makes a
 * pre-login refusal possible at all — at that point the UUID is not known
 * yet.</p>
 */
public final class ProxyBanList {

    private final Map<UUID, BanRecord> byUuid = new ConcurrentHashMap<>();
    private final Map<String, UUID> byName = new ConcurrentHashMap<>();

    private final BanStorage storage;
    private final Logger logger;

    public ProxyBanList(BanStorage storage, Logger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    public void load() {
        byUuid.clear();
        byName.clear();
        for (BanRecord record : storage.loadAll()) {
            index(record);
        }
        logger.info("Loaded {} ban(s) from {}.", byUuid.size(), storage.describe());
    }

    public BanRecord lookup(UUID uuid) {
        return live(byUuid.get(uuid));
    }

    public BanRecord lookup(String name) {
        UUID uuid = byName.get(name.toLowerCase(Locale.ROOT));
        return uuid == null ? null : live(byUuid.get(uuid));
    }

    public void add(BanRecord record) {
        index(record);
        storage.save(record);
    }

    /** @return true if anything was actually removed. */
    public boolean remove(UUID uuid, String name) {
        BanRecord removed = uuid == null ? null : byUuid.remove(uuid);

        if (removed == null && name != null) {
            UUID viaName = byName.remove(name.toLowerCase(Locale.ROOT));
            if (viaName != null) removed = byUuid.remove(viaName);
        }
        if (removed != null && removed.name() != null) {
            byName.remove(removed.name().toLowerCase(Locale.ROOT));
        }
        if (name != null) byName.remove(name.toLowerCase(Locale.ROOT));

        if (removed != null) storage.delete(removed.uuid());
        return removed != null;
    }

    public int size() {
        return byUuid.size();
    }

    /**
     * Drops a ban that has served its time. Swept on read rather than on a
     * timer: nothing else needs to know it lapsed, and the only moment the
     * answer matters is when somebody tries to join.
     */
    private BanRecord live(BanRecord record) {
        if (record == null) return null;
        if (!record.isExpired()) return record;
        remove(record.uuid(), record.name());
        return null;
    }

    private void index(BanRecord record) {
        byUuid.put(record.uuid(), record);
        if (record.name() != null && !record.name().isBlank()) {
            byName.put(record.name().toLowerCase(Locale.ROOT), record.uuid());
        }
    }
}
