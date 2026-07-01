package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Gamemode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class QueueGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Center slots in a 9×6 (54-slot) inventory
    private static final int[] GAMEMODE_SLOTS = {10, 11, 12, 13, 14, 19, 20, 21, 22, 23, 28, 29};

    private final LemonPractice plugin;
    private final Player player;
    private final NamespacedKey gamemodeKey;
    private boolean kitEditorMode;
    private Inventory inventory;
    private boolean registered = false;

    public QueueGUI(LemonPractice plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gamemodeKey = new NamespacedKey(plugin, "gamemode_id");
        this.kitEditorMode = false;
    }

    public void open() {
        Component title = MM.deserialize("<gradient:#fffb00:#00ff00>Queue</gradient>");
        openInternal(title);
    }

    public void openKitEditor() {
        kitEditorMode = true;
        Component title = MM.deserialize("<gradient:#fffb00:#00ff00>Select Gamemode to Edit Kit</gradient>");
        openInternal(title);
    }

    private void openInternal(Component title) {
        inventory = Bukkit.createInventory(null, 54, title);

        ItemStack filler = buildFiller();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        List<Gamemode> gamemodes = new ArrayList<>(plugin.getGamemodeManager().getAllGamemodes());
        for (int i = 0; i < GAMEMODE_SLOTS.length && i < gamemodes.size(); i++) {
            inventory.setItem(GAMEMODE_SLOTS[i], buildGamemodeItem(gamemodes.get(i)));
        }

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }

        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!clicker.getUniqueId().equals(player.getUniqueId())) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String gamemodeId = meta.getPersistentDataContainer().get(gamemodeKey, PersistentDataType.STRING);
        if (gamemodeId == null) return;

        Gamemode gm = plugin.getGamemodeManager().getGamemode(gamemodeId);
        if (gm == null || !gm.isEnabled()) {
            clicker.sendMessage(MM.deserialize("<red>This gamemode is currently disabled."));
            return;
        }

        if (kitEditorMode) {
            clicker.closeInventory();
            new KitTypeGUI(plugin, clicker, gamemodeId).open();
            return;
        }

        if (plugin.getQueueManager().isQueued(clicker.getUniqueId())) {
            plugin.getQueueManager().removeFromQueue(clicker.getUniqueId());
            clicker.playSound(clicker.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            clicker.sendMessage(MM.deserialize("<red>Left queue."));
        } else {
            plugin.getQueueManager().addToQueue(clicker.getUniqueId(), gamemodeId);
            clicker.playSound(clicker.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
            clicker.sendMessage(MM.deserialize("<green>Joined queue for <yellow>" + gm.getName() + "<green>."));
            clicker.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        if (!event.getInventory().equals(inventory)) return;
        unregister();
    }

    private void unregister() {
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }

    private ItemStack buildFiller() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack buildGamemodeItem(Gamemode gamemode) {
        ItemStack item = new ItemStack(gamemode.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic>" + gamemode.getDisplayName()));

        int playing = plugin.getQueueManager().getPlayingCount(gamemode.getId());
        int queuing = plugin.getQueueManager().getQueueCount(gamemode.getId());

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Playing: <green>" + playing));
        lore.add(MM.deserialize("<!italic><gray>Queuing: <green>" + queuing));
        lore.add(Component.empty());
        if (gamemode.isEnabled()) {
            lore.add(kitEditorMode ? MM.deserialize("<!italic><green>Click to edit kit!") : MM.deserialize("<!italic><green>Click to queue!"));
        } else {
            lore.add(MM.deserialize("<!italic><red>Disabled"));
        }
        meta.lore(lore);
        // Gamemode icons are weapons (swords/trident/mace/bow) — hide attack
        // damage and extra item data so the tooltip stays clean.
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        meta.getPersistentDataContainer().set(gamemodeKey, PersistentDataType.STRING, gamemode.getId());
        item.setItemMeta(meta);
        return item;
    }
}
