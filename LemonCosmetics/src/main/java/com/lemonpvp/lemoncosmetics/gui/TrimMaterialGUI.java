package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.ArmorSlotType;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class TrimMaterialGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCosmetics plugin;
    private final Player player;
    private final String patternId;
    private final ArmorSlotType armorSlot;
    private final NamespacedKey materialKey;
    private String selectedMaterialId = null;
    private Inventory inventory;
    private boolean registered = false;

    // Slots for 11 material items centered in a 9×6 grid (row 3, starting slot 19)
    private static final int[] MATERIAL_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31};
    private static final int BUY_SLOT = 49;
    private static final int BACK_SLOT = 45;

    public TrimMaterialGUI(LemonCosmetics plugin, Player player, String patternId, ArmorSlotType armorSlot) {
        this.plugin = plugin;
        this.player = player;
        this.patternId = patternId;
        this.armorSlot = armorSlot;
        this.materialKey = new NamespacedKey(plugin, "cosmetics_material_id");
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 54,
                MM.deserialize("<gradient:#fffb00:#00ff00>Select Trim Material</gradient>"));

        ItemStack filler = filler();
        for (int i = 0; i < 54; i++) inventory.setItem(i, filler);

        renderMaterials();
        inventory.setItem(BUY_SLOT, buildBuyButton());
        inventory.setItem(BACK_SLOT, backButton());

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.openInventory(inventory);
    }

    private void renderMaterials() {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        List<String> materials = plugin.getArmorTrimManager().getAllMaterialIds();
        for (int i = 0; i < materials.size() && i < MATERIAL_SLOTS.length; i++) {
            String matId = materials.get(i);
            boolean owned = cosmetics != null && cosmetics.ownsMaterial(matId);
            inventory.setItem(MATERIAL_SLOTS[i], buildMaterialItem(matId, owned));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!clicker.getUniqueId().equals(player.getUniqueId())) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) return;
        event.setCancelled(true);

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        if (slot == BACK_SLOT) {
            unregister();
            new TrimArmorPieceGUI(plugin, player, patternId).open();
            return;
        }

        if (slot == BUY_SLOT) {
            if (selectedMaterialId == null) {
                clicker.sendMessage(MM.deserialize("<red>Select a material first."));
                return;
            }
            PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(clicker.getUniqueId());
            if (cosmetics != null && cosmetics.ownsMaterial(selectedMaterialId)) {
                clicker.sendMessage(MM.deserialize("<red>You already own this material."));
                return;
            }
            int cost = plugin.getConfig().getInt("prices.trim-material", 50);
            plugin.getCosmeticsManager().buyTrimMaterial(clicker.getUniqueId(), selectedMaterialId)
                    .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success) {
                            clicker.sendMessage(MM.deserialize("<green>Purchased <yellow>"
                                    + plugin.getArmorTrimManager().getMaterialDisplayName(selectedMaterialId)
                                    + "</yellow> for <gold>" + cost + " Coins</gold>!"));
                            selectedMaterialId = null;
                            renderMaterials();
                            inventory.setItem(BUY_SLOT, buildBuyButton());
                        } else {
                            plugin.getCosmeticsManager().canAfford(clicker.getUniqueId(), cost)
                                    .thenAccept(a -> plugin.getCosmeticsManager().getLemonCore() != null
                                            ? plugin.getCosmeticsManager().getLemonCore()
                                                    .getPlayerDataManager().getCoins(clicker.getUniqueId())
                                                    .thenAccept(bal -> Bukkit.getScheduler().runTask(plugin, () ->
                                                            clicker.sendMessage(MM.deserialize("<red>You need <gold>" + cost
                                                                    + " Coins</gold> but only have <gold>" + bal + " Coins</gold>."))))
                                            : null);
                        }
                    }));
            return;
        }

        String matId = meta.getPersistentDataContainer().get(materialKey, PersistentDataType.STRING);
        if (matId == null) return;

        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(clicker.getUniqueId());
        boolean owned = cosmetics != null && cosmetics.ownsMaterial(matId);

        if (owned) {
            // Apply trim
            plugin.getCosmeticsManager().applyArmorTrim(clicker.getUniqueId(), armorSlot, patternId, matId)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        clicker.sendMessage(MM.deserialize("<green>Trim applied to <yellow>"
                                + armorSlot.getDisplayName() + "</yellow>."));
                        unregister();
                        clicker.closeInventory();
                    }));
        } else {
            selectedMaterialId = matId;
            clicker.sendMessage(MM.deserialize("<yellow>Click <gold>Buy Material</gold> to purchase "
                    + plugin.getArmorTrimManager().getMaterialDisplayName(matId) + "."));
            renderMaterials();
            inventory.setItem(BUY_SLOT, buildBuyButton());
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

    private ItemStack buildMaterialItem(String matId, boolean owned) {
        Material icon = owned
                ? plugin.getArmorTrimManager().getTrimMaterialIcon(matId)
                : Material.GRAY_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = plugin.getArmorTrimManager().getMaterialDisplayName(matId);
        meta.displayName(owned ? MM.deserialize("<white>" + name) : MM.deserialize("<gray>" + name));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (owned) {
            lore.add(MM.deserialize("<green>Owned"));
            if (matId.equals(selectedMaterialId)) {
                lore.add(MM.deserialize("<yellow>Selected"));
            } else {
                lore.add(MM.deserialize("<gray>Click to apply"));
            }
        } else {
            lore.add(MM.deserialize("<gray>Price: <gold>50 Coins"));
            lore.add(MM.deserialize("<dark_gray>Not owned"));
            if (matId.equals(selectedMaterialId)) lore.add(MM.deserialize("<yellow>Selected — click Buy Material"));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(materialKey, PersistentDataType.STRING, matId);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBuyButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(MM.deserialize("<gold>Buy Material"));
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<gray>Cost: <gold>50 Coins"));
        if (selectedMaterialId != null)
            lore.add(MM.deserialize("<yellow>Selected: " + plugin.getArmorTrimManager().getMaterialDisplayName(selectedMaterialId)));
        else
            lore.add(MM.deserialize("<gray>Select a material first"));
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
