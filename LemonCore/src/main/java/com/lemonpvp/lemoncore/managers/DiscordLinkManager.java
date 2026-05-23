package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.database.DatabaseManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class DiscordLinkManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    // No O/0/1/I to avoid confusion
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final LemonCore plugin;
    private final DatabaseManager db;
    private final Map<UUID, String> pendingCodes = new ConcurrentHashMap<>();
    private int pollTaskId = -1;
    private int cleanupTaskId = -1;

    public DiscordLinkManager(LemonCore plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    public void start() {
        // Poll every 3 seconds (60 ticks) to detect when bot completes a link
        pollTaskId = Bukkit.getScheduler()
                .runTaskTimerAsynchronously(plugin, this::pollPendingLinks, 60L, 60L)
                .getTaskId();
        // Purge expired codes from DB every minute
        cleanupTaskId = Bukkit.getScheduler()
                .runTaskTimerAsynchronously(plugin, this::cleanupExpiredCodes, 1200L, 1200L)
                .getTaskId();
    }

    public void stop() {
        if (pollTaskId != -1) Bukkit.getScheduler().cancelTask(pollTaskId);
        if (cleanupTaskId != -1) Bukkit.getScheduler().cancelTask(cleanupTaskId);
    }

    // -------------------------------------------------------------------------
    // Code generation
    // -------------------------------------------------------------------------

    public CompletableFuture<String> generateCode(UUID uuid, String playerName) {
        return db.queryAsync(conn -> {
            try {
                // Remove any existing pending code for this player
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM lc_verify_codes WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                }
                String code = randomCode();
                Timestamp expires = new Timestamp(System.currentTimeMillis() + 10 * 60 * 1000L);
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO lc_verify_codes (uuid, code, player_name, expires_at) VALUES (?,?,?,?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, code);
                    ps.setString(3, playerName);
                    ps.setTimestamp(4, expires);
                    ps.executeUpdate();
                }
                pendingCodes.put(uuid, code);
                return code;
            } catch (SQLException e) {
                plugin.getLogger().severe("Discord link code error: " + e.getMessage());
                return null;
            }
        });
    }

    // -------------------------------------------------------------------------
    // Link lookup
    // -------------------------------------------------------------------------

    public CompletableFuture<DiscordLink> getLinkedAccount(UUID uuid) {
        return db.queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT discord_id, discord_username, linked_at FROM lc_discord_links WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
                return new DiscordLink(uuid, rs.getString("discord_id"),
                        rs.getString("discord_username"), rs.getTimestamp("linked_at"));
            } catch (SQLException e) {
                plugin.getLogger().severe("Discord link lookup error: " + e.getMessage());
                return null;
            }
        });
    }

    public CompletableFuture<String> getDiscordUsername(UUID uuid) {
        return getLinkedAccount(uuid).thenApply(link -> link != null ? link.discordUsername : null);
    }

    // -------------------------------------------------------------------------
    // Unlink
    // -------------------------------------------------------------------------

    public CompletableFuture<Boolean> unlinkAccount(UUID uuid) {
        pendingCodes.remove(uuid);
        return db.queryAsync(conn -> {
            try {
                // Also remove any pending code
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM lc_verify_codes WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM lc_discord_links WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Discord unlink error: " + e.getMessage());
                return false;
            }
        });
    }

    // -------------------------------------------------------------------------
    // Polling — called off the main thread
    // -------------------------------------------------------------------------

    private void pollPendingLinks() {
        if (pendingCodes.isEmpty()) return;
        for (Map.Entry<UUID, String> entry : new HashMap<>(pendingCodes).entrySet()) {
            UUID uuid = entry.getKey();
            String code = entry.getValue();
            checkIfLinkCompleted(uuid, code);
        }
    }

    private void checkIfLinkCompleted(UUID uuid, String code) {
        db.queryAsync(conn -> {
            try {
                // Check if code still exists in DB
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT expires_at FROM lc_verify_codes WHERE uuid=? AND code=?")) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, code);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        // Code present — remove from pending if it has expired
                        if (rs.getTimestamp("expires_at").before(
                                new Timestamp(System.currentTimeMillis()))) {
                            pendingCodes.remove(uuid);
                        }
                        return null; // still waiting for bot
                    }
                }
                // Code gone — check if link was created by bot
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT discord_username FROM lc_discord_links WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    pendingCodes.remove(uuid);
                    if (rs.next()) {
                        String discordUsername = rs.getString("discord_username");
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null && player.isOnline()) {
                                player.sendMessage(MM.deserialize(
                                        "<green>Your Discord account <white>"
                                        + discordUsername
                                        + "</white> has been linked!</green>"));
                            }
                        });
                    }
                }
                return null;
            } catch (SQLException e) {
                plugin.getLogger().severe("Discord poll error: " + e.getMessage());
                return null;
            }
        });
    }

    private void cleanupExpiredCodes() {
        db.executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM lc_verify_codes WHERE expires_at < NOW()")) {
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Discord cleanup error: " + e.getMessage());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String randomCode() {
        Random rand = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(CODE_CHARS.charAt(rand.nextInt(CODE_CHARS.length())));
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Model
    // -------------------------------------------------------------------------

    public static class DiscordLink {
        public final UUID uuid;
        public final String discordId;
        public final String discordUsername;
        public final Timestamp linkedAt;

        public DiscordLink(UUID uuid, String discordId, String discordUsername, Timestamp linkedAt) {
            this.uuid = uuid;
            this.discordId = discordId;
            this.discordUsername = discordUsername;
            this.linkedAt = linkedAt;
        }
    }
}
