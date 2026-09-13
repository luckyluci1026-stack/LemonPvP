package de.lemonpvp.bettersmp.gui;

import de.lemonpvp.bettersmp.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Kleine Helfer zum Bauen hübscher GUI-Items (ohne kursiven Standardtext).
 */
public final class GuiItems {

    private GuiItems() {
    }

    public static ItemStack item(Material material, String name, List<String> lore, boolean glow) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(clean(Text.mm(name)));
        if (lore != null && !lore.isEmpty()) {
            List<Component> lines = new ArrayList<>(lore.size());
            for (String line : lore) {
                lines.add(clean(Text.mm(line)));
            }
            meta.lore(lines);
        }
        if (glow) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack filler(Material material) {
        return item(material, "<gray>", List.of(), false);
    }

    private static Component clean(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
