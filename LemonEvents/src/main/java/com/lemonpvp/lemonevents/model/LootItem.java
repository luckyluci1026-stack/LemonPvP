package com.lemonpvp.lemonevents.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class LootItem {

    private final Material material;
    private final int minAmount;
    private final int maxAmount;
    private final int weight;
    private final int customModelData;     // 0 = none
    private final String displayName;      // null = default
    private final java.util.List<String> lore;
    private final boolean enchanted;

    public LootItem(Material material, int minAmount, int maxAmount, int weight,
                    int customModelData, String displayName, java.util.List<String> lore, boolean enchanted) {
        this.material = material;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.weight = weight;
        this.customModelData = customModelData;
        this.displayName = displayName;
        this.lore = lore;
        this.enchanted = enchanted;
    }

    public Material getMaterial() { return material; }
    public int getMinAmount() { return minAmount; }
    public int getMaxAmount() { return maxAmount; }
    public int getWeight() { return weight; }
    public int getCustomModelData() { return customModelData; }
    public String getDisplayName() { return displayName; }
    public java.util.List<String> getLore() { return lore; }
    public boolean isEnchanted() { return enchanted; }

    public ItemStack buildItem(int amount, net.kyori.adventure.text.minimessage.MiniMessage mm) {
        ItemStack item = new ItemStack(material, amount);
        var meta = item.getItemMeta();
        if (meta == null) return item;
        if (customModelData > 0) meta.setCustomModelData(customModelData);
        if (displayName != null) meta.displayName(mm.deserialize(displayName));
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream().map(mm::deserialize).toList());
        }
        if (enchanted) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 1, true);
        }
        item.setItemMeta(meta);
        return item;
    }
}
