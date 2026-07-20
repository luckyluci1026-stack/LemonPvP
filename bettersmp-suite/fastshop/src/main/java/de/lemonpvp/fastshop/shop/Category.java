package de.lemonpvp.fastshop.shop;

import org.bukkit.Material;

import java.util.List;

/**
 * Eine Shop-Kategorie mit Icon, Anzeigename, Slot im Hauptmenue und Items.
 */
public record Category(String id, Material icon, String name, int slot, List<ShopItem> items) {
}
