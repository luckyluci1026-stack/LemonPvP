package com.lemonpvp.lemonlobby.gui;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.messaging.LobbyMessaging;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class TrainingGUI {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final LemonLobby plugin;
    private final LobbyMessaging lobbyMessaging;
    private final NamespacedKey guiKey;

    // Slot-to-mode mappings
    private static final int SLOT_TOTEM = 11;
    private static final int SLOT_BOW = 12;
    private static final int SLOT_MACE = 13;
    private static final int SLOT_SWORD = 14;
    private static final int SLOT_CRYSTAL = 15;

    public TrainingGUI(LemonLobby plugin, LobbyMessaging lobbyMessaging) {
        this.plugin = plugin;
        this.lobbyMessaging = lobbyMessaging;
        this.guiKey = new NamespacedKey(plugin, "training_gui");
    }

    public void open(Player player) {
        Component title = MINI_MESSAGE.deserialize("<gradient:#fffb00:#00ff00><bold>Training</bold></gradient>");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Fill all slots with gray glass pane filler
        ItemStack filler = buildFiller();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Mode items
        inv.setItem(SLOT_TOTEM, buildModeItem(Material.TOTEM_OF_UNDYING, "Totem Practice", "TOTEM"));
        inv.setItem(SLOT_BOW, buildModeItem(Material.BOW, "Bow Practice", "BOW"));
        inv.setItem(SLOT_MACE, buildModeItem(Material.MACE, "Mace Practice", "MACE"));
        inv.setItem(SLOT_SWORD, buildModeItem(Material.DIAMOND_SWORD, "Sword Practice", "SWORD"));
        inv.setItem(SLOT_CRYSTAL, buildModeItem(Material.END_CRYSTAL, "Crystal Practice", "CRYSTAL"));

        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.3f);
        player.openInventory(inv);
    }

    private ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            // Tag as GUI item so PlayerListener doesn't block it
            meta.getPersistentDataContainer().set(guiKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildModeItem(Material material, String displayName, String mode) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MINI_MESSAGE.deserialize("<!italic><white>" + displayName + "</white>"));
            // Store mode in PDC
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "training_mode"),
                    PersistentDataType.STRING,
                    mode
            );
            // Tag as GUI item
            meta.getPersistentDataContainer().set(guiKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Handles click events in the Training GUI.
     * Returns true if the event was consumed by this GUI.
     */
    public boolean handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return false;

        Component guiTitle = MINI_MESSAGE.deserialize("<gradient:#fffb00:#00ff00><bold>Training</bold></gradient>");
        if (event.getView().title().equals(guiTitle)) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return true;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return true;

            NamespacedKey modeKey = new NamespacedKey(plugin, "training_mode");
            if (!meta.getPersistentDataContainer().has(modeKey, PersistentDataType.STRING)) return true;

            String mode = meta.getPersistentDataContainer().get(modeKey, PersistentDataType.STRING);
            if (mode == null) return true;

            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
            player.closeInventory();
            lobbyMessaging.sendTrainingMode(player, mode);
            return true;
        }
        return false;
    }

    public NamespacedKey getGuiKey() {
        return guiKey;
    }
}
