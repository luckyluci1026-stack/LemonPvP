package com.lemonpvp.lemoncore.gui;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemoncore.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatsGUI {

    private final LemonCore plugin;
    private final PlayerData data;

    public StatsGUI(LemonCore plugin, PlayerData data) {
        this.plugin = plugin;
        this.data = data;
    }

    public void open(Player viewer) {
        String title = plugin.getMessagesManager().getRaw("stats.gui-title")
                .replace("{player}", data.getUsername());
        Inventory inv = Bukkit.createInventory(null, 27, TextUtil.parse(title));

        // Fill with glass pane borders
        ItemStack border = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // Kills - emerald
        inv.setItem(10, buildItem(Material.EMERALD, "<green>Kills",
                List.of("<gray>" + data.getKills())));

        // Deaths - red dye
        inv.setItem(11, buildItem(Material.REDSTONE, "<red>Deaths",
                List.of("<gray>" + data.getDeaths())));

        // K/D ratio - golden sword
        inv.setItem(12, buildItem(Material.GOLDEN_SWORD, "<yellow>K/D Ratio",
                List.of("<gray>" + data.getKDRatio())));

        // Killstreak - fire charge
        inv.setItem(13, buildItem(Material.FIRE_CHARGE, "<gold>Killstreak",
                List.of("<gray>Current: " + data.getKillstreak(),
                        "<gray>Best: " + data.getBestKillstreak())));

        // Coins - gold nugget
        inv.setItem(14, buildItem(Material.GOLD_NUGGET, "<yellow>Coins",
                List.of("<gray>" + data.getCoins())));

        // ELO - diamond
        List<String> eloLore = new ArrayList<>();
        if (data.getElo().isEmpty()) {
            eloLore.add("<gray>No ELO data yet");
        } else {
            for (Map.Entry<String, Integer> entry : data.getElo().entrySet()) {
                eloLore.add("<gray>" + capitalize(entry.getKey()) + ": <white>" + entry.getValue());
            }
        }
        inv.setItem(16, buildItem(Material.DIAMOND, "<aqua>ELO", eloLore));

        viewer.openInventory(inv);
    }

    private ItemStack buildItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse("<reset>" + name));
        List<Component> loreComponents = new ArrayList<>();
        for (String l : lore) loreComponents.add(TextUtil.parse("<reset>" + l));
        meta.lore(loreComponents);
        meta.setHideTooltip(false);
        item.setItemMeta(meta);
        return item;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
