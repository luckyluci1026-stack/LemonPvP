package com.lemonpvp.lemoncosmetics.effects;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

/**
 * Fire Swarm — a roaring firestorm: twin counter-rotating flame helixes rising
 * from the kill spot, an expanding ground shockwave of flame and lava, ember
 * crackle, and a bright flash finale.
 */
public final class FireSwarmEffect {

    private FireSwarmEffect() {}

    public static void play(LemonCosmetics plugin, Location loc) {
        final World world = loc.getWorld();
        if (world == null) return;
        final Location base = loc.clone().add(0, 0.1, 0);

        world.playSound(base, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.7f);
        world.playSound(base, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.9f);

        final int duration = 46;
        final int[] t = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (loc.getWorld() == null) { task[0].cancel(); return; }
            final int tick = t[0];
            if (tick >= duration) {
                world.spawnParticle(Particle.FLASH, base.clone().add(0, 1.0, 0), 1);
                world.spawnParticle(Particle.LAVA, base.clone().add(0, 0.8, 0), 12, 0.4, 0.4, 0.4, 0);
                world.playSound(base, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.4f);
                task[0].cancel();
                return;
            }

            // Twin rising helixes.
            final double y = tick * 0.10;
            for (int arm = 0; arm < 2; arm++) {
                double a = tick * 0.55 + arm * Math.PI;
                double x = Math.cos(a) * 0.95, z = Math.sin(a) * 0.95;
                world.spawnParticle(Particle.FLAME, base.clone().add(x, y, z), 0, 0, 0.02, 0, 0);
                world.spawnParticle(Particle.SMALL_FLAME, base.clone().add(x * 0.65, y + 0.12, z * 0.65), 0, 0, 0.01, 0, 0);
            }

            // Expanding ground shockwave during the first ~18 ticks.
            if (tick < 18) {
                double r = 0.4 + tick * 0.34;
                int pts = 12 + tick;
                for (int i = 0; i < pts; i++) {
                    double a = i * (Math.PI * 2 / pts);
                    double x = Math.cos(a) * r, z = Math.sin(a) * r;
                    world.spawnParticle(Particle.FLAME, base.clone().add(x, 0.05, z), 0, 0, 0, 0, 0);
                    if (i % 3 == 0) world.spawnParticle(Particle.LAVA, base.clone().add(x, 0.05, z), 0);
                }
            }

            if (tick % 5 == 0) world.playSound(base, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.2f);
            t[0]++;
        }, 0L, 1L);
    }
}
