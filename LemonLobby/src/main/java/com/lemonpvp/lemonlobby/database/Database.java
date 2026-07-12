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
                "CREATE TABLE IF NOT EXISTS lp_pending_queue (" +
                "    uuid VARCHAR(36) PRIMARY KEY," +
                "    gamemode VARCHAR(24) NOT NULL," +
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
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS ll_daily (" +
                "    uuid VARCHAR(36) PRIMARY KEY," +
                "    last_claim BIGINT NOT NULL," +
                "    streak INT NOT NULL" +
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

    // ── Daily Rewards ───────────────────────────────────────────────────────────

    public record DailyEntry(long lastClaim, int streak) {}

    public DailyEntry loadDaily(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT last_claim, streak FROM ll_daily WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new DailyEntry(rs.getLong("last_claim"), rs.getInt("streak"));
            }
            return null;
        } catch (SQLException e) {
            plugin.getLogger().warning("loadDaily error: " + e.getMessage());
            return null;
        }
    }

    public void saveDaily(UUID uuid, long lastClaim, int streak) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO ll_daily (uuid, last_claim, streak) VALUES (?,?,?) " +
                     "ON DUPLICATE KEY UPDATE last_claim=VALUES(last_claim), streak=VALUES(streak)")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, lastClaim);
            ps.setInt(3, streak);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("saveDaily error: " + e.getMessage());
        }
    }

    // ── Recent replays (read the shared lp_replays table written by LemonPractice) ─

    public record ReplayLite(String player1, String player2, long createdAt) {}

    /** Most recent non-expired replays. Returns empty if the table is absent. Blocking — call async. */
    public java.util.List<ReplayLite> recentReplays(int limit) {
        java.util.List<ReplayLite> out = new java.util.ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT player1, player2, created_at FROM lp_replays " +
                     "WHERE expires_at > ? ORDER BY created_at DESC LIMIT ?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ReplayLite(rs.getString("player1"), rs.getString("player2"),
                            rs.getLong("created_at")));
                }
            }
        } catch (SQLException e) {
            // table may not exist on this server yet — silent, returns empty
        }
        return out;
    }

    /**
     * Saves the gamemode a player picked in the lobby queue GUI. The duels
     * server reads + deletes it on join and auto-queues the player. Blocking —
     * call async.
     */
    public void savePendingQueue(UUID uuid, String gamemode) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "REPLACE INTO lp_pending_queue (uuid, gamemode, created_at) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, gamemode);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save pending queue: " + e.getMessage());
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
