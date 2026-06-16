package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.database.DatabaseManager;
import com.lemonpvp.lemoncore.util.TextUtil;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final LemonCore plugin;
    private final DatabaseManager db;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(LemonCore plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    public PlayerData getCached(UUID uuid) {
        return cache.get(uuid);
    }

    public void setCached(UUID uuid, PlayerData data) {
        cache.put(uuid, data);
    }

    public void removeCached(UUID uuid) {
        cache.remove(uuid);
    }

    public CompletableFuture<PlayerData> loadPlayer(UUID uuid, String username) {
        return db.queryAsync(conn -> {
            try {
                // Upsert player
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO lc_players (uuid, username) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE username=VALUES(username), last_seen=CURRENT_TIMESTAMP")) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, username);
                    ps.executeUpdate();
                }

                PlayerData data = new PlayerData(uuid, username);

                // Load stats
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT coins, kills, deaths, killstreak, best_killstreak, nick, hidden_name, recording_mode " +
                        "FROM lc_players WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        data.setCoins(rs.getLong("coins"));
                        data.setKills(rs.getInt("kills"));
                        data.setDeaths(rs.getInt("deaths"));
                        data.setKillstreak(rs.getInt("killstreak"));
                        data.setBestKillstreak(rs.getInt("best_killstreak"));
                        data.setNick(rs.getString("nick"));
                        data.setHiddenName(rs.getString("hidden_name"));
                        data.setRecordingMode(rs.getBoolean("recording_mode"));
                    }
                }

                // Load settings
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT IGNORE INTO lc_settings (uuid) VALUES (?)")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT public_chat, party_invites, messages_enabled, fast_crystals FROM lc_settings WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        data.setPublicChat(rs.getBoolean("public_chat"));
                        data.setPartyInvites(rs.getBoolean("party_invites"));
                        data.setMessagesEnabled(rs.getBoolean("messages_enabled"));
                        data.setFastCrystals(rs.getBoolean("fast_crystals"));
                    }
                }

                // Load ELO
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT gamemode, elo FROM lc_elo WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        data.setEloFor(rs.getString("gamemode"), rs.getInt("elo"));
                    }
                }

                // Load friends
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT uuid2 FROM lc_friends WHERE uuid1=? UNION SELECT uuid1 FROM lc_friends WHERE uuid2=?")) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        try { data.addFriend(UUID.fromString(rs.getString(1))); } catch (Exception ignored) {}
                    }
                }

                cache.put(uuid, data);
                return data;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player " + uuid + ": " + e.getMessage());
                return new PlayerData(uuid, username);
            }
        });
    }

    public CompletableFuture<Void> savePlayer(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return CompletableFuture.completedFuture(null);
        return db.executeAsync(conn -> {
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lc_players SET coins=?, kills=?, deaths=?, killstreak=?, best_killstreak=?, " +
                        "nick=?, hidden_name=?, recording_mode=?, last_seen=CURRENT_TIMESTAMP WHERE uuid=?")) {
                    ps.setLong(1, data.getCoins());
                    ps.setInt(2, data.getKills());
                    ps.setInt(3, data.getDeaths());
                    ps.setInt(4, data.getKillstreak());
                    ps.setInt(5, data.getBestKillstreak());
                    ps.setString(6, data.getNick());
                    ps.setString(7, data.getHiddenName());
                    ps.setBoolean(8, data.isRecordingMode());
                    ps.setString(9, uuid.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO lc_settings (uuid, public_chat, party_invites, messages_enabled, fast_crystals) " +
                        "VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                        "public_chat=VALUES(public_chat), party_invites=VALUES(party_invites), " +
                        "messages_enabled=VALUES(messages_enabled), fast_crystals=VALUES(fast_crystals)")) {
                    ps.setString(1, uuid.toString());
                    ps.setBoolean(2, data.isPublicChat());
                    ps.setBoolean(3, data.isPartyInvites());
                    ps.setBoolean(4, data.isMessagesEnabled());
                    ps.setBoolean(5, data.isFastCrystals());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save player " + uuid + ": " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> addCoins(UUID uuid, long amount, String reason, UUID adminUuid) {
        return db.executeAsync(conn -> {
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lc_players SET coins = GREATEST(0, coins + ?) WHERE uuid=?")) {
                    ps.setLong(1, amount);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO lc_coin_transactions (uuid, amount, reason, admin_uuid) VALUES (?,?,?,?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setLong(2, amount);
                    ps.setString(3, reason);
                    ps.setString(4, adminUuid != null ? adminUuid.toString() : null);
                    ps.executeUpdate();
                }
                PlayerData cached = cache.get(uuid);
                if (cached != null) cached.setCoins(cached.getCoins() + amount);
            } catch (SQLException e) {
                plugin.getLogger().severe("addCoins error: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> removeCoins(UUID uuid, long amount, String reason, UUID adminUuid) {
        return addCoins(uuid, -amount, reason, adminUuid);
    }

    public void applyNames(Player player) {
        PlayerData data = cache.get(player.getUniqueId());
        if (data == null) return;
        var name = TextUtil.parse(data.getDisplayName());
        player.displayName(name);
        player.playerListName(name);
    }

    public CompletableFuture<Void> setCoins(UUID uuid, long amount, UUID adminUuid) {
        return db.executeAsync(conn -> {
            try {
                long current = 0;
                try (PreparedStatement ps = conn.prepareStatement("SELECT coins FROM lc_players WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) current = rs.getLong(1);
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE lc_players SET coins=? WHERE uuid=?")) {
                    ps.setLong(1, amount);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO lc_coin_transactions (uuid, amount, reason, admin_uuid) VALUES (?,?,?,?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setLong(2, amount - current);
                    ps.setString(3, "set");
                    ps.setString(4, adminUuid != null ? adminUuid.toString() : null);
                    ps.executeUpdate();
                }
                PlayerData cached = cache.get(uuid);
                if (cached != null) cached.setCoins(amount);
            } catch (SQLException e) {
                plugin.getLogger().severe("setCoins error: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Long> getCoins(UUID uuid) {
        PlayerData cached = cache.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached.getCoins());
        return db.queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT coins FROM lc_players WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getLong(1) : 0L;
            } catch (SQLException e) {
                return 0L;
            }
        });
    }

    public CompletableFuture<UUID> findUUIDByName(String name) {
        return db.queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM lc_players WHERE username=? LIMIT 1")) {
                ps.setString(1, name);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return UUID.fromString(rs.getString(1));
                return null;
            } catch (SQLException e) {
                return null;
            }
        });
    }

    public CompletableFuture<PlayerData> loadOfflinePlayer(String name) {
        return db.queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT p.uuid, p.username, p.coins, p.kills, p.deaths, p.killstreak, p.best_killstreak, " +
                    "p.nick, p.hidden_name, p.recording_mode FROM lc_players p WHERE p.username=? LIMIT 1")) {
                ps.setString(1, name);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                PlayerData data = new PlayerData(uuid, rs.getString("username"));
                data.setCoins(rs.getLong("coins"));
                data.setKills(rs.getInt("kills"));
                data.setDeaths(rs.getInt("deaths"));
                data.setKillstreak(rs.getInt("killstreak"));
                data.setBestKillstreak(rs.getInt("best_killstreak"));
                data.setNick(rs.getString("nick"));
                data.setHiddenName(rs.getString("hidden_name"));
                data.setRecordingMode(rs.getBoolean("recording_mode"));
                return data;
            } catch (SQLException e) {
                plugin.getLogger().severe("loadOfflinePlayer error: " + e.getMessage());
                return null;
            }
        });
    }
}
