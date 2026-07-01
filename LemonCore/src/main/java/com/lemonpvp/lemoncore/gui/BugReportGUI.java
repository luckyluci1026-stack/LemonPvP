package com.lemonpvp.lemoncore.gui;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.ReportManager;
import com.lemonpvp.lemoncore.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interactive, paginated bug-report management GUI.
 *
 * <p>Each bug shows its reporter and the full description (word-wrapped into
 * lore). Left-click prints the full report to chat, right-click marks it
 * resolved. Navigation and an info header surround a 28-slot content area.</p>
 */
public class BugReportGUI implements Listener {

    private static final int SIZE = 54;
    private static final int[] CONTENT = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int INFO_SLOT = 4;
    private static final int PREV_SLOT = 48;
    private static final int CLOSE_SLOT = 49;
    private static final int NEXT_SLOT = 50;

    private final LemonCore plugin;
    private final UUID adminUuid;

    private Inventory inv;
    private boolean registered;
    private int page;
    private List<ReportManager.BugReport> bugs = new ArrayList<>();
    private final Map<Integer, ReportManager.BugReport> slotMap = new HashMap<>();

    public BugReportGUI(LemonCore plugin) {
        this.plugin = plugin;
        this.adminUuid = null;
    }

    private BugReportGUI(LemonCore plugin, UUID adminUuid) {
        this.plugin = plugin;
        this.adminUuid = adminUuid;
    }

    public void open(Player admin) {
        new BugReportGUI(plugin, admin.getUniqueId()).load();
    }

    private void load() {
        plugin.getReportManager().getBugReports().thenAccept(list ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    this.bugs = list != null ? list : new ArrayList<>();
                    Player a = admin();
                    if (a == null) return;
                    if (page > maxPage()) page = maxPage();
                    render(a);
                }));
    }

    private void render(Player a) {
        String title = plugin.getMessagesManager().getRaw("bug.gui-title");
        if (title == null || title.isEmpty()) title = "<gold>Bug Reports";
        if (inv == null || inv.getSize() != SIZE) {
            inv = Bukkit.createInventory(null, SIZE,
                    TextUtil.parse(title + " <dark_gray>(" + bugs.size() + ")"));
        }
        inv.clear();
        slotMap.clear();

        ItemStack border = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, border);
        }

        inv.setItem(INFO_SLOT, infoItem());

        int start = page * CONTENT.length;
        for (int i = 0; i < CONTENT.length; i++) {
            int idx = start + i;
            if (idx >= bugs.size()) break;
            ReportManager.BugReport r = bugs.get(idx);
            inv.setItem(CONTENT[i], bugItem(r));
            slotMap.put(CONTENT[i], r);
        }

        if (bugs.isEmpty()) {
            inv.setItem(22, makeItem(Material.LIME_DYE,
                    "<green>No open bug reports", List.of("<gray>All clear! ✔")));
        }

        if (page > 0) inv.setItem(PREV_SLOT, makeItem(Material.ARROW,
                "<yellow>← Page " + page, List.of("<gray>Click for the previous page")));
        inv.setItem(CLOSE_SLOT, makeItem(Material.BARRIER, "<red>Close", List.of()));
        if (page < maxPage()) inv.setItem(NEXT_SLOT, makeItem(Material.ARROW,
                "<yellow>Page " + (page + 2) + " →", List.of("<gray>Click for the next page")));

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        a.openInventory(inv);
    }

    private ItemStack infoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Open bugs: <white>" + bugs.size());
        lore.add("<gray>Page: <white>" + (page + 1) + "<gray>/<white>" + (maxPage() + 1));
        lore.add("");
        lore.add("<yellow>Left-click <gray>→ Show full report in chat");
        lore.add("<yellow>Right-click <gray>→ Mark as resolved");
        return makeItem(Material.BOOK, "<gradient:#ffb300:#e65100><bold>Bug Overview", lore);
    }

    private ItemStack bugItem(ReportManager.BugReport r) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>By: <white>" + r.reporterName);
        lore.add("<gray>When: <white>" + relativeTime(r.reportTime.getTime()));
        lore.add("");
        lore.add("<gray>Description:");
        for (String wrapped : wrap(r.description, 38)) lore.add("<white>" + wrapped);
        lore.add("");
        lore.add("<yellow>▸ Left-click: <gray>full text in chat");
        lore.add("<yellow>▸ Right-click: <gray>resolve");
        return makeItem(Material.WRITABLE_BOOK, "<red><bold>Bug #" + r.id, lore);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(adminUuid)) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == CLOSE_SLOT) { p.closeInventory(); return; }
        if (slot == PREV_SLOT && page > 0) { page--; render(p); return; }
        if (slot == NEXT_SLOT && page < maxPage()) { page++; render(p); return; }

        ReportManager.BugReport r = slotMap.get(slot);
        if (r == null) return;

        if (e.getClick() == ClickType.RIGHT) {
            plugin.getReportManager().resolveBugReport(r.id).thenRun(() ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player a = admin();
                        if (a == null) return;
                        a.sendMessage(TextUtil.parse("<green>Bug <white>#" + r.id + " <green>marked as resolved."));
                        a.playSound(a.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
                        load();
                    }));
        } else if (e.getClick() == ClickType.LEFT) {
            p.sendMessage(TextUtil.parse("<gold><bold>Bug #" + r.id + " <reset><gray>by <white>" + r.reporterName + ":"));
            p.sendMessage(TextUtil.parse("<white>" + r.description));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(adminUuid)) return;
        if (registered) { HandlerList.unregisterAll(this); registered = false; }
    }

    // -- helpers --

    private Player admin() { return adminUuid == null ? null : Bukkit.getPlayer(adminUuid); }

    private int maxPage() {
        if (bugs.isEmpty()) return 0;
        return (bugs.size() - 1) / CONTENT.length;
    }

    private static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) { out.add("-"); return out; }
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (line.length() + word.length() + 1 > width) {
                if (line.length() > 0) { out.add(line.toString()); line.setLength(0); }
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add(line.toString());
        if (out.size() > 8) {
            List<String> capped = new ArrayList<>(out.subList(0, 8));
            capped.add("…");
            return capped;
        }
        return out;
    }

    private static String relativeTime(long epochMillis) {
        long diff = Math.max(0, System.currentTimeMillis() - epochMillis);
        long sec = diff / 1000;
        if (sec < 60) return "just now";
        long min = sec / 60;
        if (min < 60) return min + "m ago";
        long hrs = min / 60;
        if (hrs < 24) return hrs + "h ago";
        return (hrs / 24) + "d ago";
    }

    private ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse("<!italic><reset>" + name));
            List<Component> loreComp = new ArrayList<>();
            for (String l : lore) loreComp.add(TextUtil.parse("<!italic><reset>" + l));
            meta.lore(loreComp);
            item.setItemMeta(meta);
        }
        return item;
    }
}
