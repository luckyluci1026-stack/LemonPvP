package de.lemonpvp.fastshop.shop;

import org.bukkit.Material;

import java.util.List;

/**
 * Ein Shop-Eintrag. buy/sell &lt; 0 bedeutet "nicht kauf-/verkaufbar".
 */
public record ShopItem(Material material, double buy, double sell, String name, List<String> lore) {

    public boolean buyable() {
        return buy >= 0;
    }

    public boolean sellable() {
        return sell >= 0;
    }
}
