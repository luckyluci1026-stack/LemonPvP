package com.lemonpvp.lemoncosmetics.listeners;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

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
}
