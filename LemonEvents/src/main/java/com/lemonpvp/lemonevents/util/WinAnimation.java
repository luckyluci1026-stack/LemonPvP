package com.lemonpvp.lemonevents.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The "epic" winner celebration shared by every event and tournament: a big
 * VICTORY title, a burst of coloured fireworks and a totem/particle shower
 * around the winner for a few seconds, with sound. Purely cosmetic and capped
 * at ~5 seconds so it never lags the server.
 */
public final class WinAnimation {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Color[] COLORS = {
            Color.YELLOW, Color.LIME, Color.AQUA, Color.FUCHSIA, Color.ORANGE, Color.WHITE
    };

    private WinAnimation() {}

    /** Plays the full celebration on the winner (and anyone nearby sees it too). */
    public static void celebrate(Plugin plugin, Player winner, String eventName) {
        if (winner == null || !winner.isOnline()) return;

        winner.showTitle(Title.title(
                MM.deserialize("<gradient:#fffb00:#ffa000><bold>✦ VICTORY ✦</bold></gradient>"),
                MM.deserialize("<gray>You won <white>" + eventName + "<gray>!"),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3200), Duration.ofMillis(900))));
        winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                Player p = winner.isOnline() ? winner : null;
                if (p == null || ticks >= 100) { cancel(); return; }
                Location loc = p.getLocation();
                // A firework roughly every 8 ticks (~6 over the run).
                if (ticks % 8 == 0) spawnFirework(loc.clone().add(rand(2.5), 0.5, rand(2.5)));
                // Continuous shimmer around the champion.
                p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1.1, 0), 18, 0.5, 0.9, 0.5, 0.25);
                p.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 1.0, 0), 6, 0.4, 0.6, 0.4, 0.02);
                if (ticks % 10 == 0) {
                    p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 2.2, 0), 12, 0.4, 0.3, 0.4, 0.0);
                }
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private static void spawnFirework(Location loc) {
        if (loc.getWorld() == null) return;
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        Color a = COLORS[ThreadLocalRandom.current().nextInt(COLORS.length)];
        Color b = COLORS[ThreadLocalRandom.current().nextInt(COLORS.length)];
        FireworkEffect.Type type = ThreadLocalRandom.current().nextBoolean()
                ? FireworkEffect.Type.BALL_LARGE : FireworkEffect.Type.STAR;
        meta.addEffect(FireworkEffect.builder()
                .withColor(a, b).withFade(Color.WHITE)
                .with(type).trail(true).flicker(true).build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }

    private static double rand(double spread) {
        return ThreadLocalRandom.current().nextDouble(-spread, spread);
    }
}
