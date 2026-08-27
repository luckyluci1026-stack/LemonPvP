package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.Bukkit;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Keeps {@code plugins/LemonCore/servers.yml} identical across the whole
 * network with zero commands: every 30 seconds each server checks whether its
 * local file was edited (mtime) — if so it uploads the content to the shared
 * database — and whether a newer version exists remotely — if so it downloads
 * it, overwrites the local file and hot-reloads the in-memory config. Add a
 * server to the yml on ANY box, save, and within a minute the whole network
 * knows it.
 */
public class ServerRegistrySync {

    private static final long INTERVAL_TICKS = 20L * 30;

    private final LemonCore plugin;
    private long lastSeenMtime;
    private long appliedVersion;

    public ServerRegistrySync(LemonCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        File f = file();
        lastSeenMtime = f.exists() ? f.lastModified() : 0L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::tick, INTERVAL_TICKS, INTERVAL_TICKS);
    }

    private File file() {
        return new File(plugin.getDataFolder(), "servers.yml");
    }

    private void tick() {
        try {
            File f = file();
            // 1. Local edit? Upload it as the new authoritative version.
            if (f.exists() && f.lastModified() > lastSeenMtime) {
                lastSeenMtime = f.lastModified();
                String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                long version = System.currentTimeMillis();
                upload(content, version);
                appliedVersion = version;
                plugin.getLogger().info("[ServerSync] Uploaded edited servers.yml (v" + version + ").");
                return;
            }
            // 2. Newer version remotely? Pull + apply.
            try (var conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT content, version FROM lc_yml_sync WHERE file='servers.yml'")) {
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return;
                long version = rs.getLong("version");
                if (version <= appliedVersion) return;
                String content = rs.getString("content");
                Files.writeString(f.toPath(), content, StandardCharsets.UTF_8);
                lastSeenMtime = f.lastModified();
                appliedVersion = version;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.reloadServersConfig();
                    plugin.getLogger().info("[ServerSync] Applied servers.yml v" + version + " from the network.");
                });
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[ServerSync] Sync failed: " + e.getMessage());
        }
    }

    private void upload(String content, long version) throws Exception {
        try (var conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO lc_yml_sync (file, content, version) VALUES ('servers.yml', ?, ?) " +
                     "ON DUPLICATE KEY UPDATE content=VALUES(content), version=VALUES(version)")) {
            ps.setString(1, content);
            ps.setLong(2, version);
            ps.executeUpdate();
        }
    }
}
