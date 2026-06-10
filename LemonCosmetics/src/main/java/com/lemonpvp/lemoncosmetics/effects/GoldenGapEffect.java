package com.lemonpvp.lemoncosmetics.effects;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitTask;

public class GoldenGapEffect {

    private GoldenGapEffect() {}

    public static void play(LemonCosmetics plugin, Location loc) {
        var world = loc.getWorld();
        if (world == null) return;
        world.playSound(loc, Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);

        BlockData goldData = Material.GOLD_BLOCK.createBlockData();
        final int[] tick = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (tick[0] >= 20) {
                task[0].cancel();
                return;
            }
            var w = loc.getWorld();
            if (w == null) { task[0].cancel(); return; }
            double radius = 0.3 + tick[0] * 0.09;
            for (int i = 0; i < 6; i++) {
                double a = tick[0] * 0.4 + i * (Math.PI / 3);
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius;
                double y = tick[0] * 0.07;
                w.spawnParticle(Particle.GLOW, loc.clone().add(x, y, z), 1, 0.05, 0.05, 0.05, 0);
                w.spawnParticle(Particle.FALLING_DUST, loc.clone().add(x, y + 0.1, z), 1, 0, 0, 0, 0, goldData);
            }
            tick[0]++;
        }, 0L, 1L);
    }
}
