package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MaintenanceManager {

    private final LemonCore plugin;
    private volatile boolean maintenanceEnabled;
    /** Currently selected reason key; null → the configured default-reason. */
    private volatile String reasonKey;
    private final Set<UUID> whitelist = ConcurrentHashMap.newKeySet();

    public MaintenanceManager(LemonCore plugin) {
        this.plugin = plugin;
    }

    // ── Reasons (config-driven) ────────────────────────────────────────────────

    public void setReasonKey(String key) { this.reasonKey = key; }

    public String getReasonKey() {
        return reasonKey != null ? reasonKey
                : plugin.getConfig().getString("maintenance.default-reason", "general");
    }

    /** Available reason keys defined in config under maintenance.reasons. */
    public java.util.List<String> reasonKeys() {
        var sec = plugin.getConfig().getConfigurationSection("maintenance.reasons");
        return sec == null ? java.util.List.of() : new java.util.ArrayList<>(sec.getKeys(false));
    }

    /** True if the given key exists in config. */
    public boolean hasReason(String key) {
        return plugin.getConfig().getString("maintenance.reasons." + key) != null;
    }

    /**
     * Resolves the current reason to a raw MiniMessage string with {discord}
     * substituted. Callers parse it (e.g. TextUtil.parse) into the kick screen.
     */
    public String resolveReasonMessage() {
        String key = getReasonKey();
        String msg = plugin.getConfig().getString("maintenance.reasons." + key);
        if (msg == null) {
            String def = plugin.getConfig().getString("maintenance.default-reason", "general");
            msg = plugin.getConfig().getString("maintenance.reasons." + def);
        }
        if (msg == null) msg = "<red>The server is currently under maintenance.</red>";
        String discord = plugin.getConfig().getString("discord.invite", "https://discord.gg/lemonpvp");
        return msg.replace("{discord}", discord).replace("\\n", "\n");
    }

    public void load() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO lemoncore_maintenance (id, maintenance_on) VALUES (1, FALSE)")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT maintenance_on FROM lemoncore_maintenance WHERE id = 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    maintenanceEnabled = rs.getBoolean("maintenance_on");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT uuid FROM lemoncore_maintenance_whitelist");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        whitelist.add(UUID.fromString(rs.getString("uuid")));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[MaintenanceManager] Failed to load state: " + e.getMessage());
        }
        plugin.getLogger().info("[MaintenanceManager] maintenance=" + maintenanceEnabled
                + ", whitelist=" + whitelist.size() + " entries");
    }

    public boolean isEnabled() {
        return maintenanceEnabled;
    }

    public boolean isWhitelisted(UUID uuid) {
        return whitelist.contains(uuid);
    }

    public CompletableFuture<Void> setEnabled(boolean enabled) {
        maintenanceEnabled = enabled;
        return plugin.getDatabaseManager().executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE lemoncore_maintenance SET maintenance_on = ? WHERE id = 1")) {
                ps.setBoolean(1, enabled);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> addToWhitelist(UUID uuid, String playerName) {
        whitelist.add(uuid);
        return plugin.getDatabaseManager().executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lemoncore_maintenance_whitelist (uuid, player_name, added_at) " +
                    "VALUES (?, ?, NOW()) ON DUPLICATE KEY UPDATE player_name = VALUES(player_name)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, playerName);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * @return CompletableFuture<Boolean> — true if added, false if removed
     */
    public CompletableFuture<Boolean> toggleWhitelist(UUID uuid, String playerName) {
        boolean adding = !whitelist.contains(uuid);
        if (adding) {
            whitelist.add(uuid);
            return plugin.getDatabaseManager().executeAsync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO lemoncore_maintenance_whitelist (uuid, player_name, added_at) " +
                        "VALUES (?, ?, NOW()) ON DUPLICATE KEY UPDATE player_name = VALUES(player_name)")) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, playerName);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }).thenApply(v -> true);
        } else {
            whitelist.remove(uuid);
            return plugin.getDatabaseManager().executeAsync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM lemoncore_maintenance_whitelist WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }).thenApply(v -> false);
        }
    }
}
