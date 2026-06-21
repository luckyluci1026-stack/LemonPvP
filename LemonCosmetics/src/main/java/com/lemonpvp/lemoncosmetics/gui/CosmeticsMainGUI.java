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

    private static final int TRIMS_SLOT   = 10;
    private static final int TRAILS_SLOT  = 11;
    private static final int EFFECTS_SLOT = 13;
    private static final int HATS_SLOT    = 14;

    private final LemonCosmetics plugin;
    private final Player player;
    private Inventory inventory;
    private boolean registered = false;

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
        if (trimsMeta != null) {
            trimsMeta.displayName(MM.deserialize("<white>Armor Trims"));
            trimsMeta.lore(List.of(MM.deserialize("<gray>Customize your armor with trims."),
                    MM.deserialize("<yellow>Click to browse")));
            armorTrims.setItemMeta(trimsMeta);
        }
        inventory.setItem(TRIMS_SLOT, armorTrims);

        // Slot 11: Arrow Trails
        ItemStack arrowTrails = new ItemStack(Material.ARROW);
        ItemMeta trailsMeta = arrowTrails.getItemMeta();
        if (trailsMeta != null) {
            trailsMeta.displayName(MM.deserialize("<white>Arrow Trails"));
            trailsMeta.lore(List.of(MM.deserialize("<gray>Leave a colorful trail behind your arrows."),
                    MM.deserialize("<yellow>Click to browse")));
            arrowTrails.setItemMeta(trailsMeta);
        }
        inventory.setItem(TRAILS_SLOT, arrowTrails);

        // Slot 13: Kill Effects
        ItemStack killEffects = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta effectsMeta = killEffects.getItemMeta();
        if (effectsMeta != null) {
            effectsMeta.displayName(MM.deserialize("<white>Kill Effects"));
            effectsMeta.lore(List.of(MM.deserialize("<gray>Trigger an effect when you eliminate a player."),
                    MM.deserialize("<yellow>Click to browse")));
            killEffects.setItemMeta(effectsMeta);
        }
        inventory.setItem(EFFECTS_SLOT, killEffects);

        // Slot 14: Hats
        ItemStack hats = new ItemStack(Material.LEATHER_HELMET);
        ItemMeta hatsBaseMeta = hats.getItemMeta();
        if (hatsBaseMeta instanceof LeatherArmorMeta hatsMeta) {
            hatsMeta.setColor(Color.fromRGB(0, 0, 0));
            hatsMeta.displayName(MM.deserialize("<white>Hats"));
            hatsMeta.lore(List.of(MM.deserialize("<gray>Wear a stylish hat on your head."),
                    MM.deserialize("<yellow>Click to browse")));
            hatsMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE);
            hats.setItemMeta(hatsMeta);
        }
        inventory.setItem(HATS_SLOT, hats);

        if (!registered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.openInventory(inventory);
    }

    private void fillInventory() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.empty());
            filler.setItemMeta(fillerMeta);
        }
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

        switch (event.getSlot()) {
            case TRIMS_SLOT -> { unregister(); new TrimPatternGUI(plugin, player).open(); }
            case TRAILS_SLOT -> { unregister(); new ArrowTrailsGUI(plugin, player).open(); }
            case EFFECTS_SLOT -> { unregister(); new KillEffectsGUI(plugin, player).open(); }
            case HATS_SLOT -> { unregister(); new HatsGUI(plugin, player).open(); }
            default -> { /* filler — ignore */ }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        unregister();
    }

    private void unregister() {
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }
}
