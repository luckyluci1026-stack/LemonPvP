package com.lemonpvp.lemoncore.gui;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemoncore.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SettingsGUI implements Listener {

    private final LemonCore plugin;
    private final Player player;
    private final PlayerData data;
    private Inventory inv;

    public SettingsGUI(LemonCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.data = plugin.getPlayerDataManager().getCached(player.getUniqueId());
    }

    public void open() {
        if (data == null) return;

        String title = plugin.getMessagesManager().getRaw("settings.gui-title");
        inv = Bukkit.createInventory(null, 27, TextUtil.parse(title));
        refresh();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(inv);
    }

    private void refresh() {
        if (data == null) return;

        // Fill border
        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // Public Chat toggle
        inv.setItem(10, makeToggleItem(
                data.isPublicChat() ? Material.LIME_DYE : Material.GRAY_DYE,
                data.isPublicChat() ? "settings.public-chat-on" : "settings.public-chat-off"));

        // Party Invites toggle
        inv.setItem(12, makeToggleItem(
                data.isPartyInvites() ? Material.LIME_DYE : Material.GRAY_DYE,
                data.isPartyInvites() ? "settings.party-invites-on" : "settings.party-invites-off"));

        // Messages toggle
        inv.setItem(14, makeToggleItem(
                data.isMessagesEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                data.isMessagesEnabled() ? "settings.messages-on" : "settings.messages-off"));

        // Fast Crystals toggle
        inv.setItem(16, makeToggleItem(
                data.isFastCrystals() ? Material.LIME_DYE : Material.GRAY_DYE,
                data.isFastCrystals() ? "settings.fast-crystals-on" : "settings.fast-crystals-off"));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() != inv) return;
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!clicker.equals(player)) return;
        event.setCancelled(true);

        if (data == null) return;

        switch (event.getSlot()) {
            case 10 -> {
                data.setPublicChat(!data.isPublicChat());
                plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
                refresh();
            }
            case 12 -> {
                data.setPartyInvites(!data.isPartyInvites());
                plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
                refresh();
            }
            case 14 -> {
                data.setMessagesEnabled(!data.isMessagesEnabled());
                plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
                refresh();
            }
            case 16 -> {
                data.setFastCrystals(!data.isFastCrystals());
                plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
                refresh();
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory() != inv) return;
        HandlerList.unregisterAll(this);
    }

    private ItemStack makeItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse("<reset>" + name));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeToggleItem(Material mat, String msgKey) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String raw = plugin.getMessagesManager().getRaw(msgKey);
        meta.displayName(TextUtil.parse("<reset>" + raw));
        meta.lore(List.of(TextUtil.parse("<reset><gray>Click to toggle")));
        item.setItemMeta(meta);
        return item;
    }
}
