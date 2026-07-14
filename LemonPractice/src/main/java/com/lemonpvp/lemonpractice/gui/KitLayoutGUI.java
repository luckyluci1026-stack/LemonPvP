package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.PlayerKit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The player kit editor as a chest GUI whose layout mirrors the full inventory —
 * including the <b>off-hand</b> and armour slots. Players drag the preset items
 * into whichever slot they want; the arrangement (real inventory slot → item,
 * off-hand = slot 40) is saved as their kit and applied verbatim in duels.
 *
 * <p>Sort-only by construction: items can't leave or enter the top grid (shift-
 * click, drops, hotbar swaps and the bottom inventory are all blocked), so the
 * saved kit is always a permutation of the preset — no integrity check needed.</p>
 *
 * <pre>
 *  Row 0:  [Reset] [Helm] [Chest] [Legs] [Boots] [ ] [Off-hand] [ ] [Save]
 *  Row 1:  separator
 *  Rows 2-4: main inventory  (real slots 9-35)
 *  Row 5:  hotbar            (real slots 0-8)
 * </pre>
 */
public class KitLayoutGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int SIZE = 54;
    private static final int RESET_SLOT = 0;
    private static final int SAVE_SLOT = 8;

    private final LemonPractice plugin;
    private final Player player;
    private final String gamemode;
    private Inventory inv;
    private boolean registered;
    private boolean closingBySave;

    public KitLayoutGUI(LemonPractice plugin, Player player, String gamemode) {
        this.plugin = plugin;
        this.player = player;
        this.gamemode = gamemode.toLowerCase();
    }

    // GUI slot ↔ real inventory slot mapping (real 0-8 hotbar, 9-35 main, 36-39 armour, 40 off-hand).
    private static int guiToReal(int gui) {
        return switch (gui) {
            case 1 -> 39; case 2 -> 38; case 3 -> 37; case 4 -> 36; // helm/chest/legs/boots
            case 6 -> 40;                                            // off-hand
            default -> {
                if (gui >= 18 && gui <= 44) yield gui - 9;   // main inventory 9-35
                if (gui >= 45 && gui <= 53) yield gui - 45;  // hotbar 0-8
                yield -1;
            }
        };
    }

    private static int realToGui(int real) {
        return switch (real) {
            case 39 -> 1; case 38 -> 2; case 37 -> 3; case 36 -> 4;
            case 40 -> 6;
            default -> {
                if (real >= 0 && real <= 8) yield real + 45;
                if (real >= 9 && real <= 35) yield real + 9;
                yield -1;
            }
        };
    }

    private static boolean isEditable(int gui) {
        return guiToReal(gui) >= 0;
    }

    public void open() {
        inv = Bukkit.createInventory(null, SIZE, MM.deserialize(
                "<!italic><gradient:#fffb00:#00ff00><bold>Kit Editor</bold></gradient> <dark_gray>» <white>" + gamemode));

        // Non-editable slots get a plain separator pane; editable slots stay empty
        // so items place/swap cleanly (no fake-item swap edge cases).
        ItemStack pane = pane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            if (!isEditable(i)) inv.setItem(i, pane);
        }
        inv.setItem(RESET_SLOT, named(Material.BARRIER, "<red><bold>Reset to Default",
                List.of("<gray>Restore the preset layout.")));
        inv.setItem(SAVE_SLOT, named(Material.LIME_DYE, "<green><bold>Save & Close",
                List.of("<gray>Or just close — it auto-saves.")));

        fillFrom(plugin.getKitManager().getEffectiveKit(player.getUniqueId(), gamemode));

        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        player.sendMessage(MM.deserialize("<!italic><gray>Top row = <white>Helmet · Chestplate · Leggings · Boots"
                + " <dark_gray>|<gray> Off-hand<gray>. Bottom rows = inventory + hotbar. Arrange freely, then Save."));
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.15f);
        player.openInventory(inv);
    }

    /** Places a kit's items into the mapped GUI slots (clearing the editable area first). */
    private void fillFrom(PlayerKit kit) {
        for (int g = 0; g < SIZE; g++) {
            if (isEditable(g)) inv.setItem(g, null);
        }
        if (kit == null) return;
        kit.getSlots().forEach((real, item) -> {
            int g = realToGui(real);
            if (g >= 0 && item != null) inv.setItem(g, item.clone());
        });
    }

    /** Reads the editable slots into a kit and persists it. */
    private void save() {
        PlayerKit kit = new PlayerKit(player.getUniqueId(), gamemode);
        for (int g = 0; g < SIZE; g++) {
            if (!isEditable(g)) continue;
            ItemStack it = inv.getItem(g);
            if (it == null || it.getType() == Material.AIR) continue;
            kit.setSlot(guiToReal(g), it.clone());
        }
        plugin.getKitManager().saveKit(player.getUniqueId(), gamemode, kit);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(player.getUniqueId())) return;

        int raw = e.getRawSlot();
        // Bottom (player) inventory or any non-editable top slot → block outright.
        if (raw >= SIZE) { e.setCancelled(true); return; }
        if (raw == RESET_SLOT) {
            e.setCancelled(true);
            fillFrom(plugin.getKitManager().getPresetKit(gamemode));
            p.playSound(p.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.6f, 1.1f);
            return;
        }
        if (raw == SAVE_SLOT) {
            e.setCancelled(true);
            closingBySave = true;
            save();
            p.sendMessage(MM.deserialize("<!italic><green>Your <yellow>" + gamemode + " <green>kit layout was saved."));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
            p.closeInventory();
            return;
        }
        if (!isEditable(raw)) { e.setCancelled(true); return; }

        // Block every action that would move an item OUT of (or INTO from) the grid.
        switch (e.getAction()) {
            case MOVE_TO_OTHER_INVENTORY, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD,
                 COLLECT_TO_CURSOR, DROP_ONE_SLOT, DROP_ALL_SLOT -> e.setCancelled(true);
            default -> { /* in-grid pickup/place/swap: allowed */ }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        for (int raw : e.getRawSlots()) {
            if (raw >= SIZE || !isEditable(raw)) { e.setCancelled(true); return; }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        if (!closingBySave) {
            save(); // auto-save on close
            if (e.getPlayer() instanceof Player p) {
                p.sendMessage(MM.deserialize("<!italic><green>Your <yellow>" + gamemode + " <green>kit layout was saved."));
            }
        }
        unregister();
    }

    private void unregister() {
        if (registered) { HandlerList.unregisterAll(this); registered = false; }
    }

    private ItemStack pane(Material mat, String mini) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(MM.deserialize("<!italic><reset>" + mini)); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack named(Material mat, String mini, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + mini));
            List<Component> l = new ArrayList<>();
            for (String s : lore) l.add(MM.deserialize("<!italic><gray>" + s));
            meta.lore(l);
            item.setItemMeta(meta);
        }
        return item;
    }
}
