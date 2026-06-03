package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

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

        // Slot 10: Armor Trims
        ItemStack armorTrims = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta trimsMeta = armorTrims.getItemMeta();
        trimsMeta.displayName(MM.deserialize("<white>Armor Trims"));
        trimsMeta.lore(List.of(MM.deserialize("<yellow>ᴄᴏᴍɪɴɢ ꜱᴏᴏɴ")));
        armorTrims.setItemMeta(trimsMeta);
        inventory.setItem(10, armorTrims);

        // Slot 11: Arrow Trails
        ItemStack arrowTrails = new ItemStack(Material.ARROW);
        ItemMeta trailsMeta = arrowTrails.getItemMeta();
        trailsMeta.displayName(MM.deserialize("<white>Arrow Trails"));
        trailsMeta.lore(List.of(MM.deserialize("<yellow>ᴄᴏᴍɪɴɢ ꜱᴏᴏɴ")));
        arrowTrails.setItemMeta(trailsMeta);
        inventory.setItem(11, arrowTrails);

        // Slot 13: Kill Effects
        ItemStack killEffects = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta effectsMeta = killEffects.getItemMeta();
        effectsMeta.displayName(MM.deserialize("<white>Kill Effects"));
        effectsMeta.lore(List.of(MM.deserialize("<yellow>ᴄᴏᴍɪɴɢ ꜱᴏᴏɴ")));
        killEffects.setItemMeta(effectsMeta);
        inventory.setItem(13, killEffects);

        // Slot 14: Hats
        ItemStack hats = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta hatsMeta = (LeatherArmorMeta) hats.getItemMeta();
        hatsMeta.setColor(Color.fromRGB(0, 0, 0));
        hatsMeta.displayName(MM.deserialize("<white>Hats"));
        hatsMeta.lore(List.of(MM.deserialize("<yellow>ᴄᴏᴍɪɴɢ ꜱᴏᴏɴ")));
        hatsMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE);
        hats.setItemMeta(hatsMeta);
        inventory.setItem(14, hats);

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
        if (slot == 10 || slot == 11 || slot == 13 || slot == 14) {
            player.sendMessage(MM.deserialize("<gradient:#fffb00:#00ff00>LemonPvP</gradient> <yellow>ᴄᴏᴍɪɴɢ ꜱᴏᴏɴ"));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        HandlerList.unregisterAll(this);
    }
}
