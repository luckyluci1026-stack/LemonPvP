package com.lemonpvp.lemonlobby.listeners;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.gui.TrainingGUI;
import com.lemonpvp.lemonlobby.managers.HotbarManager;
import com.lemonpvp.lemonlobby.messaging.LobbyMessaging;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class PlayerListener implements Listener {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final LemonLobby plugin;
    private final HotbarManager hotbarManager;
    private final LobbyMessaging lobbyMessaging;
    private final TrainingGUI trainingGUI;

    public PlayerListener(LemonLobby plugin, HotbarManager hotbarManager,
                          LobbyMessaging lobbyMessaging, TrainingGUI trainingGUI) {
        this.plugin = plugin;
        this.hotbarManager = hotbarManager;
        this.lobbyMessaging = lobbyMessaging;
        this.trainingGUI = trainingGUI;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        hotbarManager.giveHotbar(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        hotbarManager.removeHotbar(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDropItem(PlayerDropItemEvent event) {
        // Block all item drops in the lobby
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Delegate to TrainingGUI first
        if (trainingGUI.handleClick(event)) return;

        // Cancel all clicks in the player's own inventory — prevents moving any items
        if (event.getClickedInventory() != null
                && event.getClickedInventory().equals(player.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        InventoryType type = event.getInventory().getType();

        // Allow chest inventories opened by our GUI code (they have a gui PDC key on items)
        if (type == InventoryType.CHEST) {
            // Check if the top inventory has our GUI marker item
            ItemStack firstItem = event.getInventory().getItem(0);
            if (firstItem != null && firstItem.hasItemMeta()) {
                if (firstItem.getItemMeta().getPersistentDataContainer()
                        .has(trainingGUI.getGuiKey(), PersistentDataType.BYTE)) {
                    return; // Our GUI — allow it
                }
            }
        }

        // Close crafting and player inventories
        if (type == InventoryType.CRAFTING || type == InventoryType.PLAYER) {
            Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        // Only handle right-click actions
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !hotbarManager.isHotbarItem(item)) return;

        event.setCancelled(true);

        int slot = player.getInventory().getHeldItemSlot();

        switch (slot) {
            case 0 -> {
                // Queue → Practice server
                lobbyMessaging.connectToServer(player,
                        plugin.getServersConfig().getString("servers.practice.name", "practice"));
            }
            case 1 -> {
                // Training Compass - open Training GUI
                trainingGUI.open(player);
            }
            case 3 -> {
                // Events server
                lobbyMessaging.connectToServer(player,
                        plugin.getServersConfig().getString("servers.events.name", "events"));
            }
            case 4 -> {
                // Kit Editor → Practice server
                lobbyMessaging.connectToServer(player,
                        plugin.getServersConfig().getString("servers.practice.name", "practice"));
            }
            case 6 -> {
                // Cosmetics
                org.bukkit.plugin.Plugin cosmeticsPlugin = Bukkit.getPluginManager().getPlugin("LemonCosmetics");
                if (cosmeticsPlugin instanceof com.lemonpvp.lemoncosmetics.LemonCosmetics lemonCosmetics) {
                    new com.lemonpvp.lemoncosmetics.gui.CosmeticsMainGUI(lemonCosmetics, player).open();
                } else {
                    player.sendMessage(MINI_MESSAGE.deserialize("<red>Cosmetics is not available right now."));
                }
            }
            case 7 -> {
                // Settings → open settings GUI
                player.sendMessage(MINI_MESSAGE.deserialize("<yellow>Settings coming soon!"));
            }
            case 8 -> {
                // Leave - connect to hub
                lobbyMessaging.connectToServer(player, plugin.getServersConfig().getString("servers.lobby.name", "lobby"));
            }
            default -> {
                // Not a handled slot
            }
        }
    }
}
