package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.KillEffectType;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Paginated kill-effects menu. */
public class KillEffectsGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int SIZE = 54;
    private static final int[] CONTENT = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int PREV_SLOT = 48;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 50;

    private final LemonCosmetics plugin;
    private final Player player;
    private Inventory inventory;
    private boolean registered = false;
    private int page = 0;
    private final Map<Integer, KillEffectType> slotMap = new HashMap<>();

    public KillEffectsGUI(LemonCosmetics plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, SIZE,
                MM.deserialize("<!italic><gradient:#ff7043:#bf360c>Kill Effects</gradient>"));
        render();
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        player.openInventory(inventory);
    }

    private int maxPage() {
        return (KillEffectType.values().length - 1) / CONTENT.length;
    }

    private void render() {
        inventory.clear();
        slotMap.clear();
        ItemStack filler = filler();
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) inventory.setItem(i, filler);
        }

        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        KillEffectType[] effects = KillEffectType.values();
        int start = page * CONTENT.length;
        for (int i = 0; i < CONTENT.length; i++) {
            int idx = start + i;
            if (idx >= effects.length) break;
            inventory.setItem(CONTENT[i], buildEffectItem(effects[idx], cosmetics));
            slotMap.put(CONTENT[i], effects[idx]);
        }

        if (page > 0) inventory.setItem(PREV_SLOT, named(Material.SPECTRAL_ARROW, "<yellow>← Page " + page));
        inventory.setItem(BACK_SLOT, named(Material.ARROW, "<gray>← Back to Cosmetics"));
        if (page < maxPage()) inventory.setItem(NEXT_SLOT, named(Material.SPECTRAL_ARROW, "<yellow>Page " + (page + 2) + " →"));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!clicker.getUniqueId().equals(player.getUniqueId())) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot == BACK_SLOT) {
            clicker.playSound(clicker.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 0.9f);
            unregister();
            new CosmeticsMainGUI(plugin, player).open();
            return;
        }
        if (slot == PREV_SLOT && page > 0) { page--; render(); return; }
        if (slot == NEXT_SLOT && page < maxPage()) { page++; render(); return; }

        KillEffectType effect = slotMap.get(slot);
        if (effect != null) handleEffectClick(clicker, effect);
    }

    private void handleEffectClick(Player clicker, KillEffectType effect) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(clicker.getUniqueId());
        boolean owned = cosmetics != null && cosmetics.ownsEffect(effect.getId());

        if (!owned) {
            clicker.playSound(clicker.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            clicker.sendMessage(MM.deserialize("<red>You don't own <yellow>" + effect.getDisplayName()
                    + "</yellow>. Unlock it with a code!"));
            return;
        }

        UUID clickerUuid = clicker.getUniqueId();
        String activeId = cosmetics.getActiveEffectId();
        boolean deactivate = effect.getId().equals(activeId);
        plugin.getCosmeticsManager().setActiveKillEffect(clickerUuid, deactivate ? null : effect.getId())
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(clickerUuid);
                    if (p == null) return;
                    if (deactivate) {
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.9f);
                        p.sendMessage(MM.deserialize("<yellow>Kill effect deactivated."));
                    } else {
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
                        p.sendMessage(MM.deserialize("<green>Equipped: <yellow>" + effect.getDisplayName() + "</yellow>!"));
                    }
                    render();
                }));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        if (inventory == null || !event.getInventory().equals(inventory)) return;
        unregister();
    }

    private void unregister() {
        if (registered) { HandlerList.unregisterAll(this); registered = false; }
    }

    private ItemStack buildEffectItem(KillEffectType effect, PlayerCosmetics cosmetics) {
        boolean owned = cosmetics != null && cosmetics.ownsEffect(effect.getId());
        boolean active = owned && cosmetics.getActiveEffectId() != null
                && cosmetics.getActiveEffectId().equals(effect.getId());

        Material icon = owned ? effect.getIcon() : Material.GRAY_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(owned
                ? MM.deserialize("<!italic><gold>" + effect.getDisplayName())
                : MM.deserialize("<!italic><gray>" + effect.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (owned) {
            if (active) {
                lore.add(MM.deserialize("<!italic><aqua>✔ Active"));
                lore.add(MM.deserialize("<!italic><gray>Click to deactivate"));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(MM.deserialize("<!italic><green>✔ Owned"));
                lore.add(MM.deserialize("<!italic><gray>Click to equip"));
            }
        } else {
            lore.add(MM.deserialize("<!italic><dark_gray>Not owned"));
            lore.add(MM.deserialize("<!italic><gray>Unlock with a code"));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack named(Material mat, String mini) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(MM.deserialize("<!italic>" + mini)); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack filler() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); pane.setItemMeta(meta); }
        return pane;
    }
}
