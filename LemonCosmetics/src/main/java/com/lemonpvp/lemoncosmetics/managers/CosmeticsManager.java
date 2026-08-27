package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.ArmorSlotType;
import com.lemonpvp.lemoncosmetics.model.KillEffectType;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CosmeticsManager {

    private final LemonCosmetics plugin;
    private final Map<UUID, PlayerCosmetics> cache = new ConcurrentHashMap<>();

    public CosmeticsManager(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Cache management
    // -------------------------------------------------------------------------

    /**
     * Loads cosmetics from the database into the cache and returns the result.
     */
    public CompletableFuture<PlayerCosmetics> loadPlayer(UUID uuid) {
        return plugin.getDatabase().loadPlayerCosmetics(uuid).thenApply(cosmetics -> {
            cache.put(uuid, cosmetics);
            return cosmetics;
        });
    }

    /**
     * Returns the cached {@link PlayerCosmetics} for the given player, or
     * {@code null} if they have not been loaded yet.
     */
    public PlayerCosmetics getPlayerCosmetics(UUID uuid) {
        return cache.get(uuid);
    }

    /**
     * Removes the player's entry from the in-memory cache. Should be called
     * on player quit after saving.
     */
    public void unloadPlayer(UUID uuid) {
        cache.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Kill effect management
    // -------------------------------------------------------------------------

    /**
     * Adds {@code effectId} to the player's owned effects in both cache and DB.
     */
    public CompletableFuture<Void> unlockKillEffect(UUID uuid, String effectId) {
        PlayerCosmetics cosmetics = cache.get(uuid);
        if (cosmetics != null) {
            cosmetics.getOwnedEffects().add(effectId);
        }
        return plugin.getDatabase().saveKillEffect(uuid, effectId, false);
    }

    /**
     * Buys a kill effect with coins (code/quest/permission unlocks continue to
     * work as before — this is an additional path). Returns {@code false} if
     * the player cannot afford it or LemonCore is unavailable.
     */
    public CompletableFuture<Boolean> buyKillEffect(UUID uuid, KillEffectType effect) {
        if (effect == null) return CompletableFuture.completedFuture(false);
        return canAfford(uuid, effect.getPrice()).thenCompose(affordable -> {
            if (!affordable) return CompletableFuture.completedFuture(false);
            com.lemonpvp.lemoncore.LemonCore lc = getLemonCore();
            if (lc == null) return CompletableFuture.completedFuture(false);
            return lc.getPlayerDataManager()
                    .removeCoins(uuid, effect.getPrice(), "cosmetics:killeffect:" + effect.getId(), null)
                    .thenCompose(v -> unlockKillEffect(uuid, effect.getId()))
                    .thenApply(v -> true);
        });
    }

    /**
     * Sets the player's active kill effect to {@code effectId} (or unequips it
     * when {@code effectId} is {@code null}).
     */
    public CompletableFuture<Void> setActiveKillEffect(UUID uuid, String effectId) {
        PlayerCosmetics cosmetics = cache.get(uuid);
        if (cosmetics != null) {
            cosmetics.setActiveEffectId(effectId);
        }
        return effectId == null
                ? plugin.getDatabase().clearActiveEffect(uuid)
                : plugin.getDatabase().setActiveEffect(uuid, effectId);
    }

    /**
     * Returns the active {@link KillEffectType} for the given player, or
     * {@code null} if none is equipped or the player is not loaded.
     */
    public KillEffectType getActiveKillEffect(UUID uuid) {
        PlayerCosmetics cosmetics = cache.get(uuid);
        if (cosmetics == null) return null;
        String activeId = cosmetics.getActiveEffectId();
        if (activeId == null) return null;
        return KillEffectType.fromId(activeId).orElse(null);
    }

    // -------------------------------------------------------------------------
    // Trim shop
    // -------------------------------------------------------------------------

    /**
     * Attempts to purchase a trim pattern for the player. Deducts coins and
     * unlocks the pattern. Returns {@code true} on success, {@code false} if
     * the player cannot afford it.
     */
    public CompletableFuture<Boolean> buyTrimPattern(UUID uuid, String patternId) {
        int cost = plugin.getConfig().getInt("prices.trim-pattern", 250);
        return canAfford(uuid, cost).thenCompose(affordable -> {
            if (!affordable) return CompletableFuture.completedFuture(false);

            com.lemonpvp.lemoncore.LemonCore lc = getLemonCore();
            if (lc == null) return CompletableFuture.completedFuture(false);

            return lc.getPlayerDataManager()
                    .removeCoins(uuid, cost, "cosmetics:trim-pattern:" + patternId, null)
                    .thenCompose(v -> {
                        PlayerCosmetics cosmetics = cache.get(uuid);
                        if (cosmetics != null) cosmetics.getOwnedPatterns().add(patternId);
                        return plugin.getDatabase().addOwnedPattern(uuid, patternId);
                    })
                    .thenApply(v -> true);
        });
    }

    /**
     * Attempts to purchase a trim material for the player. Deducts coins and
     * unlocks the material. Returns {@code true} on success, {@code false} if
     * the player cannot afford it.
     */
    public CompletableFuture<Boolean> buyTrimMaterial(UUID uuid, String materialId) {
        int cost = plugin.getConfig().getInt("prices.trim-material", 50);
        return canAfford(uuid, cost).thenCompose(affordable -> {
            if (!affordable) return CompletableFuture.completedFuture(false);

            com.lemonpvp.lemoncore.LemonCore lc = getLemonCore();
            if (lc == null) return CompletableFuture.completedFuture(false);

            return lc.getPlayerDataManager()
                    .removeCoins(uuid, cost, "cosmetics:trim-material:" + materialId, null)
                    .thenCompose(v -> {
                        PlayerCosmetics cosmetics = cache.get(uuid);
                        if (cosmetics != null) cosmetics.getOwnedMaterials().add(materialId);
                        return plugin.getDatabase().addOwnedMaterial(uuid, materialId);
                    })
                    .thenApply(v -> true);
        });
    }

    // -------------------------------------------------------------------------
    // Trim application
    // -------------------------------------------------------------------------

    /**
     * Stores the trim for the given slot in cache and DB, then applies it to
     * the player's worn armor if they are online.
     */
    public CompletableFuture<Void> applyArmorTrim(UUID uuid, ArmorSlotType slot, String patternId, String materialId) {
        PlayerCosmetics cosmetics = cache.get(uuid);
        if (cosmetics != null) {
            cosmetics.setAppliedTrim(slot.name().toLowerCase(), patternId, materialId);
        }

        return plugin.getDatabase().saveArmorTrim(uuid, slot.name().toLowerCase(), patternId, materialId)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) plugin.getArmorTrimManager().applyTrimToPlayer(p);
                }));
    }

    /**
     * Removes the stored trim for the given slot from cache and DB, then strips
     * it from the player's worn armor if they are online.
     */
    public CompletableFuture<Void> removeArmorTrim(UUID uuid, ArmorSlotType slot) {
        PlayerCosmetics cosmetics = cache.get(uuid);
        if (cosmetics != null) {
            cosmetics.setAppliedTrim(slot.name().toLowerCase(), null, null);
        }
        return plugin.getDatabase().clearArmorTrim(uuid, slot.name().toLowerCase())
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) plugin.getArmorTrimManager().removeTrimFromPlayer(p, slot);
                }));
    }

    // -------------------------------------------------------------------------
    // LemonCore integration helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link com.lemonpvp.lemoncore.LemonCore} plugin instance, or
     * {@code null} if it is not loaded.
     */
    public com.lemonpvp.lemoncore.LemonCore getLemonCore() {
        org.bukkit.plugin.Plugin lc = Bukkit.getPluginManager().getPlugin("LemonCore");
        if (lc instanceof com.lemonpvp.lemoncore.LemonCore core) return core;
        return null;
    }

    /**
     * Checks whether the player has at least {@code cost} coins. Returns
     * {@code false} if LemonCore is unavailable.
     */
    public CompletableFuture<Boolean> canAfford(UUID uuid, long cost) {
        com.lemonpvp.lemoncore.LemonCore lc = getLemonCore();
        if (lc == null) return CompletableFuture.completedFuture(false);
        return lc.getPlayerDataManager().getCoins(uuid)
                .thenApply(coins -> coins >= cost);
    }
}
