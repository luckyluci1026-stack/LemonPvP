package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.database.DatabaseManager;
import com.lemonpvp.lemoncore.util.TextUtil;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CodeManager {

    public enum RedeemResult { SUCCESS, NOT_FOUND, ALREADY_USED, EXPIRED, MAX_USES }

    public static class CodeData {
        public String code;
        public String rewardType;
        public String rewardValue;
        public int maxUses;
        public int uses;
        public Timestamp expires;
        public boolean active;
        public RedeemResult result;
    }

    private final LemonCore plugin;
    private final DatabaseManager db;

    public CodeManager(LemonCore plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    public CompletableFuture<String> createCode(String codeName, String rewardType, String rewardValue,
                                                 int maxUses, long durationSeconds, UUID creatorUuid) {
        return db.queryAsync(conn -> {
            try {
                String code = codeName.equalsIgnoreCase("random26")
                        ? TextUtil.generateAlphanumeric(26)
                        : codeName;

                Timestamp expires = durationSeconds > 0
                        ? new Timestamp(System.currentTimeMillis() + durationSeconds * 1000L)
                        : null;

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO lc_codes (code, reward_type, reward_value, max_uses, creator_uuid, expires) " +
                        "VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                        "reward_type=VALUES(reward_type), reward_value=VALUES(reward_value), " +
                        "max_uses=VALUES(max_uses), expires=VALUES(expires), active=TRUE, uses=0")) {
                    ps.setString(1, code);
                    ps.setString(2, rewardType);
                    ps.setString(3, rewardValue);
                    ps.setInt(4, maxUses);
                    ps.setString(5, creatorUuid != null ? creatorUuid.toString() : null);
                    ps.setTimestamp(6, expires);
                    ps.executeUpdate();
                }
                return code;
            } catch (SQLException e) {
                plugin.getLogger().severe("CreateCode error: " + e.getMessage());
                return null;
            }
        });
    }

    public CompletableFuture<CodeData> redeemCode(String code, UUID playerUuid) {
        return db.queryAsync(conn -> {
            CodeData data = new CodeData();
            try {
                // Fetch code
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM lc_codes WHERE code=?")) {
                    ps.setString(1, code);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) { data.result = RedeemResult.NOT_FOUND; return data; }
                    data.code = rs.getString("code");
                    data.rewardType = rs.getString("reward_type");
                    data.rewardValue = rs.getString("reward_value");
                    data.maxUses = rs.getInt("max_uses");
                    data.uses = rs.getInt("uses");
                    data.expires = rs.getTimestamp("expires");
                    data.active = rs.getBoolean("active");
                }

                if (!data.active) { data.result = RedeemResult.NOT_FOUND; return data; }
                if (data.expires != null && data.expires.before(new Timestamp(System.currentTimeMillis()))) {
                    data.result = RedeemResult.EXPIRED; return data;
                }
                if (data.maxUses > 0 && data.uses >= data.maxUses) {
                    data.result = RedeemResult.MAX_USES; return data;
                }

                // Check if player already redeemed
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT 1 FROM lc_code_redemptions WHERE code=? AND uuid=?")) {
                    ps.setString(1, code);
                    ps.setString(2, playerUuid.toString());
                    if (ps.executeQuery().next()) { data.result = RedeemResult.ALREADY_USED; return data; }
                }

                // Record redemption
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO lc_code_redemptions (code, uuid) VALUES (?,?)")) {
                    ps.setString(1, code);
                    ps.setString(2, playerUuid.toString());
                    ps.executeUpdate();
                }

                // Increment uses
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lc_codes SET uses=uses+1 WHERE code=?")) {
                    ps.setString(1, code);
                    ps.executeUpdate();
                }

                data.result = RedeemResult.SUCCESS;
                return data;
            } catch (SQLException e) {
                plugin.getLogger().severe("RedeemCode error: " + e.getMessage());
                data.result = RedeemResult.NOT_FOUND;
                return data;
            }
        });
    }
}
