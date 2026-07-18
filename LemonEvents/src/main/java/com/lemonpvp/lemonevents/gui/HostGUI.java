package com.lemonpvp.lemonevents.gui;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.model.HostedEvent;
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
import java.util.List;

/**
 * The click-driven face of {@code /host} — no subcommands needed. State-aware:
 * with no event running it offers "Host FFA" / "Host Battle" (name typed in
 * chat); while your event is in SETUP/OPEN it shows mode toggle, open-joins,
 * begin and cancel; and for everyone else it shows a big JOIN button while an
 * event is open. The old subcommands keep working as a fallback.
 */
public class HostGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int FFA_SLOT = 11, HOSTBATTLE_SLOT = 15,
            MODE_SLOT = 10, STATUS_SLOT = 13, ACTION_SLOT = 12, CANCEL_SLOT = 16,
            JOIN_SLOT = 13, CLOSE_SLOT = 22;

    private final LemonEvents plugin;
    private final Player viewer;
    private Inventory inv;
    private boolean registered;

    public HostGUI(LemonEvents plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    public void open() {
        inv = Bukkit.createInventory(null, 27, MM.deserialize(
                "<!italic><gradient:#fffb00:#ffa751><bold>Host an Event</bold></gradient>"));
        render();
        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        viewer.openInventory(inv);
    }

    private boolean canManage(HostedEvent ev) {
        return ev != null && (viewer.getUniqueId().equals(ev.getHost())
                || viewer.hasPermission("lemonevents.admin.host"));
    }

    private void render() {
        ItemStack pane = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);
        inv.setItem(CLOSE_SLOT, named(Material.BARRIER, "<red>Close", List.of()));

        HostedEvent ev = plugin.getHostedEventManager().getActive();

        if (ev == null) {
            // Nothing running: offer to start hosting (host permission required).
            if (viewer.hasPermission("lemonevents.host")) {
                inv.setItem(FFA_SLOT, named(Material.DIAMOND_SWORD, "<yellow><bold>Host FFA Event", List.of(
                        "<gray>Free-for-all, last one standing.",
                        "", "<green>► Click, then type the event name in chat")));
                inv.setItem(HOSTBATTLE_SLOT, named(Material.NETHERITE_SWORD, "<red><bold>Host Battle", List.of(
                        "<gray>Everyone versus you.",
                        "", "<green>► Click, then type the event name in chat")));
            } else {
                inv.setItem(STATUS_SLOT, named(Material.GRAY_DYE, "<gray><bold>No Event Right Now", List.of(
                        "<dark_gray>Check back when someone is hosting!")));
            }
            return;
        }

        if (canManage(ev)) {
            boolean setup = ev.getState() == HostedEvent.State.SETUP;
            boolean open = ev.getState() == HostedEvent.State.OPEN;

            inv.setItem(STATUS_SLOT, named(Material.BEACON, "<gradient:#fffb00:#00ff00><bold>" + ev.getName(), List.of(
                    "<gray>Mode: <white>" + (ev.getMode() == HostedEvent.Mode.HOST_BATTLE ? "Host Battle" : "FFA"),
                    "<gray>State: <yellow>" + ev.getState(),
                    "<gray>Players: <white>" + ev.getParticipants().size())));

            if (setup || open) {
                inv.setItem(MODE_SLOT, named(Material.LEVER, "<aqua><bold>Mode: "
                        + (ev.getMode() == HostedEvent.Mode.HOST_BATTLE ? "Host Battle" : "FFA"), List.of(
                        "<gray>FFA — last one standing",
                        "<gray>Host Battle — everyone vs you",
                        "", "<green>► Click to toggle")));
            }
            if (setup) {
                inv.setItem(ACTION_SLOT, named(Material.LIME_DYE, "<green><bold>Open Joins", List.of(
                        "<gray>Finishes your kit and broadcasts",
                        "<gray>a clickable JOIN to the network.",
                        "", "<green>► Click to open")));
            } else if (open) {
                inv.setItem(ACTION_SLOT, named(Material.DRAGON_EGG, "<gold><bold>Begin the Fight", List.of(
                        "<gray>Teleports everyone into the arena",
                        "<gray>with your kit. <white>" + ev.getParticipants().size() + " <gray>joined.",
                        "", "<green>► Click to begin")));
            }
            inv.setItem(CANCEL_SLOT, named(Material.RED_DYE, "<red><bold>Cancel Event", List.of(
                    "<gray>Aborts and returns everyone.",
                    "", "<red>► Click to cancel")));
            return;
        }

        // Someone else's event: join while it's open.
        if (ev.getState() == HostedEvent.State.OPEN) {
            boolean joined = ev.getParticipants().contains(viewer.getUniqueId());
            inv.setItem(JOIN_SLOT, named(joined ? Material.LIME_DYE : Material.ENDER_PEARL,
                    joined ? "<green><bold>✔ Joined" : "<gradient:#fffb00:#00ff00><bold>➜ JOIN " + ev.getName(), List.of(
                    "<gray>Host: <white>" + ev.getHostName(),
                    "<gray>Mode: <white>" + (ev.getMode() == HostedEvent.Mode.HOST_BATTLE ? "Host Battle" : "FFA"),
                    "<gray>Players: <white>" + ev.getParticipants().size(),
                    "", joined ? "<dark_gray>Waiting for the host to begin…" : "<green>► Click to join")));
        } else {
            inv.setItem(STATUS_SLOT, named(Material.CLOCK, "<yellow><bold>" + ev.getName(), List.of(
                    "<gray>Hosted by <white>" + ev.getHostName(),
                    "<gray>State: <yellow>" + ev.getState(),
                    "<dark_gray>Joins aren't open right now.")));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(viewer.getUniqueId())) return;
        e.setCancelled(true);

        HostedEvent ev = plugin.getHostedEventManager().getActive();
        int slot = e.getRawSlot();

        if (slot == CLOSE_SLOT) { p.closeInventory(); return; }

        if (ev == null) {
            if (!p.hasPermission("lemonevents.host")) return;
            if (slot == FFA_SLOT) promptAndHost(p, HostedEvent.Mode.FFA);
            else if (slot == HOSTBATTLE_SLOT) promptAndHost(p, HostedEvent.Mode.HOST_BATTLE);
            return;
        }

        if (canManage(ev)) {
            switch (slot) {
                case MODE_SLOT -> {
                    HostedEvent.Mode next = ev.getMode() == HostedEvent.Mode.HOST_BATTLE
                            ? HostedEvent.Mode.FFA : HostedEvent.Mode.HOST_BATTLE;
                    plugin.getHostedEventManager().setMode(p, next);
                    click(p); render();
                }
                case ACTION_SLOT -> {
                    if (ev.getState() == HostedEvent.State.SETUP) plugin.getHostedEventManager().openJoins(p);
                    else if (ev.getState() == HostedEvent.State.OPEN) plugin.getHostedEventManager().begin(p);
                    click(p); render();
                }
                case CANCEL_SLOT -> {
                    plugin.getHostedEventManager().cancel(p);
                    p.closeInventory();
                }
                default -> { }
            }
            return;
        }

        if (slot == JOIN_SLOT && ev.getState() == HostedEvent.State.OPEN) {
            plugin.getHostedEventManager().join(p);
            click(p); render();
        }
    }

    private void promptAndHost(Player p, HostedEvent.Mode mode) {
        unregister();
        p.closeInventory();
        p.sendMessage(MM.deserialize("<!italic><yellow>Type a name for the event in chat <gray>(or <white>cancel<gray>)."));
        plugin.getChatInputManager().await(p, name -> {
            if (name == null || name.isBlank()) {
                p.sendMessage(MM.deserialize("<!italic><gray>Cancelled."));
                return;
            }
            if (plugin.getHostedEventManager().startHosting(p, name) && mode == HostedEvent.Mode.HOST_BATTLE) {
                plugin.getHostedEventManager().setMode(p, HostedEvent.Mode.HOST_BATTLE);
            }
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(viewer.getUniqueId())) return;
        unregister();
    }

    private void unregister() { if (registered) { HandlerList.unregisterAll(this); registered = false; } }
    private void click(Player p) { p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f); }

    private ItemStack named(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + name));
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
