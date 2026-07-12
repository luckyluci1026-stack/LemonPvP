package com.lemonpvp.lemonpractice.database;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.managers.EloManager;
import com.lemonpvp.lemonpractice.model.Arena;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class PracticeDatabase {

    private final LemonPractice plugin;
    private HikariDataSource dataSource;

    public PracticeDatabase(LemonPractice plugin) {
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
        config.setPoolName("LemonPractice-Pool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        dataSource = new HikariDataSource(config);
        createTables();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public CompletableFuture<Void> executeAsync(Consumer<Connection> action) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                action.accept(conn);
            } catch (SQLException e) {
                plugin.getLogger().severe("DB error: " + e.getMessage());
            }
        });
    }

    public <T> CompletableFuture<T> queryAsync(Function<Connection, T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return action.apply(conn);
            } catch (SQLException e) {
                plugin.getLogger().severe("DB error: " + e.getMessage());
                return null;
            }
        });
    }

    // ── Replays ─────────────────────────────────────────────────────────────────

    public record ReplayMeta(String name, String player1, String player2,
                             String gamemode, long createdAt, long expiresAt, int views) {}

    public CompletableFuture<Void> saveReplay(String name, String p1, String p2, String gamemode,
                                              long createdAt, long expiresAt, byte[] data) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lp_replays (name,player1,player2,gamemode,created_at,expires_at,data) " +
                    "VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE expires_at=VALUES(expires_at), data=VALUES(data)")) {
                ps.setString(1, name); ps.setString(2, p1); ps.setString(3, p2); ps.setString(4, gamemode);
                ps.setLong(5, createdAt); ps.setLong(6, expiresAt); ps.setBytes(7, data);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("saveReplay error: " + e.getMessage()); }
        });
    }

    public CompletableFuture<byte[]> loadReplayData(String name) {
        return queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT data FROM lp_replays WHERE name=? AND expires_at>?")) {
                ps.setString(1, name); ps.setLong(2, System.currentTimeMillis());
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getBytes("data"); }
            } catch (SQLException e) { plugin.getLogger().severe("loadReplay error: " + e.getMessage()); }
            return null;
        });
    }

    public CompletableFuture<ReplayMeta> getReplayMeta(String name) {
        return queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT name,player1,player2,gamemode,created_at,expires_at,views FROM lp_replays WHERE name=?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return new ReplayMeta(rs.getString("name"), rs.getString("player1"),
                            rs.getString("player2"), rs.getString("gamemode"),
                            rs.getLong("created_at"), rs.getLong("expires_at"), rs.getInt("views"));
                }
            } catch (SQLException e) { plugin.getLogger().severe("getReplayMeta error: " + e.getMessage()); }
            return null;
        });
    }

    /** Adds days to a replay's expiry. Returns the new expiry, or -1 if not found. */
    public CompletableFuture<Long> extendReplay(String name, int days) {
        return queryAsync(conn -> {
            try {
                long add = (long) days * 86_400_000L;
                try (PreparedStatement up = conn.prepareStatement(
                        "UPDATE lp_replays SET expires_at=expires_at+? WHERE name=?")) {
                    up.setLong(1, add); up.setString(2, name);
                    if (up.executeUpdate() == 0) return -1L;
                }
                try (PreparedStatement q = conn.prepareStatement(
                        "SELECT expires_at FROM lp_replays WHERE name=?")) {
                    q.setString(1, name);
                    try (ResultSet rs = q.executeQuery()) { if (rs.next()) return rs.getLong("expires_at"); }
                }
            } catch (SQLException e) { plugin.getLogger().severe("extendReplay error: " + e.getMessage()); }
            return -1L;
        });
    }

    public CompletableFuture<Integer> deleteExpiredReplays() {
        return queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lp_replays WHERE expires_at < ?")) {
                ps.setLong(1, System.currentTimeMillis());
                return ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("deleteExpiredReplays error: " + e.getMessage()); return 0; }
        });
    }

    /**
     * Reads the gamemode the player picked in the LOBBY queue GUI and deletes
     * the marker atomically. Returns null when there is none or it is older
     * than 2 minutes (stale handoff — e.g. the transfer failed midway).
     */
    public CompletableFuture<String> getPendingQueueAndDelete(java.util.UUID uuid) {
        return queryAsync(conn -> {
            try {
                String gamemode = null;
                long createdAt = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT gamemode, created_at FROM lp_pending_queue WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            gamemode = rs.getString("gamemode");
                            createdAt = rs.getLong("created_at");
                        }
                    }
                }
                if (gamemode == null) return null;
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM lp_pending_queue WHERE uuid=?")) {
                    del.setString(1, uuid.toString());
                    del.executeUpdate();
                }
                return System.currentTimeMillis() - createdAt <= 120_000L ? gamemode : null;
            } catch (SQLException e) {
                plugin.getLogger().severe("getPendingQueueAndDelete: " + e.getMessage());
                return null;
            }
        });
    }

    /** Deletes a single replay by name (admin action). Returns true if a row was removed. */
    public CompletableFuture<Boolean> deleteReplay(String name) {
        return queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lp_replays WHERE name=?")) {
                ps.setString(1, name);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { plugin.getLogger().severe("deleteReplay error: " + e.getMessage()); return false; }
        });
    }

    public CompletableFuture<List<ReplayMeta>> listReplays(int limit) {
        return listReplays(limit, "created_at");
    }

    /** Top replays by view count. */
    public CompletableFuture<List<ReplayMeta>> topReplays(int limit) {
        return listReplays(limit, "views");
    }

    private CompletableFuture<List<ReplayMeta>> listReplays(int limit, String orderColumn) {
        String order = "views".equals(orderColumn) ? "views" : "created_at";
        return queryAsync(conn -> {
            List<ReplayMeta> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT name,player1,player2,gamemode,created_at,expires_at,views FROM lp_replays " +
                    "WHERE expires_at>? ORDER BY " + order + " DESC LIMIT ?")) {
                ps.setLong(1, System.currentTimeMillis()); ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(new ReplayMeta(rs.getString("name"), rs.getString("player1"),
                            rs.getString("player2"), rs.getString("gamemode"),
                            rs.getLong("created_at"), rs.getLong("expires_at"), rs.getInt("views")));
                }
            } catch (SQLException e) { plugin.getLogger().severe("listReplays error: " + e.getMessage()); }
            return list;
        });
    }

    public CompletableFuture<Void> incrementReplayViews(String name) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE lp_replays SET views=views+1 WHERE name=?")) {
                ps.setString(1, name);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("incrementReplayViews error: " + e.getMessage()); }
        });
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lp_arenas (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(64) NOT NULL UNIQUE,
                    world_name VARCHAR(64),
                    spawn1_x DOUBLE, spawn1_y DOUBLE, spawn1_z DOUBLE,
                    spawn1_yaw FLOAT, spawn1_pitch FLOAT,
                    spawn2_x DOUBLE, spawn2_y DOUBLE, spawn2_z DOUBLE,
                    spawn2_yaw FLOAT, spawn2_pitch FLOAT,
                    spec_x DOUBLE, spec_y DOUBLE, spec_z DOUBLE,
                    spec_yaw FLOAT, spec_pitch FLOAT,
                    region_x1 INT, region_y1 INT, region_z1 INT,
                    region_x2 INT, region_y2 INT, region_z2 INT,
                    active BOOLEAN DEFAULT TRUE,
                    in_use BOOLEAN DEFAULT FALSE,
                    schematic_path VARCHAR(255),
                    dupe_group VARCHAR(64)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lp_arena_binds (
                    arena_id INT NOT NULL,
                    gamemode VARCHAR(32) NOT NULL,
                    PRIMARY KEY (arena_id, gamemode)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lp_player_kits (
                    uuid VARCHAR(36) NOT NULL,
                    gamemode VARCHAR(32) NOT NULL,
                    slot INT NOT NULL,
                    item_data MEDIUMTEXT,
                    PRIMARY KEY (uuid, gamemode, slot)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lp_duel_records (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player1_uuid VARCHAR(36),
                    player2_uuid VARCHAR(36),
                    gamemode VARCHAR(32),
                    winner_uuid VARCHAR(36),
                    elo_change_p1 INT DEFAULT 0,
                    elo_change_p2 INT DEFAULT 0,
                    duration_seconds INT DEFAULT 0,
                    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lp_replays (
                    name VARCHAR(96) PRIMARY KEY,
                    player1 VARCHAR(16),
                    player2 VARCHAR(16),
                    gamemode VARCHAR(32),
                    created_at BIGINT NOT NULL,
                    expires_at BIGINT NOT NULL,
                    views INT NOT NULL DEFAULT 0,
                    data MEDIUMBLOB NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            // Migration: add views to an older lp_replays (MySQL has no ADD COLUMN IF NOT EXISTS).
            try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, "lp_replays", "views")) {
                if (!rs.next()) stmt.executeUpdate("ALTER TABLE lp_replays ADD COLUMN views INT NOT NULL DEFAULT 0");
            } catch (Exception ignored) {}
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lp_ffa_arenas (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(64) NOT NULL UNIQUE,
                    gamemode VARCHAR(32),
                    world_name VARCHAR(64),
                    spawn_points TEXT,
                    region_x1 INT, region_y1 INT, region_z1 INT,
                    region_x2 INT, region_y2 INT, region_z2 INT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lp_pending_queue (
                    uuid VARCHAR(36) PRIMARY KEY,
                    gamemode VARCHAR(24) NOT NULL,
                    created_at BIGINT NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lp_queue (
                    uuid VARCHAR(36) PRIMARY KEY,
                    gamemode VARCHAR(32) NOT NULL,
                    queue_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lemonpractice_elo (
                    uuid VARCHAR(36) NOT NULL,
                    gamemode VARCHAR(32) NOT NULL,
                    elo INT DEFAULT 1000,
                    matches_played INT DEFAULT 0,
                    PRIMARY KEY (uuid, gamemode)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            // Cross-server direct-duel handoff: the lobby writes a row here when a
            // /duel invite is accepted, then both players are sent to the duels
            // server, which picks the row up on join and starts the match.
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lp_pending_duels (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player1 VARCHAR(36) NOT NULL,
                    player2 VARCHAR(36) NOT NULL,
                    gamemode VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        }
    }

    // -----------------------------------------------------------------------
    // Pending cross-server duels (direct /duel challenges)
    // -----------------------------------------------------------------------

    /** A queued direct duel waiting for both players to reach the duels server. */
    public record PendingDuel(long id, UUID player1, UUID player2, String gamemode) {}

    /**
     * Records a pending direct duel and prunes any stale rows (older than 2 min).
     */
    public CompletableFuture<Void> insertPendingDuel(UUID p1, UUID p2, String gamemode) {
        return executeAsync(conn -> {
            try (PreparedStatement prune = conn.prepareStatement(
                    "DELETE FROM lp_pending_duels WHERE created_at < NOW() - INTERVAL 2 MINUTE")) {
                prune.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Pending-duel prune failed: " + e.getMessage());
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lp_pending_duels (player1, player2, gamemode) VALUES (?, ?, ?)")) {
                ps.setString(1, p1.toString());
                ps.setString(2, p2.toString());
                ps.setString(3, gamemode);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("insertPendingDuel error: " + e.getMessage());
            }
        });
    }

    /**
     * Finds the most recent (≤60s old) pending duel involving {@code uuid}, or
     * {@code null} if none. Does not delete it — call {@link #deletePendingDuel(long)}
     * once both players are confirmed present to claim it race-safely.
     */
    public CompletableFuture<PendingDuel> findPendingDuel(UUID uuid) {
        return queryAsync(conn -> {
            String sql = "SELECT id, player1, player2, gamemode FROM lp_pending_duels "
                    + "WHERE (player1 = ? OR player2 = ?) AND created_at > NOW() - INTERVAL 60 SECOND "
                    + "ORDER BY created_at DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return new PendingDuel(
                            rs.getLong("id"),
                            UUID.fromString(rs.getString("player1")),
                            UUID.fromString(rs.getString("player2")),
                            rs.getString("gamemode"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("findPendingDuel error: " + e.getMessage());
            }
            return null;
        });
    }

    /**
     * Atomically claims a pending duel by id. Returns {@code true} only for the
     * caller that actually deleted the row, so exactly one server starts the duel.
     */
    public CompletableFuture<Boolean> deletePendingDuel(long id) {
        return queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM lp_pending_duels WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate() == 1;
            } catch (SQLException e) {
                plugin.getLogger().severe("deletePendingDuel error: " + e.getMessage());
                return false;
            }
        });
    }

    // Arena CRUD
    public CompletableFuture<Integer> createArena(String name) {
        return queryAsync(conn -> {
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT IGNORE INTO lp_arenas (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys();
                    if (keys.next()) return keys.getInt(1);
                }
                try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM lp_arenas WHERE name=?")) {
                    ps.setString(1, name);
                    ResultSet rs = ps.executeQuery();
                    return rs.next() ? rs.getInt(1) : -1;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("createArena: " + e.getMessage());
                return -1;
            }
        });
    }

    public CompletableFuture<Void> updateArenaSpawn(int id, int num, Location loc) {
        String prefix = num == 1 ? "spawn1" : num == 2 ? "spawn2" : "spec";
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE lp_arenas SET " + prefix + "_x=?, " + prefix + "_y=?, " + prefix + "_z=?, "
                    + prefix + "_yaw=?, " + prefix + "_pitch=?, world_name=? WHERE id=?")) {
                ps.setDouble(1, loc.getX()); ps.setDouble(2, loc.getY()); ps.setDouble(3, loc.getZ());
                ps.setFloat(4, loc.getYaw()); ps.setFloat(5, loc.getPitch());
                ps.setString(6, loc.getWorld().getName());
                ps.setInt(7, id);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("updateArenaSpawn: " + e.getMessage()); }
        });
    }

    public CompletableFuture<Void> updateArenaRegion(int id, int x1, int y1, int z1, int x2, int y2, int z2) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE lp_arenas SET region_x1=?,region_y1=?,region_z1=?,region_x2=?,region_y2=?,region_z2=? WHERE id=?")) {
                ps.setInt(1, x1); ps.setInt(2, y1); ps.setInt(3, z1);
                ps.setInt(4, x2); ps.setInt(5, y2); ps.setInt(6, z2);
                ps.setInt(7, id);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("updateArenaRegion: " + e.getMessage()); }
        });
    }

    public CompletableFuture<Void> bindArena(int id, String gamemode) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO lp_arena_binds (arena_id, gamemode) VALUES (?,?)")) {
                ps.setInt(1, id); ps.setString(2, gamemode.toLowerCase());
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("bindArena: " + e.getMessage()); }
        });
    }

    public CompletableFuture<Void> setArenaInUse(int id, boolean inUse) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE lp_arenas SET in_use=? WHERE id=?")) {
                ps.setBoolean(1, inUse); ps.setInt(2, id);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("setArenaInUse: " + e.getMessage()); }
        });
    }

    public CompletableFuture<Void> setArenaSchematic(int id, String path) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE lp_arenas SET schematic_path=? WHERE id=?")) {
                ps.setString(1, path); ps.setInt(2, id);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("setArenaSchematic: " + e.getMessage()); }
        });
    }

    public CompletableFuture<List<Arena>> loadAllArenas() {
        return queryAsync(conn -> {
            List<Arena> arenas = new ArrayList<>();
            try {
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM lp_arenas WHERE active=TRUE ORDER BY id")) {
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        Arena a = new Arena(rs.getInt("id"), rs.getString("name"));
                        a.setWorldName(rs.getString("world_name"));
                        a.setInUse(rs.getBoolean("in_use"));
                        a.setActive(rs.getBoolean("active"));
                        a.setSchematicPath(rs.getString("schematic_path"));
                        a.setDupeGroup(rs.getString("dupe_group"));
                        a.setRegionX1(rs.getInt("region_x1")); a.setRegionY1(rs.getInt("region_y1")); a.setRegionZ1(rs.getInt("region_z1"));
                        a.setRegionX2(rs.getInt("region_x2")); a.setRegionY2(rs.getInt("region_y2")); a.setRegionZ2(rs.getInt("region_z2"));

                        String worldName = rs.getString("world_name");
                        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
                        if (world != null) {
                            double s1x = rs.getDouble("spawn1_x"), s1y = rs.getDouble("spawn1_y"), s1z = rs.getDouble("spawn1_z");
                            if (s1x != 0 || s1y != 0 || s1z != 0)
                                a.setSpawn1(new Location(world, s1x, s1y, s1z, rs.getFloat("spawn1_yaw"), rs.getFloat("spawn1_pitch")));
                            double s2x = rs.getDouble("spawn2_x"), s2y = rs.getDouble("spawn2_y"), s2z = rs.getDouble("spawn2_z");
                            if (s2x != 0 || s2y != 0 || s2z != 0)
                                a.setSpawn2(new Location(world, s2x, s2y, s2z, rs.getFloat("spawn2_yaw"), rs.getFloat("spawn2_pitch")));
                            double sx = rs.getDouble("spec_x"), sy = rs.getDouble("spec_y"), sz = rs.getDouble("spec_z");
                            if (sx != 0 || sy != 0 || sz != 0)
                                a.setSpawnSpec(new Location(world, sx, sy, sz, rs.getFloat("spec_yaw"), rs.getFloat("spec_pitch")));
                        }
                        arenas.add(a);
                    }
                }
                // Load binds
                for (Arena a : arenas) {
                    try (PreparedStatement ps = conn.prepareStatement("SELECT gamemode FROM lp_arena_binds WHERE arena_id=?")) {
                        ps.setInt(1, a.getId());
                        ResultSet rs = ps.executeQuery();
                        while (rs.next()) a.addGamemodeBind(rs.getString("gamemode"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("loadAllArenas: " + e.getMessage());
            }
            return arenas;
        });
    }

    public CompletableFuture<Void> saveDuelRecord(java.util.UUID p1, java.util.UUID p2, String gamemode,
                                                    java.util.UUID winner, int eloP1, int eloP2, int duration) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lp_duel_records (player1_uuid, player2_uuid, gamemode, winner_uuid, elo_change_p1, elo_change_p2, duration_seconds) VALUES (?,?,?,?,?,?,?)")) {
                ps.setString(1, p1.toString()); ps.setString(2, p2.toString());
                ps.setString(3, gamemode);
                ps.setString(4, winner != null ? winner.toString() : null);
                ps.setInt(5, eloP1); ps.setInt(6, eloP2); ps.setInt(7, duration);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("saveDuelRecord: " + e.getMessage()); }
        });
    }

    // Kit operations
    public CompletableFuture<Void> saveKitSlot(java.util.UUID uuid, String gamemode, int slot, String itemData) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lp_player_kits (uuid, gamemode, slot, item_data) VALUES (?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE item_data=VALUES(item_data)")) {
                ps.setString(1, uuid.toString()); ps.setString(2, gamemode.toLowerCase());
                ps.setInt(3, slot); ps.setString(4, itemData);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("saveKitSlot: " + e.getMessage()); }
        });
    }

    public CompletableFuture<Map<Integer, String>> loadKit(java.util.UUID uuid, String gamemode) {
        return queryAsync(conn -> {
            Map<Integer, String> kit = new HashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT slot, item_data FROM lp_player_kits WHERE uuid=? AND gamemode=?")) {
                ps.setString(1, uuid.toString()); ps.setString(2, gamemode.toLowerCase());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) kit.put(rs.getInt("slot"), rs.getString("item_data"));
            } catch (SQLException e) { plugin.getLogger().severe("loadKit: " + e.getMessage()); }
            return kit;
        });
    }

    public CompletableFuture<Void> deleteKit(java.util.UUID uuid, String gamemode) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM lp_player_kits WHERE uuid=? AND gamemode=?")) {
                ps.setString(1, uuid.toString()); ps.setString(2, gamemode.toLowerCase());
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("deleteKit: " + e.getMessage()); }
        });
    }

    // ELO operations (lemonpractice_elo — uuid, gamemode, elo, matches_played)
    public CompletableFuture<EloManager.EloData> getEloData(java.util.UUID uuid, String gamemode) {
        return queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT elo, matches_played FROM lemonpractice_elo WHERE uuid=? AND gamemode=?")) {
                ps.setString(1, uuid.toString()); ps.setString(2, gamemode.toLowerCase());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return new EloManager.EloData(rs.getInt("elo"), rs.getInt("matches_played"));
                return null;
            } catch (SQLException e) {
                plugin.getLogger().severe("getEloData: " + e.getMessage());
                return null;
            }
        });
    }

    public CompletableFuture<Void> saveEloData(java.util.UUID uuid, String gamemode, int elo, int matchesPlayed) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lemonpractice_elo (uuid, gamemode, elo, matches_played) VALUES (?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE elo=VALUES(elo), matches_played=VALUES(matches_played)")) {
                ps.setString(1, uuid.toString()); ps.setString(2, gamemode.toLowerCase());
                ps.setInt(3, elo); ps.setInt(4, matchesPlayed);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("saveEloData: " + e.getMessage()); }
        });
    }

    // -----------------------------------------------------------------------
    // Leaderboards
    // -----------------------------------------------------------------------

    /** A single leaderboard row: the player's name plus their score value. */
    public record LeaderEntry(java.util.UUID uuid, String name, int value) {}

    /**
     * Top players by ELO for a gamemode. Only counts players past placement
     * ({@code matches_played >= minMatches}). Joins {@code lc_players} (same
     * database) for the username; falls back to the UUID prefix if missing.
     */
    public CompletableFuture<java.util.List<LeaderEntry>> getTopElo(String gamemode, int minMatches, int limit) {
        return queryAsync(conn -> {
            java.util.List<LeaderEntry> out = new java.util.ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT e.uuid AS uuid, e.elo AS val, p.username AS name " +
                    "FROM lemonpractice_elo e LEFT JOIN lc_players p ON e.uuid = p.uuid " +
                    "WHERE e.gamemode = ? AND e.matches_played >= ? " +
                    "ORDER BY e.elo DESC LIMIT ?")) {
                ps.setString(1, gamemode.toLowerCase());
                ps.setInt(2, minMatches);
                ps.setInt(3, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    java.util.UUID uuid;
                    try { uuid = java.util.UUID.fromString(rs.getString("uuid")); }
                    catch (Exception ex) { continue; }
                    String name = rs.getString("name");
                    if (name == null) name = rs.getString("uuid").substring(0, 8);
                    out.add(new LeaderEntry(uuid, name, rs.getInt("val")));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("getTopElo: " + e.getMessage());
            }
            return out;
        });
    }

    /**
     * Top players by a global {@code lc_players} stat column (kills, deaths,
     * killstreak, best_killstreak, coins). The column name is validated against
     * an allow-list before use to keep this injection-safe.
     */
    public CompletableFuture<java.util.List<LeaderEntry>> getTopStat(String column, int limit) {
        java.util.Set<String> allowed = java.util.Set.of(
                "kills", "deaths", "killstreak", "best_killstreak", "coins");
        if (!allowed.contains(column)) {
            return CompletableFuture.completedFuture(java.util.List.of());
        }
        return queryAsync(conn -> {
            java.util.List<LeaderEntry> out = new java.util.ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT uuid, username, " + column + " AS val FROM lc_players " +
                    "WHERE " + column + " > 0 ORDER BY " + column + " DESC LIMIT ?")) {
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    java.util.UUID uuid;
                    try { uuid = java.util.UUID.fromString(rs.getString("uuid")); }
                    catch (Exception ex) { continue; }
                    String name = rs.getString("username");
                    if (name == null) name = rs.getString("uuid").substring(0, 8);
                    out.add(new LeaderEntry(uuid, name, rs.getInt("val")));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("getTopStat: " + e.getMessage());
            }
            return out;
        });
    }

}
