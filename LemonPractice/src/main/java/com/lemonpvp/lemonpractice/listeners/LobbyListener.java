package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.gui.QueueGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class LobbyListener implements Listener {

    private final LemonPractice plugin;
    private final NamespacedKey hotbarActionKey;

    public LobbyListener(LemonPractice plugin) {
        this.plugin = plugin;
        this.hotbarActionKey = new NamespacedKey("lemonpractice", "hotbar_action");
    }

    // -------------------------------------------------------------------------
    // Join
    // -------------------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(null);

        Player player = event.getPlayer();

        if (!plugin.getServerType().equals("LOBBY")) {
            return;
        }

        // Give the lobby hotbar items
        plugin.getLobbyHotbarManager().setupHotbar(player);

        // Teleport to spawn if configured
        String spawnSection = "lobby.spawn";
        if (plugin.getConfig().contains(spawnSection)) {
            String worldName = plugin.getConfig().getString(spawnSection + ".world");
            double x = plugin.getConfig().getDouble(spawnSection + ".x");
            double y = plugin.getConfig().getDouble(spawnSection + ".y");
            double z = plugin.getConfig().getDouble(spawnSection + ".z");
            float yaw = (float) plugin.getConfig().getDouble(spawnSection + ".yaw", 0);
            float pitch = (float) plugin.getConfig().getDouble(spawnSection + ".pitch", 0);

            World world = worldName != null ? Bukkit.getWorld(worldName) : player.getWorld();
            if (world != null) {
                player.teleport(new Location(world, x, y, z, yaw, pitch));
            }
        }

        player.setGameMode(GameMode.ADVENTURE);

        // Clear any stale queue state
        if (plugin.getQueueManager().isQueued(player.getUniqueId())) {
            plugin.getQueueManager().removeFromQueue(player.getUniqueId());
        }
    }

    // -------------------------------------------------------------------------
    // Quit
    // -------------------------------------------------------------------------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(null);

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Remove from queue if they were queued
        if (plugin.getQueueManager().isQueued(uuid)) {
            plugin.getQueueManager().removeFromQueue(uuid);
        }

        // Remove from FFA arena — decrements player count and frees the slot
        if (plugin.getFfaManager().isInFfa(uuid)) {
            plugin.getFfaManager().leaveArena(player);
        }
    }

    // -------------------------------------------------------------------------
    // Hotbar item interaction
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        // Only care about right-clicks
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Only use the main hand to avoid double-firing
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(hotbarActionKey, PersistentDataType.STRING)) {
            return;
        }

        // Cancel the interaction so no block placement etc. happens
        event.setCancelled(true);

        String hotbarAction = pdc.get(hotbarActionKey, PersistentDataType.STRING);
        if (hotbarAction == null) {
            return;
        }

        Player player = event.getPlayer();
        switch (hotbarAction) {
            case "queue" -> new QueueGUI(plugin, player).open();
            case "kit_editor" -> new QueueGUI(plugin, player).openKitEditor();
            case "cosmetics" -> player.sendMessage(
                    MiniMessage.miniMessage().deserialize("<yellow>Coming soon!"));
            case "settings" -> player.sendMessage(
                    MiniMessage.miniMessage().deserialize("<yellow>See <white>/settings</white> command."));
            default -> {
                // Unknown action — ignore
            }
        }
    }

    // -------------------------------------------------------------------------
    // Prevent moving hotbar items in inventory
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        PersistentDataContainer pdc = clicked.getItemMeta().getPersistentDataContainer();
        if (pdc.has(hotbarActionKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Prevent dropping hotbar items
    // -------------------------------------------------------------------------

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (!dropped.hasItemMeta()) {
            return;
        }

        PersistentDataContainer pdc = dropped.getItemMeta().getPersistentDataContainer();
        if (pdc.has(hotbarActionKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Prevent picking up items in lobby
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!plugin.getServerType().equals("LOBBY")) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }
}
