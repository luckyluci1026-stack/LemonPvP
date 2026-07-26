package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Records delivered chat messages so staff can review what a player said with
 * {@code /gchathistory}. Only messages that actually go through (not muted,
 * not filtered) are stored. Rows are pruned after a configurable retention
 * window so the table can't grow without bound.
 */
public class ChatLogManager {

    private final LemonCore plugin;
    private final DatabaseManager db;

    public ChatLogManager(LemonCore plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    /** Whether chat logging is on (config {@code chat.log-enabled}, default true). */
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("chat.log-enabled", true);
    }

    /** Fire-and-forget insert of one delivered message. No-op when logging is disabled. */
    public void log(UUID uuid, String username, String message) {
        if (!isEnabled() || message == null || message.isBlank()) return;
        String server = plugin.getConfig().getString("server-name", "unknown");
        // Bound the stored length to the column width (message VARCHAR(512)).
        String stored = message.length() > 512 ? message.substring(0, 512) : message;
        long ts = System.currentTimeMillis();
        db.executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lc_chat_log (uuid, username, message, server, ts) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, username);
                ps.setString(3, stored);
                ps.setString(4, server);
                ps.setLong(5, ts);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("[ChatLog] Failed to store message: " + e.getMessage());
            }
        });
    }

    /** Most recent messages for a player, newest first, capped at {@code limit}. */
    public CompletableFuture<List<Entry>> getRecent(UUID uuid, int limit) {
        return db.queryAsync(conn -> {
            List<Entry> out = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT message, server, ts FROM lc_chat_log WHERE uuid = ? ORDER BY ts DESC LIMIT ?")) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new Entry(rs.getString("message"), rs.getString("server"), rs.getLong("ts")));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[ChatLog] Failed to read history: " + e.getMessage());
            }
            return out;
        });
    }

    /**
     * Starts a periodic prune (every 6h, first run after 2 min) deleting rows
     * older than {@code chat.log-retention-days} (default 14).
     */
    public void startPruneTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::prune,
                20L * 120, 20L * 60L * 60L * 6L);
    }

    private void prune() {
        int days = plugin.getConfig().getInt("chat.log-retention-days", 14);
        if (days <= 0) return; // 0 or negative = keep forever
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        db.executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lc_chat_log WHERE ts < ?")) {
                ps.setLong(1, cutoff);
                int removed = ps.executeUpdate();
                if (removed > 0) {
                    plugin.getLogger().info("[ChatLog] Pruned " + removed + " message(s) older than "
                            + days + " days.");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[ChatLog] Prune failed: " + e.getMessage());
            }
        });
    }

    /** One stored chat line. */
    public record Entry(String message, String server, long ts) {}
}
