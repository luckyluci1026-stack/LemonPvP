package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.database.DatabaseManager;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class OffenseManager {

    private final LemonCore plugin;
    private final DatabaseManager db;

    public OffenseManager(LemonCore plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    public CompletableFuture<Integer> getNwordCount(UUID uuid) {
        return db.queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT nword_count FROM lc_offenses WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            } catch (SQLException e) {
                return 0;
            }
        });
    }

    public CompletableFuture<Integer> incrementNword(UUID uuid) {
        return db.queryAsync(conn -> {
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO lc_offenses (uuid, nword_count, last_offense) VALUES (?, 1, CURRENT_TIMESTAMP) " +
                        "ON DUPLICATE KEY UPDATE nword_count=nword_count+1, last_offense=CURRENT_TIMESTAMP")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT nword_count FROM lc_offenses WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    return rs.next() ? rs.getInt(1) : 1;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("OffenseManager error: " + e.getMessage());
                return 1;
            }
        });
    }
}
