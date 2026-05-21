package com.lemonpvp.lemonpractice.database;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Arena;
import com.lemonpvp.lemonpractice.model.FFAArena;
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
                CREATE TABLE IF NOT EXISTS lp_queue (
                    uuid VARCHAR(36) PRIMARY KEY,
                    gamemode VARCHAR(32) NOT NULL,
                    queue_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        }
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

    // ELO operations (stored in lc_elo from LemonCore schema)
    public CompletableFuture<Integer> getElo(java.util.UUID uuid, String gamemode) {
        return queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT elo FROM lc_elo WHERE uuid=? AND gamemode=?")) {
                ps.setString(1, uuid.toString()); ps.setString(2, gamemode.toLowerCase());
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getInt("elo") : 1000;
            } catch (SQLException e) { return 1000; }
        });
    }

    public CompletableFuture<Void> setElo(java.util.UUID uuid, String gamemode, int elo) {
        return executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lc_elo (uuid, gamemode, elo) VALUES (?,?,?) ON DUPLICATE KEY UPDATE elo=VALUES(elo)")) {
                ps.setString(1, uuid.toString()); ps.setString(2, gamemode.toLowerCase());
                ps.setInt(3, elo);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("setElo: " + e.getMessage()); }
        });
    }

    // FFA Arena
    public CompletableFuture<Void> saveFfaArena(FFAArena arena) {
        return executeAsync(conn -> {
            try {
                StringBuilder spawnData = new StringBuilder();
                for (int i = 0; i < arena.getSpawnPoints().size(); i++) {
                    Location loc = arena.getSpawnPoints().get(i);
                    if (i > 0) spawnData.append(";");
                    spawnData.append(loc.getX()).append(",").append(loc.getY()).append(",").append(loc.getZ())
                            .append(",").append(loc.getYaw()).append(",").append(loc.getPitch());
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO lp_ffa_arenas (name, gamemode, world_name, spawn_points, region_x1, region_y1, region_z1, region_x2, region_y2, region_z2) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE gamemode=VALUES(gamemode), world_name=VALUES(world_name), " +
                        "spawn_points=VALUES(spawn_points), region_x1=VALUES(region_x1), region_y1=VALUES(region_y1), region_z1=VALUES(region_z1), " +
                        "region_x2=VALUES(region_x2), region_y2=VALUES(region_y2), region_z2=VALUES(region_z2)")) {
                    ps.setString(1, arena.getName()); ps.setString(2, arena.getGamemode());
                    ps.setString(3, arena.getWorldName()); ps.setString(4, spawnData.toString());
                    ps.setInt(5, arena.getRegionX1()); ps.setInt(6, arena.getRegionY1()); ps.setInt(7, arena.getRegionZ1());
                    ps.setInt(8, arena.getRegionX2()); ps.setInt(9, arena.getRegionY2()); ps.setInt(10, arena.getRegionZ2());
                    ps.executeUpdate();
                }
            } catch (SQLException e) { plugin.getLogger().severe("saveFfaArena: " + e.getMessage()); }
        });
    }

    public CompletableFuture<List<FFAArena>> loadFfaArenas() {
        return queryAsync(conn -> {
            List<FFAArena> arenas = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM lp_ffa_arenas")) {
                ResultSet rs = ps.executeQuery();
                int idx = 1;
                while (rs.next()) {
                    FFAArena a = new FFAArena(idx++, rs.getString("name"), rs.getString("gamemode"));
                    a.setWorldName(rs.getString("world_name"));
                    a.setRegionX1(rs.getInt("region_x1")); a.setRegionY1(rs.getInt("region_y1")); a.setRegionZ1(rs.getInt("region_z1"));
                    a.setRegionX2(rs.getInt("region_x2")); a.setRegionY2(rs.getInt("region_y2")); a.setRegionZ2(rs.getInt("region_z2"));
                    String spawnData = rs.getString("spawn_points");
                    if (spawnData != null && !spawnData.isEmpty()) {
                        World world = Bukkit.getWorld(a.getWorldName());
                        if (world != null) {
                            for (String sp : spawnData.split(";")) {
                                String[] parts = sp.split(",");
                                if (parts.length >= 3) {
                                    try {
                                        a.addSpawnPoint(new Location(world,
                                            Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                                            parts.length > 3 ? Float.parseFloat(parts[3]) : 0,
                                            parts.length > 4 ? Float.parseFloat(parts[4]) : 0));
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                    arenas.add(a);
                }
            } catch (SQLException e) { plugin.getLogger().severe("loadFfaArenas: " + e.getMessage()); }
            return arenas;
        });
    }
}
