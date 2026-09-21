package de.lemonpvp.bettersmp.backup;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * VOELLIG EIGENSTAENDIGE Datenbank-Verbindung, nur fuer die periodischen
 * Inventar-/Enderkisten-Sicherungen (siehe BackupManager) - bewusst
 * getrennt von der normalen Database-Klasse (Stats/Bans/Mutes): faellt
 * die Hauptdatenbank aus, wird beschaedigt oder verliert Daten, soll
 * das dieses Sicherheitsnetz nicht mitreissen. Eigener Treiber, eigene
 * Verbindung, eigener Single-Thread-Executor.
 *
 * Gleiches MariaDB-oder-SQLite-Verhalten wie die Hauptdatenbank: Standard
 * ist eine eigene lokale SQLite-Datei (backup.db, sofort einsatzbereit,
 * kein Setup), optional eine eigene MariaDB (backup.mariadb.*) - am
 * sinnvollsten eine ANDERE Datenbank/Instanz als database.mariadb, sonst
 * waere die "Trennung" nur auf Tabellenebene, nicht auf Verbindungsebene.
 */
public final class BackupDatabase {

    private final BetterSMP plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BetterSMP-Backup-DB");
        t.setDaemon(true);
        return t;
    });

    private Driver driver;
    private String url;
    private Properties props = new Properties();
    private boolean sqlite = true;
    private Connection connection;
    private volatile boolean bereit = false;

    public BackupDatabase(BetterSMP plugin) {
        this.plugin = plugin;
    }

    public boolean bereit() {
        return bereit;
    }

    public void init() {
        boolean mariadb = plugin.getConfig().getBoolean("backup.mariadb.enabled", false);
        try {
            if (mariadb) {
                setupMariaDB();
            } else {
                setupSQLite();
            }
            run(this::createTables).join();
            bereit = true;
            plugin.getLogger().info("Backup-Datenbank verbunden (" + (sqlite ? "SQLite" : "MariaDB") + ").");
        } catch (Throwable t) {
            bereit = false;
            plugin.getLogger().severe("Backup-Datenbank-Verbindung fehlgeschlagen (" + t.getMessage()
                    + ") - periodische Inventar-Sicherungen bleiben aus, bis das behoben ist.");
        }
    }

    private void setupMariaDB() throws Exception {
        String host = plugin.getConfig().getString("backup.mariadb.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("backup.mariadb.port", 3306);
        String db = plugin.getConfig().getString("backup.mariadb.database", "bettersmp_backup");
        String extra = plugin.getConfig().getString("backup.mariadb.properties", "");
        this.url = "jdbc:mariadb://" + host + ":" + port + "/" + db
                + (extra == null || extra.isBlank() ? "" : "?" + extra);
        this.props = new Properties();
        props.setProperty("user", plugin.getConfig().getString("backup.mariadb.user", "root"));
        props.setProperty("password", plugin.getConfig().getString("backup.mariadb.password", ""));
        this.driver = (Driver) Class.forName("org.mariadb.jdbc.Driver")
                .getDeclaredConstructor().newInstance();
        this.sqlite = false;
        openAndVerify();
    }

    private void setupSQLite() throws Exception {
        plugin.getDataFolder().mkdirs();
        this.url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/backup.db";
        this.props = new Properties();
        this.driver = (Driver) Class.forName("org.sqlite.JDBC")
                .getDeclaredConstructor().newInstance();
        this.sqlite = true;
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
        String sql = "CREATE TABLE IF NOT EXISTS bsmp_backup_inventar ("
                + "uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(32), "
                + "inventar LONGTEXT, ruestung LONGTEXT, offhand LONGTEXT, enderkiste LONGTEXT, "
                + "gespeichert BIGINT)";
        try (var st = conn().createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Backup-Tabelle konnte nicht angelegt werden: " + e.getMessage());
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

    private void warn(String where, Exception e) {
        plugin.getLogger().warning("Backup-DB-Fehler (" + where + "): " + e.getMessage());
    }

    // ---------------- Sichern / Lesen ----------------

    public CompletableFuture<Void> sichern(UUID uuid, String name, ItemStack[] inventar,
                                            ItemStack[] ruestung, ItemStack offhand, ItemStack[] enderkiste) {
        return run(() -> {
            try {
                String inventarDaten = BackupCodec.kodiereArray(inventar);
                String ruestungDaten = BackupCodec.kodiereArray(ruestung);
                String offhandDaten = BackupCodec.kodiereEinzeln(offhand);
                String enderkisteDaten = BackupCodec.kodiereArray(enderkiste);
                String sql = (sqlite
                        ? "INSERT OR REPLACE INTO bsmp_backup_inventar"
                        : "REPLACE INTO bsmp_backup_inventar")
                        + "(uuid,name,inventar,ruestung,offhand,enderkiste,gespeichert) VALUES(?,?,?,?,?,?,?)";
                try (PreparedStatement ps = conn().prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, name);
                    ps.setString(3, inventarDaten);
                    ps.setString(4, ruestungDaten);
                    ps.setString(5, offhandDaten);
                    ps.setString(6, enderkisteDaten);
                    ps.setLong(7, System.currentTimeMillis());
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                warn("sichern", e);
            }
        });
    }

    public CompletableFuture<Optional<BackupSnapshot>> lesen(UUID uuid) {
        return supply(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "SELECT * FROM bsmp_backup_inventar WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new BackupSnapshot(
                            uuid, rs.getString("name"),
                            BackupCodec.dekodiereArray(rs.getString("inventar")),
                            BackupCodec.dekodiereArray(rs.getString("ruestung")),
                            BackupCodec.dekodiereEinzeln(rs.getString("offhand")),
                            BackupCodec.dekodiereArray(rs.getString("enderkiste")),
                            rs.getLong("gespeichert")));
                }
            } catch (Exception e) {
                warn("lesen", e);
                return Optional.empty();
            }
        });
    }
}
