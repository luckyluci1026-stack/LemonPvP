package com.lemonpvp.lemonresourcepack.maintenance;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MaintenanceDatabase {

    private final HikariDataSource dataSource;
    private final Logger logger;

    public MaintenanceDatabase(Map<String, Object> dbConfig, Logger logger) {
        this.logger = logger;
        String host = getString(dbConfig, "host", "localhost");
        int port = getInt(dbConfig, "port", 3306);
        String database = getString(dbConfig, "database", "lemonpvp");
        String username = getString(dbConfig, "username", "root");
        String password = getString(dbConfig, "password", "password");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setPoolName("LemonProxy-Maintenance");
        config.addDataSourceProperty("cachePrepStmts", "true");
        dataSource = new HikariDataSource(config);
    }

    public boolean loadMaintenanceState() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT maintenance_on FROM lemoncore_maintenance WHERE id = 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBoolean("maintenance_on");
        } catch (SQLException e) {
            logger.error("Failed to load maintenance state: {}", e.getMessage());
        }
        return false;
    }

    public Set<UUID> loadWhitelist() {
        Set<UUID> result = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT uuid FROM lemoncore_maintenance_whitelist");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    result.add(UUID.fromString(rs.getString("uuid")));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {
            logger.error("Failed to load maintenance whitelist: {}", e.getMessage());
        }
        return result;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private static String getString(Map<String, Object> map, String key, String def) {
        Object v = map.get(key);
        return v instanceof String s ? s : def;
    }

    private static int getInt(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        if (v instanceof Integer i) return i;
        if (v instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return def;
    }
}
