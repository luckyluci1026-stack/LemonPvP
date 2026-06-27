package com.lemonpvp.lemoncosmetics.effects;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitTask;

/**
 * Golden Gap — an opulent golden showcase: a fountain of glow and gold dust,
 * expanding golden rings, floating sparkles and a satisfying shimmer. Keeps the
 * original cheeky burp as a wink, layered with bell and beacon tones.
 */
public final class GoldenGapEffect {

    private GoldenGapEffect() {}

    private static final Particle.DustOptions GOLD =
            new Particle.DustOptions(Color.fromRGB(255, 196, 0), 1.2f);

    public static void play(LemonCosmetics plugin, Location loc) {
        final World world = loc.getWorld();
        if (world == null) return;
        final Location base = loc.clone().add(0, 0.1, 0);
        final BlockData goldData = Material.GOLD_BLOCK.createBlockData();

        world.playSound(base, Sound.ENTITY_PLAYER_BURP, 0.7f, 1.0f);
        world.playSound(base, Sound.BLOCK_BELL_USE, 0.8f, 1.4f);
        world.playSound(base, Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.6f);

        final int duration = 42;
        final int[] t = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (loc.getWorld() == null) { task[0].cancel(); return; }
            final int tick = t[0];
            if (tick >= duration) { task[0].cancel(); return; }

            // Rising fountain of glow + gold falling dust.
            double y = tick * 0.10;
            for (int arm = 0; arm < 3; arm++) {
                double a = tick * 0.5 + arm * (Math.PI * 2 / 3);
                double x = Math.cos(a) * 0.7, z = Math.sin(a) * 0.7;
                world.spawnParticle(Particle.GLOW, base.clone().add(x, y, z), 0, 0, 0.02, 0, 0);
                world.spawnParticle(Particle.FALLING_DUST, base.clone().add(x, y + 0.5, z), 1, 0, 0, 0, 0, goldData);
            }

            // Expanding golden rings (first ~16 ticks).
            if (tick < 16) {
                double r = 0.4 + tick * 0.28;
                int pts = 12 + tick;
                for (int i = 0; i < pts; i++) {
                    double a = i * (Math.PI * 2 / pts);
                    double x = Math.cos(a) * r, z = Math.sin(a) * r;
                    world.spawnParticle(Particle.DUST, base.clone().add(x, 0.1, z), 1, 0, 0, 0, 0, GOLD);
                }
            }

            // Floating wealth sparkles.
            if (tick % 3 == 0) {
                double a = tick * 1.1;
                double x = Math.cos(a) * 1.1, z = Math.sin(a) * 1.1;
                world.spawnParticle(Particle.END_ROD, base.clone().add(x, 0.8 + y * 0.3, z), 0, 0, 0.01, 0, 0);
            }

            if (tick == 10) world.playSound(base, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.5f);
            t[0]++;
        }, 0L, 1L);
    }
}
