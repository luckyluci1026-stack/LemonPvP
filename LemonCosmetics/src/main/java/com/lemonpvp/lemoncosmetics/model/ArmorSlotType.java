package com.lemonpvp.lemoncosmetics.model;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

public enum ArmorSlotType {
    HEAD("Helmet", EquipmentSlot.HEAD, Material.IRON_HELMET, 3),
    CHEST("Chestplate", EquipmentSlot.CHEST, Material.IRON_CHESTPLATE, 2),
    LEGS("Leggings", EquipmentSlot.LEGS, Material.IRON_LEGGINGS, 1),
    FEET("Boots", EquipmentSlot.FEET, Material.IRON_BOOTS, 0);

    private final String displayName;
    private final EquipmentSlot equipmentSlot;
    private final Material iconMaterial;
    private final int armorIndex;

    ArmorSlotType(String displayName, EquipmentSlot equipmentSlot, Material iconMaterial, int armorIndex) {
        this.displayName = displayName;
        this.equipmentSlot = equipmentSlot;
        this.iconMaterial = iconMaterial;
        this.armorIndex = armorIndex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    /**
     * Index used with {@code PlayerInventory#setArmorContents}:
     * 3 = helmet, 2 = chestplate, 1 = leggings, 0 = boots.
     */
    public int getArmorIndex() {
        return armorIndex;
    }
}
