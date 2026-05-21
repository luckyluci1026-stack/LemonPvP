package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.KillEffectType;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
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

import java.util.ArrayList;
import java.util.List;

public class KillEffectsGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int[] EFFECT_SLOTS = {10, 12, 14, 16};
    private static final int BACK_SLOT = 22;

    private final LemonCosmetics plugin;
    private final Player player;
    private Inventory inventory;
    private boolean registered = false;

    public KillEffectsGUI(LemonCosmetics plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27,
                MM.deserialize("<gradient:#fffb00:#00ff00>Kill Effects</gradient>"));

        ItemStack filler = filler();
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        renderEffects();
        inventory.setItem(BACK_SLOT, backButton());

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.openInventory(inventory);
    }

    private void renderEffects() {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        KillEffectType[] effects = KillEffectType.values();
        for (int i = 0; i < effects.length && i < EFFECT_SLOTS.length; i++) {
            inventory.setItem(EFFECT_SLOTS[i], buildEffectItem(effects[i], cosmetics));
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
            unregister();
            new CosmeticsMainGUI(plugin, player).open();
            return;
        }

        for (int i = 0; i < EFFECT_SLOTS.length; i++) {
            if (slot == EFFECT_SLOTS[i]) {
                KillEffectType effect = KillEffectType.values()[i];
                handleEffectClick(clicker, effect);
                return;
            }
        }
    }

    private void handleEffectClick(Player clicker, KillEffectType effect) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(clicker.getUniqueId());
        boolean owned = cosmetics != null && cosmetics.ownsEffect(effect.getId());

        if (!owned) {
            clicker.sendMessage(MM.deserialize("<red>You don't own <yellow>" + effect.getDisplayName()
                    + "</yellow>. Earn it by redeeming a code!"));
            return;
        }

        String activeId = cosmetics.getActiveEffectId();
        if (effect.getId().equals(activeId)) {
            // Unequip
            plugin.getCosmeticsManager().setActiveKillEffect(clicker.getUniqueId(), null)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        clicker.sendMessage(MM.deserialize("<yellow>Kill effect unequipped."));
                        renderEffects();
                    }));
        } else {
            // Equip
            plugin.getCosmeticsManager().setActiveKillEffect(clicker.getUniqueId(), effect.getId())
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        clicker.sendMessage(MM.deserialize("<green>Equipped <yellow>" + effect.getDisplayName() + "</yellow>!"));
                        renderEffects();
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

    private ItemStack buildEffectItem(KillEffectType effect, PlayerCosmetics cosmetics) {
        boolean owned = cosmetics != null && cosmetics.ownsEffect(effect.getId());
        boolean active = owned && cosmetics.getActiveEffectId() != null
                && cosmetics.getActiveEffectId().equals(effect.getId());

        Material icon = owned ? effect.getIcon() : Material.GRAY_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(owned
                ? MM.deserialize("<white>" + effect.getDisplayName())
                : MM.deserialize("<gray>" + effect.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (owned) {
            lore.add(MM.deserialize("<green>Owned"));
            if (active) {
                lore.add(MM.deserialize("<aqua>Active — click to unequip"));
            } else {
                lore.add(MM.deserialize("<gray>Click to equip"));
            }
        } else {
            lore.add(MM.deserialize("<dark_gray>Not owned"));
            lore.add(MM.deserialize("<gray>Redeem a code to unlock"));
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
