package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.ArrowTrailType;
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
import java.util.UUID;

public class ArrowTrailsGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int[] TRAIL_SLOTS = {11, 13, 15};
    private static final int BACK_SLOT = 22;

    private final LemonCosmetics plugin;
    private final Player player;
    private Inventory inventory;
    private boolean registered = false;

    public ArrowTrailsGUI(LemonCosmetics plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27,
                MM.deserialize("<gradient:#fffb00:#00ff00>Arrow Trails</gradient>"));

        ItemStack filler = filler();
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        renderTrails();
        inventory.setItem(BACK_SLOT, backButton());

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.openInventory(inventory);
    }

    private void renderTrails() {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        ArrowTrailType[] trails = ArrowTrailType.values();
        for (int i = 0; i < trails.length && i < TRAIL_SLOTS.length; i++) {
            inventory.setItem(TRAIL_SLOTS[i], buildTrailItem(trails[i], cosmetics));
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

        for (int i = 0; i < TRAIL_SLOTS.length; i++) {
            if (slot == TRAIL_SLOTS[i]) {
                ArrowTrailType trail = ArrowTrailType.values()[i];
                handleTrailClick(clicker, trail);
                return;
            }
        }
    }

    private void handleTrailClick(Player clicker, ArrowTrailType trail) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(clicker.getUniqueId());
        boolean owned = cosmetics != null && cosmetics.ownsTrail(trail.id);
        UUID clickerUuid = clicker.getUniqueId();

        if (!owned) {
            plugin.getArrowTrailManager().buyTrail(clickerUuid, trail.id)
                    .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(clickerUuid);
                        if (p == null) return;
                        if (success) {
                            p.sendMessage(MM.deserialize("<green>Purchased <yellow>" + trail.displayName + "</yellow>!"));
                            renderTrails();
                        } else {
                            p.sendMessage(MM.deserialize("<red>You cannot afford <yellow>" + trail.displayName
                                    + "</yellow>. It costs <gold>" + trail.price + " coins</gold>."));
                        }
                    }));
            return;
        }

        String activeId = cosmetics.getActiveTrailId();
        if (trail.id.equals(activeId)) {
            plugin.getArrowTrailManager().setActiveTrail(clickerUuid, null)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(clickerUuid);
                        if (p == null) return;
                        p.sendMessage(MM.deserialize("<yellow>Arrow trail deactivated."));
                        renderTrails();
                    }));
        } else {
            plugin.getArrowTrailManager().setActiveTrail(clickerUuid, trail.id)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(clickerUuid);
                        if (p == null) return;
                        p.sendMessage(MM.deserialize("<green>Activated <yellow>" + trail.displayName + "</yellow>!"));
                        renderTrails();
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

    private ItemStack buildTrailItem(ArrowTrailType trail, PlayerCosmetics cosmetics) {
        boolean owned = cosmetics != null && cosmetics.ownsTrail(trail.id);
        boolean active = owned && trail.id.equals(cosmetics.getActiveTrailId());

        Material icon = trailIcon(trail);
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(owned
                ? MM.deserialize("<white>" + trail.displayName)
                : MM.deserialize("<gray>" + trail.displayName));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (owned) {
            if (active) {
                lore.add(MM.deserialize("<aqua>✔ Active"));
                lore.add(MM.deserialize("<gray>Click to deactivate"));
            } else {
                lore.add(MM.deserialize("<green>Owned"));
                lore.add(MM.deserialize("<gray>Click to activate"));
            }
        } else {
            lore.add(MM.deserialize("<gold>Price: <yellow>" + trail.price + " coins"));
            lore.add(MM.deserialize("<gray>Click to purchase"));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Material trailIcon(ArrowTrailType trail) {
        return switch (trail) {
            case LEMON_TRAIL -> Material.ARROW;
            case EMERALD_TRAIL -> Material.ARROW;
            case FLAME_TRAIL -> Material.BLAZE_ROD;
        };
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
