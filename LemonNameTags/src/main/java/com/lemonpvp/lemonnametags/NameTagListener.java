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
