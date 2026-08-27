package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.database.PracticeDatabase.PendingDuel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * On the duels server, claims pending direct duels (from {@code /duel} accepts)
 * when both challenger and challenged have arrived. The row is claimed
 * atomically so exactly one of the two joins actually starts the match.
 */
public class DuelInviteListener implements Listener {

    private final LemonPractice plugin;

    public DuelInviteListener(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getServerType().equals("DUELS")) return;

        UUID uuid = event.getPlayer().getUniqueId();
        // Small delay so the player is fully connected before we try to start.
        Bukkit.getScheduler().runTaskLater(plugin, () -> tryStartPendingDuel(uuid), 20L);
    }

    private void tryStartPendingDuel(UUID uuid) {
        plugin.getDatabase().findPendingDuel(uuid).thenAccept(pending -> {
            if (pending == null) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                UUID partner = pending.player1().equals(uuid) ? pending.player2() : pending.player1();
                Player partnerPlayer = Bukkit.getPlayer(partner);
                Player self = Bukkit.getPlayer(uuid);

                // Wait until both participants are present on this server; the
                // second to arrive triggers the claim.
                if (self == null || !self.isOnline() || partnerPlayer == null || !partnerPlayer.isOnline()) {
                    return;
                }

                plugin.getDatabase().deletePendingDuel(pending.id()).thenAccept(won -> {
                    if (!Boolean.TRUE.equals(won)) return; // another join already claimed it
                    Bukkit.getScheduler().runTask(plugin, () ->
                            plugin.getDuelManager().startDuel(
                                    pending.player1(), pending.player2(), pending.gamemode()));
                });
            });
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Drop any invites involving the leaving player so stale entries don't linger.
        if (plugin.getDuelInviteManager() != null) {
            plugin.getDuelInviteManager().clearInvitesFor(event.getPlayer().getUniqueId());
        }
    }
}
