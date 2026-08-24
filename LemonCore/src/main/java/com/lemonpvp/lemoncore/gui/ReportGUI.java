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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interactive, paginated player-report management GUI.
 *
 * <p>Each report is shown as the reported player's head with full context.
 * Left-click teleports the staff member to the reported player (if online),
 * right-click marks the report resolved. Navigation and an info header are
 * rendered around a 28-slot content area.</p>
 */
public class ReportGUI implements Listener {

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
    private List<ReportManager.Report> reports = new ArrayList<>();
    private final Map<Integer, ReportManager.Report> slotMap = new HashMap<>();

    public ReportGUI(LemonCore plugin) {
        this.plugin = plugin;
        this.adminUuid = null; // set in open()
    }

    private ReportGUI(LemonCore plugin, UUID adminUuid) {
        this.plugin = plugin;
        this.adminUuid = adminUuid;
    }

    public void open(Player admin) {
        // Re-create as a bound instance so adminUuid is final & per-session.
        ReportGUI bound = new ReportGUI(plugin, admin.getUniqueId());
        bound.load();
    }

    private void load() {
        plugin.getReportManager().getReports().thenAccept(list ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    this.reports = list != null ? list : new ArrayList<>();
                    Player a = admin();
                    if (a == null) return;
                    if (page > maxPage()) page = maxPage();
                    render(a);
                }));
    }

    private void render(Player a) {
        String title = plugin.getMessagesManager().getRaw("report.gui-title");
        if (title == null || title.isEmpty()) title = "<dark_red>Reports";
        if (inv == null || inv.getSize() != SIZE) {
            inv = Bukkit.createInventory(null, SIZE,
                    TextUtil.parse(title + " <dark_gray>(" + reports.size() + ")"));
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
            if (idx >= reports.size()) break;
            ReportManager.Report r = reports.get(idx);
            inv.setItem(CONTENT[i], reportItem(r));
            slotMap.put(CONTENT[i], r);
        }

        if (reports.isEmpty()) {
            inv.setItem(22, makeItem(Material.LIME_DYE,
                    "<green>No open reports", List.of("<gray>All clear! ✔")));
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
        lore.add("<gray>Open reports: <white>" + reports.size());
        lore.add("<gray>Page: <white>" + (page + 1) + "<gray>/<white>" + (maxPage() + 1));
        lore.add("");
        lore.add("<yellow>Left-click <gray>→ Teleport to player");
        lore.add("<yellow>Right-click <gray>→ Mark as resolved");
        return makeItem(Material.BOOK, "<gradient:#ff5252:#b71c1c><bold>Report Overview", lore);
    }

    private ItemStack reportItem(ReportManager.Report r) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta skull) {
            try {
                skull.setOwningPlayer(Bukkit.getOfflinePlayer(r.reportedUuid));
            } catch (Exception ignored) { }
            boolean online = Bukkit.getPlayer(r.reportedUuid) != null;
            skull.displayName(TextUtil.parse("<!italic><reset><red><bold>Report #" + r.id
                    + " <reset>" + (online ? "<green>●" : "<dark_gray>●")));
            List<Component> lore = new ArrayList<>();
            // Escape everything a player controls: the reason is free text straight from
            // /report, so unescaped it would let any player inject MiniMessage formatting
            // into this staff GUI (or break the line entirely with a malformed tag).
            lore.add(TextUtil.parse("<!italic><reset><gray>Reported: <white>"
                    + TextUtil.escapeTags(r.reportedName)
                    + (online ? " <green>(online)" : " <dark_gray>(offline)")));
            lore.add(TextUtil.parse("<!italic><reset><gray>By: <white>"
                    + TextUtil.escapeTags(r.reporterName)));
            lore.add(TextUtil.parse("<!italic><reset><gray>Reason: <yellow>"
                    + TextUtil.escapeTags(r.reason)));
            lore.add(TextUtil.parse("<!italic><reset><gray>When: <white>" + relativeTime(r.reportTime.getTime())));
            lore.add(Component.empty());
            lore.add(TextUtil.parse(online
                    ? "<reset><yellow>▸ Left-click: <gray>teleport"
                    : "<reset><dark_gray>▸ Player offline"));
            lore.add(TextUtil.parse("<!italic><reset><yellow>▸ Right-click: <gray>resolve"));
            skull.lore(lore);
            item.setItemMeta(skull);
        }
        return item;
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

        ReportManager.Report r = slotMap.get(slot);
        if (r == null) return;

        if (e.getClick() == ClickType.RIGHT) {
            plugin.getReportManager().resolveReport(r.id).thenRun(() ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player a = admin();
                        if (a == null) return;
                        a.sendMessage(TextUtil.parse("<green>Report <white>#" + r.id + " <green>marked as resolved."));
                        a.playSound(a.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
                        load();
                    }));
        } else if (e.getClick() == ClickType.LEFT) {
            Player target = Bukkit.getPlayer(r.reportedUuid);
            if (target != null) {
                p.closeInventory();
                p.teleport(target.getLocation());
                p.sendMessage(TextUtil.parse("<green>Teleported to <white>" + target.getName()));
            } else {
                p.sendMessage(TextUtil.parse("<red>" + TextUtil.escapeTags(r.reportedName)
                        + " is not online."));
            }
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
        if (reports.isEmpty()) return 0;
        return (reports.size() - 1) / CONTENT.length;
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
