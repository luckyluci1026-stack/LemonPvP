package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.managers.LobbyAmbienceManager;
import com.lemonpvp.lemonpractice.model.Gamemode;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Turns the lobby's portal arches into real queue entrances: walk into one and you join that
 * gamemode's queue.
 *
 * <p>Deliberately routed through the same checks the queue menu uses (gamemode must exist and be
 * enabled, then {@code QueueManager}), so both entry points behave identically.
 *
 * <p>Movement events fire several times a tick per player, so the handler bails out immediately
 * unless the player actually changed block, and a short per-player cooldown stops standing inside
 * an arch from re-triggering.
 */
public class LobbyPortalListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** How close to the arch centre counts as "inside" it. */
    private static final double RADIUS = 2.6;
    private static final double RADIUS_SQ = RADIUS * RADIUS;
    private static final double MAX_Y_DELTA = 4.0;

    private static final long COOLDOWN_MS = 3000L;

    private final LemonPractice plugin;
    private final Map<UUID, Long> lastTrigger = new HashMap<>();

    public LobbyPortalListener(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) return;
        Location from = event.getFrom();
        // Cheapest possible early-out: most move events are sub-block camera/position jitter.
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        if (!"LOBBY".equals(plugin.getServerType())) return;

        LobbyAmbienceManager ambience = plugin.getLobbyAmbienceManager();
        if (ambience == null || !ambience.isEnabled()) return;
        if (!plugin.getConfig().getBoolean("lobby.portals.walk-in", true)) return;

        Player player = event.getPlayer();
        LobbyAmbienceManager.PortalSpot spot = ambience.portalAt(to, RADIUS_SQ, MAX_Y_DELTA);
        if (spot == null) return;

        String gamemodeId = spot.gamemode();
        if (gamemodeId == null || gamemodeId.isBlank()) return; // a purely decorative arch

        long now = System.currentTimeMillis();
        Long last = lastTrigger.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) return;

        Gamemode gm = plugin.getGamemodeManager().getGamemode(gamemodeId);
        if (gm == null || !gm.isEnabled()) return; // silent: it's a walk-by, not a click

        String queuedFor = plugin.getQueueManager().getQueuedGamemode(player.getUniqueId());
        if (gamemodeId.equalsIgnoreCase(queuedFor)) return; // already in this queue — leave them be

        lastTrigger.put(player.getUniqueId(), now);

        // Switching queues: drop the old one first so a player can't sit in two.
        if (queuedFor != null) {
            plugin.getQueueManager().removeFromQueue(player.getUniqueId());
        }
        // requestQueue, not addToQueue: on the lobby this hands off to the duels server instead
        // of parking the player in a local queue that can never start a match.
        plugin.getQueueManager().requestQueue(player, gamemodeId);

        announce(player, gm, spot, queuedFor != null);
    }

    /** Honest wording: from the lobby the player is being transferred, not searched for yet. */
    private String subtitle(boolean switched) {
        if (!plugin.getQueueManager().queuesLocally()) return "<gray>Sending you to the duels server…";
        return switched ? "<gray>Switched queue — searching for an opponent…"
                        : "<gray>Searching for an opponent…";
    }

    /** The payoff for walking in: a title, a chime and a burst of the arch's own particle. */
    private void announce(Player player, Gamemode gm, LobbyAmbienceManager.PortalSpot spot,
                          boolean switched) {
        player.showTitle(Title.title(
                MM.deserialize(gm.getDisplayName()),
                MM.deserialize(subtitle(switched)),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1600),
                        Duration.ofMillis(500))));
        player.sendMessage(MM.deserialize("<green>Joined queue for " + gm.getDisplayName()
                + "<green>."
                + (plugin.getQueueManager().queuesLocally()
                        ? " <gray>Use the queue menu to leave." : "")));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);

        Location at = player.getLocation();
        if (at.getWorld() != null) {
            at.getWorld().spawnParticle(spot.particle(), at.getX(), at.getY() + 1.0, at.getZ(),
                    40, 0.6, 1.0, 0.6, 0.08);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastTrigger.remove(event.getPlayer().getUniqueId());
    }
}
