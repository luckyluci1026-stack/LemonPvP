package com.lemonpvp.lemoncosmetics.database;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public class CosmeticsDatabase {

    private final LemonCosmetics plugin;
    private HikariDataSource dataSource;

    public CosmeticsDatabase(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Connection lifecycle
    // -------------------------------------------------------------------------

    public void connect() throws Exception {
        var cfg = plugin.getConfig();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true&characterEncoding=utf8",
                cfg.getString("database.host", "localhost"),
                cfg.getInt("database.port", 3306),
                cfg.getString("database.database", "lemonpvp")
        ));
        hikariConfig.setUsername(cfg.getString("database.username", "root"));
        hikariConfig.setPassword(cfg.getString("database.password", "password"));
        hikariConfig.setMaximumPoolSize(cfg.getInt("database.pool-size", 10));
        hikariConfig.setPoolName("LemonCosmetics-Pool");
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(hikariConfig);
        createTables();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // -------------------------------------------------------------------------
    // Table creation
    // -------------------------------------------------------------------------

    private void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS lc_cosmetics_kill_effects (" +
                    "    uuid VARCHAR(36) NOT NULL," +
                    "    effect_id VARCHAR(64) NOT NULL," +
                    "    active BOOLEAN DEFAULT FALSE," +
                    "    PRIMARY KEY (uuid, effect_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS lc_cosmetics_trim_patterns_owned (" +
                    "    uuid VARCHAR(36) NOT NULL," +
                    "    pattern_id VARCHAR(64) NOT NULL," +
                    "    PRIMARY KEY (uuid, pattern_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS lc_cosmetics_trim_materials_owned (" +
                    "    uuid VARCHAR(36) NOT NULL," +
                    "    material_id VARCHAR(64) NOT NULL," +
                    "    PRIMARY KEY (uuid, material_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS lc_cosmetics_armor_trims (" +
                    "    uuid VARCHAR(36) NOT NULL," +
                    "    slot VARCHAR(16) NOT NULL," +
                    "    pattern_id VARCHAR(64)," +
                    "    material_id VARCHAR(64)," +
                    "    PRIMARY KEY (uuid, slot)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS lc_cosmetics_trails (" +
                    "    uuid VARCHAR(36) NOT NULL," +
                    "    trail_id VARCHAR(64) NOT NULL," +
                    "    active BOOLEAN DEFAULT FALSE," +
                    "    PRIMARY KEY (uuid, trail_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS lc_cosmetics_win_effects (" +
                    "    uuid VARCHAR(36) NOT NULL," +
                    "    effect_id VARCHAR(64) NOT NULL," +
                    "    active BOOLEAN DEFAULT FALSE," +
                    "    PRIMARY KEY (uuid, effect_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS lc_cosmetics_death_effects (" +
                    "    uuid VARCHAR(36) NOT NULL," +
                    "    effect_id VARCHAR(64) NOT NULL," +
                    "    active BOOLEAN DEFAULT FALSE," +
                    "    PRIMARY KEY (uuid, effect_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS lc_cosmetics_tags (" +
                    "    uuid VARCHAR(36) NOT NULL," +
                    "    tag_id VARCHAR(64) NOT NULL," +
                    "    equipped BOOLEAN DEFAULT FALSE," +
                    "    PRIMARY KEY (uuid, tag_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")) {
                stmt.executeUpdate();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Async execution helpers
    // -------------------------------------------------------------------------

    public CompletableFuture<Void> executeAsync(Consumer<Connection> action) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                action.accept(conn);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database execute error", e);
            }
        });
    }

    public <T> CompletableFuture<T> queryAsync(Function<Connection, T> query) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return query.apply(conn);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database query error", e);
                return null;
            }
        });
    }

    // -------------------------------------------------------------------------
    // Player data — load
    // -------------------------------------------------------------------------

    public CompletableFuture<PlayerCosmetics> loadPlayerCosmetics(UUID uuid) {
        return queryAsync(conn -> {
            PlayerCosmetics cosmetics = new PlayerCosmetics(uuid);
            String uuidStr = uuid.toString();

            // Kill effects
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT effect_id, active FROM lc_cosmetics_kill_effects WHERE uuid = ?")) {
                stmt.setString(1, uuidStr);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String effectId = rs.getString("effect_id");
                        boolean active = rs.getBoolean("active");
                        cosmetics.getOwnedEffects().add(effectId);
                        if (active) {
                            cosmetics.setActiveEffectId(effectId);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load kill effects for " + uuidStr, e);
            }

            // Death effects
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT effect_id, active FROM lc_cosmetics_death_effects WHERE uuid = ?")) {
                stmt.setString(1, uuidStr);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String effectId = rs.getString("effect_id");
                        boolean active = rs.getBoolean("active");
                        cosmetics.getOwnedDeathEffects().add(effectId);
                        if (active) {
                            cosmetics.setActiveDeathEffectId(effectId);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load death effects for " + uuidStr, e);
            }

            // Win effects
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT effect_id, active FROM lc_cosmetics_win_effects WHERE uuid = ?")) {
                stmt.setString(1, uuidStr);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String effectId = rs.getString("effect_id");
                        boolean active = rs.getBoolean("active");
                        cosmetics.getOwnedWinEffects().add(effectId);
                        if (active) {
                            cosmetics.setActiveWinEffectId(effectId);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load win effects for " + uuidStr, e);
            }

            // Owned trim patterns
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT pattern_id FROM lc_cosmetics_trim_patterns_owned WHERE uuid = ?")) {
                stmt.setString(1, uuidStr);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        cosmetics.getOwnedPatterns().add(rs.getString("pattern_id"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load owned patterns for " + uuidStr, e);
            }

            // Owned trim materials
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT material_id FROM lc_cosmetics_trim_materials_owned WHERE uuid = ?")) {
                stmt.setString(1, uuidStr);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        cosmetics.getOwnedMaterials().add(rs.getString("material_id"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load owned materials for " + uuidStr, e);
            }

            // Applied armor trims
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT slot, pattern_id, material_id FROM lc_cosmetics_armor_trims WHERE uuid = ?")) {
                stmt.setString(1, uuidStr);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String slot = rs.getString("slot");
                        String patternId = rs.getString("pattern_id");
                        String materialId = rs.getString("material_id");
                        cosmetics.setAppliedTrim(slot, patternId, materialId);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load armor trims for " + uuidStr, e);
            }

            // Arrow trails
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT trail_id, active FROM lc_cosmetics_trails WHERE uuid = ?")) {
                stmt.setString(1, uuidStr);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String trailId = rs.getString("trail_id");
                        boolean active = rs.getBoolean("active");
                        cosmetics.getOwnedTrails().add(trailId);
                        if (active) {
                            cosmetics.setActiveTrailId(trailId);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load trails for " + uuidStr, e);
            }

            // Tags
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT tag_id, equipped FROM lc_cosmetics_tags WHERE uuid = ?")) {
                stmt.setString(1, uuidStr);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String tagId = rs.getString("tag_id");
                        boolean equipped = rs.getBoolean("equipped");
                        cosmetics.getOwnedTags().add(tagId);
                        if (equipped) cosmetics.setEquippedTagId(tagId);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load tags for " + uuidStr, e);
            }

            return cosmetics;
        });
    }

    // -------------------------------------------------------------------------
    // Kill effects
    // -------------------------------------------------------------------------

    /**
     * Upserts a kill effect row. Call this when a player unlocks an effect or
     * when persisting the active state change.
     */
    public CompletableFuture<Void> saveKillEffect(UUID uuid, String effectId, boolean active) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO lc_cosmetics_kill_effects (uuid, effect_id, active) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE active = VALUES(active)")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, effectId);
                stmt.setBoolean(3, active);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save kill effect", e);
            }
        });
    }

    /** Removes an owned kill effect record entirely. */
    public CompletableFuture<Void> removeKillEffect(UUID uuid, String effectId) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM lc_cosmetics_kill_effects WHERE uuid = ? AND effect_id = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, effectId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove kill effect", e);
            }
        });
    }

    /**
     * Sets the given effect as active and marks all other effects for this
     * player as inactive in a single transaction.
     */
    public CompletableFuture<Void> setActiveEffect(UUID uuid, String effectId) {
        return executeAsync(conn -> {
            try {
                conn.setAutoCommit(false);

                // Clear active flag on all existing rows
                try (PreparedStatement clear = conn.prepareStatement(
                        "UPDATE lc_cosmetics_kill_effects SET active = FALSE WHERE uuid = ?")) {
                    clear.setString(1, uuid.toString());
                    clear.executeUpdate();
                }

                // Upsert the chosen effect with active = true
                try (PreparedStatement upsert = conn.prepareStatement(
                        "INSERT INTO lc_cosmetics_kill_effects (uuid, effect_id, active) VALUES (?, ?, TRUE) " +
                        "ON DUPLICATE KEY UPDATE active = TRUE")) {
                    upsert.setString(1, uuid.toString());
                    upsert.setString(2, effectId);
                    upsert.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
                plugin.getLogger().log(Level.SEVERE, "Failed to set active effect", e);
            } finally {
                try { conn.setAutoCommit(true); } catch (SQLException e) { /* ignore */ }
            }
        });
    }

    /** Sets all kill effects for the player to inactive. */
    public CompletableFuture<Void> clearActiveEffect(UUID uuid) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE lc_cosmetics_kill_effects SET active = FALSE WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clear active effect", e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Win effects
    // -------------------------------------------------------------------------

    /** Upserts a win-effect row. */
    public CompletableFuture<Void> saveWinEffect(UUID uuid, String effectId, boolean active) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO lc_cosmetics_win_effects (uuid, effect_id, active) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE active = VALUES(active)")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, effectId);
                stmt.setBoolean(3, active);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save win effect", e);
            }
        });
    }

    /**
     * Sets the given win effect as active and marks all other win effects for
     * this player as inactive in a single transaction.
     */
    public CompletableFuture<Void> setActiveWinEffect(UUID uuid, String effectId) {
        return executeAsync(conn -> {
            try {
                conn.setAutoCommit(false);

                try (PreparedStatement clear = conn.prepareStatement(
                        "UPDATE lc_cosmetics_win_effects SET active = FALSE WHERE uuid = ?")) {
                    clear.setString(1, uuid.toString());
                    clear.executeUpdate();
                }

                try (PreparedStatement upsert = conn.prepareStatement(
                        "INSERT INTO lc_cosmetics_win_effects (uuid, effect_id, active) VALUES (?, ?, TRUE) " +
                        "ON DUPLICATE KEY UPDATE active = TRUE")) {
                    upsert.setString(1, uuid.toString());
                    upsert.setString(2, effectId);
                    upsert.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
                plugin.getLogger().log(Level.SEVERE, "Failed to set active win effect", e);
            } finally {
                try { conn.setAutoCommit(true); } catch (SQLException e) { /* ignore */ }
            }
        });
    }

    /** Sets all win effects for the player to inactive. */
    public CompletableFuture<Void> clearActiveWinEffect(UUID uuid) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE lc_cosmetics_win_effects SET active = FALSE WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clear active win effect", e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Death effects
    // -------------------------------------------------------------------------

    /** Upserts a death-effect row. */
    public CompletableFuture<Void> saveDeathEffect(UUID uuid, String effectId, boolean active) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO lc_cosmetics_death_effects (uuid, effect_id, active) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE active = VALUES(active)")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, effectId);
                stmt.setBoolean(3, active);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save death effect", e);
            }
        });
    }

    /**
     * Sets the given death effect as active and marks all other death effects
     * for this player as inactive in a single transaction.
     */
    public CompletableFuture<Void> setActiveDeathEffect(UUID uuid, String effectId) {
        return executeAsync(conn -> {
            try {
                conn.setAutoCommit(false);

                try (PreparedStatement clear = conn.prepareStatement(
                        "UPDATE lc_cosmetics_death_effects SET active = FALSE WHERE uuid = ?")) {
                    clear.setString(1, uuid.toString());
                    clear.executeUpdate();
                }

                try (PreparedStatement upsert = conn.prepareStatement(
                        "INSERT INTO lc_cosmetics_death_effects (uuid, effect_id, active) VALUES (?, ?, TRUE) " +
                        "ON DUPLICATE KEY UPDATE active = TRUE")) {
                    upsert.setString(1, uuid.toString());
                    upsert.setString(2, effectId);
                    upsert.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
                plugin.getLogger().log(Level.SEVERE, "Failed to set active death effect", e);
            } finally {
                try { conn.setAutoCommit(true); } catch (SQLException e) { /* ignore */ }
            }
        });
    }

    /** Sets all death effects for the player to inactive. */
    public CompletableFuture<Void> clearActiveDeathEffect(UUID uuid) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE lc_cosmetics_death_effects SET active = FALSE WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clear active death effect", e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Owned trim patterns / materials
    // -------------------------------------------------------------------------

    public CompletableFuture<Void> addOwnedPattern(UUID uuid, String patternId) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT IGNORE INTO lc_cosmetics_trim_patterns_owned (uuid, pattern_id) VALUES (?, ?)")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, patternId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add owned pattern", e);
            }
        });
    }

    public CompletableFuture<Void> addOwnedMaterial(UUID uuid, String materialId) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT IGNORE INTO lc_cosmetics_trim_materials_owned (uuid, material_id) VALUES (?, ?)")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, materialId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add owned material", e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Applied armor trims
    // -------------------------------------------------------------------------

    /**
     * Upserts an armor trim row. {@code null} values are stored as SQL NULL,
     * which is legal given the schema does not enforce NOT NULL on those columns.
     */
    public CompletableFuture<Void> saveArmorTrim(UUID uuid, String slot, String patternId, String materialId) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO lc_cosmetics_armor_trims (uuid, slot, pattern_id, material_id) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE pattern_id = VALUES(pattern_id), material_id = VALUES(material_id)")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, slot);
                stmt.setString(3, patternId);
                stmt.setString(4, materialId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save armor trim", e);
            }
        });
    }

    /** Removes the armor trim row for the given slot entirely. */
    public CompletableFuture<Void> clearArmorTrim(UUID uuid, String slot) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM lc_cosmetics_armor_trims WHERE uuid = ? AND slot = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, slot);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clear armor trim", e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Arrow trails
    // -------------------------------------------------------------------------

    /** Upserts a trail row. */
    public CompletableFuture<Void> saveTrail(UUID uuid, String trailId, boolean active) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO lc_cosmetics_trails (uuid, trail_id, active) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE active = VALUES(active)")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, trailId);
                stmt.setBoolean(3, active);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save trail", e);
            }
        });
    }

    /**
     * Sets the given trail as active and marks all other trails for this player
     * as inactive in a single transaction.
     */
    public CompletableFuture<Void> setActiveTrail(UUID uuid, String trailId) {
        return executeAsync(conn -> {
            try {
                conn.setAutoCommit(false);

                try (PreparedStatement clear = conn.prepareStatement(
                        "UPDATE lc_cosmetics_trails SET active = FALSE WHERE uuid = ?")) {
                    clear.setString(1, uuid.toString());
                    clear.executeUpdate();
                }

                try (PreparedStatement upsert = conn.prepareStatement(
                        "INSERT INTO lc_cosmetics_trails (uuid, trail_id, active) VALUES (?, ?, TRUE) " +
                        "ON DUPLICATE KEY UPDATE active = TRUE")) {
                    upsert.setString(1, uuid.toString());
                    upsert.setString(2, trailId);
                    upsert.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
                plugin.getLogger().log(Level.SEVERE, "Failed to set active trail", e);
            } finally {
                try { conn.setAutoCommit(true); } catch (SQLException e) { /* ignore */ }
            }
        });
    }

    /** Clears the active flag on all trails for the player. */
    public CompletableFuture<Void> clearActiveTrail(UUID uuid) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE lc_cosmetics_trails SET active = FALSE WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clear active trail", e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Tags
    // -------------------------------------------------------------------------

    public CompletableFuture<Void> saveTag(UUID uuid, String tagId, boolean equipped) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO lc_cosmetics_tags (uuid, tag_id, equipped) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE equipped = VALUES(equipped)")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, tagId);
                stmt.setBoolean(3, equipped);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save tag", e);
            }
        });
    }

    public CompletableFuture<Void> setEquippedTag(UUID uuid, String tagId) {
        return executeAsync(conn -> {
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement clear = conn.prepareStatement(
                        "UPDATE lc_cosmetics_tags SET equipped = FALSE WHERE uuid = ?")) {
                    clear.setString(1, uuid.toString());
                    clear.executeUpdate();
                }
                try (PreparedStatement upsert = conn.prepareStatement(
                        "INSERT INTO lc_cosmetics_tags (uuid, tag_id, equipped) VALUES (?, ?, TRUE) " +
                        "ON DUPLICATE KEY UPDATE equipped = TRUE")) {
                    upsert.setString(1, uuid.toString());
                    upsert.setString(2, tagId);
                    upsert.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
                plugin.getLogger().log(Level.SEVERE, "Failed to set equipped tag", e);
            } finally {
                try { conn.setAutoCommit(true); } catch (SQLException e) { /* ignore */ }
            }
        });
    }

    public CompletableFuture<Void> clearEquippedTag(UUID uuid) {
        return executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE lc_cosmetics_tags SET equipped = FALSE WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clear equipped tag", e);
            }
        });
    }
}
