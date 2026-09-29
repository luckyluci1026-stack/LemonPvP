package de.lemonpvp.betterrtp.rtp;

import de.lemonpvp.betterrtp.BetterRTP;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Bricht den RTP-Warmup bei echter Bewegung oder Schaden ab.
 */
public final class WarmupListener implements Listener {

    private final BetterRTP plugin;

    public WarmupListener(BetterRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.rtp().isTeleporting(player.getUniqueId())
                || !plugin.getConfig().getBoolean("settings.cancel-on-move", true)) {
            return;
        }
        Location origin = plugin.rtp().warmupOrigin(player.getUniqueId());
        if (origin == null) {
            return; // Suche läuft bereits, kein Abbruch mehr
        }
        Location to = event.getTo();
        if (origin.getBlockX() != to.getBlockX()
                || origin.getBlockY() != to.getBlockY()
                || origin.getBlockZ() != to.getBlockZ()) {
            plugin.rtp().cancelWarmup(player.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (plugin.rtp().geschuetzt(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getConfig().getBoolean("settings.cancel-on-damage", true)
                && plugin.rtp().warmupOrigin(player.getUniqueId()) != null) {
            plugin.rtp().cancelWarmup(player.getUniqueId());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isInvulnerable()
                && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {
            player.setInvulnerable(false);
            plugin.getLogger().info(player.getName() + " war noch vom alten RTP-Schutz unverwundbar - behoben.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.rtp().cancelWarmup(event.getPlayer().getUniqueId());
        plugin.rtp().schutzVergessen(event.getPlayer().getUniqueId());
    }
}
