package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard practice-server ender-pearl cooldown (default 16s) for players in
 * combat (duel / bot duel / FFA / zone). Throwing during cooldown is cancelled
 * with an action-bar countdown; the pearl is refunded automatically because
 * the cancelled launch never consumes the item.
 */
public class PearlCooldownListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;
    private final Map<UUID, Long> lastPearl = new ConcurrentHashMap<>();

    public PearlCooldownListener(LemonPractice plugin) {
        this.plugin = plugin;
    }

    private boolean inCombat(UUID uuid) {
        return plugin.getDuelManager().isInDuel(uuid)
                || plugin.getBotDuelManager().isInBotDuel(uuid)
                || plugin.getFfaManager().isInFfa(uuid)
                || plugin.getZonePracticeManager().isInZone(uuid);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;
        if (!plugin.getConfig().getBoolean("pearl-cooldown.enabled", true)) return;

        UUID uuid = player.getUniqueId();
        if (!inCombat(uuid)) return;

        long cooldownMs = plugin.getConfig().getLong("pearl-cooldown.seconds", 16) * 1000L;
        long now = System.currentTimeMillis();
        Long last = lastPearl.get(uuid);

        if (last != null && now - last < cooldownMs) {
            event.setCancelled(true);
            double remaining = (cooldownMs - (now - last)) / 1000.0;
            player.sendActionBar(MM.deserialize("<!italic><red>✦ Pearl ready in <yellow>"
                    + String.format(Locale.US, "%.1f", remaining) + "s"));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.7f);
            return;
        }

        lastPearl.put(uuid, now);
        // Visible countdown like the big practice servers: xp bar as a timer.
        int seconds = (int) (cooldownMs / 1000);
        player.setCooldown(org.bukkit.Material.ENDER_PEARL, seconds * 20);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastPearl.remove(event.getPlayer().getUniqueId());
    }
}
