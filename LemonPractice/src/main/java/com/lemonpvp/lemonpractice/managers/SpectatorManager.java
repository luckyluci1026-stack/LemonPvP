package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpectatorManager {

    private final LemonPractice plugin;
    private final Set<UUID> spectators = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public SpectatorManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Make spectator
    // -----------------------------------------------------------------------

    public void makeSpectator(Player player, Location specSpawn) {
        if (player == null || !player.isOnline()) return;

        player.setGameMode(GameMode.SPECTATOR);

        // Only teleport living players. Dead players are handled by the onRespawn
        // listener which sets the respawn location before they come back to life.
        if (specSpawn != null && !player.isDead()) {
            player.teleport(specSpawn);
        }

        spectators.add(player.getUniqueId());
        plugin.getLogger().fine("[SpectatorManager] " + player.getName() + " is now spectating.");
    }

    // -----------------------------------------------------------------------
    // Remove spectator
    // -----------------------------------------------------------------------

    public void removeSpectator(Player player) {
        if (player == null) return;

        spectators.remove(player.getUniqueId());

        if (player.isOnline() && player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.ADVENTURE);
        }

        plugin.getLogger().fine("[SpectatorManager] " + player.getName() + " removed from spectator.");
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    public boolean isSpectating(UUID playerUuid) {
        return spectators.contains(playerUuid);
    }

    public Set<UUID> getAllSpectators() {
        return Collections.unmodifiableSet(spectators);
    }
}
