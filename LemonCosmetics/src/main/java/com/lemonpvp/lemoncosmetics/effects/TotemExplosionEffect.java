package com.lemonpvp.lemoncosmetics.effects;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

/**
 * Totem Explosion — a divine resurrection burst: an initial flash, radial
 * totem + heart shockwaves, a golden ascending spiral and a glittering sparkle
 * rain, with a triumphant chord.
 */
public final class TotemExplosionEffect {

    private TotemExplosionEffect() {}

    private static final Particle.DustOptions GOLD =
            new Particle.DustOptions(Color.fromRGB(255, 209, 64), 1.1f);

    public static void play(LemonCosmetics plugin, Location loc) {
        final World world = loc.getWorld();
        if (world == null) return;
        final Location base = loc.clone().add(0, 0.1, 0);

        world.spawnParticle(Particle.FLASH, base.clone().add(0, 1.0, 0), 2);
        world.playSound(base, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        world.playSound(base, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);

        final int duration = 40;
        final int[] t = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (loc.getWorld() == null) { task[0].cancel(); return; }
            final int tick = t[0];
            if (tick >= duration) { task[0].cancel(); return; }

            // Radial shockwave of totem + heart particles (first ~14 ticks).
            if (tick < 14) {
                double r = 0.5 + tick * 0.28;
                int pts = 14 + tick * 2;
                for (int i = 0; i < pts; i++) {
                    double a = i * (Math.PI * 2 / pts);
                    double x = Math.cos(a) * r, z = Math.sin(a) * r;
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, base.clone().add(x, 0.2, z), 0, 0, 0, 0, 0);
                    if (i % 4 == 0) world.spawnParticle(Particle.HEART, base.clone().add(x, 0.4, z), 0, 0, 0.05, 0, 0);
                }
            }

            // Golden ascending double spiral.
            double y = tick * 0.12;
            for (int arm = 0; arm < 2; arm++) {
                double a = tick * 0.6 + arm * Math.PI;
                double x = Math.cos(a) * 0.8, z = Math.sin(a) * 0.8;
                world.spawnParticle(Particle.DUST, base.clone().add(x, y, z), 1, 0, 0, 0, 0, GOLD);
                world.spawnParticle(Particle.END_ROD, base.clone().add(x * 0.5, y, z * 0.5), 0, 0, 0.01, 0, 0);
            }

            // Sparkle rain falling around the pillar.
            if (tick % 2 == 0) {
                double a = tick * 1.3;
                double x = Math.cos(a) * 1.4, z = Math.sin(a) * 1.4;
                world.spawnParticle(Particle.END_ROD, base.clone().add(x, 2.4 - y, z), 0, 0, -0.05, 0, 0);
            }

            if (tick == 6) world.playSound(base, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.4f);
            if (tick == 16) world.playSound(base, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
            t[0]++;
        }, 0L, 1L);
    }
}
