package de.lemonpvp.helden.listener;

import de.lemonpvp.helden.HeldenPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;

/** Alles rund um die Combat-Log-Puppe. */
public final class DummyListener implements Listener {

    private final HeldenPlugin plugin;

    public DummyListener(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!plugin.dummies().isDummy(event.getEntity())) {
            return;
        }

        Location location = event.getEntity().getLocation();
        Player killer = resolveKiller(event.getEntity());

        // Vanilla-Drops raus: die Ausruestung haengt schon an der Puppe, das
        // echte Inventar werfen wir gleich selbst aus.
        event.getDrops().clear();
        event.setDroppedExp(0);

        List<ItemStack> loot = plugin.dummies().handleDeath(event.getEntity(), killer);
        if (location.getWorld() != null) {
            for (ItemStack stack : loot) {
                location.getWorld().dropItemNaturally(location, stack);
            }
        }
    }

    /** Mobs sollen die Puppe in Ruhe lassen. */
    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (plugin.dummies().isDummy(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    private Player resolveKiller(Entity entity) {
        if (!(entity instanceof org.bukkit.entity.LivingEntity living)) {
            return null;
        }
        Player killer = living.getKiller();
        if (killer != null) {
            return killer;
        }
        if (entity.getLastDamageCause() instanceof org.bukkit.event.entity.EntityDamageByEntityEvent cause) {
            if (cause.getDamager() instanceof Player player) {
                return player;
            }
            if (cause.getDamager() instanceof Projectile projectile) {
                ProjectileSource source = projectile.getShooter();
                if (source instanceof Player shooter) {
                    return shooter;
                }
            }
        }
        return null;
    }
}
