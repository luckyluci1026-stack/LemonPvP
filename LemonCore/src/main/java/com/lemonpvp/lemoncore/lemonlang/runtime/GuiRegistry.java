package com.lemonpvp.lemoncore.lemonlang.runtime;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.lemonlang.ast.GuiDef;
import com.lemonpvp.lemoncore.lemonlang.ast.SlotDef;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.ArrayList;
import org.bukkit.entity.Player;

/**
 * Manages LemonLang GUI definitions. Stores {@link GuiDef} objects and opens
 * them as Bukkit {@link Inventory} instances for players.
 *
 * <p>Open inventories are tracked so that click events can be dispatched to
 * the correct slot trigger.
 */
public final class GuiRegistry {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Logger LOG = Logger.getLogger("LemonLang");

    /** name → GuiDef */
    private final Map<String, GuiDef> definitions = new ConcurrentHashMap<>();

    /** Inventory → GuiDef — tracks currently open LemonLang GUIs */
    private final Map<Inventory, GuiDef> openInventories = new ConcurrentHashMap<>();

    /** Registers a GUI definition. */
    public void register(GuiDef def) {
        definitions.put(def.name(), def);
    }

    /**
     * Opens the GUI with the given name for the player. Creates a new Bukkit
     * Inventory from the definition and fills all slots.
     */
    public void open(String name, Player player, LemonCore plugin) {
        GuiDef def = definitions.get(name);
        if (def == null) {
            LOG.warning("[LemonLang] Unbekanntes GUI '" + name + "' — nicht geoeffnet.");
            return;
        }

        Map<String, String> props = def.props();

        // Determine row count
        int rows = 3;
        String rowsStr = props.get("rows");
        if (rowsStr != null) {
            try { rows = Math.max(1, Math.min(6, Integer.parseInt(rowsStr.trim()))); }
            catch (NumberFormatException ignored) {}
        }
        int size = rows * 9;

        // Title
        String titleStr = props.getOrDefault("title", "<gold>" + def.name());
        net.kyori.adventure.text.Component title = MM.deserialize("<!italic>" + titleStr);

        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill slots
        for (SlotDef slot : def.slots()) {
            if (slot.slot() < 0 || slot.slot() >= size) continue;
            ItemStack stack = buildSlotItem(slot);
            inv.setItem(slot.slot(), stack);
        }

        openInventories.put(inv, def);
        player.openInventory(inv);
    }

    /**
     * Returns the {@link GuiDef} that owns the given inventory, or null if the
     * inventory is not a LemonLang GUI.
     */
    public GuiDef getDefForInventory(Inventory inv) {
        return openInventories.get(inv);
    }

    /**
     * Returns the {@link SlotDef} for the clicked slot inside a GuiDef,
     * or null if no definition exists for that slot.
     */
    public SlotDef getSlotDef(GuiDef guiDef, int slot) {
        if (guiDef == null) return null;
        for (SlotDef s : guiDef.slots()) {
            if (s.slot() == slot) return s;
        }
        return null;
    }

    /** Removes a tracked open inventory (called when player closes it). */
    public void removeOpen(Inventory inv) {
        openInventories.remove(inv);
    }

    /** Removes all definitions and closes all tracked open inventories. */
    public void clear() {
        definitions.clear();
        openInventories.clear();
    }

    /** Returns the number of registered GUIs. */
    public int size() {
        return definitions.size();
    }

    // ---- Helpers ----------------------------------------------------------------

    private ItemStack buildSlotItem(SlotDef slot) {
        Map<String, String> props = slot.props();

        String materialStr = props.getOrDefault("material", "GRAY_STAINED_GLASS_PANE").toUpperCase();
        Material material;
        try {
            material = Material.valueOf(materialStr);
        } catch (IllegalArgumentException e) {
            material = Material.GRAY_STAINED_GLASS_PANE;
        }

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        String nameStr = props.get("name");
        if (nameStr != null && !nameStr.isEmpty()) {
            meta.displayName(MM.deserialize("<!italic>" + nameStr));
        } else {
            // Hide default name
            meta.displayName(net.kyori.adventure.text.Component.empty());
        }

        if (!slot.lore().isEmpty()) {
            List<net.kyori.adventure.text.Component> loreList = new ArrayList<>();
            for (String loreLine : slot.lore()) {
                loreList.add(MM.deserialize("<!italic>" + loreLine));
            }
            meta.lore(loreList);
        }

        stack.setItemMeta(meta);
        return stack;
    }
}
