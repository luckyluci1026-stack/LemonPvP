package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.DeathEffectType;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles unlocking, buying, equipping and rendering death effects — particles
 * played at a player's own death location (the counterpart to kill effects).
 */
public class DeathEffectManager {

    private final LemonCosmetics plugin;

    public DeathEffectManager(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Access / unlock / buy
    // -----------------------------------------------------------------------

    /** Whether the player may equip this effect: owned or granted by permission. */
    public boolean canUse(Player player, DeathEffectType effect) {
        if (player.hasPermission(effect.permission())) return true;
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        return cosmetics != null && cosmetics.ownsDeathEffect(effect.id);
    }

    /** Adds the effect to the player's owned set (cache + DB) without equipping it. */
    public CompletableFuture<Void> unlock(UUID uuid, String effectId) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.getOwnedDeathEffects().add(effectId);
        return plugin.getDatabase().saveDeathEffect(uuid, effectId, false);
    }

    /** Buys a death effect with coins. Returns {@code false} if unaffordable. */
    public CompletableFuture<Boolean> buy(UUID uuid, String effectId) {
        DeathEffectType effect = DeathEffectType.fromId(effectId).orElse(null);
        if (effect == null) return CompletableFuture.completedFuture(false);

        return plugin.getCosmeticsManager().canAfford(uuid, effect.price).thenCompose(affordable -> {
            if (!affordable) return CompletableFuture.completedFuture(false);
            var lc = plugin.getCosmeticsManager().getLemonCore();
            if (lc == null) return CompletableFuture.completedFuture(false);
            return lc.getPlayerDataManager()
                    .removeCoins(uuid, effect.price, "cosmetics:deatheffect:" + effectId, null)
                    .thenCompose(v -> unlock(uuid, effectId))
                    .thenApply(v -> true);
        });
    }

    /** Sets (or clears, when {@code effectId} is null) the active death effect. */
    public CompletableFuture<Void> setActive(UUID uuid, String effectId) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.setActiveDeathEffectId(effectId);
        return effectId == null
                ? plugin.getDatabase().clearActiveDeathEffect(uuid)
                : plugin.getDatabase().setActiveDeathEffect(uuid, effectId);
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    /**
     * Plays the victim's active death effect at their death location. No-op if
     * none is equipped or the player isn't loaded.
     */
    public void playEffect(Player victim, Location deathLocation) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(victim.getUniqueId());
        if (cosmetics == null) return;
        DeathEffectType effect = DeathEffectType.fromId(cosmetics.getActiveDeathEffectId()).orElse(null);
        if (effect == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> playCollapse(effect, deathLocation));
    }

    /**
     * Generic death animation — the visual inverse of the kill burst: a ring
     * that collapses inward over ~14 ticks while a thin column rises from the
     * death point, with the effect's sound up front.
     */
    private int pc(int n) { return plugin.particleCount(n); }

    private void playCollapse(DeathEffectType type, Location loc) {
        final World world = loc.getWorld();
        if (world == null || type.particle == null) return;
        if (pc(1) <= 0) return;
        if (type.sound != null) world.playSound(loc, type.sound, 1.0f, 0.9f);

        // Heavy one-shot particles get a compact treatment instead of the full ring.
        if (type.particle == org.bukkit.Particle.SONIC_BOOM) {
            world.spawnParticle(type.particle, loc.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
            return;
        }
        if (type.particle == org.bukkit.Particle.EXPLOSION) {
            Location c = loc.clone().add(0, 0.5, 0);
            world.spawnParticle(type.particle, c, 1, 0, 0, 0, 0);
            for (int i = 0; i < 5; i++) {
                double a = i * (Math.PI * 2 / 5);
                world.spawnParticle(type.particle, c.clone().add(Math.cos(a) * 1.6, 0.2, Math.sin(a) * 1.6),
                        1, 0, 0, 0, 0);
            }
            return;
        }

        final Location base = loc.clone().add(0, 0.3, 0);
        // Opening ground pool + upward gasp so the death reads instantly.
        for (int i = 0; i < 48; i++) {
            double a = i * (Math.PI * 2 / 48);
            world.spawnParticle(type.particle, base.clone().add(Math.cos(a) * 3.6, 0.1, Math.sin(a) * 3.6),
                    pc(3), 0.06, 0.06, 0.06, 0);
        }
        world.spawnParticle(type.particle, base.clone().add(0, 1.0, 0), pc(60), 0.3, 0.9, 0.3, 0.05);

        final int[] t = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (loc.getWorld() == null || t[0] >= 20) { task[0].cancel(); return; }
            int tick = t[0];
            // Ring radius shrinks from ~3.6 to ~0.2 with a spiral drift inward.
            double r = Math.max(0.2, 3.6 - tick * 0.18);
            int pts = 40 - tick;
            for (int i = 0; i < pts; i++) {
                double a = i * (Math.PI * 2 / pts) + tick * 0.28;
                world.spawnParticle(type.particle, base.clone().add(Math.cos(a) * r, 0.1, Math.sin(a) * r),
                        pc(3), 0.04, 0.04, 0.04, 0);
                // Second, counter-rotating strand for density.
                double a2 = -a + tick * 0.15;
                world.spawnParticle(type.particle, base.clone().add(Math.cos(a2) * r * 0.7, 0.1, Math.sin(a2) * r * 0.7),
                        pc(2), 0.03, 0.03, 0.03, 0);
            }
            // Rising soul column from the death point.
            world.spawnParticle(type.particle, base.clone().add(0, 0.4 + tick * 0.22, 0),
                    pc(9), 0.14, 0.16, 0.14, 0.02);
            t[0]++;
        }, 0L, 1L);
    }
}
