package com.lemonpvp.lemontraining.gui;

import com.lemonpvp.lemontraining.LemonTraining;
import com.lemonpvp.lemontraining.model.SwordDifficulty;
import com.lemonpvp.lemontraining.practice.SwordPractice;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

public class SwordDifficultyGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonTraining plugin;
    private final SwordPractice swordPractice;
    private Inventory inv;
    private boolean registered = false;

    public SwordDifficultyGUI(LemonTraining plugin, SwordPractice swordPractice) {
        this.plugin = plugin;
        this.swordPractice = swordPractice;
    }

    public void open(Player player) {
        inv = Bukkit.createInventory(null, 27,
                MM.deserialize("<gradient:#fffb00:#00ff00><bold>sᴡᴏʀᴅ ᴘʀᴀᴄᴛɪᴄᴇ</bold></gradient>"));

        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        inv.setItem(11, makeItem(Material.LIME_DYE,
                "<!italic><green>Easy</green>",
                List.of("<!italic><gray>Zombie without AI")));

        inv.setItem(13, makeItem(Material.YELLOW_DYE,
                "<!italic><yellow>Medium</yellow>",
                List.of("<!italic><gray>Zombie with Slowness I")));

        inv.setItem(15, makeItem(Material.RED_DYE,
                "<!italic><red>Normal</red>",
                List.of("<!italic><gray>Zombie with full AI")));

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.3f);
        player.openInventory(inv);
    }

    private ItemStack makeItem(Material mat, String name) {
        return makeItem(mat, name, null);
    }

    private ItemStack makeItem(Material mat, String name, List<String> loreParts) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(name));
            if (loreParts != null) {
                meta.lore(loreParts.stream().map(MM::deserialize).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!clicker.getUniqueId().equals(swordPractice.getSession().getUuid())) return;
        if (!event.getInventory().equals(inv)) return;

        event.setCancelled(true);

        SwordDifficulty difficulty = switch (event.getSlot()) {
            case 11 -> SwordDifficulty.EASY;
            case 13 -> SwordDifficulty.MEDIUM;
            case 15 -> SwordDifficulty.NORMAL;
            default -> null;
        };

        if (difficulty != null) {
            clicker.playSound(clicker.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
            clicker.closeInventory();
            swordPractice.startWithDifficulty(difficulty);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getPlayer().getUniqueId().equals(swordPractice.getSession().getUuid())) return;
        if (!event.getInventory().equals(inv)) return;
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }
}
