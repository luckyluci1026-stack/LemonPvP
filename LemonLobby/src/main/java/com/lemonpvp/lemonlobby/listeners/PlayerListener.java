package com.lemonpvp.lemonlobby.listeners;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.gui.TrainingGUI;
import com.lemonpvp.lemonlobby.managers.HotbarManager;
import com.lemonpvp.lemonlobby.messaging.LobbyMessaging;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
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
import org.bukkit.plugin.Plugin;

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
        if (hotbarManager.isHotbarItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Delegate to TrainingGUI first
        if (trainingGUI.handleClick(event)) return;

        // If clicking in own player inventory, cancel if hotbar item
        if (event.getClickedInventory() != null
                && event.getClickedInventory().equals(player.getInventory())) {
            ItemStack clicked = event.getCurrentItem();
            if (hotbarManager.isHotbarItem(clicked)) {
                event.setCancelled(true);
            }
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
            // Also allow if the title is our training GUI title
            // (handled generously: block any non-our-code chest)
            // We'll close unknown chest inventories too to be safe
        }

        // Close crafting and player inventories
        if (type == InventoryType.CRAFTING || type == InventoryType.PLAYER) {
            Bukkit.getScheduler().runTask(plugin, player::closeInventory);
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
                // Queue
                player.sendMessage(MINI_MESSAGE.deserialize("<red>Queue is not implemented yet."));
            }
            case 1 -> {
                // Training Compass - open Training GUI
                trainingGUI.open(player);
            }
            case 3 -> {
                // Events
                player.sendMessage(MINI_MESSAGE.deserialize("<red>Events is not implemented yet."));
            }
            case 4 -> {
                // Kit Editor
                player.sendMessage(MINI_MESSAGE.deserialize("<red>Kit Editor is not implemented yet."));
            }
            case 6 -> {
                // Cosmetics - try LemonCosmetics plugin
                Plugin cosmetics = Bukkit.getPluginManager().getPlugin("LemonCosmetics");
                if (cosmetics != null && cosmetics.isEnabled()) {
                    player.sendMessage(MINI_MESSAGE.deserialize("<red>Cosmetics is not implemented yet."));
                } else {
                    player.sendMessage(MINI_MESSAGE.deserialize("<red>Cosmetics is not available right now."));
                }
            }
            case 7 -> {
                // Settings
                player.sendMessage(MINI_MESSAGE.deserialize("<red>Settings is not implemented yet."));
            }
            case 8 -> {
                // Leave - connect to hub
                lobbyMessaging.connectToServer(player, "hub");
            }
            default -> {
                // Not a handled slot
            }
        }
    }
}
