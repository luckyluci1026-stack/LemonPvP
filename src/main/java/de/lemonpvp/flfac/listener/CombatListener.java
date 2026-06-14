package de.lemonpvp.flfac.listener;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.CheckManager;
import de.lemonpvp.flfac.data.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

/** Drives the combat checks (reach, hitbox, killaura, autoclicker, velocity). */
public final class CombatListener implements Listener {

    private final FLFAC plugin;

    public CombatListener(FLFAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        Entity target = event.getEntity();
        if (target.equals(attacker)) {
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getOrCreate(attacker);
        CheckManager checks = plugin.getCheckManager();

        data.setLastAttackTime(System.currentTimeMillis());
        checks.autoClicker().recordClick(data);
        checks.killAura().handleAttack(data, attacker, target);

        if (plugin.shouldRunHeavyChecks()) {
            checks.reach().handleAttack(data, attacker, target);
            checks.hitbox().handleAttack(data, attacker, target);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        PlayerData data = plugin.getPlayerDataManager().getOrCreate(event.getPlayer());
        plugin.getCheckManager().autoClicker().recordClick(data);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVelocity(PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
        Vector velocity = event.getVelocity();
        plugin.getCheckManager().velocity().onVelocity(data, velocity.getX(), velocity.getY(), velocity.getZ());
        // Grace period so movement checks don't false-flag knockback.
        data.setVelocityGraceUntil(System.currentTimeMillis() + 1000L);
    }
}
