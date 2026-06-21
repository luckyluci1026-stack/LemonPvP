package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
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
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TrimPatternGUI implements Listener {

    static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCosmetics plugin;
    private final Player player;
    private Inventory inventory;

    private String selectedPatternId = null;
    private boolean registered = false;

    public TrimPatternGUI(LemonCosmetics plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 54,
                MM.deserialize("<!italic><gradient:#fffb00:#00ff00>Armor Trims - Pattern</gradient>"));

        renderItems();

        if (!registered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        player.openInventory(inventory);
    }

    private void renderItems() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.empty());
            filler.setItemMeta(fillerMeta);
        }
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
            inventory.setItem(i, buildPreviewItem(patternId, owned, selected));
        }

        // Slot 45: Back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(MM.deserialize("<!italic><gray>Back</gray>"));
            backButton.setItemMeta(backMeta);
        }
        inventory.setItem(45, backButton);

        // Slot 49: Buy button
        int cost = plugin.getConfig().getInt("prices.trim-pattern", 250);
        ItemStack buyButton = new ItemStack(Material.GOLD_INGOT);
        ItemMeta buyMeta = buyButton.getItemMeta();
        if (buyMeta != null) {
            buyMeta.displayName(MM.deserialize("<!italic><gold>Buy Trim Pattern</gold>"));
            buyMeta.lore(List.of(
                    MM.deserialize("<!italic><gray>Cost: <gold>" + cost + " Coins</gold></gray>"),
                    MM.deserialize("<!italic><gray>Click a pattern first, then buy</gray>")
            ));
            buyButton.setItemMeta(buyMeta);
        }
        inventory.setItem(49, buyButton);
    }

    /** Builds a diamond chestplate preview item with the actual trim applied. */
    private ItemStack buildPreviewItem(String patternId, boolean owned, boolean selected) {
        ItemStack item = new ItemStack(Material.DIAMOND_CHESTPLATE);

        if (item.getItemMeta() instanceof ArmorMeta meta) {
            // Apply the trim with gold material as a visual default
            TrimPattern pattern = Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(patternId));
            TrimMaterial material = Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft("gold"));
            if (pattern != null && material != null) {
                meta.setTrim(new ArmorTrim(material, pattern));
            }

            String displayName = plugin.getArmorTrimManager().getDisplayName(patternId);
            meta.displayName(MM.deserialize("<!italic><white>" + displayName + "</white>"));

            List<Component> lore = new ArrayList<>();
            lore.add(MM.deserialize("<!italic><dark_gray>Preview: Gold material</dark_gray>"));
            lore.add(Component.empty());

            if (owned) {
                lore.add(MM.deserialize("<!italic><green>✔ Owned</green>"));
                lore.add(MM.deserialize("<!italic><gray>Click to apply to armor</gray>"));
                // Enchanting glint to mark owned items
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else if (selected) {
                int cost = plugin.getConfig().getInt("prices.trim-pattern", 250);
                lore.add(MM.deserialize("<!italic><yellow>Selected — click Buy to confirm</yellow>"));
                lore.add(MM.deserialize("<!italic><gray>Price: <gold>" + cost + " Coins</gold></gray>"));
            } else {
                int cost = plugin.getConfig().getInt("prices.trim-pattern", 250);
                lore.add(MM.deserialize("<!italic><gray>Price: <gold>" + cost + " Coins</gold></gray>"));
                lore.add(MM.deserialize("<!italic><dark_gray>Not owned — click to select</dark_gray>"));
            }

            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
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
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                player.closeInventory();
                new TrimArmorPieceGUI(plugin, player, patternId).open();
            } else {
                selectedPatternId = patternId;
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.4f, 1.1f);
                renderItems();
                player.sendMessage(MM.deserialize("<!italic><yellow>Click <gold>Buy Trim Pattern</gold> to confirm your purchase.</yellow>"));
            }
            return;
        }

        // Back button
        if (slot == 45) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 0.9f);
            player.closeInventory();
            new CosmeticsMainGUI(plugin, player).open();
            return;
        }

        // Buy button
        if (slot == 49) {
            if (selectedPatternId == null) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                player.sendMessage(MM.deserialize("<!italic><red>Select a pattern first!</red>"));
                return;
            }
            if (cosmetics != null && cosmetics.ownsPattern(selectedPatternId)) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                player.sendMessage(MM.deserialize("<!italic><red>You already own this pattern!</red>"));
                return;
            }

            String patternToBuy = selectedPatternId;
            UUID buyerUuid = player.getUniqueId();
            plugin.getCosmeticsManager().buyTrimPattern(buyerUuid, patternToBuy)
                    .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(buyerUuid);
                        if (p == null) return;
                        if (success) {
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
                            selectedPatternId = null;
                            renderItems();
                            p.sendMessage(MM.deserialize("<!italic><green>Successfully purchased the trim pattern!</green>"));
                        } else {
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                            p.sendMessage(MM.deserialize("<!italic><red>You don't have enough coins!</red>"));
                        }
                    }));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }
}
