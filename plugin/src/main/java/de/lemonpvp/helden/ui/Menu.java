package de.lemonpvp.helden.ui;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.util.Keys;
import de.lemonpvp.helden.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Basis fuer alle Kisten-GUIs.
 *
 * <p>Bewusst ein Chest-Inventar statt Floodgate-Forms: Geyser uebersetzt
 * Kisten-GUIs automatisch fuer Bedrock, damit funktioniert dieselbe Oberflaeche
 * auf beiden Editionen ohne zusaetzliche Dependency.</p>
 */
public abstract class Menu implements InventoryHolder {

    protected final HeldenPlugin plugin;
    private Inventory inventory;

    protected Menu(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    public abstract String title();

    public abstract int size();

    protected abstract void build(Player viewer);

    /** Reaktion auf einen Klick im oberen Inventar. */
    public abstract void onClick(Player viewer, int slot, ItemStack clicked, String action);

    public void open(Player viewer) {
        inventory = Bukkit.createInventory(this, size(), Text.color(title()));
        build(viewer);
        viewer.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, size(), Text.color(title()));
        }
        return inventory;
    }

    protected void set(int slot, ItemStack stack) {
        if (slot >= 0 && slot < getInventory().getSize()) {
            getInventory().setItem(slot, stack);
        }
    }

    /** Icon mit Namen, Lore und hinterlegter Aktion. */
    protected ItemStack icon(Material material, String display, List<String> lore, String action) {
        ItemStack stack = new ItemStack(material == null ? Material.PAPER : material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.setDisplayName(Text.color(display));
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(Text.color(lore));
        }
        if (action != null && !action.isEmpty()) {
            meta.getPersistentDataContainer().set(Keys.menuAction(), PersistentDataType.STRING, action);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    /** Liest die hinterlegte Aktion aus einem angeklickten Icon. */
    public static String actionOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta == null ? null : meta.getPersistentDataContainer()
                .get(Keys.menuAction(), PersistentDataType.STRING);
    }
}
