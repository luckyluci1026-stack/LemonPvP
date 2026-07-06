package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.ArrowTrailType;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Projectile;
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
    public void startTrail(Projectile projectile, ArrowTrailType trail) {
        int entityId = projectile.getEntityId();
        UUID shooterUuid = projectile.getShooter() instanceof org.bukkit.entity.Player p ? p.getUniqueId() : null;

        if (shooterUuid != null) {
            playerArrows.computeIfAbsent(shooterUuid, k -> ConcurrentHashMap.newKeySet()).add(entityId);
        }

        final int[] life = {0};
        BukkitTask task = Bukkit.getScheduler()
                .runTaskTimer(plugin, () -> {
                    // Hard lifetime cap (30s) so a stuck projectile (e.g. a
                    // loyalty trident wedged mid-return) can't leak its task.
                    if (++life[0] > 300 || !projectile.isValid() || projectile.isOnGround()) {
                        stopTrail(entityId);
                        if (shooterUuid != null) {
                            Set<Integer> s = playerArrows.get(shooterUuid);
                            if (s != null) s.remove(entityId);
                        }
                        return;
                    }
                    spawnParticle(projectile, trail);
                }, 0L, 1L);
        arrowTasks.put(entityId, task);
    }

    private void stopTrail(int entityId) {
        BukkitTask task = arrowTasks.remove(entityId);
        if (task != null) task.cancel();
    }

    /** Cancel all active arrow trail tasks (called on plugin disable). */
    public void cancelAll() {
        arrowTasks.values().forEach(BukkitTask::cancel);
        arrowTasks.clear();
        playerArrows.clear();
    }

    /** Cancel all active arrow trail tasks for a player (called on disconnect). */
    public void stopAllTrailsForPlayer(UUID playerUuid) {
        Set<Integer> arrows = playerArrows.remove(playerUuid);
        if (arrows == null) return;
        for (int entityId : arrows) {
            stopTrail(entityId);
        }
    }

    private void spawnParticle(Projectile projectile, ArrowTrailType trail) {
        try {
            var loc = projectile.getLocation();
            var world = loc.getWorld();
            if (world == null) return;
            if (trail.particle == Particle.DUST) {
                // Denser dust: a small cluster with a touch of spread so the
                // trail reads as a solid ribbon, not a dotted line.
                Particle.DustOptions dust = new Particle.DustOptions(trail.dustColor, trail.dustSize);
                world.spawnParticle(Particle.DUST, loc, 4, 0.08, 0.08, 0.08, 0, dust);
            } else if (trail.particle == Particle.FLAME) {
                world.spawnParticle(Particle.FLAME, loc, 3, 0.06, 0.06, 0.06, 0.002);
                world.spawnParticle(Particle.SMOKE, loc, 1, 0.05, 0.05, 0.05, 0);
            } else {
                world.spawnParticle(trail.particle, loc, 3, 0.07, 0.07, 0.07, 0.01);
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
