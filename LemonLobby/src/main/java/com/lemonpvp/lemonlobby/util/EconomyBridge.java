package com.lemonpvp.lemonlobby.util;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemoncore.managers.PlayerDataManager;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Static bridge to LemonCore's economy — avoids repeating the plugin cast in every LemonLobby file.
 * All methods return safe defaults (0 / completed future) when LemonCore is absent.
 */
public final class EconomyBridge {

    private EconomyBridge() {}

    public static LemonCore core() {
        var p = Bukkit.getPluginManager().getPlugin("LemonCore");
        return p instanceof LemonCore lc ? lc : null;
    }

    public static PlayerDataManager pdm() {
        LemonCore lc = core();
        return lc != null ? lc.getPlayerDataManager() : null;
    }

    public static PlayerData cached(UUID uuid) {
        PlayerDataManager m = pdm();
        return m != null ? m.getCached(uuid) : null;
    }

    public static long apples(UUID uuid) {
        PlayerData pd = cached(uuid);
        return pd != null ? pd.getApples() : 0;
    }

    public static long planks(UUID uuid) {
        PlayerData pd = cached(uuid);
        return pd != null ? pd.getPlanks() : 0;
    }

    public static CompletableFuture<Void> addApples(UUID uuid, long amount) {
        PlayerDataManager m = pdm();
        return m != null ? m.addApples(uuid, amount) : CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> removeApples(UUID uuid, long amount) {
        PlayerDataManager m = pdm();
        return m != null ? m.removeApples(uuid, amount) : CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> addPlanks(UUID uuid, long amount) {
        PlayerDataManager m = pdm();
        return m != null ? m.addPlanks(uuid, amount) : CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> removePlanks(UUID uuid, long amount) {
        PlayerDataManager m = pdm();
        return m != null ? m.removePlanks(uuid, amount) : CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<UUID> findUUID(String name) {
        PlayerDataManager m = pdm();
        return m != null ? m.findUUIDByName(name) : CompletableFuture.completedFuture(null);
    }
}
