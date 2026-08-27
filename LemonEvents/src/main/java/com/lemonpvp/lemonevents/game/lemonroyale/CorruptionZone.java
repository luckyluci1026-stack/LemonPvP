package com.lemonpvp.lemonevents.game.lemonroyale;

import com.lemonpvp.lemonevents.LemonEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;

public class CorruptionZone {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonEvents plugin;
    private final World world;
    private final Location center;
    private final Set<UUID> participants;

    private double currentRadius;
    private final double minRadius;
    private final double shrinkRate; // blocks per tick
    private BukkitTask damageTask;
    private BukkitTask shrinkTask;

    public CorruptionZone(LemonEvents plugin, World world, Location center,
                          double initialRadius, double minRadius, double shrinkRate) {
        this.plugin = plugin;
        this.world = world;
        this.center = center;
        this.currentRadius = initialRadius;
        this.minRadius = minRadius;
        this.shrinkRate = shrinkRate;
        this.participants = java.util.Collections.emptySet();
    }

    public CorruptionZone(LemonEvents plugin, World world, Location center,
                          double initialRadius, double minRadius, double shrinkRate,
                          Set<UUID> participants) {
        this.plugin = plugin;
        this.world = world;
        this.center = center;
        this.currentRadius = initialRadius;
        this.minRadius = minRadius;
        this.shrinkRate = shrinkRate;
        this.participants = participants;
    }

    public void start(long delayTicks) {
        // Damage players outside zone every 20 ticks (1 second)
        damageTask = Bukkit.getScheduler().runTaskTimer(plugin, this::applyDamage, delayTicks, 20L);
        // Shrink the zone every 10 ticks
        shrinkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::shrink, delayTicks, 10L);
    }

    public void stop() {
        if (damageTask != null) damageTask.cancel();
        if (shrinkTask != null) shrinkTask.cancel();
    }

    private void applyDamage() {
        // Snapshot: player.damage() below can be lethal and fire PlayerDeathEvent
        // synchronously, which re-enters the game's eliminate() and removes the
        // player from this same shared participants set mid-iteration (CME).
        for (UUID uuid : new java.util.ArrayList<>(participants)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!isInside(player.getLocation())) {
                // 1 heart = 2 damage per second
                player.damage(2.0);
                player.sendActionBar(MM.deserialize(
                    "<red><bold>⚠ You are outside The Corruption! ⚠</bold></red>"));
            }
        }
    }

    private void shrink() {
        if (currentRadius > minRadius) {
            currentRadius = Math.max(minRadius, currentRadius - shrinkRate);
        }
    }

    public boolean isInside(Location location) {
        if (!location.getWorld().equals(world)) return false;
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        return Math.sqrt(dx * dx + dz * dz) <= currentRadius;
    }

    public double getCurrentRadius() { return currentRadius; }
    public Location getCenter() { return center; }
}
