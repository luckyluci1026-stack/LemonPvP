package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.duel.DuelState;
import com.lemonpvp.lemonpractice.duel.TeamDuelGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

/**
 * Bridges Bukkit events into 2v2 team duels: deaths become eliminations, quits
 * forfeit, no damage outside FIGHTING, and friendly fire is blocked.
 */
public class TeamDuelListener implements Listener {

    private final LemonPractice plugin;

    public TeamDuelListener(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!plugin.getTeamDuelManager().isInTeamDuel(uuid)) return;
        event.deathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);
        plugin.getTeamDuelManager().handleDeath(uuid);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isDead()) p.spigot().respawn();
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getTeamDuelManager().leaveQueue(uuid);
        if (plugin.getTeamDuelManager().isInTeamDuel(uuid)) {
            plugin.getTeamDuelManager().handleDeath(uuid);
        }
    }

    /** Team-duel players only take damage while FIGHTING. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        TeamDuelGame game = plugin.getTeamDuelManager().getGame(player.getUniqueId());
        if (game != null && game.getState() != DuelState.FIGHTING) {
            event.setCancelled(true);
        }
    }

    /** Blocks friendly fire and cross-match hits for team duels. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player damager = resolveDamager(event.getDamager());
        if (damager == null) return;

        TeamDuelGame victimGame = plugin.getTeamDuelManager().getGame(victim.getUniqueId());
        TeamDuelGame damagerGame = plugin.getTeamDuelManager().getGame(damager.getUniqueId());
        if (victimGame == null && damagerGame == null) return;

        // Only same-match, opposing-team, FIGHTING-state hits are allowed.
        if (victimGame != damagerGame
                || victimGame.getState() != DuelState.FIGHTING
                || victimGame.sameTeam(victim.getUniqueId(), damager.getUniqueId())) {
            event.setCancelled(true);
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
}
