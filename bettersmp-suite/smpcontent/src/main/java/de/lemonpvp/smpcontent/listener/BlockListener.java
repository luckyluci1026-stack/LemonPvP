package de.lemonpvp.smpcontent.listener;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hält eigene Blöcke stabil und sorgt für die richtigen Drops.
 *
 * Eigene Blöcke sind Notenblöcke mit einem festen Zustand. Der Zustand ist
 * aber nur das AUSSEHEN - wer der Block ist, steht im Chunk-Speicher
 * ({@link de.lemonpvp.smpcontent.content.BlockStore}). Dadurch geht ein Block
 * nicht mehr verloren, wenn irgendetwas seinen Zustand verändert: das Plugin
 * erkennt ihn weiterhin und stellt das Aussehen wieder her.
 *
 * Zusätzlich werden für genau diese Blöcke drei Vanilla-Verhalten
 * unterdrückt: Instrumentwechsel durch den Block darunter, Umstimmen per
 * Rechtsklick und der Notenklang. Normale Notenblöcke bleiben unberührt.
 */
public final class BlockListener implements Listener {

    private final SMPContent plugin;

    public BlockListener(SMPContent plugin) {
        this.plugin = plugin;
    }

    /**
     * Welcher eigene Block steht hier?
     *
     * Erst der Chunk-Speicher (schnell und zustandsunabhängig), danach als
     * Rückfalltür der Zustandsvergleich - so werden Blöcke aus älteren
     * Versionen weiterhin erkannt und dabei gleich nachgetragen.
     */
    private CustomEntry customAt(Block block) {
        if (block.getType() != Material.NOTE_BLOCK) {
            return null;
        }
        String id = plugin.blocks().idAt(block);
        if (id != null) {
            return plugin.registry().get(id);
        }
        CustomEntry byState = plugin.registry().blockAt(block.getBlockData());
        if (byState != null) {
            plugin.blocks().set(block, byState.id());
        }
        return byState;
    }

    /** Stellt das Aussehen wieder her, falls es abgewichen ist. */
    private void repair(Block block, CustomEntry entry) {
        BlockData want = plugin.registry().blockDataFor(entry);
        if (want == null || block.getType() != Material.NOTE_BLOCK) {
            return;
        }
        if (!block.getBlockData().getAsString().equals(want.getAsString())) {
            block.setBlockData(want.clone(), false);
        }
    }

    // ------------------------------------------------------------------
    //  Setzen und Abbauen
    // ------------------------------------------------------------------

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
        BlockData data = plugin.registry().blockDataFor(entry);
        if (data == null) {
            return;
        }
        Block block = event.getBlockPlaced();

        // Einen Tick später setzen, damit das Instrument nicht sofort neu
        // berechnet wird. Vorher aber prüfen, ob der Block überhaupt noch
        // steht: Plugins wie WorldGuard brechen den Bau erst bei HIGHEST
        // oder MONITOR ab - ohne diesen Test bliebe ein Geisterblock stehen.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType() != Material.NOTE_BLOCK) {
                return;
            }
            block.setBlockData(data.clone(), false);
            plugin.blocks().set(block, entry.id());
            playSound(block, entry.extras().placeSound());
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        CustomEntry entry = customAt(event.getBlock());
        if (entry == null) {
            return;
        }
        plugin.blocks().remove(event.getBlock());
        event.setDropItems(false);
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        Location at = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        playSound(event.getBlock(), entry.extras().breakSound());

        // Wie bei Vanilla-Erz: ohne passendes Werkzeug gibt es nichts
        if (!hasRequiredTool(tool, entry.extras().requiresTool())) {
            return;
        }

        int fortune = levelOf(tool, "fortune");
        boolean silk = levelOf(tool, "silk_touch") > 0;
        for (ItemStack drop : plugin.registry().rollDrops(entry, fortune, silk)) {
            event.getBlock().getWorld().dropItemNaturally(at, drop);
        }
        if (entry.experience() > 0 && !silk) {
            event.setExpToDrop(event.getExpToDrop() + entry.experience());
        }
    }

    /** "pickaxe", "axe", "shovel", "hoe" - leer heißt: jedes Werkzeug reicht. */
    private boolean hasRequiredTool(ItemStack tool, String required) {
        if (required == null || required.isBlank()) {
            return true;
        }
        if (tool == null || tool.getType().isAir()) {
            return false;
        }
        return tool.getType().name().endsWith("_" + required.toUpperCase(Locale.ROOT).trim());
    }

    private void playSound(Block block, String sound) {
        if (sound != null && !sound.isBlank()) {
            block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), sound, 1.0f, 1.0f);
        }
    }

    private int levelOf(ItemStack tool, String enchantment) {
        if (tool == null || tool.getType().isAir()) {
            return 0;
        }
        Enchantment type = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchantment));
        return type == null ? 0 : tool.getEnchantmentLevel(type);
    }

    // ------------------------------------------------------------------
    //  Zustand stabil halten
    // ------------------------------------------------------------------

    /** Verhindert, dass der Block darunter das Instrument (und damit die Textur) ändert. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        if (customAt(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    /** Verhindert das Umstimmen per Rechtsklick. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        CustomEntry entry = customAt(block);
        if (entry == null) {
            return;
        }
        if (!event.getPlayer().isSneaking()) {
            // Nur die Block-Interaktion (das Umstimmen) sperren. Wer das ganze
            // Event abbricht, kann vor einem eigenen Block auch nichts mehr
            // essen, keinen Eimer benutzen und keinen Bogen spannen.
            event.setUseInteractedBlock(Event.Result.DENY);
        }
        // Doppelt genäht: sollte doch etwas durchrutschen, sitzt der Zustand
        // einen Tick später wieder richtig.
        Bukkit.getScheduler().runTask(plugin, () -> repair(block, entry));
    }

    /** Eigene Blöcke sollen keinen Notenklang abspielen. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNotePlay(NotePlayEvent event) {
        if (customAt(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Kolben würden den Block verschieben, ohne dass der Eintrag mitwandert -
     * danach stünde ein eigener Block ohne Kennung in der Welt.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (anyCustom(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (anyCustom(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    private boolean anyCustom(List<Block> blocks) {
        for (Block block : blocks) {
            if (customAt(block) != null) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    //  Explosionen: Eintrag entfernen und richtig droppen
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        for (Block block : blocks) {
            CustomEntry entry = customAt(block);
            if (entry == null) {
                continue;
            }
            plugin.blocks().remove(block);
            Location at = block.getLocation().add(0.5, 0.5, 0.5);
            for (ItemStack drop : plugin.registry().rollDrops(entry, 0, false)) {
                block.getWorld().dropItemNaturally(at, drop);
            }
            block.setType(Material.AIR, false);
        }
    }

    // ------------------------------------------------------------------
    //  Chunks
    // ------------------------------------------------------------------

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        Map<Integer, String> table = plugin.blocks().load(chunk);
        if (table.isEmpty()) {
            return;
        }
        // Einen Tick später: falls in der Zwischenzeit etwas am Zustand
        // gedreht hat (WorldEdit, /setblock, ein anderes Plugin), sieht der
        // Block danach wieder richtig aus.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!chunk.isLoaded()) {
                return;
            }
            for (Map.Entry<Integer, String> stored : table.entrySet()) {
                CustomEntry entry = plugin.registry().get(stored.getValue());
                if (entry == null) {
                    continue;
                }
                int packed = stored.getKey();
                Block block = chunk.getBlock(packed & 15, (packed >> 8) - 2048, (packed >> 4) & 15);
                repair(block, entry);
            }
        });
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        plugin.blocks().unload(event.getChunk());
    }
}
