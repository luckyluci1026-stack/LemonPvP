package com.lemonpvp.lemonlobby.gui;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.database.Database.TournamentInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The lobby "tournament queue": lists the tournaments currently accepting
 * players (read from the shared events tables) and, on click, signs the player
 * up and sends them to the duels server queued for that gamemode — every ranked
 * win there counts toward their tournament standing.
 */
public class TournamentSelectGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int SIZE = 36;

    private final LemonLobby plugin;
    private final Player player;
    private final Map<Integer, TournamentInfo> slotMap = new HashMap<>();
    private Inventory inv;
    private boolean registered;
    private boolean busy;

    public TournamentSelectGUI(LemonLobby plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        // Read the joinable tournaments off-thread, then build the menu on the main thread.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<TournamentInfo> list = plugin.getDatabase().joinableTournaments();
            Bukkit.getScheduler().runTask(plugin, () -> build(list));
        });
    }

    private void build(List<TournamentInfo> list) {
        if (!player.isOnline()) return;
        if (list.isEmpty()) {
            player.sendMessage(MM.deserialize("<!italic><gradient:#fffb00:#ffa751><bold>Tournaments</bold></gradient> "
                    + "<gray>» There are no open tournaments right now."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            return;
        }

        inv = Bukkit.createInventory(null, SIZE, MM.deserialize(
                "<!italic><gradient:#fffb00:#ffa751><bold>Tournaments</bold></gradient> <dark_gray>» <gray>Join & queue"));
        ItemStack pane = named(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < SIZE; i++) inv.setItem(i, pane);

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < list.size() && i < slots.length; i++) {
            TournamentInfo t = list.get(i);
            int slot = slots[i];
            boolean qualifying = "QUALIFICATION".equalsIgnoreCase(t.state());
            inv.setItem(slot, named(qualifying ? Material.DIAMOND_SWORD : Material.WRITABLE_BOOK,
                    "<gold><bold>" + t.name(), List.of(
                    "",
                    "<gray>Gamemode: <white>" + t.gamemode(),
                    "<gray>Status: " + (qualifying ? "<green>Qualification live" : "<yellow>Sign-ups open"),
                    "",
                    "<green>► Click to join & queue " + t.gamemode())));
            slotMap.put(slot, t);
        }
        inv.setItem(31, named(Material.BARRIER, "<red>Close", List.of()));

        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(player.getUniqueId())) return;
        e.setCancelled(true);

        if (e.getRawSlot() == 31) { p.closeInventory(); return; }
        TournamentInfo t = slotMap.get(e.getRawSlot());
        if (t == null || busy) return;
        busy = true;

        UUID uuid = p.getUniqueId();
        p.closeInventory();
        p.sendMessage(MM.deserialize("<!italic><gradient:#fffb00:#ffa751><bold>Tournament</bold></gradient> "
                + "<gray>Signed up for <white>" + t.name() + "<gray> — sending you to queue <yellow>" + t.gamemode() + "<gray>..."));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);

        // Sign up + write the queue handoff off-thread, then switch to the duels server.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabase().tournamentSignup(t.id(), uuid);
            plugin.getDatabase().savePendingQueue(uuid, t.gamemode());
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null && online.isOnline()) {
                    plugin.getLobbyMessaging().connectToServer(online,
                            plugin.getServersConfig().getString("servers.duels.name", "duels"));
                }
            });
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        if (registered) { HandlerList.unregisterAll(this); registered = false; }
    }

    private ItemStack named(Material mat, String mini, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + mini));
            if (!lore.isEmpty()) {
                List<Component> l = new ArrayList<>();
                for (String s : lore) l.add(MM.deserialize("<!italic>" + s));
                meta.lore(l);
            }
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                    org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }
}
