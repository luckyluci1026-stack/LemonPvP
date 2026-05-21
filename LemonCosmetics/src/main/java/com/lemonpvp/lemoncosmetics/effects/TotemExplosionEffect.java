package com.lemonpvp.lemoncosmetics.effects;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitTask;

public class TotemExplosionEffect {

    private TotemExplosionEffect() {}

    public static void play(LemonCosmetics plugin, Location loc) {
        // Play sound on main thread then particles async
        plugin.getServer().getScheduler().runTask(plugin, () ->
                loc.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f));

        final int[] tick = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (tick[0] >= 10) {
                task[0].cancel();
                return;
            }
            double radius = 0.5 + tick[0] * 0.25;
            int count = 12 + tick[0] * 3;
            for (int i = 0; i < count; i++) {
                double a = i * (Math.PI * 2 / count);
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius;
                double y = tick[0] * 0.1;
                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(x, y, z), 1, 0, 0, 0, 0);
                loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(x, y + 0.2, z), 0, 0, 0.1, 0, 0);
                loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(x * 0.5, y, z * 0.5), 0, 0, 0.05, 0, 0);
            }
            tick[0]++;
        }, 0L, 1L);
    }
}
