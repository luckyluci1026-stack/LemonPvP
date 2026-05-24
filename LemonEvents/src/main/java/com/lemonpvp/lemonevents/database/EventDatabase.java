package com.lemonpvp.lemonevents.database;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.model.EventStatus;
import com.lemonpvp.lemonevents.model.EventType;
import com.lemonpvp.lemonevents.model.GameEvent;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public class EventDatabase {

    private final LemonEvents plugin;
    private HikariDataSource dataSource;

    public EventDatabase(LemonEvents plugin) {
        this.plugin = plugin;
    }

    public void connect() throws Exception {
        var cfg = plugin.getConfig();
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true&characterEncoding=utf8",
                cfg.getString("database.host", "localhost"),
                cfg.getInt("database.port", 3306),
                cfg.getString("database.database", "lemonpvp")));
        hikari.setUsername(cfg.getString("database.username", "root"));
        hikari.setPassword(cfg.getString("database.password", "password"));
        hikari.setMaximumPoolSize(cfg.getInt("database.pool-size", 10));
        hikari.setPoolName("LemonEvents-Pool");
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        dataSource = new HikariDataSource(hikari);
        createTables();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            conn.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS lemonevents_events (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  name VARCHAR(64) NOT NULL UNIQUE," +
                "  type VARCHAR(32) NOT NULL," +
                "  prize1 INT NOT NULL DEFAULT 0," +
                "  prize2 INT NOT NULL DEFAULT 0," +
                "  prize3 INT NOT NULL DEFAULT 0," +
                "  status VARCHAR(16) NOT NULL DEFAULT 'WAITING'," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            conn.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS lemonevents_participants (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  event_id INT NOT NULL," +
                "  uuid VARCHAR(36) NOT NULL," +
                "  placement INT DEFAULT NULL," +
                "  prize_paid BOOLEAN DEFAULT FALSE," +
                "  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  UNIQUE KEY uk_event_player (event_id, uuid)," +
                "  FOREIGN KEY (event_id) REFERENCES lemonevents_events(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            conn.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS lemonevents_ffa_backup (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  backup_path VARCHAR(512) NOT NULL," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    // ── Async helpers ──────────────────────────────────────────────────────────

    public CompletableFuture<Void> executeAsync(Consumer<Connection> action) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                action.accept(conn);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "DB execute error", e);
            }
        });
    }

    public <T> CompletableFuture<T> queryAsync(Function<Connection, T> fn) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return fn.apply(conn);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "DB query error", e);
                return null;
            }
        });
    }

    // ── Events ────────────────────────────────────────────────────────────────

    public CompletableFuture<GameEvent> createEvent(String name, EventType type,
                                                     int prize3, int prize2, int prize1) {
        return queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lemonevents_events (name, type, prize1, prize2, prize3) VALUES (?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, type.name());
                ps.setInt(3, prize1);
                ps.setInt(4, prize2);
                ps.setInt(5, prize3);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return new GameEvent(rs.getInt(1), name, type, prize3, prize2, prize1, EventStatus.WAITING);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "createEvent error", e);
            }
            return null;
        });
    }

    public CompletableFuture<GameEvent> loadEvent(String name) {
        return queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, name, type, prize1, prize2, prize3, status FROM lemonevents_events WHERE name=?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new GameEvent(
                            rs.getInt("id"), rs.getString("name"),
                            EventType.fromString(rs.getString("type")),
                            rs.getInt("prize3"), rs.getInt("prize2"), rs.getInt("prize1"),
                            EventStatus.valueOf(rs.getString("status")));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "loadEvent error", e);
            }
            return null;
        });
    }

    public CompletableFuture<List<GameEvent>> loadAllEvents() {
        return queryAsync(conn -> {
            List<GameEvent> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, name, type, prize1, prize2, prize3, status FROM lemonevents_events")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new GameEvent(
                            rs.getInt("id"), rs.getString("name"),
                            EventType.fromString(rs.getString("type")),
                            rs.getInt("prize3"), rs.getInt("prize2"), rs.getInt("prize1"),
                            EventStatus.valueOf(rs.getString("status"))));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "loadAllEvents error", e);
            }
            return list;
        });
    }

    public CompletableFuture<Void> updateEventStatus(int eventId, EventStatus status) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE lemonevents_events SET status=? WHERE id=?")) {
                ps.setString(1, status.name());
                ps.setInt(2, eventId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "updateEventStatus error", e);
            }
        });
    }

    // ── Participants ──────────────────────────────────────────────────────────

    public CompletableFuture<Void> addParticipant(int eventId, UUID uuid) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO lemonevents_participants (event_id, uuid) VALUES (?,?)")) {
                ps.setInt(1, eventId);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "addParticipant error", e);
            }
        });
    }

    public CompletableFuture<Void> setPlacement(int eventId, UUID uuid, int placement) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE lemonevents_participants SET placement=? WHERE event_id=? AND uuid=?")) {
                ps.setInt(1, placement);
                ps.setInt(2, eventId);
                ps.setString(3, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "setPlacement error", e);
            }
        });
    }

    public CompletableFuture<Void> markPrizePaid(int eventId, UUID uuid) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE lemonevents_participants SET prize_paid=TRUE WHERE event_id=? AND uuid=?")) {
                ps.setInt(1, eventId);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "markPrizePaid error", e);
            }
        });
    }

    // ── FFA Backup ────────────────────────────────────────────────────────────

    public CompletableFuture<Void> saveFfaBackupRecord(String path) {
        return executeAsync(conn -> {
            try {
                conn.createStatement().executeUpdate("DELETE FROM lemonevents_ffa_backup");
            } catch (SQLException ignored) {}
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lemonevents_ffa_backup (backup_path) VALUES (?)")) {
                ps.setString(1, path);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "saveFfaBackupRecord error", e);
            }
        });
    }

    public CompletableFuture<String> loadFfaBackupPath() {
        return queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT backup_path FROM lemonevents_ffa_backup ORDER BY created_at DESC LIMIT 1")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("backup_path");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "loadFfaBackupPath error", e);
            }
            return null;
        });
    }

    public CompletableFuture<Void> clearFfaBackupRecord() {
        return executeAsync(conn -> {
            try {
                conn.createStatement().executeUpdate("DELETE FROM lemonevents_ffa_backup");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "clearFfaBackupRecord error", e);
            }
        });
    }
}
