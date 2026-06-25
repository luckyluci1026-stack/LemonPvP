package com.lemonpvp.lemoncore.database;

import com.lemonpvp.lemoncore.LemonCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class DatabaseManager {

    private final LemonCore plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(LemonCore plugin) {
        this.plugin = plugin;
    }

    public void connect() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" +
                plugin.getConfig().getString("database.host", "localhost") + ":" +
                plugin.getConfig().getInt("database.port", 3306) + "/" +
                plugin.getConfig().getString("database.database", "lemonpvp") +
                "?useSSL=false&autoReconnect=true&characterEncoding=utf8&serverTimezone=UTC");
        config.setUsername(plugin.getConfig().getString("database.username", "root"));
        config.setPassword(plugin.getConfig().getString("database.password", "password"));
        config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size", 10));
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("LemonCore-Pool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);
        createTables();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Returns the underlying HikariDataSource for external use (e.g., LemonLangDB). */
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public CompletableFuture<Void> executeAsync(Consumer<Connection> action) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                action.accept(conn);
            } catch (SQLException e) {
                plugin.getLogger().severe("Database error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public <T> CompletableFuture<T> queryAsync(Function<Connection, T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return action.apply(conn);
            } catch (SQLException e) {
                plugin.getLogger().severe("Database error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    coins BIGINT DEFAULT 0,
                    kills INT DEFAULT 0,
                    deaths INT DEFAULT 0,
                    killstreak INT DEFAULT 0,
                    best_killstreak INT DEFAULT 0,
                    first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    nick VARCHAR(32) NULL,
                    hidden_name VARCHAR(32) NULL,
                    recording_mode BOOLEAN DEFAULT FALSE,
                    INDEX idx_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_elo (
                    uuid VARCHAR(36) NOT NULL,
                    gamemode VARCHAR(32) NOT NULL,
                    elo INT DEFAULT 1000,
                    PRIMARY KEY (uuid, gamemode)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_bans (
                    id VARCHAR(12) NOT NULL PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL,
                    username VARCHAR(16) NOT NULL,
                    reason VARCHAR(255) NOT NULL,
                    banner_uuid VARCHAR(36),
                    banner_name VARCHAR(16),
                    ban_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires TIMESTAMP NULL,
                    active BOOLEAN DEFAULT TRUE,
                    INDEX idx_uuid (uuid),
                    INDEX idx_active (active)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_mutes (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL,
                    username VARCHAR(16) NOT NULL,
                    reason VARCHAR(255) NOT NULL,
                    muter_uuid VARCHAR(36),
                    muter_name VARCHAR(16),
                    mute_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires TIMESTAMP NULL,
                    active BOOLEAN DEFAULT TRUE,
                    INDEX idx_uuid (uuid),
                    INDEX idx_active (active)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_kicks (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL,
                    username VARCHAR(16) NOT NULL,
                    reason VARCHAR(255) NOT NULL,
                    kicker_uuid VARCHAR(36),
                    kicker_name VARCHAR(16),
                    kick_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_uuid (uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_warns (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL,
                    username VARCHAR(16) NOT NULL,
                    reason VARCHAR(255) NOT NULL,
                    warner_uuid VARCHAR(36),
                    warner_name VARCHAR(16),
                    warn_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_uuid (uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_friends (
                    uuid1 VARCHAR(36) NOT NULL,
                    uuid2 VARCHAR(36) NOT NULL,
                    since TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (uuid1, uuid2)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_settings (
                    uuid VARCHAR(36) PRIMARY KEY,
                    public_chat BOOLEAN DEFAULT TRUE,
                    party_invites BOOLEAN DEFAULT TRUE,
                    messages_enabled BOOLEAN DEFAULT TRUE,
                    fast_crystals BOOLEAN DEFAULT FALSE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_codes (
                    code VARCHAR(64) NOT NULL PRIMARY KEY,
                    reward_type VARCHAR(32) NOT NULL,
                    reward_value VARCHAR(255) NOT NULL,
                    max_uses INT DEFAULT 1,
                    uses INT DEFAULT 0,
                    creator_uuid VARCHAR(36),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires TIMESTAMP NULL,
                    active BOOLEAN DEFAULT TRUE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_code_redemptions (
                    code VARCHAR(64) NOT NULL,
                    uuid VARCHAR(36) NOT NULL,
                    redeemed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (code, uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_coin_transactions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL,
                    amount BIGINT NOT NULL,
                    reason VARCHAR(255),
                    admin_uuid VARCHAR(36),
                    transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_uuid (uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_stats_wipes (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL,
                    kills INT NOT NULL,
                    deaths INT NOT NULL,
                    killstreak INT NOT NULL,
                    best_killstreak INT NOT NULL,
                    coins BIGINT NOT NULL,
                    wipe_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    wiper_uuid VARCHAR(36),
                    wiper_name VARCHAR(16),
                    INDEX idx_uuid (uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_reports (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    reporter_uuid VARCHAR(36) NOT NULL,
                    reporter_name VARCHAR(16) NOT NULL,
                    reported_uuid VARCHAR(36) NOT NULL,
                    reported_name VARCHAR(16) NOT NULL,
                    reason VARCHAR(255) NOT NULL,
                    report_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    resolved BOOLEAN DEFAULT FALSE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_bug_reports (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    reporter_uuid VARCHAR(36) NOT NULL,
                    reporter_name VARCHAR(16) NOT NULL,
                    description TEXT NOT NULL,
                    report_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    resolved BOOLEAN DEFAULT FALSE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_message_reports (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    reporter_uuid VARCHAR(36) NOT NULL,
                    reporter_name VARCHAR(16) NOT NULL,
                    reported_uuid VARCHAR(36) NOT NULL,
                    reported_name VARCHAR(16) NOT NULL,
                    reason VARCHAR(255) NOT NULL,
                    report_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    resolved BOOLEAN DEFAULT FALSE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_offenses (
                    uuid VARCHAR(36) PRIMARY KEY,
                    nword_count INT DEFAULT 0,
                    last_offense TIMESTAMP NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_verify_codes (
                    uuid VARCHAR(36) NOT NULL,
                    code VARCHAR(6) NOT NULL PRIMARY KEY,
                    player_name VARCHAR(16) NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    INDEX idx_vc_uuid (uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_discord_links (
                    uuid VARCHAR(36) PRIMARY KEY,
                    discord_id VARCHAR(20) NOT NULL,
                    discord_username VARCHAR(100) NOT NULL,
                    linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lemoncore_maintenance (
                    id INT PRIMARY KEY DEFAULT 1,
                    maintenance_on BOOLEAN DEFAULT FALSE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lemoncore_maintenance_whitelist (
                    uuid VARCHAR(36) PRIMARY KEY,
                    player_name VARCHAR(16) NOT NULL,
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_economy (
                    uuid VARCHAR(36) PRIMARY KEY,
                    apples BIGINT DEFAULT 0,
                    planks BIGINT DEFAULT 0
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        }
    }
}
