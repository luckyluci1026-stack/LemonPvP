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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Kit-type chooser for a gamemode:
 * <ul>
 *   <li><b>Preset Kit</b> — the server loadout; the player can only sort it.</li>
 *   <li><b>Custom Kit</b> — locked, shown as "Under Development".</li>
 * </ul>
 */
public class KitTypeGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int SIZE = 27;
    private static final int PRESET_SLOT = 11;
    private static final int CUSTOM_SLOT = 15;
    private static final int CLOSE_SLOT = 22;

    private final LemonPractice plugin;
    private final Player player;
    private final String gamemodeId;
    private Inventory inv;
    private boolean registered;

    public KitTypeGUI(LemonPractice plugin, Player player, String gamemodeId) {
        this.plugin = plugin;
        this.player = player;
        this.gamemodeId = gamemodeId;
    }

    public void open() {
        Gamemode gm = plugin.getGamemodeManager().getGamemode(gamemodeId);
        String label = gm != null ? gm.getName() : gamemodeId;
        Material presetIcon = gm != null ? gm.getMaterial() : Material.DIAMOND_SWORD;

        inv = Bukkit.createInventory(null, SIZE,
                MM.deserialize("<!italic><gradient:#fffb00:#00ff00>Kit</gradient> <dark_gray>» <white>" + label));

        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);

        inv.setItem(PRESET_SLOT, named(presetIcon, "<green><bold>Preset Kit</bold>", List.of(
                "<gray>The server's <white>" + label + " <gray>loadout.",
                "<gray>You can <white>rearrange<gray> the items,",
                "<gray>but not add or remove them.",
                "",
                "<green>► Click to arrange")));

        inv.setItem(CUSTOM_SLOT, named(Material.COMMAND_BLOCK, "<yellow><bold>Custom Kit</bold>", List.of(
                "<gray>Build a fully custom loadout.",
                "",
                "<gold>🚧 Under Development",
                "<dark_gray>Coming soon!")));

        inv.setItem(CLOSE_SLOT, named(Material.BARRIER, "<red>Close", List.of()));

        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(player.getUniqueId())) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == CLOSE_SLOT) { p.closeInventory(); return; }
        if (slot == PRESET_SLOT) {
            unregister();
            p.closeInventory();
            // GUI editor (chest layout) — arrange the kit into any slot, off-hand included.
            new KitLayoutGUI(plugin, p, gamemodeId).open();
            return;
        }
        if (slot == CUSTOM_SLOT) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
            p.sendMessage(MM.deserialize("<!italic><gold>Custom kits are <yellow>under development<gold> — coming soon!"));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        unregister();
    }

    private void unregister() {
        if (registered) { HandlerList.unregisterAll(this); registered = false; }
    }

    private ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack named(Material mat, String mini, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + mini));
            if (lore != null && !lore.isEmpty()) {
                List<Component> l = new ArrayList<>();
                for (String s : lore) l.add(MM.deserialize("<!italic>" + s));
                meta.lore(l);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
