package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles death / respawn / quit for Zone Practice participants:
 * eliminates them from the round and returns them to the lobby with the
 * lobby hotbar restored.
 */
public class ZoneListener implements Listener {

    private final LemonPractice plugin;

    /**
     * Players who died IN a zone round and are awaiting respawn so we can return
     * them to the lobby. Needed because by respawn time they've already been
     * removed from the session (so isInZone would be false) — without this set
     * we'd wrongly teleport every normally-respawning player (duels, FFA, …) to
     * the lobby and wipe their inventory.
     */
    private final Set<UUID> pendingReturn = ConcurrentHashMap.newKeySet();

    public ZoneListener(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Death — eliminate the participant
    // ------------------------------------------------------------------

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Only act on players who are actually in a zone round.
        if (!plugin.getZonePracticeManager().isInZone(uuid)) {
            return;
        }

        event.deathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Mark for lobby return on respawn, then eliminate (this may end the round).
        pendingReturn.add(uuid);
        plugin.getZonePracticeManager().eliminate(uuid);

        // Auto-respawn after a short delay (mirrors the FFA flow).
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isDead()) {
                p.spigot().respawn();
            }
        }, 20L);
    }

    // ------------------------------------------------------------------
    // Respawn — send the eliminated zone player back to the lobby
    // ------------------------------------------------------------------

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // Only handle players who died inside a zone round.
        if (!pendingReturn.remove(uuid)) {
            return;
        }
        // Restore lobby state one tick after respawn so location/inventory settle.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) return;
            if (plugin.getZonePracticeManager().isInZone(uuid)) return; // re-joined meanwhile
            plugin.getZonePracticeManager().returnToLobby(p);
        }, 1L);
    }

    // ------------------------------------------------------------------
    // Quit — remove from the round
    // ------------------------------------------------------------------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pendingReturn.remove(uuid);
        if (plugin.getZonePracticeManager().isInZone(uuid)) {
            plugin.getZonePracticeManager().eliminate(uuid);
        }
    }
}
