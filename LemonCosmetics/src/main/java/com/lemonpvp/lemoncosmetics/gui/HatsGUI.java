package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.HatType;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HatsGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int[] HAT_SLOTS = {10, 11, 12, 13, 14, 15};
    private static final int BACK_SLOT = 22;

    private final LemonCosmetics plugin;
    private final Player player;
    private Inventory inventory;
    private boolean registered = false;

    public HatsGUI(LemonCosmetics plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27,
                MM.deserialize("<gradient:#fffb00:#00ff00>Hats</gradient>"));

        ItemStack filler = filler();
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        renderHats();
        inventory.setItem(BACK_SLOT, backButton());

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        player.openInventory(inventory);
    }

    private void renderHats() {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        HatType[] hats = HatType.values();
        for (int i = 0; i < hats.length && i < HAT_SLOTS.length; i++) {
            inventory.setItem(HAT_SLOTS[i], buildHatItem(hats[i], cosmetics));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!clicker.getUniqueId().equals(player.getUniqueId())) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) return;
        event.setCancelled(true);

        int slot = event.getSlot();

        if (slot == BACK_SLOT) {
            clicker.playSound(clicker.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 0.9f);
            unregister();
            new CosmeticsMainGUI(plugin, player).open();
            return;
        }

        for (int i = 0; i < HAT_SLOTS.length; i++) {
            if (slot == HAT_SLOTS[i]) {
                HatType hat = HatType.values()[i];
                handleHatClick(clicker, hat);
                return;
            }
        }
    }

    private void handleHatClick(Player clicker, HatType hat) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(clicker.getUniqueId());
        boolean owned = cosmetics != null && cosmetics.ownsHat(hat.id);
        UUID clickerUuid = clicker.getUniqueId();

        if (!owned) {
            plugin.getHatManager().buyHat(clickerUuid, hat.id)
                    .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(clickerUuid);
                        if (p == null) return;
                        if (success) {
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
                            p.sendMessage(MM.deserialize("<green>Purchased <yellow>" + hat.displayName + "</yellow>!"));
                            renderHats();
                        } else {
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                            p.sendMessage(MM.deserialize("<red>You cannot afford <yellow>" + hat.displayName
                                    + "</yellow>. It costs <gold>" + hat.price + " coins</gold>."));
                        }
                    }));
            return;
        }

        String equippedId = cosmetics.getEquippedHatId();
        if (hat.id.equals(equippedId)) {
            plugin.getHatManager().unequipHat(clickerUuid)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(clickerUuid);
                        if (p == null) return;
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.9f);
                        p.sendMessage(MM.deserialize("<yellow>Hat unequipped."));
                        renderHats();
                    }));
        } else {
            plugin.getHatManager().equipHat(clickerUuid, hat.id)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(clickerUuid);
                        if (p == null) return;
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
                        p.sendMessage(MM.deserialize("<green>Equipped <yellow>" + hat.displayName + "</yellow>!"));
                        renderHats();
                    }));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        if (!event.getInventory().equals(inventory)) return;
        unregister();
    }

    private void unregister() {
        if (registered) { HandlerList.unregisterAll(this); registered = false; }
    }

    private ItemStack buildHatItem(HatType hat, PlayerCosmetics cosmetics) {
        boolean owned = cosmetics != null && cosmetics.ownsHat(hat.id);
        boolean equipped = owned && hat.id.equals(cosmetics.getEquippedHatId());

        ItemStack item = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta == null) return item;

        meta.setColor(Color.fromRGB(0, 0, 0));
        meta.setCustomModelData(hat.customModelData);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE);

        meta.displayName(owned
                ? MM.deserialize("<white>" + hat.displayName)
                : MM.deserialize("<gray>" + hat.displayName));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (owned) {
            if (equipped) {
                lore.add(MM.deserialize("<aqua>✔ Equipped"));
                lore.add(MM.deserialize("<gray>Click to unequip"));
            } else {
                lore.add(MM.deserialize("<green>Owned"));
                lore.add(MM.deserialize("<gray>Click to equip"));
            }
        } else {
            lore.add(MM.deserialize("<gold>Price: <yellow>" + hat.price + " coins"));
            lore.add(MM.deserialize("<gray>Click to purchase"));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(MM.deserialize("<gray>Back")); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack filler() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); pane.setItemMeta(meta); }
        return pane;
    }
}
