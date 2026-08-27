package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.ExplosionPreset;
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

/** Paginated explosion-preset menu — click to buy (coins), equip, or unequip. */
public class ExplosionParticlesGUI implements Listener {

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
    private boolean busy = false; // debounce purchases
    private final Map<Integer, ExplosionPreset> slotMap = new HashMap<>();

    public ExplosionParticlesGUI(LemonCosmetics plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, SIZE,
                MM.deserialize("<!italic><gradient:#ff9100:#dd2c00>Explosion Particles</gradient>"));
        render();
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        player.openInventory(inventory);
    }

    private List<ExplosionPreset> presets() {
        return plugin.getExplosionParticleManager().getAll();
    }

    private int maxPage() {
        int n = presets().size();
        return n == 0 ? 0 : (n - 1) / CONTENT.length;
    }

    private void render() {
        inventory.clear();
        slotMap.clear();
        ItemStack filler = filler();
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) inventory.setItem(i, filler);
        }

        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        List<ExplosionPreset> all = presets();
        int start = page * CONTENT.length;
        for (int i = 0; i < CONTENT.length; i++) {
            int idx = start + i;
            if (idx >= all.size()) break;
            inventory.setItem(CONTENT[i], buildPresetItem(all.get(idx), cosmetics));
            slotMap.put(CONTENT[i], all.get(idx));
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

        ExplosionPreset preset = slotMap.get(slot);
        if (preset != null && !busy) handlePresetClick(clicker, preset);
    }

    private void handlePresetClick(Player clicker, ExplosionPreset preset) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(clicker.getUniqueId());
        if (cosmetics == null) return;
        UUID uuid = clicker.getUniqueId();

        // Not usable yet — try to buy it with coins.
        if (!plugin.getExplosionParticleManager().canUse(clicker, preset)) {
            busy = true;
            plugin.getExplosionParticleManager().buy(uuid, preset.id())
                    .thenAccept(bought -> Bukkit.getScheduler().runTask(plugin, () -> {
                        busy = false;
                        Player p = Bukkit.getPlayer(uuid);
                        if (p == null) return;
                        if (bought) {
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.3f);
                            p.sendMessage(MM.deserialize("<green>Purchased: <yellow>" + preset.displayName() + "</yellow>!"));
                        } else {
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                            p.sendMessage(MM.deserialize("<red>You can't afford <yellow>" + preset.displayName()
                                    + "</yellow> <gray>(" + preset.price() + " coins)</gray>."));
                        }
                        render();
                    }));
            return;
        }

        // Owned — toggle equip.
        String activeId = cosmetics.getActiveExplosionId();
        boolean deactivate = preset.id().equals(activeId);
        plugin.getExplosionParticleManager().setActive(uuid, deactivate ? null : preset.id())
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) return;
                    if (deactivate) {
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.9f);
                        p.sendMessage(MM.deserialize("<yellow>Explosion preset deactivated."));
                    } else {
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
                        p.sendMessage(MM.deserialize("<green>Equipped: <yellow>" + preset.displayName() + "</yellow>!"));
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

    private ItemStack buildPresetItem(ExplosionPreset preset, PlayerCosmetics cosmetics) {
        boolean usable = plugin.getExplosionParticleManager().canUse(player, preset);
        boolean active = usable && cosmetics != null
                && preset.id().equals(cosmetics.getActiveExplosionId());

        Material icon = usable ? preset.icon() : Material.GRAY_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(usable
                ? MM.deserialize("<!italic><gold>" + preset.displayName())
                : MM.deserialize("<!italic><gray>" + preset.displayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Plays when your crystals or"));
        lore.add(MM.deserialize("<!italic><gray>TNT explode."));
        lore.add(Component.empty());
        if (usable) {
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
            lore.add(MM.deserialize("<!italic><gray>Price: <gold>" + preset.price() + " Coins"));
            lore.add(MM.deserialize("<!italic><yellow>► Click to buy"));
        }
        if (!preset.builtin()) {
            lore.add(MM.deserialize("<!italic><dark_gray>Custom preset"));
        }
        meta.lore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
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
