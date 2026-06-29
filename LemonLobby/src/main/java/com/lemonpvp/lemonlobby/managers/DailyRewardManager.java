package com.lemonpvp.lemonlobby.managers;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.database.Database;
import com.lemonpvp.lemonlobby.model.BoosterTier;
import com.lemonpvp.lemonlobby.util.EconomyBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Daily login rewards with a {@value #CYCLE}-day streak. Each day grants apples
 * (escalating); the final day of the cycle also grants a booster. Missing a day
 * resets the streak. State is persisted per player (last claim day + streak).
 */
public class DailyRewardManager {

    public static final int CYCLE = 7;

    private final LemonLobby plugin;
    private final Map<UUID, Database.DailyEntry> cache = new ConcurrentHashMap<>();

    public DailyRewardManager(LemonLobby plugin) {
        this.plugin = plugin;
    }

    // ── Load / unload ───────────────────────────────────────────────────────────

    public void load(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Database.DailyEntry e = plugin.getDatabase().loadDaily(uuid);
            if (e != null) cache.put(uuid, e);
        });
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    // ── Date helpers ─────────────────────────────────────────────────────────────

    private ZoneId zone() {
        try { return ZoneId.of(plugin.getConfig().getString("daily.timezone", "Europe/Berlin")); }
        catch (Exception e) { return ZoneId.systemDefault(); }
    }

    private LocalDate dayOf(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(zone()).toLocalDate();
    }

    private LocalDate today() {
        return LocalDate.now(zone());
    }

    // ── Queries ──────────────────────────────────────────────────────────────────

    public boolean canClaim(UUID uuid) {
        Database.DailyEntry e = cache.get(uuid);
        if (e == null) return true;
        return !dayOf(e.lastClaim()).equals(today());
    }

    /** The streak value that applies to the next/most-recent claim (1-based). */
    public int effectiveStreak(UUID uuid) {
        Database.DailyEntry e = cache.get(uuid);
        if (e == null) return 1;
        LocalDate last = dayOf(e.lastClaim());
        if (last.equals(today())) return e.streak();              // already claimed today
        if (last.equals(today().minusDays(1))) return e.streak() + 1; // consecutive
        return 1;                                                 // missed → reset
    }

    /** 1..CYCLE position in the reward calendar for a given streak. */
    public int cycleDay(int streak) {
        return ((Math.max(1, streak) - 1) % CYCLE) + 1;
    }

    public int appleRewardForDay(int day) {
        List<Integer> list = plugin.getConfig().getIntegerList("daily.apples");
        if (list != null && list.size() >= CYCLE) return list.get(day - 1);
        int[] def = {100, 150, 200, 300, 400, 500, 1000};
        return def[Math.min(Math.max(day, 1), CYCLE) - 1];
    }

    /** Booster tier id granted on the final cycle day, or null. */
    public String boosterTierForDay(int day) {
        if (day != CYCLE) return null;
        return plugin.getConfig().getString("daily.day7-booster", "L1");
    }

    // ── Claim ────────────────────────────────────────────────────────────────────

    /**
     * Claims today's reward. Returns the claimed cycle day (1..CYCLE), or -1 if
     * the player already claimed today.
     */
    public int claim(Player player) {
        UUID uuid = player.getUniqueId();
        if (!canClaim(uuid)) return -1;

        int streak = effectiveStreak(uuid);
        int day = cycleDay(streak);
        long now = System.currentTimeMillis();

        cache.put(uuid, new Database.DailyEntry(now, streak));
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getDatabase().saveDaily(uuid, now, streak));

        EconomyBridge.addApples(uuid, appleRewardForDay(day));

        String boosterId = boosterTierForDay(day);
        if (boosterId != null) {
            BoosterTier tier = plugin.getBoosterConfig().fromId(boosterId);
            if (tier != null) plugin.getBoosterManager().activate(uuid, tier);
        }
        return day;
    }
}
