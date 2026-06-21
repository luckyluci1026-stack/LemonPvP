package com.lemonpvp.lemoncore.gui;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.ReportManager;
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

public class ReportGUI {

    private final LemonCore plugin;

    public ReportGUI(LemonCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player admin) {
        String title = plugin.getMessagesManager().getRaw("report.gui-title");
        UUID adminUuid = admin.getUniqueId();
        plugin.getReportManager().getReports().thenAccept(reports -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player a = Bukkit.getPlayer(adminUuid);
                if (a == null) return;
                int size = Math.max(9, Math.min(54, ((reports.size() + 8) / 9) * 9));
                if (size == 0) size = 9;
                Inventory inv = Bukkit.createInventory(null, size, TextUtil.parse(title));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                int slot = 0;
                for (ReportManager.Report r : reports) {
                    if (slot >= size) break;
                    List<String> lore = new ArrayList<>();
                    lore.add("<gray>Reporter: <white>" + r.reporterName);
                    lore.add("<gray>Reported: <white>" + r.reportedName);
                    lore.add("<gray>Reason: <white>" + r.reason);
                    lore.add("<gray>Date: <white>" + sdf.format(r.reportTime));
                    inv.setItem(slot++, makeItem(Material.PAPER, "<yellow>Report #" + r.id, lore));
                }
                a.openInventory(inv);
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
