package com.lemonpvp.lemonevents.listeners;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.model.HostedEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

/**
 * Bridges Bukkit events into the {@link com.lemonpvp.lemonevents.managers.HostedEventManager}:
 * turns deaths into eliminations, protects players during the pre-fight freeze,
 * and forfeits players who disconnect mid-event.
 */
public class HostedEventListener implements Listener {

    private final LemonEvents plugin;

    public HostedEventListener(LemonEvents plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        HostedEvent ev = plugin.getHostedEventManager().getActive();
        if (ev == null || ev.getState() != HostedEvent.State.RUNNING) return;
        if (!ev.getAlive().contains(player.getUniqueId())) return;
        // No damage while the countdown freeze is active.
        if (ev.isFrozen()) event.setCancelled(true);
    }

    /**
     * In Host Battle, challengers can't hurt each other (only the host), and the
     * last challenger to hit the host is remembered so the kill can be credited.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        HostedEvent ev = plugin.getHostedEventManager().getActive();
        if (ev == null || ev.getState() != HostedEvent.State.RUNNING) return;
        if (ev.getMode() != HostedEvent.Mode.HOST_BATTLE) return;
        if (!ev.getAlive().contains(victim.getUniqueId())) return;

        Player damager = resolveDamager(event.getDamager());
        if (damager == null || !ev.getAlive().contains(damager.getUniqueId())) return;

        // Two sides: the host's team (host + chosen teammates) and the challengers.
        boolean victimHostSide = ev.isHostSide(victim.getUniqueId());
        boolean damagerHostSide = ev.isHostSide(damager.getUniqueId());
        if (victimHostSide == damagerHostSide) {
            event.setCancelled(true); // no friendly fire within a side
        } else if (victimHostSide) {
            ev.setLastHostDamager(damager.getUniqueId()); // credit the finishing blow
        }
    }

    private Player resolveDamager(org.bukkit.entity.Entity entity) {
        if (entity instanceof Player p) return p;
        if (entity instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) return p;
        }
        return null;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        HostedEvent ev = plugin.getHostedEventManager().getActive();
        if (ev == null || !ev.getAlive().contains(uuid)) return;

        // No item scatter inside the arena; clear the death screen quickly.
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.deathMessage(null);
        plugin.getHostedEventManager().onDeath(uuid);

        // Auto-respawn so the spectator gamemode set in onDeath takes effect.
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null && p.isDead()) p.spigot().respawn();
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getHostedEventManager().handleQuit(event.getPlayer().getUniqueId());
    }

    /**
     * A player sent here from another server after clicking JOIN: consume the
     * cross-server handoff marker and add them to the open hosted event.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getDatabase().takePendingEventJoin(uuid).thenAccept(wanted -> {
            if (!Boolean.TRUE.equals(wanted)) return;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) plugin.getHostedEventManager().join(p);
            }, 20L);
        });
    }
}
