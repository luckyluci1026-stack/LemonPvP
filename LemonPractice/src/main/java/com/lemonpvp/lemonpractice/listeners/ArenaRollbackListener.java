package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

/**
 * Snapshots the ORIGINAL state of every block that changes inside a vanilla
 * duel world, so {@link com.lemonpvp.lemonpractice.managers.ArenaRollbackManager}
 * can heal the world after the match. All handlers record BEFORE the change and
 * never cancel — they only observe.
 *
 * <p>Covers the terrain-touching sources across all gamemodes: mining/placing
 * (sword/uhc), end-crystal & TNT-minecart explosions (crystal/cart), fire
 * (fire-aspect/crystal), and buckets + liquid flow (uhc water/lava).</p>
 */
public class ArenaRollbackListener implements Listener {

    private final LemonPractice plugin;

    public ArenaRollbackListener(LemonPractice plugin) {
        this.plugin = plugin;
    }

    private com.lemonpvp.lemonpractice.managers.ArenaRollbackManager rb() {
        return plugin.getArenaRollbackManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        rb().record(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        // The block is already placed at MONITOR, so take the REPLACED state
        // (usually air) as the original — restore then removes the placed block.
        rb().record(e.getBlockPlaced(), e.getBlockReplacedState().getBlockData());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        for (Block b : e.blockList()) rb().record(b);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        for (Block b : e.blockList()) rb().record(b);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        rb().record(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        rb().record(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        rb().record(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        rb().record(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent e) {
        // The block the liquid flows INTO is the one that changes.
        rb().record(e.getToBlock());
    }
}
