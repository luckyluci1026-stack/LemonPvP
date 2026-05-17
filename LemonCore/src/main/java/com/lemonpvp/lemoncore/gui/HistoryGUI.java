package com.lemonpvp.lemoncore.gui;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.BanRecord;
import com.lemonpvp.lemoncore.managers.MuteRecord;
import com.lemonpvp.lemoncore.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HistoryGUI {

    private final LemonCore plugin;
    private final UUID targetUuid;
    private final String targetName;

    public HistoryGUI(LemonCore plugin, UUID targetUuid, String targetName) {
        this.plugin = plugin;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    public void open(Player admin) {
        String title = plugin.getMessagesManager().getRaw("history.gui-title")
                .replace("{player}", targetName);

        plugin.getBanManager().getHistory(targetUuid).thenAccept(bans -> {
            plugin.getMuteManager().getHistory(targetUuid).thenAccept(mutes -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    int size = Math.max(9, Math.min(54, ((bans.size() + mutes.size() + 8) / 9) * 9));
                    if (size == 0) size = 9;
                    Inventory inv = Bukkit.createInventory(null, size, TextUtil.parse(title));

                    int slot = 0;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                    for (BanRecord ban : bans) {
                        if (slot >= size) break;
                        List<String> lore = new ArrayList<>();
                        lore.add("<gray>Reason: <white>" + ban.reason);
                        lore.add("<gray>By: <white>" + (ban.bannerName != null ? ban.bannerName : "System"));
                        lore.add("<gray>Date: <white>" + sdf.format(ban.banTime));
                        lore.add("<gray>Expires: <white>" + (ban.isPermanent() ? "Permanent" : sdf.format(ban.expires)));
                        lore.add("<gray>Active: <white>" + (ban.active && !ban.isExpired()));
                        lore.add("<gray>ID: <white>" + ban.id);
                        inv.setItem(slot++, makeItem(Material.BARRIER, "<red>Ban", lore));
                    }

                    for (MuteRecord mute : mutes) {
                        if (slot >= size) break;
                        List<String> lore = new ArrayList<>();
                        lore.add("<gray>Reason: <white>" + mute.reason);
                        lore.add("<gray>By: <white>" + (mute.muterName != null ? mute.muterName : "System"));
                        lore.add("<gray>Date: <white>" + sdf.format(mute.muteTime));
                        lore.add("<gray>Expires: <white>" + (mute.isPermanent() ? "Permanent" : sdf.format(mute.expires)));
                        lore.add("<gray>Active: <white>" + (mute.active && !mute.isExpired()));
                        inv.setItem(slot++, makeItem(Material.NAME_TAG, "<yellow>Mute", lore));
                    }

                    if (slot == 0) {
                        admin.sendMessage(plugin.getMessagesManager().get("history.no-history", "player", targetName));
                        return;
                    }

                    admin.openInventory(inv);
                });
            });
        });
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse("<reset>" + name));
        List<Component> loreComp = new ArrayList<>();
        for (String l : lore) loreComp.add(TextUtil.parse("<reset>" + l));
        meta.lore(loreComp);
        item.setItemMeta(meta);
        return item;
    }
}
