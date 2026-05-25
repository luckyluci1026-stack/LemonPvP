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

    public SwordDifficultyGUI(LemonTraining plugin, SwordPractice swordPractice) {
        this.plugin = plugin;
        this.swordPractice = swordPractice;
    }

    public void open(Player player) {
        inv = Bukkit.createInventory(null, 27,
                MM.deserialize("<font:lemonpvp:default>sᴡᴏʀᴅ ᴘʀᴀᴄᴛɪᴄᴇ</font>"));

        // Fill with gray glass panes
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Slot 11: Easy
        inv.setItem(11, makeItem(Material.LIME_DYE,
                "<green>Easy",
                List.of("<gray>Zombie without AI</gray>")));

        // Slot 13: Medium
        inv.setItem(13, makeItem(Material.YELLOW_DYE,
                "<yellow>Medium",
                List.of("<gray>Zombie with Slowness I</gray>")));

        // Slot 15: Normal
        inv.setItem(15, makeItem(Material.RED_DYE,
                "<red>Normal",
                List.of("<gray>Zombie with full AI</gray>")));

        Bukkit.getPluginManager().registerEvents(this, plugin);
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
            clicker.closeInventory();
            swordPractice.startWithDifficulty(difficulty);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getPlayer().getUniqueId().equals(swordPractice.getSession().getUuid())) return;
        HandlerList.unregisterAll(this);
    }
}
