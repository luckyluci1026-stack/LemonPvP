package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.database.DatabaseManager;
import com.lemonpvp.lemoncore.util.TextUtil;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BanManager {

    private final LemonCore plugin;
    private final DatabaseManager db;

    public BanManager(LemonCore plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    public CompletableFuture<BanRecord> banPlayer(UUID uuid, String username, String reason,
                                                   UUID bannerUuid, String bannerName, long durationSeconds) {
        return db.queryAsync(conn -> {
            try {
                // Deactivate existing bans
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lc_bans SET active=FALSE WHERE uuid=? AND active=TRUE")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                }

                String id = TextUtil.generateId(10);
                Timestamp expires = durationSeconds > 0
                        ? new Timestamp(System.currentTimeMillis() + durationSeconds * 1000L)
                        : null;

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO lc_bans (id, uuid, username, reason, banner_uuid, banner_name, expires) " +
                        "VALUES (?,?,?,?,?,?,?)")) {
                    ps.setString(1, id);
                    ps.setString(2, uuid.toString());
                    ps.setString(3, username);
                    ps.setString(4, reason);
                    ps.setString(5, bannerUuid != null ? bannerUuid.toString() : null);
                    ps.setString(6, bannerName);
                    ps.setTimestamp(7, expires);
                    ps.executeUpdate();
                }

                BanRecord record = new BanRecord();
                record.id = id;
                record.uuid = uuid;
                record.username = username;
                record.reason = reason;
                record.bannerUuid = bannerUuid;
                record.bannerName = bannerName;
                record.banTime = new Timestamp(System.currentTimeMillis());
                record.expires = expires;
                record.active = true;
                return record;
            } catch (SQLException e) {
                plugin.getLogger().severe("Ban error: " + e.getMessage());
                return null;
            }
        }).thenApply(record -> {
            if (record != null) plugin.getDiscordWebhookManager().sendBan(record);
            return record;
        });
    }

    public CompletableFuture<BanRecord> getActiveBan(UUID uuid) {
        return db.queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM lc_bans WHERE uuid=? AND active=TRUE ORDER BY ban_time DESC LIMIT 1")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
                BanRecord r = mapRecord(rs);
                // Auto-expire check
                if (r.isExpired()) {
                    unbanById(conn, r.id);
                    return null;
                }
                return r;
            } catch (SQLException e) {
                return null;
            }
        });
    }

    public CompletableFuture<BanRecord> getActiveBanByName(String username) {
        return db.queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM lc_bans WHERE username=? AND active=TRUE ORDER BY ban_time DESC LIMIT 1")) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
                BanRecord r = mapRecord(rs);
                if (r.isExpired()) {
                    unbanById(conn, r.id);
                    return null;
                }
                return r;
            } catch (SQLException e) {
                return null;
            }
        });
    }

    /**
     * Deactivates the active ban matching the given player name or ban ID.
     * Returns the BanRecord that was lifted, or null if nothing was found.
     */
    public CompletableFuture<BanRecord> unban(String nameOrId) {
        return db.queryAsync(conn -> {
            try {
                // Try to fetch by ID first, then by username
                BanRecord record = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM lc_bans WHERE id=? AND active=TRUE LIMIT 1")) {
                    ps.setString(1, nameOrId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) record = mapRecord(rs);
                }
                if (record == null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT * FROM lc_bans WHERE username=? AND active=TRUE ORDER BY ban_time DESC LIMIT 1")) {
                        ps.setString(1, nameOrId);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) record = mapRecord(rs);
                    }
                }
                if (record == null) return null;

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lc_bans SET active=FALSE WHERE id=?")) {
                    ps.setString(1, record.id);
                    ps.executeUpdate();
                }
                return record;
            } catch (SQLException e) {
                plugin.getLogger().severe("Unban error: " + e.getMessage());
                return null;
            }
        });
    }

    private void unbanById(Connection conn, String id) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE lc_bans SET active=FALSE WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[BanManager] unbanById failed for id=" + id + ": " + e.getMessage());
        }
    }

    public CompletableFuture<List<BanRecord>> getHistory(UUID uuid) {
        return db.queryAsync(conn -> {
            List<BanRecord> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM lc_bans WHERE uuid=? ORDER BY ban_time DESC")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapRecord(rs));
            } catch (SQLException e) {
                plugin.getLogger().severe("History error: " + e.getMessage());
            }
            return list;
        });
    }

    private BanRecord mapRecord(ResultSet rs) throws SQLException {
        BanRecord r = new BanRecord();
        r.id = rs.getString("id");
        r.uuid = UUID.fromString(rs.getString("uuid"));
        r.username = rs.getString("username");
        r.reason = rs.getString("reason");
        String bu = rs.getString("banner_uuid");
        r.bannerUuid = bu != null ? UUID.fromString(bu) : null;
        r.bannerName = rs.getString("banner_name");
        r.banTime = rs.getTimestamp("ban_time");
        r.expires = rs.getTimestamp("expires");
        r.active = rs.getBoolean("active");
        return r;
    }
}
