package com.lemonpvp.lemontraining.database;

import com.lemonpvp.lemontraining.LemonTraining;
import com.lemonpvp.lemontraining.model.PracticeMode;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class TrainingDatabase {

    private final LemonTraining plugin;
    private HikariDataSource dataSource;

    public TrainingDatabase(LemonTraining plugin) {
        this.plugin = plugin;
    }

    public void connect() throws Exception {
        FileConfiguration cfg = plugin.getConfig();
        String host = cfg.getString("database.host", "localhost");
        int port = cfg.getInt("database.port", 3306);
        String database = cfg.getString("database.database", "lemonpvp");
        String username = cfg.getString("database.username", "root");
        String password = cfg.getString("database.password", "");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("LemonTraining-Pool");

        dataSource = new HikariDataSource(config);
        createTable();
        plugin.getLogger().info("Database connected successfully.");
    }

    private void createTable() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS lemontraining_sessions (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        uuid VARCHAR(36) NOT NULL,
                        mode VARCHAR(16) NOT NULL,
                        started_at BIGINT NOT NULL,
                        ended_at BIGINT DEFAULT NULL,
                        hits INT DEFAULT 0
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
        }
    }

    /**
     * Inserts a new session row and returns the generated session id.
     */
    public CompletableFuture<Long> startSession(UUID uuid, PracticeMode mode) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO lemontraining_sessions (uuid, mode, started_at) VALUES (?, ?, ?)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, mode.name());
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to start session in DB", e);
            }
            return -1L;
        });
    }

    /**
     * Updates ended_at and hits for the given session id.
     */
    public CompletableFuture<Void> endSession(long sessionId, int hits) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE lemontraining_sessions SET ended_at = ?, hits = ? WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setInt(2, hits);
                ps.setLong(3, sessionId);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to end session in DB", e);
            }
        });
    }

    /**
     * Convenience: start session async without needing the returned id.
     */
    public void startSessionAsync(UUID uuid, PracticeMode mode) {
        startSession(uuid, mode);
    }

    /**
     * Convenience: end session async by uuid (updates the most recent open session).
     */
    public void endSessionAsync(UUID uuid, int hits) {
        CompletableFuture.runAsync(() -> {
            String sql = "UPDATE lemontraining_sessions SET ended_at = ?, hits = ? "
                    + "WHERE uuid = ? AND ended_at IS NULL ORDER BY started_at DESC LIMIT 1";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setInt(2, hits);
                ps.setString(3, uuid.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to end session (by uuid) in DB", e);
            }
        });
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
