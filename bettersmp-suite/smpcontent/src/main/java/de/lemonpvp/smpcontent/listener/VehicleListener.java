package de.lemonpvp.smpcontent.listener;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.vehicle.VehicleType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Fahrzeuge hinstellen, einsteigen und wieder einsammeln.
 *
 * Rechtsklick auf den Boden mit dem Fahrzeug-Item stellt es hin,
 * Rechtsklick auf das Fahrzeug steigt ein, Schleichen + Rechtsklick mit
 * leerer Hand nimmt es wieder mit.
 */
public final class VehicleListener implements Listener {

    private final SMPContent plugin;

    public VehicleListener(SMPContent plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) {
            return;
        }
        ItemStack item = event.getItem();
        String id = plugin.registry().idOf(item);
        VehicleType type = plugin.vehicles().byItem(id);
        if (type == null) {
            return;
        }
        event.setCancelled(true);
        if (!event.getPlayer().hasPermission("smpcontent.place")) {
            plugin.msgs().send(event.getPlayer(), "no-permission");
            return;
        }
        Block block = event.getClickedBlock();
        Location where = block.getRelative(event.getBlockFace())
                .getLocation().add(0.5, 0, 0.5);
        plugin.vehicles().spawn(type, where, event.getPlayer().getLocation().getYaw());
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE && item != null) {
            item.setAmount(item.getAmount() - 1);
        }
        plugin.msgs().send(event.getPlayer(), "vehicle-placed", "name", type.id());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnter(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        boolean emptyHand = event.getPlayer().getInventory()
                .getItemInMainHand().getType().isAir();
        if (event.getPlayer().isSneaking() && emptyHand) {
            if (plugin.vehicles().pickUp(event.getPlayer(), event.getRightClicked())) {
                event.setCancelled(true);
            }
            return;
        }
        if (plugin.vehicles().enter(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    /** Wer gerade geht oder gestorben ist, darf immer aussteigen. */
    private final java.util.Set<java.util.UUID> gehen = new java.util.HashSet<>();

    /**
     * Im Flug steigt man nicht einfach aus.
     *
     * Sonst wäre der erste Schleicher schon der Absprung und der
     * Doppel-Schleicher käme nie zustande. Am Boden geht Aussteigen normal.
     *
     * Beim Verlassen des Servers und beim Tod wird nicht gebremst - sonst
     * bliebe ein Spieler an einem Fahrzeug hängen, das es für ihn gar nicht
     * mehr gibt.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDismount(org.bukkit.event.entity.EntityDismountEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player player)) {
            return;
        }
        if (gehen.contains(player.getUniqueId()) || player.isDead()
                || !player.isValid()) {
            return;
        }
        if (plugin.vehicles().airborne(event.getDismounted())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        gehen.add(event.getPlayer().getUniqueId());
        // Ein Schirm gehört niemandem mehr, wenn der Spieler weg ist
        plugin.parachutes().close(event.getPlayer());
        org.bukkit.Bukkit.getScheduler().runTask(plugin,
                () -> gehen.remove(event.getPlayer().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        plugin.parachutes().close(event.getEntity());
    }

    /** Rechtsklick mit dem Fallschirm im Fall: Schirm auf. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onParachute(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || !event.getAction().isRightClick()) {
            return;
        }
        String id = plugin.registry().idOf(event.getItem());
        if (id == null || !id.equalsIgnoreCase(plugin.vehicles().parachuteItem())) {
            return;
        }
        var player = event.getPlayer();
        if (player.isOnGround() || plugin.parachutes().isOpen(player)) {
            return;
        }
        event.setCancelled(true);
        plugin.parachutes().deploy(player, plugin.vehicles().parachuteSettings());
    }

    /** Nach einem Neustart stehen die Fahrzeuge noch da - wieder aufnehmen. */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.vehicles().adoptChunk(event.getChunk());
    }
}
