package com.lemonpvp.lemoncosmetics.effects;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitTask;

public class FireSwarmEffect {

    private FireSwarmEffect() {}

    public static void play(LemonCosmetics plugin, Location loc) {
        final int[] tick = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (tick[0] >= 30) {
                task[0].cancel();
                return;
            }
            double angle = tick[0] * (Math.PI * 2 / 10);
            double radius = 0.3 + tick[0] * 0.07;
            for (int i = 0; i < 8; i++) {
                double a = angle + i * (Math.PI / 4);
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius;
                double y = tick[0] * 0.05;
                loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(x, y, z), 0, 0, 0.05, 0, 0);
                loc.getWorld().spawnParticle(Particle.SMALL_FLAME, loc.clone().add(x, y + 0.15, z), 0, 0, 0.02, 0, 0);
            }
            tick[0]++;
        }, 0L, 1L);
    }
}
