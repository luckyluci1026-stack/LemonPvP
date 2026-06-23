package com.lemonpvp.lemonlobby.managers;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.database.Database;
import com.lemonpvp.lemonlobby.model.BoosterTier;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class BoosterManager {

    private final LemonLobby plugin;
    private final Map<UUID, Database.BoosterEntry> cache = new ConcurrentHashMap<>();

    public BoosterManager(LemonLobby plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> load(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            Database.BoosterEntry entry = plugin.getDatabase().loadBoosterEntry(uuid);
            if (entry != null && isActive(entry)) {
                cache.put(uuid, entry);
            }
        });
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    /** Returns the bonus apples this player earns per leaf click, or 0 if no booster. */
    public int getBonus(UUID uuid) {
        Database.BoosterEntry entry = cache.get(uuid);
        if (entry == null || !isActive(entry)) {
            cache.remove(uuid);
            return 0;
        }
        return entry.tier().bonusApples;
    }

    public Database.BoosterEntry getActive(UUID uuid) {
        Database.BoosterEntry entry = cache.get(uuid);
        if (entry == null || !isActive(entry)) {
            cache.remove(uuid);
            return null;
        }
        return entry;
    }

    /** Decrements one use. Returns false if the booster is now exhausted/expired (already removed). */
    public boolean consumeUse(UUID uuid) {
        Database.BoosterEntry entry = cache.get(uuid);
        if (entry == null || !isActive(entry)) {
            cache.remove(uuid);
            return false;
        }
        int remaining = entry.remainingUses() - 1;
        if (remaining <= 0) {
            cache.remove(uuid);
            Bukkit.getScheduler().runTaskAsynchronously(plugin,
                    () -> plugin.getDatabase().deleteBoosterEntry(uuid));
            return false;
        }
        Database.BoosterEntry updated = new Database.BoosterEntry(entry.tier(), remaining, entry.expiresAt());
        cache.put(uuid, updated);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getDatabase().saveBoosterEntry(uuid, entry.tier(), remaining, entry.expiresAt()));
        return true;
    }

    public void activate(UUID uuid, BoosterTier tier) {
        long expiresAt = System.currentTimeMillis() + (long) tier.durationSeconds * 1000;
        Database.BoosterEntry entry = new Database.BoosterEntry(tier, tier.maxUses, expiresAt);
        cache.put(uuid, entry);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getDatabase().saveBoosterEntry(uuid, tier, tier.maxUses, expiresAt));
    }

    private boolean isActive(Database.BoosterEntry entry) {
        return System.currentTimeMillis() < entry.expiresAt() && entry.remainingUses() > 0;
    }
}
