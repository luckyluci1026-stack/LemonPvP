package com.lemonpvp.lemonlobby.gui;

import com.lemonpvp.lemonlobby.LemonLobby;
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
 * The lobby "play" menu: pick a gamemode, get sent to the duels server and
 * auto-queued there (handoff via the shared lp_pending_queue table, read by
 * LemonPractice on join). One click in the lobby → fighting within seconds
 * (the duels server falls back to a bot match if the queue stays empty).
 */
public class QueueSelectGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int SIZE = 36;

    /** id, display name, icon, slot — mirrors the duels server's gamemodes. */
    private record Mode(String id, String display, Material icon, int slot) {}

    private static final List<Mode> MODES = List.of(
            new Mode("sword",      "<yellow>Sword",          Material.DIAMOND_SWORD,  10),
            new Mode("shield",     "<blue>Shield",           Material.SHIELD,         11),
            new Mode("crystal",    "<aqua>Crystal",          Material.END_CRYSTAL,    12),
            new Mode("mace",       "<gold>Mace",             Material.MACE,           13),
            new Mode("uhc",        "<red>UHC",               Material.GOLDEN_APPLE,   14),
            new Mode("smp",        "<green>SMP",             Material.NETHERITE_SWORD, 15),
            new Mode("spear-mace", "<light_purple>Spear Mace", Material.TRIDENT,      16),
            new Mode("cart",       "<dark_aqua>Cart",        Material.TNT_MINECART,   22));

    private final LemonLobby plugin;
    private final Player player;
    private final Map<Integer, Mode> slotMap = new HashMap<>();
    private Inventory inv;
    private boolean registered;
    private boolean busy;

    public QueueSelectGUI(LemonLobby plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inv = Bukkit.createInventory(null, SIZE, MM.deserialize(
                "<!italic><gradient:#fffb00:#00ff00><bold>Play</bold></gradient> <dark_gray>» <gray>Pick a gamemode"));

        ItemStack pane = named(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < SIZE; i++) inv.setItem(i, pane);

        for (Mode m : MODES) {
            inv.setItem(m.slot(), named(m.icon(), "<bold>" + m.display(), List.of(
                    "",
                    "<gray>Queue up for a ranked <white>" + m.id() + " <gray>duel.",
                    "<gray>No opponent within <white>15s<gray>? You fight a bot",
                    "<gray>matched to your skill instead.",
                    "",
                    "<green>► Click to play")));
            slotMap.put(m.slot(), m);
        }
        inv.setItem(31, named(Material.BARRIER, "<red>Close", List.of()));

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(player.getUniqueId())) return;
        e.setCancelled(true);

        if (e.getRawSlot() == 31) { p.closeInventory(); return; }
        Mode mode = slotMap.get(e.getRawSlot());
        if (mode == null || busy) return;
        busy = true;

        UUID uuid = p.getUniqueId();
        p.closeInventory();
        p.sendMessage(MM.deserialize("<!italic><gradient:#fffb00:#00ff00><bold>Queue</bold></gradient> "
                + "<gray>Sending you into a <yellow>" + mode.id() + " <gray>match..."));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.4f);

        // Write the handoff marker async, THEN switch servers — the duels server
        // reads + deletes it on join and queues the player automatically.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabase().savePendingQueue(uuid, mode.id());
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
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
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
