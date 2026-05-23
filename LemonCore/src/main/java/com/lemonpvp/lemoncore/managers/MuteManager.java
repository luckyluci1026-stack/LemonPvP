package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.database.DatabaseManager;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MuteManager {

    private final LemonCore plugin;
    private final DatabaseManager db;

    public MuteManager(LemonCore plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    public CompletableFuture<MuteRecord> mutePlayer(UUID uuid, String username, String reason,
                                                     UUID muterUuid, String muterName, long durationSeconds) {
        return db.queryAsync(conn -> {
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lc_mutes SET active=FALSE WHERE uuid=? AND active=TRUE")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                }

                Timestamp expires = durationSeconds > 0
                        ? new Timestamp(System.currentTimeMillis() + durationSeconds * 1000L)
                        : null;

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO lc_mutes (uuid, username, reason, muter_uuid, muter_name, expires) VALUES (?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, username);
                    ps.setString(3, reason);
                    ps.setString(4, muterUuid != null ? muterUuid.toString() : null);
                    ps.setString(5, muterName);
                    ps.setTimestamp(6, expires);
                    ps.executeUpdate();

                    MuteRecord record = new MuteRecord();
                    ResultSet keys = ps.getGeneratedKeys();
                    if (keys.next()) record.id = keys.getInt(1);
                    record.uuid = uuid;
                    record.username = username;
                    record.reason = reason;
                    record.muterUuid = muterUuid;
                    record.muterName = muterName;
                    record.muteTime = new Timestamp(System.currentTimeMillis());
                    record.expires = expires;
                    record.active = true;
                    return record;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Mute error: " + e.getMessage());
                return null;
            }
        }).thenApply(record -> {
            if (record != null) plugin.getDiscordWebhookManager().sendMute(record);
            return record;
        });
    }

    public CompletableFuture<MuteRecord> getActiveMute(UUID uuid) {
        return db.queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM lc_mutes WHERE uuid=? AND active=TRUE ORDER BY mute_time DESC LIMIT 1")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
                MuteRecord r = mapRecord(rs);
                if (r.isExpired()) {
                    unmuteById(r.id);
                    return null;
                }
                return r;
            } catch (SQLException e) {
                return null;
            }
        });
    }

    /**
     * Deactivates the active mute for the given player name or UUID string.
     * Returns the MuteRecord that was lifted, or null if nothing was found.
     */
    public CompletableFuture<MuteRecord> unmute(String nameOrUuid) {
        return db.queryAsync(conn -> {
            try {
                MuteRecord record = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM lc_mutes WHERE username=? AND active=TRUE ORDER BY mute_time DESC LIMIT 1")) {
                    ps.setString(1, nameOrUuid);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) record = mapRecord(rs);
                }
                if (record == null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT * FROM lc_mutes WHERE uuid=? AND active=TRUE ORDER BY mute_time DESC LIMIT 1")) {
                        ps.setString(1, nameOrUuid);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) record = mapRecord(rs);
                    }
                }
                if (record == null) return null;

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lc_mutes SET active=FALSE WHERE id=?")) {
                    ps.setInt(1, record.id);
                    ps.executeUpdate();
                }
                return record;
            } catch (SQLException e) {
                plugin.getLogger().severe("Unmute error: " + e.getMessage());
                return null;
            }
        });
    }

    private void unmuteById(int id) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE lc_mutes SET active=FALSE WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public CompletableFuture<List<MuteRecord>> getHistory(UUID uuid) {
        return db.queryAsync(conn -> {
            List<MuteRecord> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM lc_mutes WHERE uuid=? ORDER BY mute_time DESC")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapRecord(rs));
            } catch (SQLException e) {
                plugin.getLogger().severe("Mute history error: " + e.getMessage());
            }
            return list;
        });
    }

    private MuteRecord mapRecord(ResultSet rs) throws SQLException {
        MuteRecord r = new MuteRecord();
        r.id = rs.getInt("id");
        r.uuid = UUID.fromString(rs.getString("uuid"));
        r.username = rs.getString("username");
        r.reason = rs.getString("reason");
        String mu = rs.getString("muter_uuid");
        r.muterUuid = mu != null ? UUID.fromString(mu) : null;
        r.muterName = rs.getString("muter_name");
        r.muteTime = rs.getTimestamp("mute_time");
        r.expires = rs.getTimestamp("expires");
        r.active = rs.getBoolean("active");
        return r;
    }
}
