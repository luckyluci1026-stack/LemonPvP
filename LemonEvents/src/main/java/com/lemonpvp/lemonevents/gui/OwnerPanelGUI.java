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
 * The owner's control panel for hosting official events — deliberately separate
 * from the {@code /host} flow content creators use. Opens with
 * {@code /eventpanel}. Left/right-click actions drive the hosted-event
 * lifecycle and the scripted-event wizard. Tournaments are 1v1s and are managed
 * on the duels server via {@code /tournament}.
 */
public class OwnerPanelGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int FFA_SLOT = 10, HOSTBATTLE_SLOT = 11, ACTIVE_SLOT = 13,
            CREATE_EVENT_SLOT = 15, TOURNEY_INFO_SLOT = 16, CLOSE_SLOT = 22;

    private final LemonEvents plugin;
    private final Player owner;
    private Inventory inv;
    private boolean registered;

    public OwnerPanelGUI(LemonEvents plugin, Player owner) {
        this.plugin = plugin;
        this.owner = owner;
    }

    public void open() {
        inv = Bukkit.createInventory(null, 27, MM.deserialize(
                "<!italic><gradient:#fffb00:#ffa751><bold>Event Control</bold></gradient>"));
        render();
        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        owner.playSound(owner.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        owner.openInventory(inv);
    }

    private void render() {
        ItemStack pane = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        inv.setItem(FFA_SLOT, named(Material.DIAMOND_SWORD, "<yellow><bold>Host FFA Event", List.of(
                "<gray>Free-for-all, last one standing.",
                "", "<green>► Click to build the kit & host")));
        inv.setItem(HOSTBATTLE_SLOT, named(Material.NETHERITE_SWORD, "<red><bold>Host Battle", List.of(
                "<gray>Everyone versus you (the host).",
                "", "<green>► Click to build the kit & host")));
        inv.setItem(ACTIVE_SLOT, activeEventItem());
        inv.setItem(CREATE_EVENT_SLOT, named(Material.ENDER_EYE, "<light_purple><bold>Create Scripted Event", List.of(
                "<gray>Build a LemonRoyale / HungerGames /",
                "<gray>PvP / Horror event with prizes.",
                "", "<green>► Click to open the wizard")));
        inv.setItem(TOURNEY_INFO_SLOT, named(Material.GOLDEN_SWORD, "<gold><bold>Tournaments", List.of(
                "<gray>Tournaments are 1v1s and live on",
                "<gray>the <white>duels server<gray>: manage them",
                "<gray>there with <white>/tournament<gray>.")));
        inv.setItem(CLOSE_SLOT, named(Material.BARRIER, "<red>Close", List.of()));
    }

    private ItemStack activeEventItem() {
        HostedEvent ev = plugin.getHostedEventManager().getActive();
        if (ev == null) {
            return named(Material.GRAY_DYE, "<gray><bold>No Active Event", List.of(
                    "<dark_gray>Nothing running right now."));
        }
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Name: <white>" + ev.getName());
        lore.add("<gray>Mode: <white>" + (ev.getMode() == HostedEvent.Mode.HOST_BATTLE ? "Host Battle" : "FFA"));
        lore.add("<gray>State: <yellow>" + ev.getState());
        lore.add("<gray>Players: <white>" + ev.getParticipants().size());
        lore.add("");
        switch (ev.getState()) {
            case SETUP -> lore.add("<green>► Left-click: open joins (broadcast)");
            case OPEN -> lore.add("<green>► Left-click: begin the fight");
            default -> lore.add("<dark_gray>Running…");
        }
        lore.add("<red>► Right-click: cancel");
        return named(Material.BEACON, "<gradient:#fffb00:#00ff00><bold>Active Event", lore);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(owner.getUniqueId())) return;
        e.setCancelled(true);

        switch (e.getRawSlot()) {
            case FFA_SLOT -> promptAndHost(p, HostedEvent.Mode.FFA);
            case HOSTBATTLE_SLOT -> promptAndHost(p, HostedEvent.Mode.HOST_BATTLE);
            case ACTIVE_SLOT -> handleActive(p, e.isRightClick());
            case CREATE_EVENT_SLOT -> { unregister(); new EventCreateGUI(plugin, p).open(); }
            case CLOSE_SLOT -> p.closeInventory();
            default -> { /* border */ }
        }
    }

    private void promptAndHost(Player p, HostedEvent.Mode mode) {
        if (plugin.getHostedEventManager().getActive() != null) {
            msg(p, "<red>An event is already active — cancel it first.");
            return;
        }
        unregister();
        p.closeInventory();
        msg(p, "<yellow>Type a name for the event in chat <gray>(or <white>cancel<gray>).");
        plugin.getChatInputManager().await(p, name -> {
            if (name == null || name.isBlank()) { msg(p, "<gray>Cancelled."); return; }
            if (plugin.getHostedEventManager().startHosting(p, name)
                    && mode == HostedEvent.Mode.HOST_BATTLE) {
                plugin.getHostedEventManager().setMode(p, HostedEvent.Mode.HOST_BATTLE);
            }
        });
    }

    private void handleActive(Player p, boolean rightClick) {
        HostedEvent ev = plugin.getHostedEventManager().getActive();
        if (ev == null) { render(); return; }
        if (rightClick) {
            plugin.getHostedEventManager().cancel(p);
        } else if (ev.getState() == HostedEvent.State.SETUP) {
            plugin.getHostedEventManager().openJoins(p);
        } else if (ev.getState() == HostedEvent.State.OPEN) {
            plugin.getHostedEventManager().begin(p);
        }
        render();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(owner.getUniqueId())) return;
        unregister();
    }

    private void unregister() { if (registered) { HandlerList.unregisterAll(this); registered = false; } }
    private void msg(Player p, String mini) { p.sendMessage(MM.deserialize("<!italic>" + mini)); }

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
