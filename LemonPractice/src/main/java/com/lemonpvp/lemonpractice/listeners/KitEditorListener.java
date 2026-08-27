package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.gui.KitEditorGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the sort-only preset kit editor: keeps the session save-on-close,
 * locks the armor / crafting / off-hand slots so the player can only rearrange
 * the preset items (no adding/removing), and handles the two-step RESET control.
 */
public class KitEditorListener implements Listener {

    private final LemonPractice plugin;

    /** Tracks when a player clicked the RESET button (ms timestamp). */
    private final Map<UUID, Long> resetConfirmTime = new HashMap<>();

    /** Active KitEditorGUI instances keyed by player UUID. */
    private final Map<UUID, KitEditorGUI> activeEditors = new HashMap<>();

    /** The PDC key set on a player while they have the kit editor open. */
    private final NamespacedKey editingGamemodeKey;

    /** The PDC key set on GUI action items (the RESET button). */
    private final NamespacedKey kitActionKey;

    /** How long the player has to confirm a reset (ms). */
    private static final long CONFIRM_WINDOW_MS = 3_000L;

    public KitEditorListener(LemonPractice plugin) {
        this.plugin = plugin;
        this.editingGamemodeKey = new NamespacedKey("lemonpractice", "editing_gamemode");
        this.kitActionKey = new NamespacedKey("lemonpractice", "kit_action");
    }

    public void trackEditor(UUID playerUuid, KitEditorGUI gui) {
        activeEditors.put(playerUuid, gui);
    }

    // -------------------------------------------------------------------------
    // Close — auto-save the arrangement
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

        // Only the editor canvas (the player's own inventory) saves; ignore any
        // other inventory the player might have open.
        InventoryType type = event.getInventory().getType();
        if (type != InventoryType.CRAFTING && type != InventoryType.PLAYER) {
            return;
        }

        String gamemode = playerPdc.get(editingGamemodeKey, PersistentDataType.STRING);
        if (gamemode == null) {
            return;
        }

        // saveKit reads the inventory, re-attaches preset armor, removes the PDC
        // and sends the "saved" message.
        new KitEditorGUI(plugin).saveKit(player, gamemode);

        // Restore the lobby hotbar now that the editor is closed.
        plugin.getLobbyHotbarManager().setupHotbar(player);
        activeEditors.remove(player.getUniqueId());
    }

    // -------------------------------------------------------------------------
    // Click — lock armor/crafting/off-hand slots and handle RESET
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        PersistentDataContainer playerPdc = player.getPersistentDataContainer();
        if (!playerPdc.has(editingGamemodeKey, PersistentDataType.STRING)) {
            return;
        }

        String gamemode = playerPdc.get(editingGamemodeKey, PersistentDataType.STRING);

        // Block dropping (Q / cursor-drop) and off-hand swaps (F) — items must
        // stay within the sortable area.
        switch (event.getAction()) {
            case DROP_ONE_SLOT, DROP_ALL_SLOT, DROP_ONE_CURSOR, DROP_ALL_CURSOR -> {
                event.setCancelled(true);
                return;
            }
            default -> { /* continue */ }
        }
        if (event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
            return;
        }

        // The RESET control sentinel? Checked BEFORE the locked-slot rule below,
        // because the control now lives in the off-hand (raw 45), which is
        // otherwise a locked slot — the lock would swallow the click.
        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && clicked.hasItemMeta()) {
            PersistentDataContainer itemPdc = clicked.getItemMeta().getPersistentDataContainer();
            if ("reset".equals(itemPdc.get(kitActionKey, PersistentDataType.STRING))) {
                event.setCancelled(true);
                handleReset(player, gamemode);
                return;
            }
        }

        // Lock the crafting grid + result (raw 0–4), armor (raw 5–8) and off-hand
        // (raw 45) so items can't be removed/added — only sorted within 0–35.
        int raw = event.getRawSlot();
        if ((raw >= 0 && raw <= 8) || raw == 45) {
            event.setCancelled(true);
        }
    }

    /** Two-step RESET: deletes the saved arrangement so the preset default returns. */
    private void handleReset(Player player, String gamemode) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastClick = resetConfirmTime.get(uuid);

        if (lastClick != null && (now - lastClick) <= CONFIRM_WINDOW_MS) {
            resetConfirmTime.remove(uuid);
            plugin.getKitManager().evict(uuid);
            plugin.getDatabase().deleteKit(uuid, gamemode);
            activeEditors.remove(uuid);
            player.getPersistentDataContainer().remove(editingGamemodeKey);
            player.closeInventory();
            plugin.getLobbyHotbarManager().setupHotbar(player);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 0.7f, 0.8f);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<!italic><green>Your <yellow>" + gamemode + " <green>kit was reset to the default layout."));
        } else {
            resetConfirmTime.put(uuid, now);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<!italic><red>Click <bold>Reset to Default</bold> again within <bold>3 seconds</bold> to confirm."));
        }
    }

    // -------------------------------------------------------------------------
    // Drag — block drags that touch a locked slot (armor/crafting/off-hand)
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.getPersistentDataContainer().has(editingGamemodeKey, PersistentDataType.STRING)) {
            return;
        }
        for (int raw : event.getRawSlots()) {
            if ((raw >= 0 && raw <= 8) || raw == 45) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Drop — prevent dropping items while the editor is open
    // -------------------------------------------------------------------------

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getPersistentDataContainer().has(editingGamemodeKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Quit — clean up per-player state
    // -------------------------------------------------------------------------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        activeEditors.remove(uuid);
        resetConfirmTime.remove(uuid);
    }
}
