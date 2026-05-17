package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.database.DatabaseManager;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class StatsManager {

    public static class WipeRecord {
        public int id;
        public UUID uuid;
        public int kills, deaths, killstreak, bestKillstreak;
        public long coins;
        public Timestamp wipeTime;
        public String wiperName;

        public String getFormattedDate() {
            return new SimpleDateFormat("yyyy-MM-dd").format(wipeTime);
        }
    }

    private final LemonCore plugin;
    private final DatabaseManager db;

    public StatsManager(LemonCore plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    public CompletableFuture<Void> wipeStats(UUID target, String targetName, UUID wiper, String wiperName) {
        return db.executeAsync(conn -> {
            try {
                // Save current stats
                int kills = 0, deaths = 0, ks = 0, bks = 0;
                long coins = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT kills, deaths, killstreak, best_killstreak, coins FROM lc_players WHERE uuid=?")) {
                    ps.setString(1, target.toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        kills = rs.getInt("kills");
                        deaths = rs.getInt("deaths");
                        ks = rs.getInt("killstreak");
                        bks = rs.getInt("best_killstreak");
                        coins = rs.getLong("coins");
                    }
                }
                // Archive wipe
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO lc_stats_wipes (uuid, kills, deaths, killstreak, best_killstreak, coins, wiper_uuid, wiper_name) " +
                        "VALUES (?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, target.toString());
                    ps.setInt(2, kills); ps.setInt(3, deaths);
                    ps.setInt(4, ks); ps.setInt(5, bks);
                    ps.setLong(6, coins);
                    ps.setString(7, wiper != null ? wiper.toString() : null);
                    ps.setString(8, wiperName);
                    ps.executeUpdate();
                }
                // Reset stats
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lc_players SET kills=0, deaths=0, killstreak=0, best_killstreak=0, coins=0 WHERE uuid=?")) {
                    ps.setString(1, target.toString());
                    ps.executeUpdate();
                }
                // Reset ELO
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lc_elo SET elo=1000 WHERE uuid=?")) {
                    ps.setString(1, target.toString());
                    ps.executeUpdate();
                }
                // Update cache
                PlayerData cached = plugin.getPlayerDataManager().getCached(target);
                if (cached != null) {
                    cached.setKills(0); cached.setDeaths(0);
                    cached.setKillstreak(0); cached.setBestKillstreak(0);
                    cached.setCoins(0);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Wipe error: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<WipeRecord> findWipeByDate(UUID uuid, String dateStr) {
        return db.queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM lc_stats_wipes WHERE uuid=? AND DATE(wipe_time)=? ORDER BY wipe_time DESC LIMIT 1")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, dateStr);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
                return mapWipe(rs);
            } catch (SQLException e) {
                return null;
            }
        });
    }

    public CompletableFuture<Void> restoreStats(UUID target, WipeRecord wipe) {
        return db.executeAsync(conn -> {
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lc_players SET kills=?, deaths=?, killstreak=?, best_killstreak=?, coins=? WHERE uuid=?")) {
                    ps.setInt(1, wipe.kills); ps.setInt(2, wipe.deaths);
                    ps.setInt(3, wipe.killstreak); ps.setInt(4, wipe.bestKillstreak);
                    ps.setLong(5, wipe.coins);
                    ps.setString(6, target.toString());
                    ps.executeUpdate();
                }
                PlayerData cached = plugin.getPlayerDataManager().getCached(target);
                if (cached != null) {
                    cached.setKills(wipe.kills); cached.setDeaths(wipe.deaths);
                    cached.setKillstreak(wipe.killstreak); cached.setBestKillstreak(wipe.bestKillstreak);
                    cached.setCoins(wipe.coins);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Restore error: " + e.getMessage());
            }
        });
    }

    private WipeRecord mapWipe(ResultSet rs) throws SQLException {
        WipeRecord w = new WipeRecord();
        w.id = rs.getInt("id");
        w.uuid = UUID.fromString(rs.getString("uuid"));
        w.kills = rs.getInt("kills");
        w.deaths = rs.getInt("deaths");
        w.killstreak = rs.getInt("killstreak");
        w.bestKillstreak = rs.getInt("best_killstreak");
        w.coins = rs.getLong("coins");
        w.wipeTime = rs.getTimestamp("wipe_time");
        w.wiperName = rs.getString("wiper_name");
        return w;
    }
}
