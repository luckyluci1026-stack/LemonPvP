package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.ArrowTrailType;
import com.lemonpvp.lemoncosmetics.model.HatType;
import com.lemonpvp.lemoncosmetics.model.KillEffectType;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class CosmeticsMainGUI implements Listener {

    static final MiniMessage MM = MiniMessage.miniMessage();

    // Category slots
    private static final int TRIMS_SLOT   = 10;
    private static final int TRAILS_SLOT  = 12;
    private static final int EFFECTS_SLOT = 14;
    private static final int HATS_SLOT    = 16;
    private static final int PROFILE_SLOT = 4;

    private final LemonCosmetics plugin;
    private final Player player;
    private Inventory inventory;
    private boolean registered = false;

    // Reusable filler items
    private static final ItemStack BLACK_FILLER = buildFiller(Material.BLACK_STAINED_GLASS_PANE);
    private static final ItemStack GRAY_FILLER  = buildFiller(Material.GRAY_STAINED_GLASS_PANE);

    public CosmeticsMainGUI(LemonCosmetics plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27,
                MM.deserialize("<!italic><gradient:#fffb00:#00ff00>ᴄᴏsᴍᴇᴛɪᴄs</gradient>"));

        fillBorders();

        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());

        inventory.setItem(PROFILE_SLOT,  buildProfileItem(cosmetics));
        inventory.setItem(TRIMS_SLOT,    buildTrimsItem(cosmetics));
        inventory.setItem(TRAILS_SLOT,   buildTrailsItem(cosmetics));
        inventory.setItem(EFFECTS_SLOT,  buildEffectsItem(cosmetics));
        inventory.setItem(HATS_SLOT,     buildHatsItem(cosmetics));

        if (!registered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        player.openInventory(inventory);
    }

    // -------------------------------------------------------------------------
    // Layout
    // -------------------------------------------------------------------------

    private void fillBorders() {
        // Row 0 and Row 2: full black border
        for (int i = 0; i < 9; i++) inventory.setItem(i, BLACK_FILLER);
        for (int i = 18; i < 27; i++) inventory.setItem(i, BLACK_FILLER);
        // Row 1: black edges, gray spacers between categories
        inventory.setItem(9,  BLACK_FILLER);
        inventory.setItem(11, GRAY_FILLER);
        inventory.setItem(13, GRAY_FILLER);
        inventory.setItem(15, GRAY_FILLER);
        inventory.setItem(17, BLACK_FILLER);
    }

    // -------------------------------------------------------------------------
    // Item builders
    // -------------------------------------------------------------------------

    private ItemStack buildProfileItem(PlayerCosmetics cosmetics) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skull = (SkullMeta) head.getItemMeta();
        if (skull == null) return head;

        skull.setOwningPlayer(player);
        skull.displayName(MM.deserialize("<!italic><gradient:#fffb00:#00ff00>" + player.getName() + "</gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // Coins — read from LemonCore cache (synchronous)
        long coins = getCachedCoins();
        lore.add(MM.deserialize("<!italic><gray>Münzen: <gold>" + formatNumber(coins) + " ⭐"));

        lore.add(Component.empty());

        if (cosmetics != null) {
            lore.add(MM.deserialize("<!italic><gray>Hüte: <white>" + cosmetics.getOwnedHats().size()
                    + "<gray>/<white>" + HatType.values().length));
            lore.add(MM.deserialize("<!italic><gray>Trails: <white>" + cosmetics.getOwnedTrails().size()
                    + "<gray>/<white>" + ArrowTrailType.values().length));
            lore.add(MM.deserialize("<!italic><gray>Effekte: <white>" + cosmetics.getOwnedEffects().size()
                    + "<gray>/<white>" + KillEffectType.values().length));
            int totalPatterns = plugin.getArmorTrimManager().getAllPatternIds().size();
            lore.add(MM.deserialize("<!italic><gray>Trim-Muster: <white>" + cosmetics.getOwnedPatterns().size()
                    + "<gray>/<white>" + totalPatterns));
        } else {
            lore.add(MM.deserialize("<!italic><gray>Lade Daten..."));
        }

        skull.lore(lore);
        head.setItemMeta(skull);
        return head;
    }

    private ItemStack buildTrimsItem(PlayerCosmetics cosmetics) {
        ItemStack item = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic><gradient:#4fc3f7:#0288d1>Rüstungs-Trims</gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Passe deine Rüstung mit"));
        lore.add(MM.deserialize("<!italic><gray>einzigartigen Trims an."));
        lore.add(Component.empty());

        if (cosmetics != null) {
            int totalPatterns = plugin.getArmorTrimManager().getAllPatternIds().size();
            lore.add(MM.deserialize("<!italic><dark_gray>Muster: <white>" + cosmetics.getOwnedPatterns().size()
                    + "<dark_gray>/<white>" + totalPatterns));
            lore.add(MM.deserialize("<!italic><dark_gray>Materialien: <white>" + cosmetics.getOwnedMaterials().size()));
        }

        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><aqua>➜ Klicken zum Öffnen"));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildTrailsItem(PlayerCosmetics cosmetics) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic><gradient:#ab47bc:#7b1fa2>Pfeil-Trails</gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Hinterlasse einen farbigen Effekt"));
        lore.add(MM.deserialize("<!italic><gray>bei deinen Pfeilen."));
        lore.add(Component.empty());

        if (cosmetics != null) {
            int owned = cosmetics.getOwnedTrails().size();
            int total = ArrowTrailType.values().length;
            lore.add(MM.deserialize("<!italic><dark_gray>Besessen: <white>" + owned + "<dark_gray>/<white>" + total));
            String activeTrail = cosmetics.getActiveTrailId();
            if (activeTrail != null) {
                lore.add(MM.deserialize("<!italic><dark_gray>Aktiv: <light_purple>" + activeTrail));
            } else {
                lore.add(MM.deserialize("<!italic><dark_gray>Aktiv: <gray>Keiner"));
            }
        }

        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><aqua>➜ Klicken zum Öffnen"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildEffectsItem(PlayerCosmetics cosmetics) {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic><gradient:#ff7043:#bf360c>Kill-Effekte</gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Löse einen Effekt aus, wenn"));
        lore.add(MM.deserialize("<!italic><gray>du einen Spieler eliminierst."));
        lore.add(Component.empty());

        if (cosmetics != null) {
            int owned = cosmetics.getOwnedEffects().size();
            int total = KillEffectType.values().length;
            lore.add(MM.deserialize("<!italic><dark_gray>Besessen: <white>" + owned + "<dark_gray>/<white>" + total));
            String activeEffect = cosmetics.getActiveEffectId();
            if (activeEffect != null) {
                lore.add(MM.deserialize("<!italic><dark_gray>Aktiv: <red>" + activeEffect));
            } else {
                lore.add(MM.deserialize("<!italic><dark_gray>Aktiv: <gray>Keiner"));
            }
        }

        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><aqua>➜ Klicken zum Öffnen"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildHatsItem(PlayerCosmetics cosmetics) {
        ItemStack item = new ItemStack(Material.LEATHER_HELMET);
        if (item.getItemMeta() instanceof LeatherArmorMeta hatsMeta) {
            hatsMeta.setColor(Color.fromRGB(255, 251, 0));
            hatsMeta.displayName(MM.deserialize("<!italic><gradient:#fffb00:#ff9800>Hüte</gradient>"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><gray>Trage einen stylischen Hut"));
            lore.add(MM.deserialize("<!italic><gray>auf deinem Kopf."));
            lore.add(Component.empty());

            if (cosmetics != null) {
                int owned = cosmetics.getOwnedHats().size();
                int total = HatType.values().length;
                lore.add(MM.deserialize("<!italic><dark_gray>Besessen: <white>" + owned + "<dark_gray>/<white>" + total));
                String equippedHat = cosmetics.getEquippedHatId();
                if (equippedHat != null) {
                    lore.add(MM.deserialize("<!italic><dark_gray>Ausgerüstet: <yellow>" + equippedHat));
                } else {
                    lore.add(MM.deserialize("<!italic><dark_gray>Ausgerüstet: <gray>Keiner"));
                }
            }

            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><aqua>➜ Klicken zum Öffnen"));

            hatsMeta.lore(lore);
            hatsMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE);
            item.setItemMeta(hatsMeta);
        }
        return item;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private long getCachedCoins() {
        com.lemonpvp.lemoncosmetics.managers.CosmeticsManager cm = plugin.getCosmeticsManager();
        com.lemonpvp.lemoncore.LemonCore lc = cm.getLemonCore();
        if (lc == null) return 0;
        com.lemonpvp.lemoncore.managers.PlayerData pd =
                lc.getPlayerDataManager().getCached(player.getUniqueId());
        return pd != null ? pd.getCoins() : 0;
    }

    private String formatNumber(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fk", n / 1_000.0);
        return String.valueOf(n);
    }

    private static ItemStack buildFiller(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    // -------------------------------------------------------------------------
    // Event handling
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getWhoClicked().getUniqueId().equals(player.getUniqueId())) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) return;

        switch (event.getSlot()) {
            case TRIMS_SLOT -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                unregister();
                new TrimPatternGUI(plugin, player).open();
            }
            case TRAILS_SLOT -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                unregister();
                new ArrowTrailsGUI(plugin, player).open();
            }
            case EFFECTS_SLOT -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                unregister();
                new KillEffectsGUI(plugin, player).open();
            }
            case HATS_SLOT -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                unregister();
                new HatsGUI(plugin, player).open();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        unregister();
    }

    private void unregister() {
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }
}
