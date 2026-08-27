package com.lemonpvp.lemoncosmetics.listeners;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.UUID;

/**
 * Attributes explosions to players and plays their equipped explosion-particle
 * preset (see {@code ExplosionParticleManager}):
 * <ul>
 *   <li>End crystals — the last player to damage the crystal owns the blast
 *       (melee or projectile).</li>
 *   <li>Primed TNT — the entity source, when it is a player.</li>
 * </ul>
 * Purely visual: never modifies the explosion, its damage, or its block list.
 */
public class ExplosionListener implements Listener {

    private final LemonCosmetics plugin;

    public ExplosionListener(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    /** Track who hit an end crystal so its (instant) explosion is attributable. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCrystalHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null) return;
        plugin.getExplosionParticleManager().recordCrystalHit(crystal.getEntityId(), attacker.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        Entity exploded = event.getEntity();
        UUID owner = null;

        if (exploded instanceof EnderCrystal) {
            owner = plugin.getExplosionParticleManager().consumeCrystalOwner(exploded.getEntityId());
        } else if (exploded instanceof TNTPrimed tnt && tnt.getSource() instanceof Player igniter) {
            owner = igniter.getUniqueId();
        }

        if (owner != null) {
            plugin.getExplosionParticleManager().playFor(owner, event.getLocation());
        }
    }

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }
}
