package com.lemonpvp.lemoncore.lemonlang.runtime;

import com.lemonpvp.lemoncore.LemonCore;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Database operations for LemonLang scripts.
 * Uses the LemonCore HikariCP DataSource via {@code getDatabaseManager().getDataSource()}.
 * Table: lemonlang_data (key_col VARCHAR, field VARCHAR, value TEXT, PRIMARY KEY(key_col, field))
 */
public final class LemonLangDB {

    private static final Logger LOG = Logger.getLogger("LemonLang");
    private static final String TABLE = "lemonlang_data";

    private LemonLangDB() {}

    /** Ensure the table exists. Called once at startup. */
    public static void ensureTable(LemonCore plugin) {
        HikariDataSource ds = getDataSource(plugin);
        if (ds == null) {
            LOG.warning("[LemonLang] Keine Datenbank verfügbar für DB-Operationen.");
            return;
        }
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "  key_col VARCHAR(128) NOT NULL," +
                "  field   VARCHAR(128) NOT NULL," +
                "  value   TEXT," +
                "  PRIMARY KEY (key_col, field)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            LOG.info("[LemonLang] Tabelle '" + TABLE + "' bereit.");
        } catch (SQLException e) {
            LOG.warning("[LemonLang] Fehler beim Erstellen der DB-Tabelle: " + e.getMessage());
        }
    }

    /** Async write. Returns a future that completes when done. */
    public static CompletableFuture<Void> writeAsync(LemonCore plugin, String key, String field, String value) {
        return CompletableFuture.runAsync(() -> writeSync(plugin, key, field, value));
    }

    /** Sync write. Logs a warning if called on the main thread. */
    public static void writeSync(LemonCore plugin, String key, String field, String value) {
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            LOG.warning("[LemonLang] db write sync auf dem Main-Thread! Besser: db write (async).");
        }
        HikariDataSource ds = getDataSource(plugin);
        if (ds == null) return;
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO " + TABLE + " (key_col, field, value) VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE value=VALUES(value)"
             )) {
            ps.setString(1, key);
            ps.setString(2, field);
            ps.setString(3, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("[LemonLang] DB-Schreibfehler: " + e.getMessage());
        }
    }

    /** Async read. Returns {@code Optional<String>} in the future. */
    public static CompletableFuture<Optional<String>> readAsync(LemonCore plugin, String key, String field) {
        return CompletableFuture.supplyAsync(() -> readSync(plugin, key, field));
    }

    /** Sync read. */
    public static Optional<String> readSync(LemonCore plugin, String key, String field) {
        HikariDataSource ds = getDataSource(plugin);
        if (ds == null) return Optional.empty();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT value FROM " + TABLE + " WHERE key_col=? AND field=?"
             )) {
            ps.setString(1, key);
            ps.setString(2, field);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.ofNullable(rs.getString("value"));
            }
        } catch (SQLException e) {
            LOG.warning("[LemonLang] DB-Lesefehler: " + e.getMessage());
        }
        return Optional.empty();
    }

    /** Async delete. Returns a future that completes when done. */
    public static CompletableFuture<Void> deleteAsync(LemonCore plugin, String key, String field) {
        return CompletableFuture.runAsync(() -> deleteSync(plugin, key, field));
    }

    /** Sync delete. */
    public static void deleteSync(LemonCore plugin, String key, String field) {
        HikariDataSource ds = getDataSource(plugin);
        if (ds == null) return;
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM " + TABLE + " WHERE key_col=? AND field=?"
             )) {
            ps.setString(1, key);
            ps.setString(2, field);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("[LemonLang] DB-Löschfehler: " + e.getMessage());
        }
    }

    private static HikariDataSource getDataSource(LemonCore plugin) {
        if (plugin == null || plugin.getDatabaseManager() == null) return null;
        return plugin.getDatabaseManager().getDataSource();
    }
}
