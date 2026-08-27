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
        removeHotbar(player);

        player.getInventory().setItem(0, buildItem(
                Material.DIAMOND_SWORD,
                "<gradient:#fffb00:#00ff00>Qᴜᴇᴜᴇ</gradient>",
                -1
        ));

        player.getInventory().setItem(1, buildItem(
                Material.COMPASS,
                "<aqua>ᴛʀᴀɪɴɪɴɢ</aqua>",
                -1
        ));

        player.getInventory().setItem(2, buildItem(
                Material.FIREWORK_ROCKET,
                "<gradient:#ff9800:#ff5722>ʙᴏᴏsᴛ</gradient>",
                -1
        ));

        player.getInventory().setItem(3, buildItem(
                Material.NETHER_STAR,
                "<gold>ᴇᴠᴇɴᴛs</gold>",
                -1
        ));

        player.getInventory().setItem(4, buildItem(
                Material.WRITTEN_BOOK,
                "<yellow>Kɪᴛ ᴇᴅɪᴛᴏʀ</yellow>",
                -1
        ));

        player.getInventory().setItem(5, buildItem(
                Material.GOLDEN_SWORD,
                "<gradient:#fffb00:#ffa751>ᴛᴏᴜʀɴᴀᴍᴇɴᴛ</gradient>",
                -1
        ));

        player.getInventory().setItem(6, buildItem(
                Material.DIAMOND,
                "<light_purple>ᴄᴏsᴍᴇᴛɪᴄs</light_purple>",
                -1
        ));

        player.getInventory().setItem(7, buildItem(
                Material.REPEATER,
                "<gray>sᴇᴛᴛɪɴɢs</gray>",
                -1
        ));

        player.getInventory().setItem(8, buildItem(
                Material.RED_BED,
                "<red>ʟᴇᴀᴠᴇ</red>",
                -1
        ));
    }

    private ItemStack buildItem(Material material, String miniMessageName, int customModelData) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        Component displayName = MINI_MESSAGE.deserialize("<!italic>" + miniMessageName + "");
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
