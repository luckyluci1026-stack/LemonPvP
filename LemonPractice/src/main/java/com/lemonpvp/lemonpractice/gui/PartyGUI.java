package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The click-driven face of {@code /party} — no subcommands needed.
 * <ul>
 *   <li>Not in a party: pending invites shown as heads (left accept / right
 *       deny) and every invitable online player as a clickable head.</li>
 *   <li>In a party: member heads (the leader can right-click to kick,
 *       shift-right-click to promote), plus invite heads for the leader,
 *       a 2v2 queue button, leave and disband.</li>
 * </ul>
 * All actions delegate to {@link com.lemonpvp.lemonpractice.managers.PartyManager},
 * so chat feedback and rules stay identical to the commands.
 */
public class PartyGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int STATUS_SLOT = 4;
    private static final int[] MEMBER_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int INVITE_LABEL_SLOT = 27;
    private static final int[] INVITE_SLOTS = {28, 29, 30, 31, 32, 33, 34, 35, 37, 38, 39, 40, 41, 42, 43, 44};
    private static final int QUEUE_SLOT = 48, LEAVE_SLOT = 50, DISBAND_SLOT = 52, CLOSE_SLOT = 49;

    /** PDC marker values distinguishing what a clicked head means. */
    private static final byte KIND_MEMBER = 1, KIND_INVITABLE = 2, KIND_PENDING = 3;

    private final LemonPractice plugin;
    private final Player viewer;
    private final NamespacedKey kindKey;
    private final NamespacedKey nameKey;
    private Inventory inv;
    private boolean registered;

    public PartyGUI(LemonPractice plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.kindKey = new NamespacedKey(plugin, "party_kind");
        this.nameKey = new NamespacedKey(plugin, "party_name");
    }

    public void open() {
        inv = Bukkit.createInventory(null, 54, MM.deserialize(
                "<!italic><gradient:#00c8ff:#0066ff><bold>Party</bold></gradient>"));
        render();
        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        viewer.openInventory(inv);
    }

    private void render() {
        ItemStack pane = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);
        inv.setItem(CLOSE_SLOT, named(Material.BARRIER, "<red>Close", List.of()));

        Party party = plugin.getPartyManager().getParty(viewer.getUniqueId());
        boolean leader = party != null && party.isLeader(viewer.getUniqueId());

        if (party == null) {
            inv.setItem(STATUS_SLOT, named(Material.CAKE, "<aqua><bold>No Party", List.of(
                    "<gray>Invite someone below to start one,",
                    "<gray>or accept a pending invite.")));
            renderPendingInvites();
            renderInvitables(null);
            return;
        }

        inv.setItem(STATUS_SLOT, named(Material.CAKE, "<aqua><bold>Your Party", List.of(
                "<gray>Members: <white>" + party.size(),
                "<gray>Leader: <white>" + nameOf(party.getLeader()),
                party.size() == 2 ? "<gray>Duos queue as a <aqua>2v2 team<gray>!" : "<dark_gray>Exactly 2 members → 2v2 team queue")));

        int i = 0;
        for (UUID member : party.getMembers()) {
            if (i >= MEMBER_SLOTS.length) break;
            inv.setItem(MEMBER_SLOTS[i++], memberHead(party, member, leader));
        }

        if (leader) renderInvitables(party);

        inv.setItem(QUEUE_SLOT, named(Material.DIAMOND_SWORD, "<gradient:#fffb00:#00ff00><bold>Queue 2v2", List.of(
                party.size() == 2
                        ? "<gray>Pick a gamemode and fight the next duo."
                        : "<red>Needs exactly 2 members <gray>(you have " + party.size() + ").",
                "", "<green>► Click to pick a gamemode")));
        inv.setItem(LEAVE_SLOT, named(Material.OAK_DOOR, "<yellow><bold>Leave Party", List.of(
                "", "<yellow>► Click to leave")));
        if (leader) {
            inv.setItem(DISBAND_SLOT, named(Material.TNT, "<red><bold>Disband Party", List.of(
                    "", "<red>► Click to disband")));
        }
    }

    private void renderPendingInvites() {
        List<String> pending = plugin.getPartyManager().getPendingInviterNames(viewer.getUniqueId());
        int i = 0;
        for (String inviter : pending) {
            if (i >= MEMBER_SLOTS.length) break;
            ItemStack head = playerHead(inviter, "<green><bold>Invite from " + inviter, List.of(
                    "", "<green>► Left-click: accept", "<red>► Right-click: deny"));
            tag(head, KIND_PENDING, inviter);
            inv.setItem(MEMBER_SLOTS[i++], head);
        }
    }

    private void renderInvitables(Party party) {
        inv.setItem(INVITE_LABEL_SLOT, named(Material.OAK_SIGN, "<gray><bold>Invite players", List.of(
                "<dark_gray>Click a head to invite them.")));
        int i = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (i >= INVITE_SLOTS.length) break;
            if (online.getUniqueId().equals(viewer.getUniqueId())) continue;
            if (party != null && party.isMember(online.getUniqueId())) continue;
            if (plugin.getPartyManager().isInParty(online.getUniqueId())) continue;
            ItemStack head = playerHead(online.getName(), "<white>" + online.getName(), List.of(
                    "", "<green>► Click to invite"));
            tag(head, KIND_INVITABLE, online.getName());
            inv.setItem(INVITE_SLOTS[i++], head);
        }
    }

    private ItemStack memberHead(Party party, UUID member, boolean viewerIsLeader) {
        boolean isLeader = party.isLeader(member);
        boolean self = member.equals(viewer.getUniqueId());
        List<String> lore = new ArrayList<>();
        if (isLeader) lore.add("<gold>★ Party leader");
        if (viewerIsLeader && !self) {
            lore.add("");
            lore.add("<red>► Right-click: kick");
            lore.add("<gold>► Shift-right-click: promote to leader");
        }
        String name = nameOf(member);
        ItemStack head = playerHead(name, (isLeader ? "<gold><bold>" : "<white>") + name, lore);
        tag(head, KIND_MEMBER, name);
        return head;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(viewer.getUniqueId())) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == CLOSE_SLOT) { p.closeInventory(); return; }

        Party party = plugin.getPartyManager().getParty(p.getUniqueId());
        boolean leader = party != null && party.isLeader(p.getUniqueId());

        if (slot == QUEUE_SLOT && party != null) {
            if (party.size() != 2) {
                msg(p, "<red>2v2 needs exactly 2 members.");
                return;
            }
            if (!leader) { msg(p, "<red>Only the leader can queue the party."); return; }
            unregister();
            new Party2v2PickerGUI(plugin, p).open();
            return;
        }
        if (slot == LEAVE_SLOT && party != null) {
            plugin.getPartyManager().leave(p);
            p.closeInventory();
            return;
        }
        if (slot == DISBAND_SLOT && leader) {
            plugin.getPartyManager().disband(p);
            p.closeInventory();
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        Byte kind = meta.getPersistentDataContainer().get(kindKey, PersistentDataType.BYTE);
        String name = meta.getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        if (kind == null || name == null) return;

        switch (kind) {
            case KIND_PENDING -> {
                if (e.isLeftClick()) plugin.getPartyManager().accept(p, name);
                else plugin.getPartyManager().deny(p, name);
                click(p); render();
            }
            case KIND_INVITABLE -> {
                Player target = Bukkit.getPlayerExact(name);
                if (target != null && target.isOnline()) {
                    plugin.getPartyManager().invite(p, target);
                    click(p); render();
                }
            }
            case KIND_MEMBER -> {
                if (!leader || name.equals(p.getName())) return;
                if (e.isShiftClick() && e.isRightClick()) {
                    plugin.getPartyManager().promote(p, name);
                    click(p); render();
                } else if (e.isRightClick()) {
                    plugin.getPartyManager().kick(p, name);
                    click(p); render();
                }
            }
            default -> { }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(viewer.getUniqueId())) return;
        unregister();
    }

    private void unregister() { if (registered) { HandlerList.unregisterAll(this); registered = false; } }
    private void click(Player p) { p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f); }
    private void msg(Player p, String mini) { p.sendMessage(MM.deserialize("<!italic>" + mini)); }

    private void tag(ItemStack item, byte kind, String name) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(kindKey, PersistentDataType.BYTE, kind);
        meta.getPersistentDataContainer().set(nameKey, PersistentDataType.STRING, name);
        item.setItemMeta(meta);
    }

    private ItemStack playerHead(String playerName, String display, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta meta) {
            Player online = Bukkit.getPlayerExact(playerName);
            if (online != null) meta.setOwningPlayer(online);
            meta.displayName(MM.deserialize("<!italic>" + display));
            if (!lore.isEmpty()) {
                List<Component> l = new ArrayList<>();
                for (String s : lore) l.add(MM.deserialize("<!italic>" + s));
                meta.lore(l);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String nameOf(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (p != null) return p.getName();
        String n = Bukkit.getOfflinePlayer(id).getName();
        return n != null ? n : id.toString().substring(0, 8);
    }

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
