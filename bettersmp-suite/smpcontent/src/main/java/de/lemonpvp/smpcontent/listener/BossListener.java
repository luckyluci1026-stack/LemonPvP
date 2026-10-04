package de.lemonpvp.smpcontent.listener;

import de.lemonpvp.smpcontent.SMPContent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTransformEvent;

/** Alles, was ein Boss an Ereignissen braucht: Tod und Verwandlung. */
public final class BossListener implements Listener {

    private final SMPContent plugin;

    public BossListener(SMPContent plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        if (plugin.bosses().isBoss(mob)) {
            plugin.bosses().onDeath(mob, event.getDrops());
        }
    }

    /**
     * Ein Boss verwandelt sich nicht.
     *
     * Sonst wird aus dem Zombie-Boss beim Ertrinken ein gewöhnlicher
     * Ertrunkener - mit seinen Werten, aber ohne Phasen, ohne Leiste und
     * ohne Beute. Dasselbe gilt für Schweine im Gewitter und Spinnen mit
     * Reiter.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTransform(EntityTransformEvent event) {
        if (plugin.bosses().isBoss(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
