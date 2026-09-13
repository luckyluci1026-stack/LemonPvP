/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.storage.DataStore;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.model.SettingRecord;
import ac.grim.grimac.api.storage.model.SettingScope;
import ac.grim.grimac.api.storage.query.Page;
import ac.grim.grimac.api.storage.query.Queries;
import ac.grim.grimac.utils.anticheat.LogUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * BuckSMPAC's own ban list, so the anticheat does not depend on an external
 * ban plugin being reachable from the server it happens to be running on.
 *
 * <h2>Where bans live</h2>
 *
 * <p>In the datastore BuckSMPAC already has — the {@code SETTING} category,
 * routed by {@code database.yml}. That matters: point every server's
 * BuckSMPAC at the same MySQL/MariaDB and one ban covers all of them, because
 * they are all reading the same row. Left on the default SQLite, the ban is
 * local to that server.</p>
 *
 * <h2>Two rows per ban</h2>
 *
 * <p>The ban itself is keyed by UUID, which is what a joining player presents.
 * A second row maps the lowercased name to that UUID, because an admin
 * unbanning somebody has a name and not a UUID, and the player is offline by
 * then so the server cannot look it up.</p>
 */
public class AcBanStore {

    private static final String KEY_BAN = "bucksmpac.ban";
    private static final String KEY_NAME_INDEX = "bucksmpac.ban.name";
    /** Field separator. A space is safe: the reason is the last field and
     *  the split is limited to three parts, so spaces inside it survive. */
    private static final char SEP = ' ';

    /** One stored ban. {@code expiresEpochMs} of 0 means it never lifts. */
    public record BanRecord(long whenEpochMs, long expiresEpochMs, @NotNull String actor, @NotNull String reason) {
        public boolean isExpired() {
            return expiresEpochMs != AcBanDuration.PERMANENT && System.currentTimeMillis() >= expiresEpochMs;
        }
    }

    private static @Nullable DataStore store() {
        try {
            return GrimAPI.INSTANCE.getDataStoreLifecycle().dataStore();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** @return true when bans can actually be stored and read. */
    public static boolean isAvailable() {
        return store() != null;
    }

    /**
     * Looks up a ban.
     *
     * @return the record, or null when the player is not banned. Never fails
     * the future — a storage error resolves to null and is logged, because
     * refusing a join on a database hiccup is worse than missing one ban.
     */
    public static CompletableFuture<@Nullable BanRecord> lookup(@NotNull UUID uuid) {
        DataStore store = store();
        if (store == null) return CompletableFuture.completedFuture(null);

        return store.query(Categories.SETTING, new Queries.GetSetting(SettingScope.PLAYER, uuid.toString(), KEY_BAN))
                .handle((page, error) -> {
                    if (error != null) {
                        LogUtil.warn("Could not read the BuckSMPAC ban list for " + uuid + ": " + error);
                        return null;
                    }
                    return decode(page);
                })
                .toCompletableFuture();
    }

    /** Resolves a (case-insensitive) name to the UUID it was banned under, or null. */
    public static CompletableFuture<@Nullable UUID> resolveBannedName(@NotNull String name) {
        DataStore store = store();
        if (store == null) return CompletableFuture.completedFuture(null);

        String key = nameKey(name);
        return store.query(Categories.SETTING, new Queries.GetSetting(SettingScope.PLAYER, key, KEY_NAME_INDEX))
                .handle((page, error) -> {
                    if (error != null) {
                        LogUtil.warn("Could not read the BuckSMPAC ban name index for " + name + ": " + error);
                        return null;
                    }
                    String raw = readString(page);
                    if (raw == null || raw.isBlank()) return null;
                    try {
                        return UUID.fromString(raw.trim());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .toCompletableFuture();
    }

    public static void ban(@NotNull UUID uuid, @NotNull String name, @NotNull String reason,
                           @NotNull String actor, long expiresEpochMs) {
        DataStore store = store();
        if (store == null) {
            LogUtil.warn("Refusing to ban " + name + " — BuckSMPAC's datastore is not available, "
                    + "so the ban could not be saved. Check database.yml.");
            return;
        }

        long now = System.currentTimeMillis();
        String encoded = now + String.valueOf(SEP) + expiresEpochMs + SEP + actor + SEP + reason;

        write(store, uuid.toString(), KEY_BAN, encoded, now);
        write(store, nameKey(name), KEY_NAME_INDEX, uuid.toString(), now);

        // Push it to the proxy so the player is refused at PreLoginEvent
        // rather than only being kept out of each backend.
        AcBanProxyBridge.sendBan(uuid, name, now, expiresEpochMs, actor, reason);
    }

    /** Clears the ban. The name index is cleared too so a stale row cannot resurrect it. */
    public static void unban(@NotNull UUID uuid, @Nullable String name) {
        DataStore store = store();
        if (store == null) return;

        long now = System.currentTimeMillis();
        write(store, uuid.toString(), KEY_BAN, "", now);
        if (name != null) write(store, nameKey(name), KEY_NAME_INDEX, "", now);

        AcBanProxyBridge.sendUnban(uuid, name);
    }

    private static void write(DataStore store, String scopeKey, String key, String value, long now) {
        try {
            store.submit(Categories.SETTING, e -> e
                    .scope(SettingScope.PLAYER)
                    .scopeKey(scopeKey)
                    .key(key)
                    .value(value.getBytes(StandardCharsets.UTF_8))
                    .updatedEpochMs(now));
        } catch (Throwable t) {
            LogUtil.warn("Failed to write to the BuckSMPAC ban list: " + t);
        }
    }

    private static String nameKey(String name) {
        return "name:" + name.toLowerCase(Locale.ROOT);
    }

    private static @Nullable String readString(@Nullable Page<SettingRecord> page) {
        if (page == null || page.items().isEmpty()) return null;
        byte[] value = page.items().get(0).value();
        if (value == null || value.length == 0) return null;
        return new String(value, StandardCharsets.UTF_8);
    }

    private static @Nullable BanRecord decode(@Nullable Page<SettingRecord> page) {
        String raw = readString(page);
        // An unban writes an empty value rather than deleting the row, so an
        // empty string here means "not banned", not "corrupt".
        if (raw == null || raw.isEmpty()) return null;

        String[] parts = raw.split(String.valueOf(SEP), 4);
        if (parts.length < 3) return null;

        long when = parseLong(parts[0]);
        long expires = parseLong(parts[1]);
        String reason = parts.length >= 4 ? parts[3] : "";

        BanRecord record = new BanRecord(when, expires, parts[2], reason);
        // A lapsed ban is simply not a ban. Reporting it as one would keep the
        // player out past their sentence just because nothing swept the row.
        return record.isExpired() ? null : record;
    }

    private static long parseLong(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
