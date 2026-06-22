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
import java.util.UUID;

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
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
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
            clicker.playSound(clicker.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 0.9f);
            unregister();
            new TrimArmorPieceGUI(plugin, player, patternId).open();
            return;
        }

        if (slot == BUY_SLOT) {
            if (selectedMaterialId == null) {
                clicker.playSound(clicker.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                clicker.sendMessage(MM.deserialize("<red>Wähle zuerst ein Material aus."));
                return;
            }
            PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(clicker.getUniqueId());
            if (cosmetics != null && cosmetics.ownsMaterial(selectedMaterialId)) {
                clicker.playSound(clicker.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                clicker.sendMessage(MM.deserialize("<red>Du besitzt dieses Material bereits."));
                return;
            }
            int cost = plugin.getConfig().getInt("prices.trim-material", 50);
            UUID buyerUuid = clicker.getUniqueId();
            String buyMatId = selectedMaterialId;
            plugin.getCosmeticsManager().buyTrimMaterial(buyerUuid, buyMatId)
                    .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(buyerUuid);
                        if (p == null) return;
                        if (success) {
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
                            p.sendMessage(MM.deserialize("<green>Gekauft: <yellow>"
                                    + plugin.getArmorTrimManager().getMaterialDisplayName(buyMatId)
                                    + "</yellow> für <gold>" + cost + " Münzen</gold>!"));
                            selectedMaterialId = null;
                            renderMaterials();
                            inventory.setItem(BUY_SLOT, buildBuyButton());
                        } else {
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                            if (plugin.getCosmeticsManager().getLemonCore() != null) {
                                plugin.getCosmeticsManager().getLemonCore()
                                        .getPlayerDataManager().getCoins(buyerUuid)
                                        .thenAccept(bal -> Bukkit.getScheduler().runTask(plugin, () -> {
                                            Player p2 = Bukkit.getPlayer(buyerUuid);
                                            if (p2 == null) return;
                                            p2.sendMessage(MM.deserialize("<red>Du brauchst <gold>" + cost
                                                    + " Münzen</gold>, hast aber nur <gold>" + bal + " Münzen</gold>."));
                                        }));
                            }
                        }
                    }));
            return;
        }

        String matId = meta.getPersistentDataContainer().get(materialKey, PersistentDataType.STRING);
        if (matId == null) return;

        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(clicker.getUniqueId());
        boolean owned = cosmetics != null && cosmetics.ownsMaterial(matId);

        if (owned) {
            UUID applyUuid = clicker.getUniqueId();
            plugin.getCosmeticsManager().applyArmorTrim(applyUuid, armorSlot, patternId, matId)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(applyUuid);
                        unregister();
                        if (p == null) return;
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.3f);
                        p.sendMessage(MM.deserialize("<green>Trim auf <yellow>"
                                + armorSlot.getDisplayName() + "</yellow> angewendet."));
                        p.closeInventory();
                    }));
        } else {
            selectedMaterialId = matId;
            clicker.playSound(clicker.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.4f, 1.1f);
            clicker.sendMessage(MM.deserialize("<yellow>Klicke <gold>Material kaufen</gold>, um "
                    + plugin.getArmorTrimManager().getMaterialDisplayName(matId) + " zu kaufen."));
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
        // Always show the actual material icon so players can see what they're buying
        Material icon = plugin.getArmorTrimManager().getTrimMaterialIcon(matId);
        ItemStack item = new ItemStack(icon != null ? icon : Material.STONE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = plugin.getArmorTrimManager().getMaterialDisplayName(matId);
        meta.displayName(MM.deserialize("<!italic><white>" + name + "</white>"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (owned) {
            lore.add(MM.deserialize("<!italic><green>✔ Owned</green>"));
            if (matId.equals(selectedMaterialId)) {
                lore.add(MM.deserialize("<!italic><yellow>Selected</yellow>"));
            } else {
                lore.add(MM.deserialize("<!italic><gray>Click to apply</gray>"));
            }
            // Enchanting glint for owned items
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        } else {
            int cost = plugin.getConfig().getInt("prices.trim-material", 50);
            lore.add(MM.deserialize("<!italic><gray>Price: <gold>" + cost + " Coins</gold></gray>"));
            if (matId.equals(selectedMaterialId)) {
                lore.add(MM.deserialize("<!italic><yellow>Selected — click Buy Material</yellow>"));
            } else {
                lore.add(MM.deserialize("<!italic><dark_gray>Not owned — click to select</dark_gray>"));
            }
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
        int cost = plugin.getConfig().getInt("prices.trim-material", 50);
        meta.displayName(MM.deserialize("<!italic><gold>Buy Material</gold>"));
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic><gray>Cost: <gold>" + cost + " Coins</gold></gray>"));
        if (selectedMaterialId != null)
            lore.add(MM.deserialize("<!italic><yellow>Selected: " + plugin.getArmorTrimManager().getMaterialDisplayName(selectedMaterialId) + "</yellow>"));
        else
            lore.add(MM.deserialize("<!italic><gray>Select a material first</gray>"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(MM.deserialize("<!italic><gray>Back</gray>")); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack filler() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); pane.setItemMeta(meta); }
        return pane;
    }
}
