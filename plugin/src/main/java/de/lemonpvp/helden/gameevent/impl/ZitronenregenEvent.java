package de.lemonpvp.helden.gameevent.impl;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.gameevent.GameEvent;
import de.lemonpvp.helden.util.Compat;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/** Zitronenregen: am Spawn fallen Zitronen vom Himmel. */
public final class ZitronenregenEvent extends GameEvent {

    public ZitronenregenEvent(HeldenPlugin plugin) {
        super(plugin, "zitronenregen");
    }

    @Override
    public int durationSeconds() {
        return option("duration-seconds", 180);
    }

    @Override
    public boolean onStart() {
        return plugin.settings().spawnOrDefault() != null;
    }

    @Override
    public void onTick(int secondsElapsed) {
        int interval = Math.max(1, option("drop-interval-seconds", 10));
        if (secondsElapsed == 0 || secondsElapsed % interval != 0) {
            return;
        }

        Location spawn = plugin.settings().spawnOrDefault();
        if (spawn == null || spawn.getWorld() == null) {
            return;
        }

        int radius = Math.max(1, option("radius", 12));
        int amount = Math.max(1, option("zitronen-per-drop", 3));
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < amount; i++) {
            Location drop = spawn.clone().add(
                    random.nextDouble(-radius, radius),
                    8.0,
                    random.nextDouble(-radius, radius));
            ItemStack stack = plugin.items().stack(plugin.settings().currencyTokenItemId(), 1);
            if (stack == null) {
                return;
            }
            drop.getWorld().dropItem(drop, stack);
            Compat.spawnParticle(drop.getWorld(), Compat.particle("END_ROD"), drop, 6, 0.2, 0.2, 0.2, 0.01);
        }
        Compat.sound(spawn, "entity.player.levelup", 0.6f, 1.8f);
    }
}
