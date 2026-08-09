package de.lemonpvp.fastshop.gui;

import de.lemonpvp.fastshop.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/** Baut hübsche GUI-Items ohne kursiven Standardtext. */
public final class GuiUtil {

    private GuiUtil() {
    }

    public static ItemStack item(Material material, int amount, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material, Math.max(1, Math.min(64, amount)));
        apply(stack, name, lore);
        return stack;
    }

    /** Spielerkopf - für die Kontostand-Anzeige. */
    public static ItemStack head(OfflinePlayer owner, String name, List<String> lore) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof SkullMeta skull) {
            skull.setOwningPlayer(owner);
        }
        stack.setItemMeta(meta);
        apply(stack, name, lore);
        return stack;
    }

    private static void apply(ItemStack stack, String name, List<String> lore) {
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
    }

    /** Item mit Leucht-Effekt (für hervorgehobene Buttons). */
    public static ItemStack glowing(Material material, int amount, String name, List<String> lore) {
        ItemStack stack = item(material, amount, name, lore);
        ItemMeta meta = stack.getItemMeta();
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack filler(Material material) {
        return item(material, 1, " ", List.of());
    }

    private static Component clean(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
