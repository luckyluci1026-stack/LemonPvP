package com.lemonpvp.lemonlobby.managers;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.database.Database;
import com.lemonpvp.lemonlobby.model.PlankTier;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TreeUpgradeManager {

    private final LemonLobby plugin;
    private final Map<UUID, Database.PlankEntry> cache = new ConcurrentHashMap<>();

    public TreeUpgradeManager(LemonLobby plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> load(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            Database.PlankEntry entry = plugin.getDatabase().loadPlankEntry(uuid);
            if (entry != null && !isExpired(entry)) {
                cache.put(uuid, entry);
            }
        });
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    public PlankTier getCurrentTier(UUID uuid) {
        Database.PlankEntry entry = cache.get(uuid);
        if (entry == null || isExpired(entry)) {
            cache.remove(uuid);
            return PlankTier.OAK;
        }
        return entry.tier();
    }

    public long getExpiresAt(UUID uuid) {
        Database.PlankEntry entry = cache.get(uuid);
        if (entry == null || isExpired(entry)) return -1;
        return entry.expiresAt();
    }

    public void upgrade(UUID uuid, PlankTier tier) {
        long expiresAt = tier.durationDays < 0
                ? Long.MAX_VALUE
                : System.currentTimeMillis() + (long) tier.durationDays * 24 * 60 * 60 * 1000;
        Database.PlankEntry entry = new Database.PlankEntry(tier, expiresAt);
        cache.put(uuid, entry);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getDatabase().savePlankEntry(uuid, tier, expiresAt));
    }

    private boolean isExpired(Database.PlankEntry entry) {
        return entry.expiresAt() != Long.MAX_VALUE && System.currentTimeMillis() > entry.expiresAt();
    }
}
