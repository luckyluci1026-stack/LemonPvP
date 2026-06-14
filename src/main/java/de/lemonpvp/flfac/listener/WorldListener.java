package de.lemonpvp.flfac.listener;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.CheckManager;
import de.lemonpvp.flfac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/** Drives the world checks (scaffold, nuker, fastbreak). */
public final class WorldListener implements Listener {

    private final FLFAC plugin;

    public WorldListener(FLFAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
        plugin.getCheckManager().scaffold().handleBlockPlace(data, player, event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStartDigging(BlockDamageEvent event) {
        PlayerData data = plugin.getPlayerDataManager().getOrCreate(event.getPlayer());
        plugin.getCheckManager().fastBreak().handleStartDigging(data);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
        CheckManager checks = plugin.getCheckManager();
        checks.nuker().handleBlockBreak(data, player);
        checks.fastBreak().handleBlockBreak(data, player, event.getBlock());
    }
}
