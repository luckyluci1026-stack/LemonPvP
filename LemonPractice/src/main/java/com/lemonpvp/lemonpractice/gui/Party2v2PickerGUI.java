package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Gamemode;
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
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Small gamemode picker for the party 2v2 queue: click a mode and the duo is
 * queued as a team via {@code PartyManager.queueParty}. Opened from the
 * {@link PartyGUI} Queue button.
 */
public class Party2v2PickerGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int BACK_SLOT = 18;

    private final LemonPractice plugin;
    private final Player leader;
    private final NamespacedKey modeKey;
    private Inventory inv;
    private boolean registered;

    public Party2v2PickerGUI(LemonPractice plugin, Player leader) {
        this.plugin = plugin;
        this.leader = leader;
        this.modeKey = new NamespacedKey(plugin, "party_queue_mode");
    }

    public void open() {
        inv = Bukkit.createInventory(null, 27, MM.deserialize(
                "<!italic><gradient:#00c8ff:#0066ff><bold>2v2</bold></gradient> <dark_gray>» <gray>Pick a gamemode"));

        ItemStack pane = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);
        inv.setItem(BACK_SLOT, named(Material.ARROW, "<gray>← Back", List.of()));

        int i = 0;
        for (Gamemode gm : plugin.getGamemodeManager().getAllGamemodes()) {
            if (!gm.isEnabled() || gm.getMaterial() == null || i >= SLOTS.length) continue;
            ItemStack item = named(gm.getMaterial(), "<bold>" + gm.getDisplayName(), List.of(
                    "", "<gray>Queue your duo for a <white>2v2 " + gm.getId() + "<gray>.",
                    "", "<green>► Click to queue"));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(modeKey, PersistentDataType.STRING, gm.getId());
                item.setItemMeta(meta);
            }
            inv.setItem(SLOTS[i++], item);
        }

        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        leader.playSound(leader.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        leader.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(leader.getUniqueId())) return;
        e.setCancelled(true);

        if (e.getRawSlot() == BACK_SLOT) {
            unregister();
            new PartyGUI(plugin, p).open();
            return;
        }
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        String mode = meta.getPersistentDataContainer().get(modeKey, PersistentDataType.STRING);
        if (mode == null) return;

        unregister();
        p.closeInventory();
        plugin.getPartyManager().queueParty(p, mode);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.4f);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(leader.getUniqueId())) return;
        unregister();
    }

    private void unregister() { if (registered) { HandlerList.unregisterAll(this); registered = false; } }

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
