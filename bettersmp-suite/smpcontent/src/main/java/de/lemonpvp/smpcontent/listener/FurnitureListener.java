package de.lemonpvp.smpcontent.listener;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Alles, was an einem Möbelstück direkt passiert: draufhauen (abbauen),
 * rechtsklicken (hinsetzen) und aufstehen.
 *
 * Der Platzhalterblock darunter ist eine Barriere und damit unzerstörbar -
 * abgebaut wird deshalb über das Klickfeld, nicht über den Block. Das ist
 * auch das, was Spieler erwarten: man haut auf den Stuhl, nicht auf die Luft.
 */
public final class FurnitureListener implements Listener {

    private final SMPContent plugin;

    public FurnitureListener(SMPContent plugin) {
        this.plugin = plugin;
    }

    /** Draufhauen baut ab - inklusive Schutz-Plugins und der normalen Drops. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Interaction hitbox)
                || !(event.getDamager() instanceof Player player)) {
            return;
        }
        Block block = plugin.furniture().blockOf(hitbox);
        if (block == null) {
            return;
        }
        event.setCancelled(true);
        CustomEntry entry = plugin.registry().get(plugin.furniture().idOf(hitbox));
        if (entry == null || !entry.isFurniture()) {
            return;
        }
        if (!player.hasPermission("smpcontent.place")) {
            plugin.msgs().send(player, "no-permission");
            return;
        }

        // Über ein echtes BlockBreakEvent, damit WorldGuard & Co. mitreden
        // können und der BlockListener seine Drops wie gewohnt auswirft.
        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) {
            return;
        }
        // Der Platzhalter zerbricht nicht von selbst, also von Hand weg
        plugin.furniture().clear(block);
        plugin.blocks().remove(block);
        if (breakEvent.getExpToDrop() > 0 && player.getGameMode() != GameMode.CREATIVE) {
            Location at = block.getLocation().add(0.5, 0.5, 0.5);
            block.getWorld().spawn(at, ExperienceOrb.class,
                    orb -> orb.setExperience(breakEvent.getExpToDrop()));
        }
    }

    /**
     * Rechtsklick: hinsetzen, wenn es ein Sitzmöbel ist.
     *
     * Hier steht bewusst die Oberklasse: PlayerInteractAtEntityEvent teilt
     * sich mit ihr die Handler-Liste, so werden beide Varianten erwischt.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction hitbox)
                || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = plugin.furniture().blockOf(hitbox);
        if (block == null) {
            return;
        }
        CustomEntry entry = plugin.registry().get(plugin.furniture().idOf(hitbox));
        if (entry == null || !entry.isFurniture() || !entry.furniture().seat()) {
            return;
        }
        // Mit etwas in der Hand will man meist bauen, nicht sitzen
        if (!event.getPlayer().getInventory().getItemInMainHand().getType().isAir()) {
            return;
        }
        if (plugin.furniture().sit(event.getPlayer(), block, entry)) {
            event.setCancelled(true);
        }
    }

    /** Aufstehen: der unsichtbare Sitz wird nicht mehr gebraucht. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onStandUp(EntityDismountEvent event) {
        plugin.furniture().clearSeat(event.getDismounted());
    }

    /** Das Modell selbst ist unantastbar - kaputt geht es über das Klickfeld. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDisplayHit(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof ItemDisplay display
                && plugin.furniture().blockOf(display) != null) {
            event.setCancelled(true);
        }
    }
}
