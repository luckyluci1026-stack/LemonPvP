package de.lemonpvp.helden.listener;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.hero.Hero;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/** Regelt Teamschutz, Spawnschutz, Heldenwerte und den Combat-Tag. */
public final class CombatListener implements Listener {

    private final HeldenPlugin plugin;

    public CombatListener(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player victim = event.getEntity() instanceof Player player ? player : null;
        boolean fromProjectile = event.getDamager() instanceof Projectile;
        Player attacker = resolveAttacker(event.getDamager());

        if (victim != null && plugin.lives().isFallen(victim)) {
            event.setCancelled(true);
            return;
        }

        if (victim != null && attacker != null && !attacker.equals(victim)) {
            if (!plugin.settings().teamFriendlyFire() && plugin.teams().sameTeam(attacker, victim)) {
                event.setCancelled(true);
                plugin.messages().send(attacker, "team.friendly-fire", "%player%", victim.getName());
                return;
            }
            if (plugin.combat().isProtected(victim)) {
                event.setCancelled(true);
                plugin.messages().send(attacker, "combat.protected-target", "%player%", victim.getName());
                return;
            }
            // Wer zuschlaegt, gibt seinen eigenen Spawnschutz auf.
            plugin.combat().clearProtection(attacker);
        }

        event.setDamage(event.getDamage() * damageFactor(attacker, victim, fromProjectile));

        if (victim != null && attacker != null && !attacker.equals(victim)) {
            plugin.combat().tag(victim, attacker);
        }
    }

    private double damageFactor(Player attacker, Player victim, boolean fromProjectile) {
        double factor = 1.0;

        if (attacker != null) {
            Hero hero = plugin.heroes().of(attacker);
            if (hero != null) {
                factor *= hero.damageDealtMultiplier();
                if (fromProjectile && hero.projectileDamagePercent() != 0.0) {
                    factor *= 1.0 + hero.projectileDamagePercent() / 100.0;
                }
            }
        }
        if (victim != null) {
            Hero hero = plugin.heroes().of(victim);
            if (hero != null) {
                factor *= hero.damageTakenMultiplier();
            }
        }
        if (attacker != null && victim != null) {
            factor *= plugin.events().pvpDamageMultiplier();
        }
        return factor;
    }

    /** Loest Pfeile, Feuerbaelle und Co. auf den schiessenden Spieler auf. */
    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player shooter) {
                return shooter;
            }
        }
        return null;
    }
}
