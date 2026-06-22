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
                MM.deserialize("<!italic><gradient:#ab47bc:#7b1fa2>Pfeil-Trails</gradient>"));

        ItemStack filler = filler();
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        renderTrails();
        inventory.setItem(BACK_SLOT, backButton());

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
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
            clicker.playSound(clicker.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 0.9f);
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
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
                            p.sendMessage(MM.deserialize("<green>Gekauft: <yellow>" + trail.displayName + "</yellow>!"));
                            renderTrails();
                        } else {
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                            p.sendMessage(MM.deserialize("<red>Du kannst dir <yellow>" + trail.displayName
                                    + "</yellow> nicht leisten. Kostet <gold>" + trail.price + " Münzen</gold>."));
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
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.9f);
                        p.sendMessage(MM.deserialize("<yellow>Pfeil-Trail deaktiviert."));
                        renderTrails();
                    }));
        } else {
            plugin.getArrowTrailManager().setActiveTrail(clickerUuid, trail.id)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(clickerUuid);
                        if (p == null) return;
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
                        p.sendMessage(MM.deserialize("<green>Aktiviert: <yellow>" + trail.displayName + "</yellow>!"));
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
                ? MM.deserialize("<!italic><light_purple>" + trail.displayName)
                : MM.deserialize("<!italic><gray>" + trail.displayName));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (owned) {
            if (active) {
                lore.add(MM.deserialize("<!italic><aqua>✔ Aktiv"));
                lore.add(MM.deserialize("<!italic><gray>Klicken zum Deaktivieren"));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(MM.deserialize("<!italic><green>✔ Besessen"));
                lore.add(MM.deserialize("<!italic><gray>Klicken zum Aktivieren"));
            }
        } else {
            lore.add(MM.deserialize("<!italic><gold>Preis: <yellow>" + trail.price + " Münzen ⭐"));
            lore.add(MM.deserialize("<!italic><gray>Klicken zum Kaufen"));
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
        if (meta != null) { meta.displayName(MM.deserialize("<!italic><gray>← Zurück")); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack filler() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); pane.setItemMeta(meta); }
        return pane;
    }
}
