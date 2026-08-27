package com.lemonpvp.lemoncore.gui;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemoncore.util.TextUtil;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Player settings menu: five persisted toggles (public chat, party invites,
 * private messages, fast crystals, recording mode), each shown as a themed
 * icon with a status pane beneath it. Clicking either the icon or its pane
 * toggles and saves.
 */
public class SettingsGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int SIZE = 45;
    private static final int HEAD_SLOT = 4;
    private static final int CLOSE_SLOT = 40;
    private static final int[] TOGGLE_SLOTS = {20, 21, 22, 23, 24};
    private static final int[] STATUS_SLOTS = {29, 30, 31, 32, 33};

    /** One toggleable setting: icon, name, description, getter and setter. */
    private record Setting(Material icon, String name, String description,
                           BooleanSupplier get, Consumer<Boolean> set) {}

    private final LemonCore plugin;
    private final Player player;
    private final PlayerData data;
    private final List<Setting> settings = new ArrayList<>();

    private Inventory inv;
    private boolean registered = false;

    public SettingsGUI(LemonCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.data = plugin.getPlayerDataManager().getCached(player.getUniqueId());
        if (data == null) return;

        settings.add(new Setting(Material.OAK_SIGN, "<yellow>Public Chat",
                "See messages from the public chat.",
                data::isPublicChat, data::setPublicChat));
        settings.add(new Setting(Material.CAKE, "<light_purple>Party Invites",
                "Allow players to invite you to parties.",
                data::isPartyInvites, data::setPartyInvites));
        settings.add(new Setting(Material.WRITABLE_BOOK, "<aqua>Private Messages",
                "Receive private messages from players.",
                data::isMessagesEnabled, data::setMessagesEnabled));
        settings.add(new Setting(Material.END_CRYSTAL, "<red>Fast Crystals",
                "Left-click also detonates end crystals.",
                data::isFastCrystals, data::setFastCrystals));
        settings.add(new Setting(Material.SPYGLASS, "<gold>Recording Mode",
                "Marks you with a camera icon for viewers.",
                data::isRecordingMode, this::setRecordingMode));
    }

    /** Keeps the /recording side effect (camera prefix on the nameplate). */
    private void setRecordingMode(boolean on) {
        data.setRecordingMode(on);
        if (on) {
            player.customName(TextUtil.parse("<red>📹</red> " + data.getDisplayName()));
        } else {
            player.customName(TextUtil.parse(data.getDisplayName()));
        }
    }

    public void open() {
        if (data == null) {
            player.sendMessage(MM.deserialize("<red>Your data is still loading — try again in a moment."));
            return;
        }

        inv = Bukkit.createInventory(null, SIZE,
                MM.deserialize("<!italic><gradient:#fffb00:#00ff00><bold>Settings</bold></gradient>"));
        render();

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.3f);
        player.openInventory(inv);
    }

    private void render() {
        ItemStack black = pane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack gray = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) ? black : gray);
        }

        inv.setItem(HEAD_SLOT, buildHead());

        for (int i = 0; i < settings.size() && i < TOGGLE_SLOTS.length; i++) {
            Setting s = settings.get(i);
            boolean on = s.get().getAsBoolean();
            inv.setItem(TOGGLE_SLOTS[i], buildToggle(s, on));
            inv.setItem(STATUS_SLOTS[i], buildStatus(on));
        }

        inv.setItem(CLOSE_SLOT, named(Material.BARRIER, "<red>Close"));
    }

    private ItemStack buildHead() {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (skull.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(player);
            meta.displayName(MM.deserialize("<!italic><gradient:#fffb00:#00ff00><bold>"
                    + player.getName() + "'s Settings"));
            meta.lore(List.of(MM.deserialize("<!italic><gray>Toggle how the network behaves for you.")));
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private ItemStack buildToggle(Setting s, boolean on) {
        ItemStack item = new ItemStack(s.icon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic><bold>" + s.name()));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><gray>" + s.description()));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><gray>Status: "
                    + (on ? "<green><bold>ON" : "<red><bold>OFF")));
            lore.add(MM.deserialize("<!italic><yellow>► Click to toggle"));
            meta.lore(lore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                    org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildStatus(boolean on) {
        ItemStack item = new ItemStack(on ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + (on ? "<green><bold>ON" : "<red><bold>OFF")));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (inv == null || !event.getInventory().equals(inv)) return;
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!clicker.getUniqueId().equals(player.getUniqueId())) return;
        event.setCancelled(true);
        if (data == null) return;

        int slot = event.getRawSlot();
        if (slot == CLOSE_SLOT) {
            clicker.closeInventory();
            return;
        }

        int idx = indexOf(TOGGLE_SLOTS, slot);
        if (idx < 0) idx = indexOf(STATUS_SLOTS, slot);
        if (idx < 0 || idx >= settings.size()) return;

        Setting s = settings.get(idx);
        boolean now = !s.get().getAsBoolean();
        s.set().accept(now);
        plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
        clicker.playSound(clicker.getLocation(),
                now ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.BLOCK_NOTE_BLOCK_BASS,
                0.5f, now ? 1.2f : 0.8f);
        render();
    }

    private static int indexOf(int[] arr, int value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == value) return i;
        }
        return -1;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (inv == null || !event.getInventory().equals(inv)) return;
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }

    private ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack named(Material mat, String mini) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + mini));
            item.setItemMeta(meta);
        }
        return item;
    }
}
