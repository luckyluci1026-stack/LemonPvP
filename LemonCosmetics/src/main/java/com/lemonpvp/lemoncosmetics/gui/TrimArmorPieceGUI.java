package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.ArmorSlotType;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
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

public class TrimArmorPieceGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCosmetics plugin;
    private final Player player;
    private final String selectedPatternId;
    private final NamespacedKey slotKey;
    private Inventory inventory;
    private boolean registered = false;

    public TrimArmorPieceGUI(LemonCosmetics plugin, Player player, String selectedPatternId) {
        this.plugin = plugin;
        this.player = player;
        this.selectedPatternId = selectedPatternId;
        this.slotKey = new NamespacedKey(plugin, "cosmetics_slot");
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27,
                MM.deserialize("<gradient:#fffb00:#00ff00>Select Armor Piece</gradient>"));

        ItemStack filler = filler();
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());

        int[] slots = {11, 12, 14, 15};
        ArmorSlotType[] pieces = ArmorSlotType.values();
        for (int i = 0; i < pieces.length; i++) {
            inventory.setItem(slots[i], buildPieceItem(pieces[i], cosmetics));
        }

        inventory.setItem(22, backButton());

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
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

        String slotName = meta.getPersistentDataContainer().get(slotKey, PersistentDataType.STRING);
        if (slotName == null) {
            if (clicked.getType() == Material.ARROW) {
                unregister();
                new TrimPatternGUI(plugin, player).open();
            }
            return;
        }

        try {
            ArmorSlotType slot = ArmorSlotType.valueOf(slotName.toUpperCase());
            unregister();
            new TrimMaterialGUI(plugin, player, selectedPatternId, slot).open();
        } catch (IllegalArgumentException ignored) {}
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        if (!event.getInventory().equals(inventory)) return;
        unregister();
    }

    private void unregister() {
        if (registered) { HandlerList.unregisterAll(this); registered = false; }
    }

    private ItemStack buildPieceItem(ArmorSlotType slot, PlayerCosmetics cosmetics) {
        ItemStack item = new ItemStack(slot.getIconMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<white>" + slot.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        String[] trim = cosmetics != null ? cosmetics.getAppliedTrim(slot.name().toLowerCase()) : null;
        if (trim != null) {
            String patDisp = plugin.getArmorTrimManager().getDisplayName(trim[0]);
            String matDisp = plugin.getArmorTrimManager().getMaterialDisplayName(trim[1]);
            lore.add(MM.deserialize("<gray>Current trim: <yellow>" + patDisp + " / " + matDisp));
        } else {
            lore.add(MM.deserialize("<gray>Current trim: <gray>None"));
        }
        lore.add(Component.empty());
        lore.add(MM.deserialize("<green>Click to change material"));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(slotKey, PersistentDataType.STRING, slot.name().toLowerCase());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(MM.deserialize("<gray>Back")); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack filler() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); pane.setItemMeta(meta); }
        return pane;
    }
}
