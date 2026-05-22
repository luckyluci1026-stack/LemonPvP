package com.lemonpvp.lemonquests.database;

import com.lemonpvp.lemonquests.LemonQuests;
import com.lemonpvp.lemonquests.model.QuestProgress;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class QuestsDatabase {

    private final LemonQuests plugin;
    private HikariDataSource dataSource;

    public QuestsDatabase(LemonQuests plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void connect() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://"
                + plugin.getConfig().getString("database.host", "localhost") + ":"
                + plugin.getConfig().getInt("database.port", 3306) + "/"
                + plugin.getConfig().getString("database.database", "lemonpvp")
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8&serverTimezone=UTC");
        config.setUsername(plugin.getConfig().getString("database.username", "root"));
        config.setPassword(plugin.getConfig().getString("database.password", "password"));
        config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size", 10));
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("LemonQuests-Pool");
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

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public CompletableFuture<Void> executeAsync(Consumer<Connection> action) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                action.accept(conn);
            } catch (SQLException e) {
                plugin.getLogger().severe("[QuestsDatabase] executeAsync error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public <T> CompletableFuture<T> queryAsync(Function<Connection, T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return action.apply(conn);
            } catch (SQLException e) {
                plugin.getLogger().severe("[QuestsDatabase] queryAsync error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    // -------------------------------------------------------------------------
    // Schema
    // -------------------------------------------------------------------------

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lemonquests_players (
                    uuid VARCHAR(36) NOT NULL,
                    level INT NOT NULL DEFAULT 1,
                    xp INT NOT NULL DEFAULT 0,
                    last_daily_reset TIMESTAMP NULL,
                    PRIMARY KEY (uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lemonquests_progress (
                    uuid VARCHAR(36) NOT NULL,
                    quest_id VARCHAR(64) NOT NULL,
                    date DATE NOT NULL,
                    progress INT NOT NULL DEFAULT 0,
                    completed BOOLEAN NOT NULL DEFAULT FALSE,
                    reward_claimed BOOLEAN NOT NULL DEFAULT FALSE,
                    PRIMARY KEY (uuid, quest_id, date)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lemonquests_rank_history (
                    id INT NOT NULL AUTO_INCREMENT,
                    uuid VARCHAR(36) NOT NULL,
                    rank VARCHAR(32) NOT NULL,
                    unlocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    INDEX idx_uuid_rank (uuid, rank)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lemonquests_daily_selection (
                    date DATE NOT NULL,
                    quest_id VARCHAR(64) NOT NULL,
                    PRIMARY KEY (date, quest_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        }
    }

    // -------------------------------------------------------------------------
    // Player data (level / xp)
    // -------------------------------------------------------------------------

    /**
     * Loads a player's level and XP from the database.
     *
     * @return a CompletableFuture containing an int[] where [0] = level, [1] = xp.
     *         If the player has no record yet, returns {1, 0}.
     */
    public CompletableFuture<int[]> loadPlayerData(UUID uuid) {
        return queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT level, xp FROM lemonquests_players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new int[]{rs.getInt("level"), rs.getInt("xp")};
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[QuestsDatabase] loadPlayerData error: " + e.getMessage());
                e.printStackTrace();
            }
            return new int[]{1, 0};
        });
    }

    /**
     * Upserts a player's level and XP into the database.
     */
    public CompletableFuture<Void> savePlayerData(UUID uuid, int level, int xp) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO lemonquests_players (uuid, level, xp)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE level = VALUES(level), xp = VALUES(xp)
            """)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, level);
                ps.setInt(3, xp);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("[QuestsDatabase] savePlayerData error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Quest progress
    // -------------------------------------------------------------------------

    /**
     * Loads a single quest progress record for the given player, quest, and date.
     * Returns null if no record exists.
     */
    public CompletableFuture<QuestProgress> loadProgress(UUID uuid, String questId, LocalDate date) {
        return queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                SELECT progress, completed, reward_claimed
                FROM lemonquests_progress
                WHERE uuid = ? AND quest_id = ? AND date = ?
            """)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, questId);
                ps.setDate(3, Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new QuestProgress(
                                questId,
                                date.toString(),
                                rs.getInt("progress"),
                                rs.getBoolean("completed"),
                                rs.getBoolean("reward_claimed")
                        );
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[QuestsDatabase] loadProgress error: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Upserts a QuestProgress record (insert or update progress, completed, reward_claimed).
     */
    public CompletableFuture<Void> saveProgress(UUID uuid, QuestProgress progress) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO lemonquests_progress (uuid, quest_id, date, progress, completed, reward_claimed)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    progress = VALUES(progress),
                    completed = VALUES(completed),
                    reward_claimed = VALUES(reward_claimed)
            """)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, progress.getQuestId());
                ps.setDate(3, Date.valueOf(progress.getDate()));
                ps.setInt(4, progress.getProgress());
                ps.setBoolean(5, progress.isCompleted());
                ps.setBoolean(6, progress.isRewardClaimed());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("[QuestsDatabase] saveProgress error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Returns all progress records for a player on a given date.
     */
    public CompletableFuture<List<QuestProgress>> loadAllProgressToday(UUID uuid, LocalDate date) {
        return queryAsync(conn -> {
            List<QuestProgress> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("""
                SELECT quest_id, progress, completed, reward_claimed
                FROM lemonquests_progress
                WHERE uuid = ? AND date = ?
            """)) {
                ps.setString(1, uuid.toString());
                ps.setDate(2, Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new QuestProgress(
                                rs.getString("quest_id"),
                                date.toString(),
                                rs.getInt("progress"),
                                rs.getBoolean("completed"),
                                rs.getBoolean("reward_claimed")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[QuestsDatabase] loadAllProgressToday error: " + e.getMessage());
                e.printStackTrace();
            }
            return list;
        });
    }

    /**
     * Atomically increments a player's progress for a quest on a given date.
     * Uses INSERT … ON DUPLICATE KEY UPDATE so the record is created if absent.
     */
    public CompletableFuture<Void> incrementProgress(UUID uuid, String questId, LocalDate date, int amount) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO lemonquests_progress (uuid, quest_id, date, progress, completed, reward_claimed)
                VALUES (?, ?, ?, ?, FALSE, FALSE)
                ON DUPLICATE KEY UPDATE progress = progress + VALUES(progress)
            """)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, questId);
                ps.setDate(3, Date.valueOf(date));
                ps.setInt(4, amount);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("[QuestsDatabase] incrementProgress error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Marks a quest as completed and its reward as claimed for a player on a given date.
     */
    public CompletableFuture<Void> markCompleted(UUID uuid, String questId, LocalDate date) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE lemonquests_progress
                SET completed = TRUE, reward_claimed = TRUE
                WHERE uuid = ? AND quest_id = ? AND date = ?
            """)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, questId);
                ps.setDate(3, Date.valueOf(date));
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("[QuestsDatabase] markCompleted error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Rank history
    // -------------------------------------------------------------------------

    /**
     * Returns true if the player has a rank-unlock record for the given rank.
     */
    public CompletableFuture<Boolean> hasRankUnlocked(UUID uuid, String rank) {
        return queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                SELECT 1 FROM lemonquests_rank_history
                WHERE uuid = ? AND rank = ?
                LIMIT 1
            """)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, rank);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[QuestsDatabase] hasRankUnlocked error: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Records a rank unlock event for the given player.
     */
    public CompletableFuture<Void> recordRankUnlock(UUID uuid, String rank) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO lemonquests_rank_history (uuid, rank) VALUES (?, ?)
            """)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, rank);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("[QuestsDatabase] recordRankUnlock error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Daily quest selection
    // -------------------------------------------------------------------------

    /**
     * Replaces the daily selection for the given date with the provided quest IDs.
     * Deletes any existing entries for that date before inserting.
     */
    public CompletableFuture<Void> saveDailySelection(LocalDate date, List<String> questIds) {
        return executeAsync(conn -> {
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM lemonquests_daily_selection WHERE date = ?")) {
                    del.setDate(1, Date.valueOf(date));
                    del.executeUpdate();
                }
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO lemonquests_daily_selection (date, quest_id) VALUES (?, ?)")) {
                    for (String questId : questIds) {
                        ins.setDate(1, Date.valueOf(date));
                        ins.setString(2, questId);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
                plugin.getLogger().severe("[QuestsDatabase] saveDailySelection error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        });
    }

    /**
     * Returns the list of quest IDs that were selected for the given date, or an
     * empty list if none have been saved yet.
     */
    public CompletableFuture<List<String>> loadDailySelection(LocalDate date) {
        return queryAsync(conn -> {
            List<String> ids = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT quest_id FROM lemonquests_daily_selection WHERE date = ?")) {
                ps.setDate(1, Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getString("quest_id"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[QuestsDatabase] loadDailySelection error: " + e.getMessage());
                e.printStackTrace();
            }
            return ids;
        });
    }
}
