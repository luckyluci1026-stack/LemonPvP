package com.lemonpvp.lemonlobby.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Spawns rising floating-text holograms using Paper 1.21.4 TextDisplay entities. */
public final class HologramUtil {

    private HologramUtil() {}

    /**
     * Spawns a floating text that rises ~1.2 blocks and removes itself after ~1 second.
     *
     * @param plugin  owning plugin (for scheduler)
     * @param origin  block-center where the text appears (typically block.getLocation().add(0.5,1.5,0.5))
     * @param text    the Adventure component to display
     */
    public static void spawnRising(Plugin plugin, Location origin, Component text) {
        if (origin.getWorld() == null) return;
        TextDisplay display = origin.getWorld().spawn(origin, TextDisplay.class, d -> {
            d.text(text);
            d.setBillboard(Display.Billboard.CENTER);
            d.setShadowed(true);
            d.setDefaultBackground(false);
        });

        // Move up by 0.12 blocks every 2 ticks for 10 steps (~1 second, ~1.2 blocks total)
        new BukkitRunnable() {
            int steps = 0;
            @Override
            public void run() {
                if (!display.isValid() || steps++ >= 10) {
                    if (display.isValid()) display.remove();
                    cancel();
                    return;
                }
                display.teleport(display.getLocation().add(0, 0.12, 0));
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
