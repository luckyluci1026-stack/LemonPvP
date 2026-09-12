package de.lemonpvp.punishplus.store;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Geteilte Sperrliste per MariaDB - eine Tabelle, von allen Servern
 * gemeinsam genutzt: /offend bzw. /punish auf einem Server wirkt sofort
 * ueberall, weil jeder Server beim Login dieselbe Tabelle abfragt.
 *
 * Gleiches Verbindungsmuster wie BetterSMPs Database-Klasse: reines
 * JDBC ueber einen Single-Thread-Executor (eine Verbindung, nie
 * gleichzeitig von zwei Threads benutzt), Treiber per Class.forName
 * statt als Maven-Abhaengigkeit - Paper stellt ihn zur Laufzeit selbst
 * bereit (siehe bettersmp/storage/Database.java).
 *
 * Schlaegt die Verbindung fehl, laesst aktiv() lieber jeden rein
 * (Optional.empty statt eine Ausnahme) als das ganze Netzwerk
 * auszusperren - genau wie BetterSMPs eigener Ban-Check bei einem
 * Datenbankfehler.
 */
public final class MariaDbPunishRepository implements PunishRepository {

    private static final String TABELLE = "punishplus_gesperrt";

    private final JavaPlugin plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PunishPlus-DB");
        t.setDaemon(true);
        return t;
    });

    private Driver driver;
    private String url;
    private Properties props;
    private Connection connection;

    public MariaDbPunishRepository(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        try {
            String host = plugin.getConfig().getString("database.host", "127.0.0.1");
            int port = plugin.getConfig().getInt("database.port", 3306);
            String db = plugin.getConfig().getString("database.database", "bettersmp");
            String extra = plugin.getConfig().getString("database.properties", "");
            this.url = "jdbc:mariadb://" + host + ":" + port + "/" + db
                    + (extra == null || extra.isBlank() ? "" : "?" + extra);
            this.props = new Properties();
            props.setProperty("user", plugin.getConfig().getString("database.user", "root"));
            props.setProperty("password", plugin.getConfig().getString("database.password", ""));
            this.driver = (Driver) Class.forName("org.mariadb.jdbc.Driver")
                    .getDeclaredConstructor().newInstance();
            this.connection = driver.connect(url, props);
            if (connection == null) {
                throw new SQLException("Treiber akzeptierte die URL nicht: " + url);
            }
            try (var st = connection.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABELLE + " ("
                        + "uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(32), grund_id VARCHAR(64), "
                        + "grund_text VARCHAR(255), art VARCHAR(16), von BIGINT, bis BIGINT, "
                        + "ausfuehrer VARCHAR(48))");
            }
            plugin.getLogger().info("PunishPlus: mit MariaDB verbunden - Sperren gelten netzwerkweit.");
        } catch (Exception e) {
            driver = null;
            plugin.getLogger().severe("PunishPlus: MariaDB-Verbindung fehlgeschlagen (" + e.getMessage()
                    + ") - database.enabled steht an, aber es geht nichts. /offend und /punish wirken"
                    + " auf diesem Server dadurch gar nicht, bis die Verbindung klappt.");
        }
    }

    private Connection conn() throws SQLException {
        if (connection == null || !connection.isValid(2)) {
            connection = driver.connect(url, props);
        }
        return connection;
    }

    @Override
    public void speichern(PunishRecord record) {
        if (driver == null) {
            return;
        }
        executor.submit(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "REPLACE INTO " + TABELLE + "(uuid,name,grund_id,grund_text,art,von,bis,ausfuehrer)"
                            + " VALUES(?,?,?,?,?,?,?,?)")) {
                ps.setString(1, record.spieler().toString());
                ps.setString(2, record.spielerName());
                ps.setString(3, record.grundId());
                ps.setString(4, record.grundText());
                ps.setString(5, record.art());
                ps.setLong(6, record.von());
                ps.setLong(7, record.bis());
                ps.setString(8, record.ausfuehrer());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("PunishPlus: Sperre konnte nicht gespeichert werden: " + e.getMessage());
            }
        });
    }

    /** Blockierend - siehe Interface-Doku, nur aus bereits-asynchronem Kontext aufrufen. */
    @Override
    public Optional<PunishRecord> aktiv(UUID spieler) {
        if (driver == null) {
            return Optional.empty();
        }
        try {
            return executor.submit(() -> aktivAufDbThread(spieler)).get();
        } catch (Exception e) {
            plugin.getLogger().warning("PunishPlus: Sperre konnte nicht gelesen werden: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Laeuft bereits auf dem einzigen DB-Thread - hier ist eine normale (nicht nochmal
     *  ueber den Executor laufende) Verbindungsnutzung sicher. */
    private Optional<PunishRecord> aktivAufDbThread(UUID spieler) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM " + TABELLE + " WHERE uuid=?")) {
            ps.setString(1, spieler.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                PunishRecord record = new PunishRecord(
                        spieler, rs.getString("name"), rs.getString("grund_id"), rs.getString("grund_text"),
                        rs.getString("art"), rs.getLong("von"), rs.getLong("bis"), rs.getString("ausfuehrer"));
                if (!record.aktiv()) {
                    try (PreparedStatement del = conn().prepareStatement(
                            "DELETE FROM " + TABELLE + " WHERE uuid=?")) {
                        del.setString(1, spieler.toString());
                        del.executeUpdate();
                    }
                    return Optional.empty();
                }
                return Optional.of(record);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("PunishPlus: Sperre konnte nicht gelesen werden: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
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
}
