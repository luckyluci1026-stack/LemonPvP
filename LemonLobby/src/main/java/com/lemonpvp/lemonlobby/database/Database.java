package com.lemonpvp.lemonlobby.database;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import com.lemonpvp.lemonlobby.model.BoosterTier;
import com.lemonpvp.lemonlobby.model.PlankTier;

import java.sql.*;
import java.util.UUID;

public class Database {

    private final LemonLobby plugin;
    private HikariDataSource dataSource;

    public Database(LemonLobby plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        HikariConfig config = new HikariConfig();
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.database", "lemonpvp");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "");

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("LemonLobby-Pool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
        createTables();
        plugin.getLogger().info("Database connection established.");
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS lemontraining_pending (" +
                "    uuid VARCHAR(36) PRIMARY KEY," +
                "    mode VARCHAR(16) NOT NULL," +
                "    created_at BIGINT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS ll_plank_upgrades (" +
                "    uuid VARCHAR(36) PRIMARY KEY," +
                "    tier VARCHAR(16) NOT NULL," +
                "    expires_at BIGINT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS ll_boosters (" +
                "    uuid VARCHAR(36) PRIMARY KEY," +
                "    tier INT NOT NULL," +
                "    expires_at BIGINT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            // Migration: drop the legacy remaining_uses column if an older schema
            // still has it. Done via metadata so it works on both MySQL (no
            // DROP COLUMN IF EXISTS) and MariaDB. Leaving a NOT NULL column with
            // no default would otherwise break every new booster INSERT.
            try (ResultSet rs = conn.getMetaData()
                    .getColumns(conn.getCatalog(), null, "ll_boosters", "remaining_uses")) {
                if (rs.next()) {
                    stmt.executeUpdate("ALTER TABLE ll_boosters DROP COLUMN remaining_uses");
                    plugin.getLogger().info("Migrated ll_boosters: dropped legacy remaining_uses column.");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not drop legacy remaining_uses column: " + e.getMessage());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create tables: " + e.getMessage());
        }
    }

    // ── Plank Tier ────────────────────────────────────────────────────────────

    public record PlankEntry(PlankTier tier, long expiresAt) {}

    public PlankEntry loadPlankEntry(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT tier, expires_at FROM ll_plank_upgrades WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PlankTier tier = PlankTier.fromName(rs.getString("tier"));
                    return new PlankEntry(tier, rs.getLong("expires_at"));
                }
            }
            return null;
        } catch (SQLException e) {
            plugin.getLogger().warning("loadPlankEntry error: " + e.getMessage());
            return null;
        }
    }

    public void savePlankEntry(UUID uuid, PlankTier tier, long expiresAt) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO ll_plank_upgrades (uuid, tier, expires_at) VALUES (?,?,?) " +
                     "ON DUPLICATE KEY UPDATE tier=VALUES(tier), expires_at=VALUES(expires_at)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, tier.name());
            ps.setLong(3, expiresAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("savePlankEntry error: " + e.getMessage());
        }
    }

    // ── Booster ───────────────────────────────────────────────────────────────

    public record BoosterEntry(BoosterTier tier, long expiresAt) {}

    public BoosterEntry loadBoosterEntry(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT tier, expires_at FROM ll_boosters WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BoosterTier tier = plugin.getBoosterConfig().fromLevel(rs.getInt("tier"));
                    if (tier == null) return null;
                    return new BoosterEntry(tier, rs.getLong("expires_at"));
                }
            }
            return null;
        } catch (SQLException e) {
            plugin.getLogger().warning("loadBoosterEntry error: " + e.getMessage());
            return null;
        }
    }

    public void saveBoosterEntry(UUID uuid, BoosterTier tier, long expiresAt) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO ll_boosters (uuid, tier, expires_at) VALUES (?,?,?) " +
                     "ON DUPLICATE KEY UPDATE tier=VALUES(tier), expires_at=VALUES(expires_at)")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, tier.level);
            ps.setLong(3, expiresAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("saveBoosterEntry error: " + e.getMessage());
        }
    }

    public void deleteBoosterEntry(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM ll_boosters WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("deleteBoosterEntry error: " + e.getMessage());
        }
    }

    /** Saves or overwrites a pending training mode for the given player. Blocking — call async. */
    public void savePendingTrainingMode(UUID uuid, String mode) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "REPLACE INTO lemontraining_pending (uuid, mode, created_at) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, mode);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save pending training mode: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection closed.");
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}
