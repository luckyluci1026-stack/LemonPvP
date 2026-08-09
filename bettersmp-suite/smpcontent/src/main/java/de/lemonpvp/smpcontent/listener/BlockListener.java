package de.lemonpvp.smpcontent.listener;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Hält eigene Blöcke stabil und sorgt für die richtigen Drops.
 *
 * Eigene Blöcke sind Notenblöcke mit einem festen Zustand. Damit dieser
 * Zustand erhalten bleibt, werden für GENAU diese Blöcke drei Vanilla-
 * Verhalten unterdrückt: Instrumentwechsel durch den Block darunter,
 * Umstimmen per Rechtsklick und der Notenklang. Normale Notenblöcke
 * bleiben davon unberührt.
 */
public final class BlockListener implements Listener {

    private final SMPContent plugin;

    public BlockListener(SMPContent plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        String id = plugin.registry().idOf(item);
        if (id == null) {
            return;
        }
        CustomEntry entry = plugin.registry().get(id);
        if (entry == null || !entry.block()) {
            return;
        }
        if (!event.getPlayer().hasPermission("smpcontent.place")) {
            event.setCancelled(true);
            plugin.msgs().send(event.getPlayer(), "no-permission");
            return;
        }
        Block block = event.getBlockPlaced();
        // Ohne Physik setzen, damit das Instrument nicht sofort neu berechnet wird
        Bukkit.getScheduler().runTask(plugin, () ->
                block.setBlockData(Bukkit.createBlockData(entry.state()), false));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        CustomEntry entry = plugin.registry().blockAt(event.getBlock().getBlockData());
        if (entry == null) {
            return;
        }
        event.setDropItems(false);
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            event.getBlock().getWorld().dropItemNaturally(
                    event.getBlock().getLocation().add(0.5, 0.5, 0.5),
                    plugin.registry().create(entry, 1));
        }
    }

    /** Verhindert, dass der Block darunter das Instrument (und damit die Textur) ändert. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        if (plugin.registry().blockAt(event.getBlock().getBlockData()) != null) {
            event.setCancelled(true);
        }
    }

    /** Verhindert das Umstimmen per Rechtsklick. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (plugin.registry().blockAt(event.getClickedBlock().getBlockData()) != null
                && !event.getPlayer().isSneaking()) {
            event.setCancelled(true);
        }
    }

    /** Eigene Blöcke sollen keinen Notenklang abspielen. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNotePlay(NotePlayEvent event) {
        if (plugin.registry().blockAt(event.getBlock().getBlockData()) != null) {
            event.setCancelled(true);
        }
    }
}
