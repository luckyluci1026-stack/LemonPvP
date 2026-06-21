package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.duel.DuelGame;
import com.lemonpvp.lemonpractice.duel.DuelState;
import com.lemonpvp.lemonpractice.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class DuelListener implements Listener {

    private final LemonPractice plugin;

    public DuelListener(LemonPractice plugin) {
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
        UUID uuid = player.getUniqueId();

        if (plugin.getDuelManager().isInDuel(uuid)) {
            plugin.getDuelManager().handleDeath(uuid);

            // Auto-respawn after 1 tick so the spectator setup in makeSpectator takes effect
            // (setGameMode SPECTATOR on a dead player requires a respawn cycle to apply).
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isDead()) {
                    p.spigot().respawn();
                }
            }, 1L);
        }
    }

    // -------------------------------------------------------------------------
    // Respawn — send spectating players to the arena spectator spawn
    // -------------------------------------------------------------------------

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (plugin.getSpectatorManager().isSpectating(player.getUniqueId())) {
            // The spectator manager holds the spectator location; use it if available.
            // Fall back to the arena spec spawn for the duel the player was in.
            DuelGame game = plugin.getDuelManager().getDuel(player.getUniqueId());
            if (game != null) {
                Arena arena = game.getArena();
                if (arena != null && arena.getSpawnSpec() != null) {
                    event.setRespawnLocation(arena.getSpawnSpec());
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Damage — cancel while spectating or during frozen phases
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // Spectators take no damage at all
        if (plugin.getSpectatorManager().isSpectating(uuid)) {
            event.setCancelled(true);
            return;
        }

        // Players in a duel only take damage while FIGHTING
        if (plugin.getDuelManager().isInDuel(uuid)) {
            DuelGame game = plugin.getDuelManager().getDuel(uuid);
            if (game == null || game.getState() != DuelState.FIGHTING) {
                event.setCancelled(true);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Damage by entity — must be the same duel and in FIGHTING state
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        // Unwrap projectile shooters etc. — for simplicity handle direct Player->Player
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        UUID damagerUuid = damager.getUniqueId();
        UUID victimUuid = victim.getUniqueId();

        // If either player is spectating, cancel
        if (plugin.getSpectatorManager().isSpectating(damagerUuid)
                || plugin.getSpectatorManager().isSpectating(victimUuid)) {
            event.setCancelled(true);
            return;
        }

        DuelGame damagerGame = plugin.getDuelManager().getDuel(damagerUuid);
        DuelGame victimGame = plugin.getDuelManager().getDuel(victimUuid);

        // Both must be in duels and in the SAME duel
        if (damagerGame == null || victimGame == null || damagerGame != victimGame) {
            event.setCancelled(true);
            return;
        }

        // Must be in FIGHTING state
        if (damagerGame.getState() != DuelState.FIGHTING) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Movement — freeze players during WAITING / COUNTDOWN
    // -------------------------------------------------------------------------

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!plugin.getDuelManager().isInDuel(uuid)) {
            return;
        }

        DuelGame game = plugin.getDuelManager().getDuel(uuid);
        if (game == null) {
            return;
        }

        DuelState state = game.getState();
        if (state != DuelState.WAITING && state != DuelState.COUNTDOWN) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        // Allow head rotation but block positional movement
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            // Preserve the look direction but snap position back
            Location corrected = from.clone();
            corrected.setYaw(to.getYaw());
            corrected.setPitch(to.getPitch());
            event.setTo(corrected);
        }
    }

    // -------------------------------------------------------------------------
    // Join
    // -------------------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(null);

        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
    }

    // -------------------------------------------------------------------------
    // Quit — forfeit the duel
    // -------------------------------------------------------------------------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(null);

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (plugin.getDuelManager().isInDuel(uuid)) {
            // Treat disconnect as a loss for the quitting player
            plugin.getDuelManager().handleDeath(uuid);
        }
    }
}
