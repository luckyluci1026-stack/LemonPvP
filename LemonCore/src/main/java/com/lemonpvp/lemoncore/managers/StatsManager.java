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
        /** Per-gamemode ELO snapshot, serialized as {@code gm=elo;gm=elo}. */
        public String eloData;

        public String getFormattedDate() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(wipeTime);
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
                // Snapshot per-gamemode ELO so unwipe can put it back exactly.
                String eloData = serializeElo(readElo(conn, target));
                // Archive wipe
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO lc_stats_wipes (uuid, kills, deaths, killstreak, best_killstreak, coins, wiper_uuid, wiper_name, elo_data) " +
                        "VALUES (?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, target.toString());
                    ps.setInt(2, kills); ps.setInt(3, deaths);
                    ps.setInt(4, ks); ps.setInt(5, bks);
                    ps.setLong(6, coins);
                    ps.setString(7, wiper != null ? wiper.toString() : null);
                    ps.setString(8, wiperName);
                    ps.setString(9, eloData);
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

    /** All wipe snapshots for a player, most recent first (newest = index 0). */
    public CompletableFuture<List<WipeRecord>> listWipes(UUID uuid) {
        return db.queryAsync(conn -> {
            List<WipeRecord> out = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM lc_stats_wipes WHERE uuid=? ORDER BY wipe_time DESC")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) out.add(mapWipe(rs));
            } catch (SQLException e) {
                plugin.getLogger().severe("listWipes error: " + e.getMessage());
            }
            return out;
        });
    }

    /** The most recent wipe snapshot for a player, or {@code null} if none. */
    public CompletableFuture<WipeRecord> findLatestWipe(UUID uuid) {
        return db.queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM lc_stats_wipes WHERE uuid=? ORDER BY wipe_time DESC LIMIT 1")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                return rs.next() ? mapWipe(rs) : null;
            } catch (SQLException e) {
                return null;
            }
        });
    }

    /** A specific wipe snapshot by its archive id (scoped to the player). */
    public CompletableFuture<WipeRecord> findWipeById(UUID uuid, int id) {
        return db.queryAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM lc_stats_wipes WHERE uuid=? AND id=? LIMIT 1")) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, id);
                ResultSet rs = ps.executeQuery();
                return rs.next() ? mapWipe(rs) : null;
            } catch (SQLException e) {
                return null;
            }
        });
    }

    /**
     * Restores a player's stats from a wipe snapshot. Unlike the old version,
     * this also puts back the per-gamemode ELO archived at wipe time, and
     * deletes the snapshot afterwards so it can't be applied twice.
     */
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
                // Restore per-gamemode ELO from the snapshot.
                Map<String, Integer> elo = deserializeElo(wipe.eloData);
                for (Map.Entry<String, Integer> e : elo.entrySet()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO lc_elo (uuid, gamemode, elo) VALUES (?,?,?) " +
                            "ON DUPLICATE KEY UPDATE elo=VALUES(elo)")) {
                        ps.setString(1, target.toString());
                        ps.setString(2, e.getKey());
                        ps.setInt(3, e.getValue());
                        ps.executeUpdate();
                    }
                }
                // Consume the snapshot so a second unwipe can't re-apply it.
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM lc_stats_wipes WHERE id=?")) {
                    ps.setInt(1, wipe.id);
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
        try { w.eloData = rs.getString("elo_data"); } catch (SQLException ignored) { w.eloData = null; }
        return w;
    }

    // -- ELO snapshot (de)serialization: "gm=elo;gm=elo" --------------------

    private Map<String, Integer> readElo(Connection conn, UUID uuid) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT gamemode, elo FROM lc_elo WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) map.put(rs.getString("gamemode"), rs.getInt("elo"));
        }
        return map;
    }

    private String serializeElo(Map<String, Integer> elo) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : elo.entrySet()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    private Map<String, Integer> deserializeElo(String data) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (data == null || data.isBlank()) return map;
        for (String pair : data.split(";")) {
            int eq = pair.lastIndexOf('=');
            if (eq <= 0) continue;
            try {
                map.put(pair.substring(0, eq), Integer.parseInt(pair.substring(eq + 1)));
            } catch (NumberFormatException ignored) {}
        }
        return map;
    }
}
