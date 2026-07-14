package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Gamemode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin kit manager: one tile per gamemode showing whether it uses a custom
 * admin preset or the kits.yml default. Left-click a tile to edit its preset
 * (enters the free inventory editor), right-click to delete the custom preset
 * and fall back to kits.yml. Opened by {@code /kitadmin}.
 */
public class KitAdminGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;
    private final Player admin;
    private Inventory inv;
    private boolean registered;
    private final Map<Integer, String> slotMap = new HashMap<>();

    public KitAdminGUI(LemonPractice plugin, Player admin) {
        this.plugin = plugin;
        this.admin = admin;
    }

    public void open() {
        List<Gamemode> modes = new ArrayList<>(plugin.getGamemodeManager().getAllGamemodes());
        int rows = Math.max(3, (int) Math.ceil((modes.size() + 1) / 9.0) + 1);
        inv = Bukkit.createInventory(null, rows * 9,
                MM.deserialize("<!italic><gradient:#fffb00:#00ff00><bold>Kit Admin</bold></gradient> <dark_gray>» <gray>Presets"));

        ItemStack pane = named(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);

        int slot = 10;
        for (Gamemode gm : modes) {
            if (slot % 9 == 8) slot += 2; // skip the right border
            if (slot >= inv.getSize() - 1) break; // ran out of room (never at 8 modes)
            boolean custom = plugin.getKitManager().hasAdminKit(gm.getId());
            inv.setItem(slot, tile(gm, custom));
            slotMap.put(slot, gm.getId());
            slot++;
        }

        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        admin.playSound(admin.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.1f);
        admin.openInventory(inv);
    }

    private ItemStack tile(Gamemode gm, boolean custom) {
        ItemStack item = new ItemStack(gm.getMaterial() != null ? gm.getMaterial() : Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic><bold><gradient:#fffb00:#00ff00>" + gm.getDisplayName()));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MM.deserialize(custom
                    ? "<!italic><green>● Custom admin preset"
                    : "<!italic><gray>○ Default (kits.yml)"));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><yellow>► Left-click to edit"));
            if (custom) lore.add(MM.deserialize("<!italic><red>► Right-click to delete (revert to default)"));
            meta.lore(lore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                    org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(admin.getUniqueId())) return;
        e.setCancelled(true);

        String gm = slotMap.get(e.getRawSlot());
        if (gm == null) return;

        if (e.getClick() == ClickType.RIGHT && plugin.getKitManager().hasAdminKit(gm)) {
            plugin.getKitManager().deleteAdminKit(gm);
            p.sendMessage(MM.deserialize("<!italic><yellow>" + gm
                    + " <gray>reverted to the kits.yml default."));
            p.playSound(p.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.6f, 1.1f);
            open(); // refresh
            return;
        }

        // Left-click (or right on a default) → edit. Close, then enter the editor.
        unregister();
        p.closeInventory();
        plugin.getKitAdminManager().enter(p, gm);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(admin.getUniqueId())) return;
        unregister();
    }

    private void unregister() {
        if (registered) { HandlerList.unregisterAll(this); registered = false; }
    }

    private ItemStack named(Material mat, String mini, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + mini));
            item.setItemMeta(meta);
        }
        return item;
    }
}
