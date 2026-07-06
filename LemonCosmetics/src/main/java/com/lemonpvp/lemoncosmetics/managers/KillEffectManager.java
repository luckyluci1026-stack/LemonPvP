package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.effects.FireSwarmEffect;
import com.lemonpvp.lemoncosmetics.effects.GoldenGapEffect;
import com.lemonpvp.lemoncosmetics.effects.SpookSwarmEffect;
import com.lemonpvp.lemoncosmetics.effects.TotemExplosionEffect;
import com.lemonpvp.lemoncosmetics.model.KillEffectType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class KillEffectManager {

    private final LemonCosmetics plugin;

    public KillEffectManager(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    /**
     * Plays the active kill effect for {@code killer} at {@code killLocation}.
     * The effect is dispatched asynchronously. If the killer has no active
     * effect this method is a no-op.
     *
     * @param killer       the player who scored the kill
     * @param killLocation the location where the kill occurred
     */
    public void playEffect(Player killer, Location killLocation) {
        KillEffectType effectType = plugin.getCosmeticsManager()
                .getActiveKillEffect(killer.getUniqueId());
        if (effectType == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (effectType) {
                case FIRE_SWARM      -> FireSwarmEffect.play(plugin, killLocation);
                case SPOOK_SWARM     -> SpookSwarmEffect.play(plugin, killLocation);
                case TOTEM_EXPLOSION -> TotemExplosionEffect.play(plugin, killLocation);
                case GOLDEN_GAP      -> GoldenGapEffect.play(plugin, killLocation);
                default              -> playGeneric(effectType, killLocation);
            }
        });
    }

    /**
     * Generic, data-driven kill burst: an expanding particle ring plus a rising
     * column over ~12 ticks, with the effect's sound up front. Used by every
     * effect that doesn't have a bespoke animation class.
     */
    private void playGeneric(KillEffectType type, Location loc) {
        final World world = loc.getWorld();
        if (world == null || type.particle == null) return;
        if (type.sound != null) world.playSound(loc, type.sound, 1.0f, 1.0f);

        // SONIC_BOOM is a huge one-shot particle — a ring of them would be absurd.
        if (type.particle == org.bukkit.Particle.SONIC_BOOM) {
            world.spawnParticle(type.particle, loc.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
            return;
        }

        final Location base = loc.clone().add(0, 0.3, 0);
        // Opening flash: a dense pop so the kill reads instantly.
        world.spawnParticle(type.particle, base.clone().add(0, 0.6, 0), 40, 0.5, 0.6, 0.5, 0.05);

        final int[] t = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (loc.getWorld() == null || t[0] >= 14) { task[0].cancel(); return; }
            int tick = t[0];
            // Two concentric expanding rings for a fuller shockwave.
            double r = 0.5 + tick * 0.34;
            double r2 = r * 0.62;
            int pts = 18 + tick * 2;
            for (int i = 0; i < pts; i++) {
                double a = i * (Math.PI * 2 / pts);
                world.spawnParticle(type.particle,
                        base.clone().add(Math.cos(a) * r, 0.05, Math.sin(a) * r), 2, 0.04, 0.04, 0.04, 0.01);
                if (i % 2 == 0) {
                    world.spawnParticle(type.particle,
                            base.clone().add(Math.cos(a) * r2, 0.35, Math.sin(a) * r2), 1, 0.03, 0.03, 0.03, 0);
                }
            }
            // Rising twinkling column.
            world.spawnParticle(type.particle, base.clone().add(0, tick * 0.16, 0), 6, 0.16, 0.16, 0.16, 0.02);
            t[0]++;
        }, 0L, 1L);
    }
}
