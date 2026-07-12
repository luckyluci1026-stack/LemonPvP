package com.lemonpvp.lemonlobby.listeners;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.gui.DailyRewardGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Makes the lobby buildings FUNCTIONAL: right-clicking their signature blocks
 * opens the matching feature, so the builds aren't just decoration.
 *
 * <ul>
 *   <li>Anvil / smithing table / grindstone (Kit Forge) → kit editor</li>
 *   <li>Lectern (Cathedral chapels, Observatory desk) → leaderboard</li>
 *   <li>Ender chest (Bazaar gazebo) → daily reward</li>
 *   <li>Brewing stand (Bazaar potion corner) → shop</li>
 *   <li>Gold / portal glass deep in the cathedral (z &lt; -40) → queue (duels)</li>
 * </ul>
 * Creative players are exempt so builders can still use the real blocks.
 */
public class LandmarkListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonLobby plugin;

    public LandmarkListener(LemonLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        Material type = block.getType();
        switch (type) {
            case ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL, SMITHING_TABLE, GRINDSTONE -> {
                event.setCancelled(true);
                if (Bukkit.getPluginManager().isPluginEnabled("LemonPractice")) {
                    player.performCommand("kiteditor");
                } else {
                    player.sendMessage(MM.deserialize("<red>The kit editor is not available right now."));
                }
            }
            case LECTERN -> {
                event.setCancelled(true);
                if (Bukkit.getPluginManager().isPluginEnabled("LemonPractice")) {
                    player.performCommand("leaderboard");
                }
            }
            case ENDER_CHEST -> {
                event.setCancelled(true);
                new DailyRewardGUI(plugin, player).open();
            }
            case BREWING_STAND -> {
                event.setCancelled(true);
                player.performCommand("shop");
            }
            case GOLD_BLOCK, YELLOW_STAINED_GLASS, LIME_STAINED_GLASS, SEA_LANTERN -> {
                // Only the queue portal deep inside the cathedral (north wing).
                if (block.getZ() < -40) {
                    event.setCancelled(true);
                    player.playSound(player.getLocation(),
                            org.bukkit.Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.6f);
                    plugin.getLobbyMessaging().connectToServer(player,
                            plugin.getServersConfig().getString("servers.duels.name", "duels"));
                }
            }
            default -> { /* not a landmark block */ }
        }
    }
}
