package com.lemonpvp.lemonlobby.managers;

import com.lemonpvp.lemonlobby.LemonLobby;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class HotbarManager {

    private final LemonLobby plugin;
    private final NamespacedKey hotbarKey;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public HotbarManager(LemonLobby plugin) {
        this.plugin = plugin;
        this.hotbarKey = new NamespacedKey(plugin, "hotbar");
    }

    public void giveHotbar(Player player) {
        // Clear existing hotbar items first
        removeHotbar(player);

        // Slot 0: Diamond Sword - Queue
        player.getInventory().setItem(0, buildItem(
                Material.DIAMOND_SWORD,
                "<font:lemonpvp:default>Qᴜᴇᴜᴇ</font>",
                0
        ));

        // Slot 1: Compass - Training
        player.getInventory().setItem(1, buildItem(
                Material.COMPASS,
                "<font:lemonpvp:default>ᴛʀᴀɪɴɪɴɢ</font>",
                -1
        ));

        // Slot 3: Nether Star - Events
        player.getInventory().setItem(3, buildItem(
                Material.NETHER_STAR,
                "<font:lemonpvp:default>ᴇᴠᴇɴᴛs</font>",
                -1
        ));

        // Slot 4: Written Book - Kit Editor
        player.getInventory().setItem(4, buildItem(
                Material.WRITTEN_BOOK,
                "<font:lemonpvp:default>Kɪᴛ ᴇᴅɪᴛᴏʀ</font>",
                -1
        ));

        // Slot 6: Diamond - Cosmetics
        player.getInventory().setItem(6, buildItem(
                Material.DIAMOND,
                "<font:lemonpvp:default>ᴄᴏsᴍᴇᴛɪᴄs</font>",
                -1
        ));

        // Slot 7: Compass - Settings
        player.getInventory().setItem(7, buildItem(
                Material.COMPASS,
                "<font:lemonpvp:default>sᴇᴛᴛɪɴɢs</font>",
                -1
        ));

        // Slot 8: Red Bed - Leave
        player.getInventory().setItem(8, buildItem(
                Material.RED_BED,
                "<font:lemonpvp:default>ʟᴇᴀᴠᴇ</font>",
                -1
        ));
    }

    private ItemStack buildItem(Material material, String miniMessageName, int customModelData) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        Component displayName = MINI_MESSAGE.deserialize("<!italic>" + miniMessageName + "</!italic>");
        meta.displayName(displayName);

        if (customModelData >= 0) {
            meta.setCustomModelData(customModelData);
        }

        // Mark as hotbar item via PDC
        meta.getPersistentDataContainer().set(hotbarKey, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public void removeHotbar(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isHotbarItem(item)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    public boolean isHotbarItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(hotbarKey, PersistentDataType.BYTE);
    }

    public NamespacedKey getHotbarKey() {
        return hotbarKey;
    }
}
