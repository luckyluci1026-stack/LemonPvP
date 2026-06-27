package com.lemonpvp.lemoncosmetics.effects;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

/**
 * Spook Swarm — a haunting soul vortex: soul-fire flames spiral upward into a
 * ghostly column, drifting smoke wisps peel off, and an eerie sonic crescendo
 * closes it out.
 */
public final class SpookSwarmEffect {

    private SpookSwarmEffect() {}

    public static void play(LemonCosmetics plugin, Location loc) {
        final World world = loc.getWorld();
        if (world == null) return;
        final Location base = loc.clone().add(0, 0.1, 0);

        world.playSound(base, Sound.PARTICLE_SOUL_ESCAPE, 1.0f, 0.6f);
        world.playSound(base, Sound.ENTITY_VEX_CHARGE, 0.7f, 0.7f);

        final int duration = 50;
        final int[] t = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (loc.getWorld() == null) { task[0].cancel(); return; }
            final int tick = t[0];
            if (tick >= duration) {
                world.spawnParticle(Particle.SONIC_BOOM, base.clone().add(0, 1.0, 0), 1);
                world.playSound(base, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 1.6f);
                task[0].cancel();
                return;
            }

            // Tightening upward vortex (radius shrinks as it rises).
            final double y = tick * 0.10;
            final double rad = Math.max(0.15, 1.2 - tick * 0.02);
            for (int arm = 0; arm < 3; arm++) {
                double a = tick * 0.6 + arm * (Math.PI * 2 / 3);
                double x = Math.cos(a) * rad, z = Math.sin(a) * rad;
                Particle p = (arm == 0) ? Particle.SOUL_FIRE_FLAME : Particle.SOUL;
                world.spawnParticle(p, base.clone().add(x, y, z), 0, 0, 0.01, 0, 0);
            }

            // Drifting wisps peel outward every few ticks.
            if (tick % 4 == 0) {
                double a = tick * 0.9;
                double x = Math.cos(a) * (rad + 0.6), z = Math.sin(a) * (rad + 0.6);
                world.spawnParticle(Particle.LARGE_SMOKE, base.clone().add(x, y + 0.2, z), 0, 0, 0.02, 0, 0);
            }

            if (tick % 8 == 0) world.playSound(base, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.6f, 0.8f);
            t[0]++;
        }, 0L, 1L);
    }
}
