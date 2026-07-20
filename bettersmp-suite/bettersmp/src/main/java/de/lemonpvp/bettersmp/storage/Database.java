package de.lemonpvp.bettersmp.storage;

import de.lemonpvp.bettersmp.BetterSMP;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Datenbank-Schicht fuer Stats, Bans und Mutes.
 *
 * Nutzt MariaDB, wenn konfiguriert - sonst automatisch eine lokale SQLite-Datei.
 * Alle Zugriffe laufen ueber einen Single-Thread-Executor (eine Verbindung),
 * damit der Bukkit-Main-Thread nie durch Datenbank-IO blockiert wird und
 * SQLite-Sperren kein Thema sind.
 */
public final class Database {

    private final BetterSMP plugin;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "BetterSMP-DB");
                t.setDaemon(true);
                return t;
            });

    private Driver driver;
    private String url;
    private Properties props = new Properties();
    private boolean sqlite = true;
    private Connection connection;
    private String typeName = "SQLite";

    public Database(BetterSMP plugin) {
        this.plugin = plugin;
    }

    public String typeName() {
        return typeName;
    }

    /** Initialisiert die Verbindung und legt die Tabellen an. Fällt bei
     *  MariaDB-Fehlern auf SQLite zurueck. */
    public void init() {
        boolean mariadb = "mariadb".equalsIgnoreCase(plugin.getConfig().getString("database.type"))
                || (plugin.getConfig().getString("database.type", "auto").equalsIgnoreCase("auto")
                    && plugin.getConfig().getBoolean("database.mariadb.enabled", false));
        try {
            if (mariadb) {
                setupMariaDB();
                openAndVerify();
                typeName = "MariaDB";
                sqlite = false;
            } else {
                setupSQLite();
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("MariaDB-Verbindung fehlgeschlagen (" + t.getMessage()
                    + ") - nutze SQLite.");
            try {
                setupSQLite();
            } catch (Throwable inner) {
                plugin.getLogger().severe("Auch SQLite fehlgeschlagen: " + inner.getMessage());
                return;
            }
        }
        run(this::createTables).join();
        plugin.getLogger().info("Datenbank verbunden: " + typeName);
    }

    private void setupMariaDB() throws Exception {
        String host = plugin.getConfig().getString("database.mariadb.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("database.mariadb.port", 3306);
        String db = plugin.getConfig().getString("database.mariadb.database", "bettersmp");
        String extra = plugin.getConfig().getString("database.mariadb.properties", "");
        this.url = "jdbc:mariadb://" + host + ":" + port + "/" + db
                + (extra == null || extra.isBlank() ? "" : "?" + extra);
        this.props = new Properties();
        props.setProperty("user", plugin.getConfig().getString("database.mariadb.user", "root"));
        props.setProperty("password", plugin.getConfig().getString("database.mariadb.password", ""));
        this.driver = (Driver) Class.forName("org.mariadb.jdbc.Driver")
                .getDeclaredConstructor().newInstance();
        this.sqlite = false;
        this.typeName = "MariaDB";
    }

    private void setupSQLite() throws Exception {
        plugin.getDataFolder().mkdirs();
        this.url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/data.db";
        this.props = new Properties();
        this.driver = (Driver) Class.forName("org.sqlite.JDBC")
                .getDeclaredConstructor().newInstance();
        this.sqlite = true;
        this.typeName = "SQLite";
        openAndVerify();
    }

    private void openAndVerify() throws SQLException {
        this.connection = driver.connect(url, props);
        if (connection == null) {
            throw new SQLException("Treiber akzeptierte die URL nicht: " + url);
        }
    }

    private Connection conn() throws SQLException {
        if (connection == null || !connection.isValid(2)) {
            connection = driver.connect(url, props);
        }
        return connection;
    }

    private void createTables() {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS bsmp_stats ("
                + "uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(32), "
                + "kills INT DEFAULT 0, deaths INT DEFAULT 0, mob_kills INT DEFAULT 0, "
                + "playtime BIGINT DEFAULT 0, joins INT DEFAULT 0, last_seen BIGINT DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS bsmp_bans ("
                + "uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(32), reason VARCHAR(64), "
                + "display VARCHAR(255), screen TEXT, expires BIGINT, actor VARCHAR(48), created BIGINT)",
            "CREATE TABLE IF NOT EXISTS bsmp_mutes ("
                + "uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(32), reason VARCHAR(64), "
                + "display VARCHAR(255), expires BIGINT, actor VARCHAR(48), created BIGINT)"
        };
        try (var st = conn().createStatement()) {
            for (String sql : ddl) {
                st.executeUpdate(sql);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Tabellen konnten nicht angelegt werden: " + e.getMessage());
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // egal beim Herunterfahren
        }
    }

    // ---------------- Ausfuehrungs-Helfer ----------------

    private CompletableFuture<Void> run(Runnable action) {
        return CompletableFuture.runAsync(action, executor);
    }

    private <T> CompletableFuture<T> supply(Supplier<T> action) {
        return CompletableFuture.supplyAsync(action, executor);
    }

    private void ensureStatsRow(String uuid, String name) throws SQLException {
        String sql = (sqlite ? "INSERT OR IGNORE" : "INSERT IGNORE")
                + " INTO bsmp_stats(uuid, name) VALUES(?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.executeUpdate();
        }
    }

    // ---------------- Stats ----------------

    public CompletableFuture<Void> recordJoin(UUID uuid, String name) {
        return run(() -> {
            try {
                ensureStatsRow(uuid.toString(), name);
                try (PreparedStatement ps = conn().prepareStatement(
                        "UPDATE bsmp_stats SET name=?, joins=joins+1, last_seen=? WHERE uuid=?")) {
                    ps.setString(1, name);
                    ps.setLong(2, System.currentTimeMillis());
                    ps.setString(3, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                warn("recordJoin", e);
            }
        });
    }

    public CompletableFuture<Void> bump(UUID uuid, String column, long delta) {
        return run(() -> {
            try {
                ensureStatsRow(uuid.toString(), null);
                try (PreparedStatement ps = conn().prepareStatement(
                        "UPDATE bsmp_stats SET " + column + "=" + column + "+? WHERE uuid=?")) {
                    ps.setLong(1, delta);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                warn("bump " + column, e);
            }
        });
    }

    public CompletableFuture<Void> setLastSeen(UUID uuid, long time) {
        return run(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "UPDATE bsmp_stats SET last_seen=? WHERE uuid=?")) {
                ps.setLong(1, time);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                warn("setLastSeen", e);
            }
        });
    }

    public CompletableFuture<StatSnapshot> getStats(UUID uuid) {
        return supply(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "SELECT * FROM bsmp_stats WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new StatSnapshot(uuid, rs.getString("name"),
                                rs.getInt("kills"), rs.getInt("deaths"), rs.getInt("mob_kills"),
                                rs.getLong("playtime"), rs.getInt("joins"), rs.getLong("last_seen"));
                    }
                }
            } catch (SQLException e) {
                warn("getStats", e);
            }
            return StatSnapshot.empty(uuid);
        });
    }

    // ---------------- Bans ----------------

    public CompletableFuture<Void> setBan(Punishment ban) {
        return run(() -> {
            try {
                delete("bsmp_bans", ban.uuid());
                try (PreparedStatement ps = conn().prepareStatement(
                        "INSERT INTO bsmp_bans(uuid,name,reason,display,screen,expires,actor,created)"
                                + " VALUES(?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, ban.uuid().toString());
                    ps.setString(2, ban.name());
                    ps.setString(3, ban.reason());
                    ps.setString(4, ban.display());
                    ps.setString(5, ban.screen());
                    ps.setLong(6, ban.expires());
                    ps.setString(7, ban.actor());
                    ps.setLong(8, ban.created());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                warn("setBan", e);
            }
        });
    }

    public CompletableFuture<Boolean> removeBan(UUID uuid) {
        return supply(() -> delete("bsmp_bans", uuid));
    }

    /** Blockierender Ban-Lookup - nur aus Async-Kontext (Login-Event) aufrufen. */
    public Punishment getBanBlocking(UUID uuid) {
        try {
            return supply(() -> readBan(uuid)).get();
        } catch (Exception e) {
            return null;
        }
    }

    public CompletableFuture<Punishment> getBan(UUID uuid) {
        return supply(() -> readBan(uuid));
    }

    private Punishment readBan(UUID uuid) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM bsmp_bans WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Punishment ban = new Punishment(uuid, rs.getString("name"), rs.getString("reason"),
                            rs.getString("display"), rs.getString("screen"), rs.getLong("expires"),
                            rs.getString("actor"), rs.getLong("created"));
                    if (ban.isExpired()) {
                        delete("bsmp_bans", uuid);
                        return null;
                    }
                    return ban;
                }
            }
        } catch (SQLException e) {
            warn("readBan", e);
        }
        return null;
    }

    // ---------------- Mutes ----------------

    public CompletableFuture<Void> setMute(Punishment mute) {
        return run(() -> {
            try {
                delete("bsmp_mutes", mute.uuid());
                try (PreparedStatement ps = conn().prepareStatement(
                        "INSERT INTO bsmp_mutes(uuid,name,reason,display,expires,actor,created)"
                                + " VALUES(?,?,?,?,?,?,?)")) {
                    ps.setString(1, mute.uuid().toString());
                    ps.setString(2, mute.name());
                    ps.setString(3, mute.reason());
                    ps.setString(4, mute.display());
                    ps.setLong(5, mute.expires());
                    ps.setString(6, mute.actor());
                    ps.setLong(7, mute.created());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                warn("setMute", e);
            }
        });
    }

    public CompletableFuture<Boolean> removeMute(UUID uuid) {
        return supply(() -> delete("bsmp_mutes", uuid));
    }

    public CompletableFuture<Punishment> getMute(UUID uuid) {
        return supply(() -> readMute(uuid));
    }

    private Punishment readMute(UUID uuid) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM bsmp_mutes WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Punishment mute = new Punishment(uuid, rs.getString("name"), rs.getString("reason"),
                            rs.getString("display"), null, rs.getLong("expires"),
                            rs.getString("actor"), rs.getLong("created"));
                    if (mute.isExpired()) {
                        delete("bsmp_mutes", uuid);
                        return null;
                    }
                    return mute;
                }
            }
        } catch (SQLException e) {
            warn("readMute", e);
        }
        return null;
    }

    // ---------------- intern ----------------

    private boolean delete(String table, UUID uuid) {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM " + table + " WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            warn("delete " + table, e);
            return false;
        }
    }

    private void warn(String where, SQLException e) {
        plugin.getLogger().warning("DB-Fehler (" + where + "): " + e.getMessage());
    }
}
