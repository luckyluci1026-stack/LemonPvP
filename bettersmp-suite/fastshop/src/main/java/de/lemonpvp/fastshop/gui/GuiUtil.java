package de.lemonpvp.fastshop.gui;

import de.lemonpvp.fastshop.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Baut huebsche GUI-Items ohne kursiven Standardtext. */
public final class GuiUtil {

    private GuiUtil() {
    }

    public static ItemStack item(Material material, int amount, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = stack.getItemMeta();
        if (name != null) {
            meta.displayName(clean(Text.mm(name)));
        }
        if (lore != null && !lore.isEmpty()) {
            List<Component> lines = new ArrayList<>(lore.size());
            for (String line : lore) {
                lines.add(clean(Text.mm(line)));
            }
            meta.lore(lines);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack filler(Material material) {
        return item(material, 1, "<gray>", List.of());
    }

    private static Component clean(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
