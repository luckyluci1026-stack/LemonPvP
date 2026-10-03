package de.lemonpvp.reportplus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/** Baut GUI-Items ohne kursiven Standardtext - kleine, eigene Kopie statt Abhaengigkeit zu FastShop. */
public final class GuiItem {

    private GuiItem() {
    }

    public static ItemStack of(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        apply(stack, name, lore);
        return stack;
    }

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

    public static ItemStack filler(Material material) {
        return of(material, " ", List.of());
    }

    /** Fuellt das ganze Inventar mit einem Rahmen-Item. */
    public static void fuelle(org.bukkit.inventory.Inventory inv, Material material) {
        fuelleBereich(inv, 0, inv.getSize(), material);
    }

    /** Fuellt die Slots [von, bis) mit einem Rahmen-Item. */
    public static void fuelleBereich(org.bukkit.inventory.Inventory inv, int von, int bis, Material material) {
        ItemStack rahmen = filler(material);
        for (int i = von; i < bis; i++) {
            inv.setItem(i, rahmen);
        }
    }

    private static void apply(ItemStack stack, String name, List<String> lore) {
        ItemMeta meta = stack.getItemMeta();
        if (name != null) {
            meta.displayName(clean(mm(name)));
        }
        if (lore != null && !lore.isEmpty()) {
            List<Component> lines = new ArrayList<>(lore.size());
            for (String line : lore) {
                lines.add(clean(mm(line)));
            }
            meta.lore(lines);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        stack.setItemMeta(meta);
    }

    public static Component mm(String text) {
        return MiniMessage.miniMessage().deserialize(text == null ? "" : text);
    }

    private static Component clean(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
