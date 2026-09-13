/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.proxy;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The proxy's ban list, held in memory and mirrored to a plain text file.
 *
 * <p>Deliberately not a database. The proxy has to answer during
 * {@code PreLoginEvent}, before the player reaches any backend, so it cannot
 * ask the anticheat — and giving it its own JDBC connection would mean sharing
 * a table schema with the backend plugin, which is one more thing to keep in
 * step and get wrong. Instead the backend pushes each ban over a plugin
 * message and this file is the proxy's own copy.</p>
 *
 * <p>One record per line, pipe-separated:
 * {@code uuid|name|epochMs|actor|reason}. Readable and hand-editable on
 * purpose — deleting a line is a valid way to unban somebody.</p>
 */
public final class ProxyBanList {

    /** Both maps point at the same records; name lookup is for PreLoginEvent. */
    private final Map<UUID, BanRecord> byUuid = new ConcurrentHashMap<>();
    private final Map<String, UUID> byName = new ConcurrentHashMap<>();

    private final Path file;
    private final Logger logger;

    /** {@code expiresEpochMs} of 0 means the ban never lifts. */
    public record BanRecord(UUID uuid, String name, long whenEpochMs, long expiresEpochMs,
                            String actor, String reason) {
        public boolean isExpired() {
            return expiresEpochMs != 0L && System.currentTimeMillis() >= expiresEpochMs;
        }
    }

    public ProxyBanList(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    public void load() {
        byUuid.clear();
        byName.clear();

        if (!Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                BanRecord record = parse(line);
                if (record != null) index(record);
            }
            logger.info("Loaded {} ban(s).", byUuid.size());
        } catch (IOException e) {
            logger.error("Could not read {} — nobody will be blocked at the proxy until this is fixed.", file, e);
        }
    }

    public BanRecord lookup(UUID uuid) {
        return live(byUuid.get(uuid));
    }

    public BanRecord lookup(String name) {
        UUID uuid = byName.get(name.toLowerCase(Locale.ROOT));
        return uuid == null ? null : live(byUuid.get(uuid));
    }

    /**
     * Drops a ban that has served its time. Swept on read rather than on a
     * timer: nothing else needs to know, and the only moment the answer
     * matters is when somebody tries to join.
     */
    private BanRecord live(BanRecord record) {
        if (record == null) return null;
        if (!record.isExpired()) return record;
        remove(record.uuid(), record.name());
        return null;
    }

    public void add(BanRecord record) {
        index(record);
        save();
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

        if (removed != null) save();
        return removed != null;
    }

    public int size() {
        return byUuid.size();
    }

    private void index(BanRecord record) {
        byUuid.put(record.uuid(), record);
        if (record.name() != null && !record.name().isBlank()) {
            byName.put(record.name().toLowerCase(Locale.ROOT), record.uuid());
        }
    }

    private synchronized void save() {
        List<String> lines = new ArrayList<>();
        lines.add("# BuckSMPAC proxy ban list. One ban per line:");
        lines.add("#   uuid|name|epochMillis|expiresEpochMillis|bannedBy|reason");
        lines.add("# An expiry of 0 means the ban never lifts.");
        lines.add("# Deleting a line unbans that player on the next proxy start,");
        lines.add("# or immediately with /acunban on the proxy console.");
        for (BanRecord r : byUuid.values()) {
            lines.add(String.join("|",
                    r.uuid().toString(),
                    nullSafe(r.name()),
                    Long.toString(r.whenEpochMs()),
                    Long.toString(r.expiresEpochMs()),
                    nullSafe(r.actor()),
                    // The reason is last, so a pipe inside it cannot shift the
                    // other fields - but strip it anyway to keep lines clean.
                    nullSafe(r.reason()).replace('|', '/')));
        }
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Could not write {} — this ban is active now but will be lost on restart.", file, e);
        }
    }

    private static BanRecord parse(String line) {
        String[] parts = line.split("\\|", 6);
        if (parts.length < 4) return null;
        try {
            return new BanRecord(
                    UUID.fromString(parts[0].trim()),
                    parts[1],
                    Long.parseLong(parts[2].trim()),
                    Long.parseLong(parts[3].trim()),
                    parts.length > 4 ? parts[4] : "",
                    parts.length > 5 ? parts[5] : "");
        } catch (IllegalArgumentException e) {
            return null; // malformed line: skip rather than refuse to start
        }
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
