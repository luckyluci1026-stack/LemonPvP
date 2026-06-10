package com.lemonpvp.lemoncosmetics.effects;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitTask;

public class SpookSwarmEffect {

    private SpookSwarmEffect() {}

    public static void play(LemonCosmetics plugin, Location loc) {
        if (loc.getWorld() == null) return;
        final int[] tick = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (tick[0] >= 30) {
                task[0].cancel();
                return;
            }
            var world = loc.getWorld();
            if (world == null) { task[0].cancel(); return; }
            double angle = tick[0] * (Math.PI * 2 / 8);
            double radius = 0.4 + tick[0] * 0.08;
            for (int i = 0; i < 8; i++) {
                double a = angle + i * (Math.PI / 4);
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius;
                double y = tick[0] * 0.06;
                Particle particle = (tick[0] % 2 == 0) ? Particle.FLAME : Particle.SOUL_FIRE_FLAME;
                world.spawnParticle(particle, loc.clone().add(x, y, z), 0, 0, 0.04, 0, 0);
            }
            tick[0]++;
        }, 0L, 1L);
    }
}
