package de.lemonpvp.dbwipe;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

/**
 * Baut ein vollstaendiges SQL-Backup ALLER Tabellen der konfigurierten
 * Datenbank als .tar.gz (nicht nur Tabellen von DBWipe selbst - dieses
 * Plugin hat gar keine eigenen) und kann auf Wunsch danach alle
 * Tabellen loeschen.
 *
 * Eigene, direkte JDBC-Verbindung ohne Single-Thread-Executor wie bei
 * den anderen Plugins dieser Suite - dieser Vorgang laeuft ohnehin nur
 * einmal, von der Konsole ausgeloest, und WipeCommand ruft ihn schon
 * asynchron auf.
 */
final class DatabaseDumper {

    private final JavaPlugin plugin;

    DatabaseDumper(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private Connection verbinden() throws Exception {
        String host = plugin.getConfig().getString("database.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String db = plugin.getConfig().getString("database.database", "bettersmp");
        String extra = plugin.getConfig().getString("database.properties", "");
        String url = "jdbc:mariadb://" + host + ":" + port + "/" + db
                + (extra == null || extra.isBlank() ? "" : "?" + extra);
        Properties props = new Properties();
        props.setProperty("user", plugin.getConfig().getString("database.user", "root"));
        props.setProperty("password", plugin.getConfig().getString("database.password", ""));
        Driver driver = (Driver) Class.forName("org.mariadb.jdbc.Driver").getDeclaredConstructor().newInstance();
        Connection connection = driver.connect(url, props);
        if (connection == null) {
            throw new SQLException("Treiber akzeptierte die URL nicht: " + url);
        }
        return connection;
    }

    /**
     * @return Pfad des erstellten Backups.
     * Wirft eine Exception, wenn irgendetwas schiefgeht - der Aufrufer
     * (WipeCommand) darf dann unter GAR KEINEN Umstaenden mit dem
     * Loeschen fortfahren.
     */
    File sichern() throws Exception {
        try (Connection conn = verbinden()) {
            List<String> tabellen = tabellenListe(conn);
            File ordner = new File(plugin.getDataFolder(), "backups");
            if (!ordner.exists() && !ordner.mkdirs()) {
                throw new IOException("Backup-Ordner konnte nicht angelegt werden: " + ordner);
            }
            String zeitstempel = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            File ziel = new File(ordner, "db_backup_" + zeitstempel + ".tar.gz");

            try (FileOutputStream fos = new FileOutputStream(ziel);
                 GZIPOutputStream gzip = new GZIPOutputStream(fos);
                 TarWriter tar = new TarWriter(gzip)) {
                for (String tabelle : tabellen) {
                    byte[] dump = tabelleDumpen(conn, tabelle);
                    tar.schreibeDatei(tabelle + ".sql", dump);
                }
            }

            if (!ziel.exists() || ziel.length() == 0) {
                throw new IOException("Backup-Datei fehlt oder ist leer nach dem Schreiben: " + ziel);
            }
            return ziel;
        }
    }

    /** Loescht ALLE Tabellen der Datenbank. Nur aufrufen, nachdem sichern() ohne Fehler durchgelaufen ist. */
    int alleTabellenLoeschen() throws Exception {
        try (Connection conn = verbinden()) {
            List<String> tabellen = tabellenListe(conn);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("SET FOREIGN_KEY_CHECKS=0");
                for (String tabelle : tabellen) {
                    st.executeUpdate("DROP TABLE IF EXISTS `" + tabelle.replace("`", "``") + "`");
                }
                st.executeUpdate("SET FOREIGN_KEY_CHECKS=1");
            }
            return tabellen.size();
        }
    }

    private List<String> tabellenListe(Connection conn) throws SQLException {
        List<String> ergebnis = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                ergebnis.add(rs.getString("TABLE_NAME"));
            }
        }
        return ergebnis;
    }

    private byte[] tabelleDumpen(Connection conn, String tabelle) throws SQLException {
        String sicher = tabelle.replace("`", "``");
        StringBuilder sql = new StringBuilder();
        sql.append("-- Tabelle: ").append(tabelle).append("\n");
        sql.append("DROP TABLE IF EXISTS `").append(sicher).append("`;\n");

        try (Statement st = conn.createStatement();
             ResultSet ddl = st.executeQuery("SHOW CREATE TABLE `" + sicher + "`")) {
            if (ddl.next()) {
                sql.append(ddl.getString(2)).append(";\n\n");
            }
        }

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM `" + sicher + "`")) {
            ResultSetMetaData meta = rs.getMetaData();
            int spalten = meta.getColumnCount();
            while (rs.next()) {
                sql.append("INSERT INTO `").append(sicher).append("` VALUES (");
                for (int i = 1; i <= spalten; i++) {
                    if (i > 1) {
                        sql.append(", ");
                    }
                    sql.append(sqlWert(rs, meta, i));
                }
                sql.append(");\n");
            }
        }
        return sql.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String sqlWert(ResultSet rs, ResultSetMetaData meta, int spalte) throws SQLException {
        Object wert = rs.getObject(spalte);
        if (wert == null || rs.wasNull()) {
            return "NULL";
        }
        int typ = meta.getColumnType(spalte);
        return switch (typ) {
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT,
                 Types.FLOAT, Types.DOUBLE, Types.DECIMAL, Types.NUMERIC,
                 Types.BOOLEAN, Types.BIT -> String.valueOf(wert);
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> hexLiteral(rs.getBytes(spalte));
            default -> "'" + escape(String.valueOf(wert)) + "'";
        };
    }

    private String hexLiteral(byte[] bytes) {
        StringBuilder hex = new StringBuilder("X'");
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.append("'").toString();
    }

    private String escape(String wert) {
        return wert.replace("\\", "\\\\").replace("'", "''");
    }
}
