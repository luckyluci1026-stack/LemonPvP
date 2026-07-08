package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.database.PracticeDatabase;
import net.kyori.adventure.text.Component;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Paginated browser of available duel replays — click one to watch it. */
public class ReplayBrowserGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int SIZE = 54;
    private static final int[] CONTENT = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int PREV_SLOT = 48;
    private static final int CLOSE_SLOT = 49;
    private static final int NEXT_SLOT = 50;

    private final LemonPractice plugin;
    private final Player player;
    private final boolean admin;
    private Inventory inv;
    private boolean registered;
    private int page;
    private List<PracticeDatabase.ReplayMeta> replays = new ArrayList<>();
    private final Map<Integer, String> slotMap = new HashMap<>();

    public ReplayBrowserGUI(LemonPractice plugin, Player player) {
        this(plugin, player, false);
    }

    /** @param admin when true, the browser shows an admin header and right-click deletes a replay. */
    public ReplayBrowserGUI(LemonPractice plugin, Player player, boolean admin) {
        this.plugin = plugin;
        this.player = player;
        this.admin = admin;
    }

    public void open() {
        plugin.getDatabase().listReplays(200).thenAccept(list -> Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayer(player.getUniqueId());
            if (p == null) return;
            this.replays = list != null ? list : new ArrayList<>();
            String title = admin
                    ? "<!italic><gradient:#ff5555:#ffaa00>Admin Replays</gradient> <dark_gray>(" + replays.size() + ")"
                    : "<!italic><gradient:#fffb00:#00ff00>Replays</gradient> <dark_gray>(" + replays.size() + ")";
            inv = Bukkit.createInventory(null, SIZE, MM.deserialize(title));
            render();
            if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
            p.openInventory(inv);
        }));
    }

    private int maxPage() {
        if (replays.isEmpty()) return 0;
        return (replays.size() - 1) / CONTENT.length;
    }

    private void render() {
        inv.clear();
        slotMap.clear();
        ItemStack border = pane();
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, border);
        }
        if (replays.isEmpty()) {
            inv.setItem(22, named(Material.BARRIER, "<red>No replays yet", List.of("<gray>Play some duels first!")));
        }
        int start = page * CONTENT.length;
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        for (int i = 0; i < CONTENT.length; i++) {
            int idx = start + i;
            if (idx >= replays.size()) break;
            PracticeDatabase.ReplayMeta m = replays.get(idx);
            inv.setItem(CONTENT[i], buildItem(m, sdf));
            slotMap.put(CONTENT[i], m.name());
        }
        if (page > 0) inv.setItem(PREV_SLOT, named(Material.SPECTRAL_ARROW, "<yellow>← Page " + page, List.of()));
        inv.setItem(CLOSE_SLOT, named(Material.BARRIER, "<red>Close", List.of()));
        if (page < maxPage()) inv.setItem(NEXT_SLOT, named(Material.SPECTRAL_ARROW, "<yellow>Page " + (page + 2) + " →", List.of()));
    }

    private ItemStack buildItem(PracticeDatabase.ReplayMeta m, SimpleDateFormat sdf) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta sm) {
            try { sm.setOwningPlayer(Bukkit.getOfflinePlayer(m.player1())); } catch (Exception ignored) {}
            sm.displayName(MM.deserialize("<!italic><yellow>" + m.player1() + " <gray>vs <yellow>" + m.player2()));
            long daysLeft = Math.max(0, (m.expiresAt() - System.currentTimeMillis()) / 86_400_000L);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><gray>Mode: <white>" + m.gamemode()));
            lore.add(MM.deserialize("<!italic><gray>Played: <white>" + sdf.format(new Date(m.createdAt()))));
            lore.add(MM.deserialize("<!italic><gray>Expires in: <white>" + daysLeft + " days"));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><green>► Click to watch"));
            if (admin) lore.add(MM.deserialize("<!italic><red>► Right-click to delete"));
            sm.lore(lore);
            item.setItemMeta(sm);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(player.getUniqueId())) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot == CLOSE_SLOT) { p.closeInventory(); return; }
        if (slot == PREV_SLOT && page > 0) { page--; render(); return; }
        if (slot == NEXT_SLOT && page < maxPage()) { page++; render(); return; }
        String name = slotMap.get(slot);
        if (name != null) {
            if (admin && e.isRightClick()) {
                deleteReplay(p, name);
                return;
            }
            unregister();
            p.closeInventory();
            plugin.getReplayManager().play(p, name);
        }
    }

    /** Admin-only: delete a replay from the DB, then refresh the open browser in place. */
    private void deleteReplay(Player clicker, String name) {
        plugin.getDatabase().deleteReplay(name).thenAccept(deleted -> Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayer(player.getUniqueId());
            if (p == null || inv == null) return;
            if (Boolean.TRUE.equals(deleted)) {
                replays.removeIf(r -> r.name().equals(name));
                if (page > maxPage()) page = maxPage();
                render();
                p.sendMessage(MM.deserialize("<!italic><red>Deleted replay <yellow>" + name));
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_GRINDSTONE_USE, 0.6f, 1.2f);
            } else {
                p.sendMessage(MM.deserialize("<!italic><red>Could not delete <yellow>" + name));
            }
        }));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        unregister();
    }

    private void unregister() {
        if (registered) { HandlerList.unregisterAll(this); registered = false; }
    }

    private ItemStack pane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack named(Material mat, String mini, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + mini));
            if (lore != null && !lore.isEmpty()) {
                List<Component> l = new ArrayList<>();
                for (String s : lore) l.add(MM.deserialize("<!italic>" + s));
                meta.lore(l);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
