package de.lemonpvp.helden.gameevent.impl;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.gameevent.GameEvent;
import de.lemonpvp.helden.util.Compat;
import de.lemonpvp.helden.util.Text;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Blutmond: alle richten mehr Schaden an, die Nacht bricht herein. */
public final class BlutmondEvent extends GameEvent {

    public BlutmondEvent(HeldenPlugin plugin) {
        super(plugin, "blutmond");
    }

    @Override
    public int durationSeconds() {
        return option("duration-seconds", 600);
    }

    public int damageBonusPercent() {
        return option("damage-bonus-percent", 20);
    }

    /** Faktor, mit dem PvP-Schaden waehrend des Events multipliziert wird. */
    public double damageMultiplier() {
        return 1.0 + damageBonusPercent() / 100.0;
    }

    @Override
    public String description() {
        return Text.replace(plugin.messages().raw("events.blutmond-description"),
                "%bonus%", damageBonusPercent());
    }

    @Override
    public boolean onStart() {
        if (option("set-night", true)) {
            for (World world : plugin.getServer().getWorlds()) {
                if (world.getEnvironment() == World.Environment.NORMAL) {
                    world.setTime(18000L);
                }
            }
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Compat.sound(player, "entity.wither.spawn", 0.5f, 0.7f);
        }
        return true;
    }

    @Override
    public void onTick(int secondsElapsed) {
        if (secondsElapsed % 15 != 0) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Compat.spawnParticle(player.getWorld(), Compat.particle("DUST_PLUME", "SMOKE", "SMOKE_NORMAL"),
                    player.getLocation().add(0, 2.4, 0), 6, 0.6, 0.2, 0.6, 0.01);
        }
    }
}
