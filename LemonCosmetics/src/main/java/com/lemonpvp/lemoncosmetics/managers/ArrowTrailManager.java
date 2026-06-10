package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.ArrowTrailType;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ArrowTrailManager {

    private final LemonCosmetics plugin;
    // arrowEntityId → task
    private final Map<Integer, BukkitTask> arrowTasks = new ConcurrentHashMap<>();
    // shooterUuid → set of arrow entity IDs with active trails
    private final Map<UUID, Set<Integer>> playerArrows = new ConcurrentHashMap<>();

    public ArrowTrailManager(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    public ArrowTrailType getActiveTrail(UUID uuid) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics == null) return null;
        String id = cosmetics.getActiveTrailId();
        if (id == null) return null;
        return ArrowTrailType.fromId(id).orElse(null);
    }

    /** Called from ProjectileLaunchEvent when a player with an active trail shoots. */
    public void startTrail(Arrow arrow, ArrowTrailType trail) {
        int entityId = arrow.getEntityId();
        UUID shooterUuid = arrow.getShooter() instanceof org.bukkit.entity.Player p ? p.getUniqueId() : null;

        if (shooterUuid != null) {
            playerArrows.computeIfAbsent(shooterUuid, k -> ConcurrentHashMap.newKeySet()).add(entityId);
        }

        BukkitTask task = Bukkit.getScheduler()
                .runTaskTimerAsynchronously(plugin, () -> {
                    if (!arrow.isValid() || arrow.isOnGround()) {
                        stopTrail(entityId);
                        if (shooterUuid != null) {
                            Set<Integer> s = playerArrows.get(shooterUuid);
                            if (s != null) s.remove(entityId);
                        }
                        return;
                    }
                    spawnParticle(arrow, trail);
                }, 0L, 2L);
        arrowTasks.put(entityId, task);
    }

    private void stopTrail(int entityId) {
        BukkitTask task = arrowTasks.remove(entityId);
        if (task != null) task.cancel();
    }

    /** Cancel all active arrow trail tasks for a player (called on disconnect). */
    public void stopAllTrailsForPlayer(UUID playerUuid) {
        Set<Integer> arrows = playerArrows.remove(playerUuid);
        if (arrows == null) return;
        for (int entityId : arrows) {
            stopTrail(entityId);
        }
    }

    private void spawnParticle(Arrow arrow, ArrowTrailType trail) {
        try {
            var loc = arrow.getLocation();
            if (trail.particle == Particle.DUST) {
                Particle.DustOptions dust = new Particle.DustOptions(trail.dustColor, trail.dustSize);
                loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
            } else if (trail.particle == Particle.FLAME) {
                // Flame + smoke alternating based on entity ID % 2
                Particle p = (arrow.getEntityId() % 2 == 0) ? Particle.FLAME : Particle.SMOKE;
                loc.getWorld().spawnParticle(p, loc, 1, 0, 0, 0, 0);
            } else {
                loc.getWorld().spawnParticle(trail.particle, loc, 1, 0, 0, 0, 0);
            }
        } catch (Exception ignored) {}
    }

    /** Unlock trail for player. */
    public CompletableFuture<Void> unlockTrail(UUID uuid, String trailId) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.getOwnedTrails().add(trailId);
        return plugin.getDatabase().saveTrail(uuid, trailId, false);
    }

    /** Set active trail. */
    public CompletableFuture<Void> setActiveTrail(UUID uuid, String trailId) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.setActiveTrailId(trailId);
        return trailId == null
                ? plugin.getDatabase().clearActiveTrail(uuid)
                : plugin.getDatabase().setActiveTrail(uuid, trailId);
    }

    /** Buy a trail using coins. */
    public CompletableFuture<Boolean> buyTrail(UUID uuid, String trailId) {
        ArrowTrailType trail = ArrowTrailType.fromId(trailId).orElse(null);
        if (trail == null) return CompletableFuture.completedFuture(false);

        return plugin.getCosmeticsManager().canAfford(uuid, trail.price).thenCompose(affordable -> {
            if (!affordable) return CompletableFuture.completedFuture(false);
            var lc = plugin.getCosmeticsManager().getLemonCore();
            if (lc == null) return CompletableFuture.completedFuture(false);
            return lc.getPlayerDataManager()
                    .removeCoins(uuid, trail.price, "cosmetics:trail:" + trailId, null)
                    .thenCompose(v -> unlockTrail(uuid, trailId))
                    .thenApply(v -> true);
        });
    }
}
