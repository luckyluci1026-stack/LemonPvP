package de.lemonpvp.duelplus.session;

import de.lemonpvp.duelplus.DuelPlus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.PortalCreateEvent;

public final class PortalSperre implements Listener {

    private final DuelPlus plugin;

    public PortalSperre(DuelPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimPortalBauen(PortalCreateEvent event) {
        event.setCancelled(true);
        if (event.getEntity() instanceof Player spieler) {
            plugin.msgs().send(spieler, "portal-blocked");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimPortalBetreten(PlayerPortalEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimPortalFuerDinge(EntityPortalEvent event) {
        event.setCancelled(true);
    }
}
