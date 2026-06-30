package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.gui.KitEditorGUI;
import com.lemonpvp.lemonpractice.gui.KitItemPaletteGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class KitEditorListener implements Listener {

    private final LemonPractice plugin;

    /** Tracks when a player clicked the DELETE button (ms timestamp). */
    private final Map<UUID, Long> deleteConfirmTime = new HashMap<>();

    /**
     * Players whose next editor-inventory close should NOT trigger a save — set
     * just before we open the item palette (which closes the editor canvas).
     */
    private final Set<UUID> suppressNextSave = new HashSet<>();

    /**
     * Active KitEditorGUI instances keyed by player UUID.
     * Populated by {@link com.lemonpvp.lemonpractice.gui.QueueGUI} (or any code that opens
     * the editor) via {@link #trackEditor(UUID, KitEditorGUI)}.
     */
    private final Map<UUID, KitEditorGUI> activeEditors = new HashMap<>();

    /** The PDC key set on a player while they have the kit editor open. */
    private final NamespacedKey editingGamemodeKey;

    /** The PDC key set on GUI action items (e.g. the DELETE button). */
    private final NamespacedKey kitActionKey;

    /** How long the player has to confirm a delete (ms). */
    private static final long CONFIRM_WINDOW_MS = 3_000L;

    public KitEditorListener(LemonPractice plugin) {
        this.plugin = plugin;
        this.editingGamemodeKey = new NamespacedKey("lemonpractice", "editing_gamemode");
        this.kitActionKey = new NamespacedKey("lemonpractice", "kit_action");
    }

    /**
     * Called by the GUI layer when a {@link KitEditorGUI} is opened for a player.
     * This associates the GUI instance so that {@link #onInventoryClose} can call
     * {@code saveKit} on the correct object.
     */
    public void trackEditor(UUID playerUuid, KitEditorGUI gui) {
        activeEditors.put(playerUuid, gui);
    }

    // -------------------------------------------------------------------------
    // Close — auto-save the kit
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        PersistentDataContainer playerPdc = player.getPersistentDataContainer();
        if (!playerPdc.has(editingGamemodeKey, PersistentDataType.STRING)) {
            return;
        }

        // A side GUI (the item palette) closed — never save on that; just clear
        // any pending suppression and keep the editing session alive.
        InventoryType type = event.getInventory().getType();
        if (type != InventoryType.CRAFTING && type != InventoryType.PLAYER) {
            suppressNextSave.remove(player.getUniqueId());
            return;
        }

        // The editor canvas closing because we opened the palette — skip the save.
        if (suppressNextSave.remove(player.getUniqueId())) {
            return;
        }

        String gamemode = playerPdc.get(editingGamemodeKey, PersistentDataType.STRING);
        if (gamemode == null) {
            return;
        }

        // saveKit reads the player inventory + removes editingGamemodeKey + sends "Kit saved" message
        new KitEditorGUI(plugin).saveKit(player, gamemode);

        // Restore the lobby hotbar now that the editor is closed
        plugin.getLobbyHotbarManager().setupHotbar(player);
        activeEditors.remove(player.getUniqueId());
    }

    // -------------------------------------------------------------------------
    // Hotbar right-click — open the item palette from the editor
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.getPersistentDataContainer().has(editingGamemodeKey, PersistentDataType.STRING)) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        String action2 = item.getItemMeta().getPersistentDataContainer()
                .get(kitActionKey, PersistentDataType.STRING);
        if (!"items".equals(action2)) return;

        event.setCancelled(true);
        openPalette(player);
    }

    /** Opens the item palette next tick, suppressing the editor's close-save. */
    private void openPalette(Player player) {
        suppressNextSave.add(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()
                    && player.getPersistentDataContainer().has(editingGamemodeKey, PersistentDataType.STRING)) {
                new KitItemPaletteGUI(plugin, player).open();
            } else {
                suppressNextSave.remove(player.getUniqueId());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Click — handle DELETE confirmation and guard the inventory edges
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        PersistentDataContainer playerPdc = player.getPersistentDataContainer();
        if (!playerPdc.has(editingGamemodeKey, PersistentDataType.STRING)) {
            return;
        }

        String gamemode = playerPdc.get(editingGamemodeKey, PersistentDataType.STRING);

        // Prevent keyboard-drop (Q) from inside the editor
        switch (event.getAction()) {
            case DROP_ONE_SLOT, DROP_ALL_SLOT, DROP_ONE_CURSOR, DROP_ALL_CURSOR -> {
                event.setCancelled(true);
                return;
            }
            default -> { /* continue */ }
        }

        // Check if the clicked item is a special action button
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        PersistentDataContainer itemPdc = clicked.getItemMeta().getPersistentDataContainer();
        if (!itemPdc.has(kitActionKey, PersistentDataType.STRING)) {
            return;
        }

        String kitAction = itemPdc.get(kitActionKey, PersistentDataType.STRING);

        // Item-selector clicked — open the palette.
        if ("items".equals(kitAction)) {
            event.setCancelled(true);
            openPalette(player);
            return;
        }

        if (!"delete".equals(kitAction)) {
            return;
        }

        // DELETE button clicked — two-step confirmation
        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastClick = deleteConfirmTime.get(uuid);

        if (lastClick != null && (now - lastClick) <= CONFIRM_WINDOW_MS) {
            // Confirmed — delete the kit
            deleteConfirmTime.remove(uuid);
            plugin.getKitManager().evict(uuid);
            plugin.getDatabase().deleteKit(uuid, gamemode);
            activeEditors.remove(uuid);
            playerPdc.remove(editingGamemodeKey);
            player.closeInventory();
            plugin.getLobbyHotbarManager().setupHotbar(player);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 0.7f, 0.8f);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<!italic><red>Your <yellow>" + gamemode + " <red>kit was deleted!"));
        } else {
            // First click — start the confirmation window
            deleteConfirmTime.put(uuid, now);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<!italic><red>Click <bold>Delete Kit</bold> again within <bold>3 seconds</bold> to confirm."));
        }
    }

    // -------------------------------------------------------------------------
    // Drop — prevent dropping items while the editor is open
    // -------------------------------------------------------------------------

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer playerPdc = player.getPersistentDataContainer();
        if (playerPdc.has(editingGamemodeKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Quit — clean up all per-player state so maps don't grow forever
    // -------------------------------------------------------------------------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        activeEditors.remove(uuid);
        deleteConfirmTime.remove(uuid);
        suppressNextSave.remove(uuid);
    }
}
