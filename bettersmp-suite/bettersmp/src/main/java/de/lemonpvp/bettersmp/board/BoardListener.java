package de.lemonpvp.bettersmp.board;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Richtet das Board eines Spielers beim Join ein und raeumt es beim Quit auf.
 */
public final class BoardListener implements Listener {

    private final BetterSMP plugin;

    public BoardListener(BetterSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Kurz verzoegern, damit LuckPerms-Daten sicher geladen sind
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> {
                    if (event.getPlayer().isOnline()) {
                        plugin.board().setup(event.getPlayer());
                    }
                }, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.board().remove(event.getPlayer());
    }
}
