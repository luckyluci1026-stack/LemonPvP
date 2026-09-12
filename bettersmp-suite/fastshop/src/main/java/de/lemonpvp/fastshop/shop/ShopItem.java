package de.lemonpvp.fastshop.shop;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;

/**
 * Ein Shop-Eintrag. buy/sell &lt; 0 bedeutet "nicht kauf-/verkaufbar".
 *
 * @param enchants Verzauberungen, die beim Kauf mitgegeben werden (leer = keine).
 *                 Absichtlich am Item und nicht global: so kann dieselbe
 *                 Kategorie sowohl schlichte als auch fertig verzauberte
 *                 Varianten anbieten, rein über shop.yml.
 */
public record ShopItem(Material material, double buy, double sell, String name, List<String> lore,
                        Map<Enchantment, Integer> enchants) {

    public boolean buyable() {
        return buy >= 0;
    }

    public boolean sellable() {
        return sell >= 0;
    }
}
