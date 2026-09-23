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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bans in a plain text file, for a proxy with no database configured.
 *
 * <p>One record per line, pipe-separated:
 * {@code uuid|name|epochMs|expiresEpochMs|actor|reason}. Readable and
 * hand-editable on purpose — deleting a line is a valid way to unban.</p>
 */
public final class FileBanStorage implements BanStorage {

    private final Path file;
    private final Logger logger;
    /** Mirrors the file so a single write can rewrite the whole thing. */
    private final Map<UUID, BanRecord> known = new LinkedHashMap<>();

    public FileBanStorage(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    @Override
    public synchronized Collection<BanRecord> loadAll() {
        known.clear();
        if (!Files.exists(file)) return List.of();

        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                BanRecord record = parse(line);
                if (record != null) known.put(record.uuid(), record);
            }
        } catch (IOException e) {
            logger.error("Could not read {} — nobody will be blocked at the proxy until this is fixed.", file, e);
        }
        return new ArrayList<>(known.values());
    }

    @Override
    public synchronized void save(BanRecord record) {
        known.put(record.uuid(), record);
        write();
    }

    @Override
    public synchronized void delete(UUID uuid) {
        if (uuid != null && known.remove(uuid) != null) write();
    }

    @Override
    public String describe() {
        return "the file " + file.getFileName();
    }

    private void write() {
        List<String> lines = new ArrayList<>();
        lines.add("# BuckSMPAC proxy ban list. One ban per line:");
        lines.add("#   uuid|name|epochMillis|expiresEpochMillis|bannedBy|reason");
        lines.add("# An expiry of 0 means the ban never lifts.");
        lines.add("# Deleting a line unbans that player on the next proxy start,");
        lines.add("# or immediately with /acunban on the proxy console.");

        for (BanRecord r : known.values()) {
            lines.add(String.join("|",
                    r.uuid().toString(),
                    nullSafe(r.name()),
                    Long.toString(r.whenEpochMs()),
                    Long.toString(r.expiresEpochMs()),
                    nullSafe(r.actor()),
                    // The reason is last so a pipe inside it cannot shift the
                    // other fields - but strip it anyway to keep lines clean.
                    nullSafe(r.reason()).replace('|', '/')));
        }
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Could not write {} — this change is live now but will be lost on restart.", file, e);
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
