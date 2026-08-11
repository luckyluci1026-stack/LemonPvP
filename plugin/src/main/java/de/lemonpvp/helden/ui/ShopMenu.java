package de.lemonpvp.helden.ui;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.util.Compat;
import de.lemonpvp.helden.util.Keys;
import de.lemonpvp.helden.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Der Zitronen-Shop aus shop.yml. */
public final class ShopMenu extends Menu {

    public ShopMenu(HeldenPlugin plugin) {
        super(plugin);
    }

    @Override
    public String title() {
        return plugin.shopConfig().getString("title", plugin.messages().raw("shop.title"));
    }

    @Override
    public int size() {
        int rows = Math.max(1, Math.min(6, plugin.shopConfig().getInt("rows", 3)));
        return rows * 9;
    }

    public boolean isEnabled() {
        return plugin.shopConfig().getBoolean("enabled", true);
    }

    @Override
    protected void build(Player viewer) {
        ConfigurationSection entries = plugin.shopConfig().getConfigurationSection("entries");
        if (entries == null) {
            return;
        }

        int fallbackSlot = 0;
        for (String id : entries.getKeys(false)) {
            ConfigurationSection entry = entries.getConfigurationSection(id);
            if (entry == null) {
                continue;
            }
            ItemStack stack = createStack(entry);
            if (stack == null) {
                plugin.getLogger().warning("Shop-Eintrag '" + id + "' hat weder ein gueltiges 'item' noch 'material'.");
                continue;
            }
            decorate(stack, entry, id);

            int slot = entry.getInt("slot", -1);
            set(slot >= 0 ? slot : fallbackSlot++, stack);
        }
    }

    private ItemStack createStack(ConfigurationSection entry) {
        int amount = Math.max(1, entry.getInt("amount", 1));
        String itemId = entry.getString("item", "");
        if (itemId != null && !itemId.isEmpty()) {
            return plugin.items().stack(itemId, amount);
        }
        Material material = Compat.material(entry.getString("material", ""));
        return material == null ? null : new ItemStack(material, amount);
    }

    /** Preis-Lore und Kauf-Aktion auf das Vorschauitem legen. */
    private void decorate(ItemStack stack, ConfigurationSection entry, String id) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        String display = entry.getString("display", "");
        if (display != null && !display.isEmpty()) {
            meta.setDisplayName(Text.color(display));
        }

        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
        lore.add("");
        lore.add(Text.color(Text.replace(plugin.messages().raw("shop.price-lore"),
                "%price%", entry.getInt("price", 0),
                "%currency%", plugin.economy().currencyName())));
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(Keys.menuAction(), PersistentDataType.STRING, "buy:" + id);
        stack.setItemMeta(meta);
    }

    @Override
    public void onClick(Player viewer, int slot, ItemStack clicked, String action) {
        if (action == null || !action.startsWith("buy:")) {
            return;
        }
        ConfigurationSection entry = plugin.shopConfig()
                .getConfigurationSection("entries." + action.substring("buy:".length()));
        if (entry == null) {
            return;
        }

        ItemStack purchase = createStack(entry);
        if (purchase == null) {
            return;
        }
        if (viewer.getInventory().firstEmpty() == -1) {
            plugin.messages().send(viewer, "shop.inventory-full");
            return;
        }

        int price = Math.max(0, entry.getInt("price", 0));
        if (!plugin.economy().take(viewer, price, "economy.reason-shop")) {
            Compat.sound(viewer, "block.note_block.bass", 0.7f, 0.8f);
            return;
        }

        viewer.getInventory().addItem(purchase);
        plugin.messages().send(viewer, "shop.bought",
                "%item%", displayNameOf(purchase, entry),
                "%price%", price,
                "%currency%", plugin.economy().currencyName());
        Compat.sound(viewer, "entity.experience_orb.pickup", 1.0f, 1.4f);
    }

    private String displayNameOf(ItemStack stack, ConfigurationSection entry) {
        String display = entry.getString("display", "");
        if (display != null && !display.isEmpty()) {
            return display;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return Text.capitalize(stack.getType().name().replace('_', ' '));
    }
}
