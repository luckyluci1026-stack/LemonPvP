package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import net.kyori.adventure.text.Component;
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

public class CosmeticsMainGUI implements Listener {

    static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCosmetics plugin;
    private final Player player;
    private Inventory inventory;

    public CosmeticsMainGUI(LemonCosmetics plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27,
                MM.deserialize("<gradient:#fffb00:#00ff00>Cosmetics</gradient>"));

        fillInventory();

        // Slot 11: Armor Trims
        ItemStack armorTrims = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta trimsMeta = armorTrims.getItemMeta();
        trimsMeta.displayName(MM.deserialize("<white>Armor Trims"));
        trimsMeta.lore(List.of(MM.deserialize("<gray>Customize your armor trims")));
        armorTrims.setItemMeta(trimsMeta);
        inventory.setItem(11, armorTrims);

        // Slot 13: Kill Effects
        ItemStack killEffects = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta effectsMeta = killEffects.getItemMeta();
        effectsMeta.displayName(MM.deserialize("<white>Kill Effects"));
        effectsMeta.lore(List.of(MM.deserialize("<gray>Show off your kills")));
        killEffects.setItemMeta(effectsMeta);
        inventory.setItem(13, killEffects);

        // Slot 15: Coming Soon
        ItemStack comingSoon = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta comingSoonMeta = comingSoon.getItemMeta();
        comingSoonMeta.displayName(MM.deserialize("<gray>Coming Soon"));
        comingSoonMeta.lore(List.of(MM.deserialize("<dark_gray>(Reserved)")));
        comingSoon.setItemMeta(comingSoonMeta);
        inventory.setItem(15, comingSoon);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    private void fillInventory() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getWhoClicked().getUniqueId().equals(player.getUniqueId())) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) return;

        int slot = event.getSlot();
        if (slot == 11) {
            player.closeInventory();
            new TrimPatternGUI(plugin, player).open();
        } else if (slot == 13) {
            player.closeInventory();
            new KillEffectsGUI(plugin, player).open();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        HandlerList.unregisterAll(this);
    }
}
