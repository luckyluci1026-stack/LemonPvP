package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

/**
 * Completes the lobby → duels queue handoff: when a player picked a gamemode
 * in the lobby's play menu, the marker written to lp_pending_queue is consumed
 * here and the player is auto-queued the moment they arrive — one click in the
 * lobby, fighting within seconds (bot fallback included).
 */
public class PendingQueueListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;

    public PendingQueueListener(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getDatabase().getPendingQueueAndDelete(uuid).thenAccept(gamemode -> {
            if (gamemode == null) return;
            // Small delay so join handling (hotbar, teleports) settles first.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) return;
                plugin.getQueueManager().addToQueue(uuid, gamemode);
                p.sendMessage(MM.deserialize("<!italic><gradient:#fffb00:#00ff00><bold>Queue</bold></gradient> "
                        + "<gray>Searching for a <yellow>" + gamemode + " <gray>opponent..."));
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.5f);
            }, 20L);
        });
    }
}
