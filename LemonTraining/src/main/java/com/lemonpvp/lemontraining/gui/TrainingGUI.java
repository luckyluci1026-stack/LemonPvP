package com.lemonpvp.lemontraining.gui;

import com.lemonpvp.lemontraining.LemonTraining;
import com.lemonpvp.lemontraining.model.PracticeMode;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class TrainingGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonTraining plugin;
    private final Player player;
    private Inventory inv;

    public TrainingGUI(LemonTraining plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inv = Bukkit.createInventory(null, 27,
                MM.deserialize("<font:lemonpvp:default>ᴛʀᴀɪɴɪɴɢ</font>"));

        // Fill with gray glass panes
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Slot 11: Iron Sword → TOTEM
        inv.setItem(11, makeItem(Material.IRON_SWORD,
                "<!italic><yellow>Totem Practice</yellow>",
                List.of("<!italic><gray>Practice catching anvils with totems.</gray>")));

        // Slot 12: Bow → BOW
        inv.setItem(12, makeItem(Material.BOW,
                "<!italic><aqua>Bow Practice</aqua>",
                List.of("<!italic><gray>Hit targets with bow and crossbow.</gray>")));

        // Slot 13: Mace → MACE
        inv.setItem(13, makeItem(Material.MACE,
                "<!italic><light_purple>Mace Practice</light_purple>",
                List.of("<!italic><gray>Practice mace dive attacks.</gray>")));

        // Slot 14: Diamond Sword → SWORD
        inv.setItem(14, makeItem(Material.DIAMOND_SWORD,
                "<!italic><green>Sword Practice</green>",
                List.of("<!italic><gray>Practice sword combat.</gray>")));

        // Slot 15: End Crystal → CRYSTAL
        inv.setItem(15, makeItem(Material.END_CRYSTAL,
                "<!italic><red>Crystal Practice</red>",
                List.of("<!italic><gray>Practice crystal PvP.</gray>")));

        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(inv);
    }

    private ItemStack makeItem(Material mat, String name) {
        return makeItem(mat, name, null);
    }

    private ItemStack makeItem(Material mat, String name, List<String> loreParts) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(name));
            if (loreParts != null) {
                meta.lore(loreParts.stream().map(MM::deserialize).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!clicker.getUniqueId().equals(player.getUniqueId())) return;
        if (!event.getInventory().equals(inv)) return;

        event.setCancelled(true);

        PracticeMode mode = switch (event.getSlot()) {
            case 11 -> PracticeMode.TOTEM;
            case 12 -> PracticeMode.BOW;
            case 13 -> PracticeMode.MACE;
            case 14 -> PracticeMode.SWORD;
            case 15 -> PracticeMode.CRYSTAL;
            default -> null;
        };

        if (mode != null) {
            player.closeInventory();
            plugin.getPracticeManager().startPractice(player, mode);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        HandlerList.unregisterAll(this);
    }
}
