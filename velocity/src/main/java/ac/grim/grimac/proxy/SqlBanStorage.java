/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.proxy;

import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Bans in MySQL/MariaDB, in a table this plugin owns outright.
 *
 * <h2>Why its own table</h2>
 *
 * <p>The anticheat's own storage lives behind an abstraction whose table shape
 * is defined inside a library, not here. Reading it from the proxy would mean
 * depending on column names nobody wrote down — fine until the day they change
 * and bans silently stop being read. {@code bucksmpac_bans} is six columns
 * defined in this file and nowhere else.</p>
 *
 * <h2>Why the connection is opened per operation</h2>
 *
 * <p>Writes happen when somebody is banned or unbanned — a handful a day on a
 * busy network — and reads happen once at startup. A pool would be more
 * moving parts than the workload justifies, and a pooled connection left idle
 * for hours has its own failure mode. Logins never touch this class: they are
 * answered from the in-memory index.</p>
 */
public final class SqlBanStorage implements BanStorage {

    private static final String TABLE = "bucksmpac_bans";

    private final String url;
    private final String user;
    private final String password;
    private final Logger logger;

    public SqlBanStorage(String host, int port, String database, String user, String password, Logger logger) {
        this.url = "jdbc:mariadb://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true";
        this.user = user;
        this.password = password;
        this.logger = logger;
    }

    /** @throws SQLException when the database cannot be reached or the table cannot be made. */
    public void connectAndPrepare() throws SQLException {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS %s (
                      uuid       VARCHAR(36)  NOT NULL PRIMARY KEY,
                      name       VARCHAR(32),
                      created_at BIGINT       NOT NULL,
                      expires_at BIGINT       NOT NULL,
                      actor      VARCHAR(32),
                      reason     VARCHAR(512),
                      INDEX idx_name (name)
                    )""".formatted(TABLE));
        }
    }

    @Override
    public Collection<BanRecord> loadAll() {
        List<BanRecord> out = new ArrayList<>();
        String sql = "SELECT uuid, name, created_at, expires_at, actor, reason FROM " + TABLE;

        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {

            while (rows.next()) {
                try {
                    out.add(new BanRecord(
                            UUID.fromString(rows.getString("uuid")),
                            rows.getString("name"),
                            rows.getLong("created_at"),
                            rows.getLong("expires_at"),
                            rows.getString("actor"),
                            rows.getString("reason")));
                } catch (IllegalArgumentException e) {
                    // A single unreadable row must not cost us the whole list.
                    logger.warn("Skipping a ban row with an unusable uuid.");
                }
            }
        } catch (SQLException e) {
            logger.error("Could not read {} — the proxy starts with whatever it already had in memory.", TABLE, e);
        }
        return out;
    }

    @Override
    public void save(BanRecord record) {
        String sql = "REPLACE INTO " + TABLE
                + " (uuid, name, created_at, expires_at, actor, reason) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, record.uuid().toString());
            statement.setString(2, trim(record.name(), 32));
            statement.setLong(3, record.whenEpochMs());
            statement.setLong(4, record.expiresEpochMs());
            statement.setString(5, trim(record.actor(), 32));
            statement.setString(6, trim(record.reason(), 512));
            statement.executeUpdate();

        } catch (SQLException e) {
            logger.error("Could not store the ban for {} — it is active now but will be gone on restart.",
                    record.name(), e);
        }
    }

    @Override
    public void delete(UUID uuid) {
        if (uuid == null) return;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM " + TABLE + " WHERE uuid = ?")) {

            statement.setString(1, uuid.toString());
            statement.executeUpdate();

        } catch (SQLException e) {
            logger.error("Could not delete the ban for {} — it may come back on restart.", uuid, e);
        }
    }

    @Override
    public String describe() {
        // Never the password, and never the whole URL: it carries credentials
        // in some setups and this line goes into a log people paste around.
        int slash = url.lastIndexOf('/');
        int query = url.indexOf('?');
        String database = slash < 0 ? "?" : url.substring(slash + 1, query < 0 ? url.length() : query);
        return "MySQL/MariaDB, table " + TABLE + " in " + database;
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private static String trim(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
