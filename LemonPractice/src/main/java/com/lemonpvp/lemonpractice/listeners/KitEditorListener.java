package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.gui.KitEditorGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class KitEditorListener implements Listener {

    private final LemonPractice plugin;

    /** Tracks when a player clicked the DELETE button (ms timestamp). */
    private final Map<UUID, Long> deleteConfirmTime = new HashMap<>();

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

        // Prevent moving items outside the editor window
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlot() >= topSize && event.getRawSlot() != event.getSlot()) {
            // Click is in the player's own inventory below the editor — allow free drag
            // but cancel DROP-key actions that would pull items out
        }

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
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Kit deleted!"));
        } else {
            // First click — start the confirmation window
            deleteConfirmTime.put(uuid, now);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Click <bold>DELETE</bold> again within 3 seconds to confirm deletion."));
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
    }

    // -------------------------------------------------------------------------
    // Move — clean up stale confirm entries
    // -------------------------------------------------------------------------

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (deleteConfirmTime.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = deleteConfirmTime.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if ((now - entry.getValue()) > CONFIRM_WINDOW_MS) {
                it.remove();
            }
        }
    }
}
