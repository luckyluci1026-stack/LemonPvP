package com.lemonpvp.lemoncore.gui;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Friends overview, FoxPvP/FlowPvP style. Shows each friend as a player head,
 * online friends first (lit green) then offline (gray). Online friends are
 * paginated across the middle rows. Self-contained {@link Listener}; cancels
 * all clicks and unregisters itself on close.
 */
public class FriendsGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int[] FRIEND_SLOTS = {
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
    private final Player viewer;
    private int page;
    private Inventory inv;
    private boolean registered;

    public FriendsGUI(LemonCore plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    public void open() {
        List<UUID> friends = sortedFriends();
        int perPage = FRIEND_SLOTS.length;
        int maxPage = Math.max(0, (friends.size() - 1) / perPage);
        if (page > maxPage) page = maxPage;
        if (page < 0) page = 0;

        inv = Bukkit.createInventory(null, 54,
                MM.deserialize("<!italic><gradient:#69f0ae:#00b0ff><bold>ꜰʀɪᴇɴᴅs</bold></gradient>"
                        + " <dark_gray>» <white>Page " + (page + 1) + "/" + (maxPage + 1)));

        ItemStack black = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int s = 0; s < 9; s++) inv.setItem(s, black);
        for (int s = 45; s < 54; s++) inv.setItem(s, black);
        inv.setItem(9, black);
        inv.setItem(17, black);
        inv.setItem(18, black);
        inv.setItem(26, black);
        inv.setItem(27, black);
        inv.setItem(35, black);
        inv.setItem(36, black);
        inv.setItem(44, black);

        int onlineCount = (int) friends.stream().filter(u -> Bukkit.getPlayer(u) != null).count();
        inv.setItem(INFO_SLOT, infoItem(friends.size(), onlineCount));

        int start = page * perPage;
        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= friends.size()) {
                inv.setItem(FRIEND_SLOTS[i], null);
                continue;
            }
            inv.setItem(FRIEND_SLOTS[i], friendHead(friends.get(idx)));
        }

        if (page > 0) {
            inv.setItem(PREV_SLOT, simpleItem(Material.ARROW,
                    "<!italic><yellow>◀ Previous Page", List.of()));
        }
        if (page < maxPage) {
            inv.setItem(NEXT_SLOT, simpleItem(Material.ARROW,
                    "<!italic><yellow>Next Page ▶", List.of()));
        }
        inv.setItem(CLOSE_SLOT, simpleItem(Material.BARRIER,
                "<!italic><red><bold>Cʟᴏsᴇ", List.of("<!italic><gray>Close menu")));

        viewer.openInventory(inv);
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.3f);

        if (!registered) {
            registered = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    private List<UUID> sortedFriends() {
        PlayerData data = plugin.getPlayerDataManager().getCached(viewer.getUniqueId());
        List<UUID> list = new ArrayList<>();
        if (data != null) list.addAll(data.getFriends());
        // Online friends first, then alphabetical by name.
        list.sort(Comparator
                .comparing((UUID u) -> Bukkit.getPlayer(u) == null)
                .thenComparing(u -> {
                    String n = Bukkit.getOfflinePlayer(u).getName();
                    return n != null ? n.toLowerCase() : u.toString();
                }));
        return list;
    }

    private ItemStack infoItem(int total, int online) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(viewer);
            meta.displayName(MM.deserialize("<!italic><gradient:#69f0ae:#00b0ff><bold>Yᴏᴜʀ Fʀɪᴇɴᴅs"));
            meta.lore(List.of(
                    Component.empty(),
                    MM.deserialize("<!italic><gray>Total: <white>" + total),
                    MM.deserialize("<!italic><gray>Online: <green>" + online),
                    Component.empty(),
                    MM.deserialize("<!italic><dark_gray>/friend add <name>")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack friendHead(UUID uuid) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (skull.getItemMeta() instanceof SkullMeta meta) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            meta.setOwningPlayer(op);
            Player online = Bukkit.getPlayer(uuid);
            String name = op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);

            if (online != null) {
                meta.displayName(MM.deserialize("<!italic><green><bold>" + name));
                meta.lore(List.of(
                        Component.empty(),
                        MM.deserialize("<!italic><gray>Status: <green>● Online"),
                        Component.empty(),
                        MM.deserialize("<!italic><dark_gray>/friend remove " + name)));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.displayName(MM.deserialize("<!italic><gray><bold>" + name));
                meta.lore(List.of(
                        Component.empty(),
                        MM.deserialize("<!italic><gray>Status: <red>● Offline"),
                        Component.empty(),
                        MM.deserialize("<!italic><dark_gray>/friend remove " + name)));
            }
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private ItemStack simpleItem(Material mat, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(name));
            if (!loreLines.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String l : loreLines) lore.add(MM.deserialize(l));
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!p.getUniqueId().equals(viewer.getUniqueId())) return;
        if (!e.getInventory().equals(inv)) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == CLOSE_SLOT) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            p.closeInventory();
        } else if (slot == PREV_SLOT && page > 0) {
            page--;
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            open();
        } else if (slot == NEXT_SLOT) {
            page++;
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            open();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!p.getUniqueId().equals(viewer.getUniqueId())) return;
        if (!e.getInventory().equals(inv)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (viewer.getOpenInventory() != null
                    && viewer.getOpenInventory().getTopInventory().equals(inv)) return;
            if (registered) {
                registered = false;
                HandlerList.unregisterAll(this);
            }
        });
    }
}
