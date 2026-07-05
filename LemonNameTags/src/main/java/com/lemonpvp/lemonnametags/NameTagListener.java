package com.lemonpvp.lemonnametags;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

/** Wires player lifecycle events to the {@link NameTagManager}. */
public final class NameTagListener implements Listener {

    private final LemonNameTags plugin;
    private final NameTagManager manager;

    public NameTagListener(LemonNameTags plugin, NameTagManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        manager.hideVanillaName(player);
        // Delay so LuckPerms has loaded the user before the first render.
        int delay = Math.max(1, plugin.getConfig().getInt("update-delay-ticks", 10));
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) manager.create(p);
        }, delay);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.remove(event.getPlayer().getUniqueId());
        manager.showVanillaName(event.getPlayer());
        lastHealthRefresh.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        // Death ejects the passenger — remove the display immediately so no
        // ghost tag floats at the kill site while the victim sits on the death
        // screen. The respawn handler below rebuilds it.
        manager.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        // Rebuild the tag shortly after respawn (create() is idempotent).
        UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) manager.create(p);
        }, 2L);
    }

    // ── Live health line (only active when show-health is enabled) ──────────

    /** uuid -> last health-refresh timestamp (throttles fight spam). */
    private final java.util.Map<UUID, Long> lastHealthRefresh = new java.util.HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p) refreshHealthLine(p);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegain(org.bukkit.event.entity.EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player p) refreshHealthLine(p);
    }

    /** Rebuilds the tag text next tick (post-damage health), max ~6x/second. */
    private void refreshHealthLine(Player player) {
        if (!plugin.getConfig().getBoolean("show-health", false)) return;
        long now = System.currentTimeMillis();
        Long last = lastHealthRefresh.get(player.getUniqueId());
        if (last != null && now - last < 150) return;
        lastHealthRefresh.put(player.getUniqueId(), now);
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && !p.isDead()) manager.update(p);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        // Teleports eject passengers and leave the display at the origin (in
        // the old world for cross-world teleports) — remove it now and rebuild
        // right after arrival instead of waiting for the validate task.
        UUID uuid = event.getPlayer().getUniqueId();
        manager.remove(uuid);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && !p.isDead()) manager.create(p);
        }, 2L);
    }
}
