package de.lemonpvp.helden.listener;

import de.lemonpvp.helden.HeldenPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/** Spawnschutz, Combat-Tag und die Sonderregeln fuer Dummies. */
public final class CombatListener implements Listener {

    private final HeldenPlugin plugin;

    public CombatListener(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());

        // Dummies duerfen wahlweise nur von Spielern erledigt werden, sonst
        // raeumt sie der naechstbeste Zombie ab.
        if (plugin.dummies().isDummy(event.getEntity())) {
            if (plugin.settings().dummyPlayersOnly() && attacker == null) {
                event.setCancelled(true);
            }
            return;
        }

        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        // Ausgeschiedene Spieler sind aus dem Spiel - kein Nachtreten.
        if (plugin.game().isEliminated(plugin.profiles().getOrCreate(victim))) {
            event.setCancelled(true);
            return;
        }

        if (attacker != null && !attacker.equals(victim)) {
            if (plugin.combat().isProtected(victim)) {
                event.setCancelled(true);
                plugin.messages().send(attacker, "combat.protected-target", "%player%", victim.getName());
                return;
            }
            // Wer selbst zuschlaegt, gibt seinen Spawnschutz auf.
            plugin.combat().clearProtection(attacker);
            plugin.combat().tag(victim, attacker);
        }
    }

    /** Loest Pfeile, Wurftraenke und Co. auf den schiessenden Spieler auf. */
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
