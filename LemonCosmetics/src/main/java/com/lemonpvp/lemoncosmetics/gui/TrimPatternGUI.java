package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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

import java.util.ArrayList;
import java.util.List;

public class TrimPatternGUI implements Listener {

    static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCosmetics plugin;
    private final Player player;
    private Inventory inventory;

    private String selectedPatternId = null;

    public TrimPatternGUI(LemonCosmetics plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 54,
                MM.deserialize("<gradient:#fffb00:#00ff00>Armor Trims - Pattern</gradient>"));

        renderItems();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    private void renderItems() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        List<String> patternIds = plugin.getArmorTrimManager().getAllPatternIds();
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());

        int patternCount = Math.min(patternIds.size(), 18);
        for (int i = 0; i < patternCount; i++) {
            String patternId = patternIds.get(i);
            boolean owned = cosmetics != null && cosmetics.ownsPattern(patternId);
            boolean selected = patternId.equals(selectedPatternId);

            ItemStack item;
            if (owned) {
                Material icon = plugin.getArmorTrimManager().getTrimPatternIcon(patternId);
                item = new ItemStack(icon != null ? icon : Material.PAPER);
            } else {
                item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            }

            ItemMeta meta = item.getItemMeta();
            String displayName = plugin.getArmorTrimManager().getDisplayName(patternId);
            meta.displayName(MM.deserialize("<white>" + displayName));

            List<Component> lore = new ArrayList<>();
            if (owned) {
                lore.add(MM.deserialize("<green>Owned"));
                lore.add(MM.deserialize("<gray>Click to select"));
            } else {
                int cost = plugin.getConfig().getInt("prices.trim-pattern", 250);
                lore.add(MM.deserialize("<gray>Price: <gold>" + cost + " Coins"));
                lore.add(MM.deserialize("<dark_gray>Not owned"));
            }
            meta.lore(lore);

            if (selected && !owned) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }

        // Slot 45: Back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.displayName(MM.deserialize("<gray>Back"));
        backButton.setItemMeta(backMeta);
        inventory.setItem(45, backButton);

        // Slot 49: Buy button
        int cost = plugin.getConfig().getInt("prices.trim-pattern", 250);
        ItemStack buyButton = new ItemStack(Material.GOLD_INGOT);
        ItemMeta buyMeta = buyButton.getItemMeta();
        buyMeta.displayName(MM.deserialize("<gold>Buy Trim Pattern"));
        buyMeta.lore(List.of(
                MM.deserialize("<gray>Cost: <gold>" + cost + " Coins"),
                MM.deserialize("<gray>Click a pattern to buy it")
        ));
        buyButton.setItemMeta(buyMeta);
        inventory.setItem(49, buyButton);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getWhoClicked().getUniqueId().equals(player.getUniqueId())) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) return;

        int slot = event.getSlot();
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        List<String> patternIds = plugin.getArmorTrimManager().getAllPatternIds();

        // Pattern slots 0-17
        if (slot >= 0 && slot <= 17 && slot < patternIds.size()) {
            String patternId = patternIds.get(slot);
            boolean owned = cosmetics != null && cosmetics.ownsPattern(patternId);

            if (owned) {
                player.closeInventory();
                new TrimArmorPieceGUI(plugin, player, patternId).open();
            } else {
                selectedPatternId = patternId;
                renderItems();
                player.sendMessage(MM.deserialize("<yellow>Click <gold>Buy Trim Pattern</gold> to confirm your purchase."));
            }
            return;
        }

        // Back button
        if (slot == 45) {
            player.closeInventory();
            new CosmeticsMainGUI(plugin, player).open();
            return;
        }

        // Buy button
        if (slot == 49) {
            if (selectedPatternId == null) {
                player.sendMessage(MM.deserialize("<red>Select a pattern first!"));
                return;
            }
            if (cosmetics != null && cosmetics.ownsPattern(selectedPatternId)) {
                player.sendMessage(MM.deserialize("<red>You already own this pattern!"));
                return;
            }

            String patternToBuy = selectedPatternId;
            plugin.getCosmeticsManager().buyTrimPattern(player.getUniqueId(), patternToBuy)
                    .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success) {
                            selectedPatternId = null;
                            renderItems();
                            player.sendMessage(MM.deserialize("<green>Successfully purchased the trim pattern!"));
                        } else {
                            player.sendMessage(MM.deserialize("<red>You don't have enough coins!"));
                        }
                    }));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        HandlerList.unregisterAll(this);
    }
}
