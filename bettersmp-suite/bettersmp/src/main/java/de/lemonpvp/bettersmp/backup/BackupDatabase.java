package de.lemonpvp.bettersmp.backup;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    private final Map<String, Long> letzteWarnung = new ConcurrentHashMap<>();
    private volatile long letzterNeuaufbau;
    private boolean nochmal;
    private volatile boolean bereit = false;
    private final Map<UUID, byte[]> letzterStand = new HashMap<>();
    private final Map<UUID, Long> geschrieben = new HashMap<>();
    private final Map<UUID, Long> bestaetigt = new HashMap<>();

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
        } catch (Throwable t) {
            plugin.getLogger().warning("Backup-MariaDB-Verbindung fehlgeschlagen (" + t.getMessage()
                    + ") - nutze stattdessen die lokale Backup-SQLite-Datei, damit dieses "
                    + "Sicherheitsnetz nicht durch genau die Art von Datenbank-Problem ausfaellt, vor der es schuetzen soll.");
            try {
                setupSQLite();
            } catch (Throwable inner) {
                bereit = false;
                plugin.getLogger().severe("Auch die lokale Backup-SQLite-Datei fehlgeschlagen (" + inner.getMessage()
                        + ") - periodische Inventar-Sicherungen bleiben aus, bis das behoben ist.");
                return;
            }
        }
        run(this::createTables).join();
        bereit = true;
        plugin.getLogger().info("Backup-Datenbank verbunden (" + (sqlite ? "SQLite" : "MariaDB") + ").");
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
        einstellen(connection);
    }

    private Connection conn() throws SQLException {
        if (connection == null || (sqlite ? connection.isClosed() : !connection.isValid(2))) {
            connection = driver.connect(url, props);
            einstellen(connection);
        }
        return connection;
    }

    private void einstellen(Connection verbindung) {
        if (!sqlite || verbindung == null) {
            return;
        }
        try (var st = verbindung.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            st.execute("PRAGMA busy_timeout=5000");
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite-Einstellungen konnten nicht gesetzt werden: " + e.getMessage());
        }
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
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Backup-Datenbank-Aufgaben nach 10 Sekunden nicht fertig - Verbindung wird trotzdem geschlossen.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
        return CompletableFuture.runAsync(() -> {
            nochmal = false;
            action.run();
            if (nochmal) {
                nochmal = false;
                action.run();
            }
        }, executor);
    }

    private <T> CompletableFuture<T> supply(Supplier<T> action) {
        return CompletableFuture.supplyAsync(() -> {
            nochmal = false;
            T wert = action.get();
            if (nochmal) {
                nochmal = false;
                wert = action.get();
            }
            return wert;
        }, executor);
    }

    private void warn(String where, Exception e) {
        melden("Backup-DB-Fehler (" + where + ")", e);
    }

    private void melden(String text, Exception e) {
        if (tabelleFehlt(e)) {
            tabellenNeuAnlegen();
            return;
        }
        long jetzt = System.currentTimeMillis();
        Long zuletzt = letzteWarnung.get(text);
        if (zuletzt != null && jetzt - zuletzt < 60_000L) {
            return;
        }
        letzteWarnung.put(text, jetzt);
        plugin.getLogger().warning(text + ": " + e.getMessage());
    }

    static boolean tabelleFehlt(Throwable fehler) {
        for (Throwable t = fehler; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && (sql.getErrorCode() == 1146 || "42S02".equals(sql.getSQLState())
                    || (sql.getMessage() != null && sql.getMessage().contains("no such table")))) {
                return true;
            }
        }
        return false;
    }

    private void tabellenNeuAnlegen() {
        long jetzt = System.currentTimeMillis();
        if (jetzt - letzterNeuaufbau < 10_000L) {
            return;
        }
        letzterNeuaufbau = jetzt;
        createTables();
        letzterStand.clear();
        geschrieben.clear();
        nochmal = true;
        plugin.getLogger().warning("Backup-Tabelle fehlte (z. B. nach /dbwipe) - ist neu angelegt.");
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
                byte[] stand = pruefsumme(name, inventarDaten, ruestungDaten, offhandDaten, enderkisteDaten);
                long jetzt = System.currentTimeMillis();
                Long zuletzt = geschrieben.get(uuid);
                if (Arrays.equals(stand, letzterStand.get(uuid)) && zuletzt != null
                        && jetzt - zuletzt < TimeUnit.MINUTES.toMillis(10)) {
                    bestaetigt.put(uuid, jetzt);
                    return;
                }
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
                    ps.setLong(7, jetzt);
                    ps.executeUpdate();
                }
                letzterStand.put(uuid, stand);
                geschrieben.put(uuid, jetzt);
                bestaetigt.put(uuid, jetzt);
            } catch (Exception e) {
                letzterStand.remove(uuid);
                warn("sichern", e);
            }
        });
    }

    private static byte[] pruefsumme(String... teile) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (String teil : teile) {
            byte[] bytes = teil == null ? new byte[0] : teil.getBytes(StandardCharsets.UTF_8);
            digest.update((byte) (teil == null ? 0 : 1));
            digest.update(new byte[]{(byte) (bytes.length >>> 24), (byte) (bytes.length >>> 16),
                    (byte) (bytes.length >>> 8), (byte) bytes.length});
            digest.update(bytes);
        }
        return digest.digest();
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
                            Math.max(rs.getLong("gespeichert"), bestaetigt.getOrDefault(uuid, 0L))));
                }
            } catch (Exception e) {
                warn("lesen", e);
                return Optional.empty();
            }
        });
    }
}
