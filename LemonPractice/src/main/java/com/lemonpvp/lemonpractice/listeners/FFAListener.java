package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.FFAArena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class FFAListener implements Listener {

    private final LemonPractice plugin;

    public FFAListener(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Death
    // -------------------------------------------------------------------------

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.deathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player player = event.getPlayer();

        if (plugin.getFfaManager().getArena(player.getUniqueId()) == null) {
            return;
        }

        Player killer = event.getPlayer().getKiller();
        if (killer != null) {
            plugin.getFfaManager().handleKill(killer, player);
        }

        // Auto-respawn after 3 seconds (60 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.isDead()) {
                player.respawn();
            }
        }, 60L);
    }

    // -------------------------------------------------------------------------
    // Respawn — send the player back to a random arena spawn
    // -------------------------------------------------------------------------

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        FFAArena arena = plugin.getFfaManager().getArena(player.getUniqueId());

        if (arena == null) {
            return;
        }

        Location spawn = arena.getRandomSpawn();
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }
    }

    // -------------------------------------------------------------------------
    // Damage — FFA players may only damage each other; non-arena players are safe
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        // Only care about Player vs Player
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        UUID damagerUuid = damager.getUniqueId();
        UUID victimUuid = victim.getUniqueId();

        // Cancel damage if either player is spectating
        if (plugin.getSpectatorManager().isSpectating(damagerUuid)
                || plugin.getSpectatorManager().isSpectating(victimUuid)) {
            event.setCancelled(true);
            return;
        }

        FFAArena damagerArena = plugin.getFfaManager().getArena(damagerUuid);
        FFAArena victimArena = plugin.getFfaManager().getArena(victimUuid);

        // If one is in FFA and the other is not — cancel to prevent griefing
        if ((damagerArena != null) != (victimArena != null)) {
            event.setCancelled(true);
            return;
        }

        // Both are in FFA — they must be in the SAME arena
        if (damagerArena != null && damagerArena.getId() != victimArena.getId()) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Hunger — no hunger outside of FFA arenas (lobby, spectating, etc.)
    // -------------------------------------------------------------------------

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        boolean inFfa = plugin.getFfaManager().getArena(uuid) != null;
        boolean inDuel = plugin.getDuelManager().isInDuel(uuid);

        // Only drain hunger during an active duel or FFA fight
        if (!inFfa && !inDuel) {
            event.setCancelled(true);
        }
    }
}
