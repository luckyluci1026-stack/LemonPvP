package com.lemonpvp.lemoncosmetics.listeners;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

public class KillListener implements Listener {

    private final LemonCosmetics plugin;

    public KillListener(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Victim's own death effect (players only), regardless of the cause.
        if (event.getEntity() instanceof Player victim) {
            plugin.getDeathEffectManager().playEffect(victim, victim.getLocation());
        }
        // Killer's kill effect.
        if (!(event.getEntity().getKiller() instanceof Player killer)) return;
        plugin.getKillEffectManager().playEffect(killer, event.getEntity().getLocation());
    }

    /** Win-effect celebration fireworks are purely visual — cancel their blast damage. */
    @EventHandler(ignoreCancelled = true)
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Firework fw)) return;
        if (fw.getPersistentDataContainer().has(
                plugin.getWinEffectManager().getFireworkKey(), PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }
}
