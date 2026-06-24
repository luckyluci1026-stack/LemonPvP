package com.lemonpvp.lemonlobby.gui;

import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.database.Database;
import com.lemonpvp.lemonlobby.model.BoosterTier;
import com.lemonpvp.lemonlobby.model.PlankTier;
import com.lemonpvp.lemonlobby.util.EconomyBridge;
import com.lemonpvp.lemonlobby.util.FormatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ShopGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int PROGRESS_BARS = 10;

    // ── Slot layout (54 slots / 6 rows) ──────────────────────────────────────
    private static final int   BALANCE_SLOT      = 4;
    private static final int[] PLANK_SLOTS       = {10, 11, 12, 13, 14, 15};
    private static final int   CURRENT_TIER_SLOT = 16;
    private static final int[] BOOSTER_SLOTS     = {28, 29, 30, 31, 32};
    private static final int   ACTIVE_BOOSTER_SLOT = 34;
    private static final int   CLOSE_SLOT        = 49;

    private final LemonLobby plugin;
    private final Player player;
    private Inventory inv;
    private boolean registered;

    // Prevents double-clicks from triggering two purchases simultaneously
    private boolean purchasing;

    public ShopGUI(LemonLobby plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inv = Bukkit.createInventory(null, 54,
                MM.deserialize("<!italic><gradient:#fffb00:#00ff00><bold>Aᴘꜰᴇʟ-Sʜᴏᴘ</bold></gradient>"));

        ItemStack black = pane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack gray  = pane(Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < 54; i++) inv.setItem(i, black);
        for (int s : PLANK_SLOTS)   inv.setItem(s, gray);
        for (int s : BOOSTER_SLOTS) inv.setItem(s, gray);

        // Section labels
        inv.setItem(9,  sectionLabel("<#D2691E><bold>Baum-Upgrades"));
        inv.setItem(27, sectionLabel("<gold><bold>Verstärker"));

        inv.setItem(BALANCE_SLOT, buildBalanceItem());
        fillPlankTiers();
        fillBoosters();
        inv.setItem(CURRENT_TIER_SLOT,    buildCurrentTierItem());
        inv.setItem(ACTIVE_BOOSTER_SLOT,  buildActiveBoosterItem());
        inv.setItem(CLOSE_SLOT,           buildCloseItem());

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);

        if (!registered) {
            registered = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    // ── Plank Tiers ──────────────────────────────────────────────────────────

    private void fillPlankTiers() {
        PlankTier[] tiers   = PlankTier.values();
        PlankTier current   = plugin.getTreeUpgradeManager().getCurrentTier(player.getUniqueId());
        long playerApples   = getApples();

        for (int i = 0; i < PLANK_SLOTS.length && i < tiers.length; i++) {
            inv.setItem(PLANK_SLOTS[i], buildTierItem(tiers[i], current, playerApples));
        }
    }

    private ItemStack buildTierItem(PlankTier tier, PlankTier current, long playerApples) {
        boolean isActive = tier == current;
        boolean isOwned  = tier.isFree() || isActive;

        ItemStack item = new ItemStack(tier.material);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        String nameColor = isActive ? "<gradient:#fffb00:#00ff00>" : isOwned ? "<green>" : "<gray>";
        meta.displayName(MM.deserialize("<!italic><bold>" + nameColor + tier.displayName));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Planks / Klick: <white>" + tier.planksPerClick));
        lore.add(MM.deserialize("<!italic><gray>Dauer: " + tier.durationDisplay()));
        lore.add(Component.empty());

        if (isActive) {
            lore.add(MM.deserialize("<!italic><gradient:#fffb00:#00ff00>✔ Aktiv"));
            long expiresAt = plugin.getTreeUpgradeManager().getExpiresAt(player.getUniqueId());
            if (expiresAt > 0 && expiresAt != Long.MAX_VALUE) {
                lore.add(MM.deserialize("<!italic><gray>Läuft ab: <white>" + formatExpiry(expiresAt)));
            }
        } else if (tier.isFree()) {
            lore.add(MM.deserialize("<!italic><green>✔ Standard"));
        } else {
            // Progress bar toward this tier
            String bar = progressBar(playerApples, tier.appleCost, PROGRESS_BARS);
            lore.add(MM.deserialize("<!italic>" + bar));
            lore.add(MM.deserialize("<!italic><gray>"
                    + FormatUtil.formatAmount(Math.min(playerApples, tier.appleCost))
                    + " <dark_gray>/ <white>" + FormatUtil.formatAmount(tier.appleCost) + " <green>✿"));
            lore.add(Component.empty());
            if (playerApples >= tier.appleCost) {
                lore.add(MM.deserialize("<!italic><yellow>► Klicken zum Kaufen"));
            } else {
                long needed = tier.appleCost - playerApples;
                lore.add(MM.deserialize("<!italic><dark_gray>Noch <white>"
                        + FormatUtil.formatAmount(needed) + " <green>✿ <dark_gray>benötigt"));
            }
        }
        lore.add(Component.empty());
        meta.lore(lore);

        if (isActive) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCurrentTierItem() {
        PlankTier tier    = plugin.getTreeUpgradeManager().getCurrentTier(player.getUniqueId());
        ItemStack item    = new ItemStack(tier.material);  // show actual log, not sapling
        ItemMeta  meta    = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic><bold><gradient:#fffb00:#00ff00>Dein Baum-Upgrade"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Aktiv: <white>" + tier.displayName));
        lore.add(MM.deserialize("<!italic><gray>Planks / Klick: <white>" + tier.planksPerClick));
        long expiresAt = plugin.getTreeUpgradeManager().getExpiresAt(player.getUniqueId());
        if (expiresAt > 0 && expiresAt != Long.MAX_VALUE) {
            lore.add(MM.deserialize("<!italic><gray>Läuft ab: <white>" + formatExpiry(expiresAt)));
        } else {
            lore.add(MM.deserialize("<!italic><green>Dauerhaft"));
        }
        lore.add(Component.empty());
        meta.lore(lore);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    // ── Boosters ─────────────────────────────────────────────────────────────

    private void fillBoosters() {
        BoosterTier[]        tiers  = BoosterTier.values();
        Database.BoosterEntry active = plugin.getBoosterManager().getActive(player.getUniqueId());
        long playerApples            = getApples();

        for (int i = 0; i < BOOSTER_SLOTS.length && i < tiers.length; i++) {
            inv.setItem(BOOSTER_SLOTS[i], buildBoosterItem(tiers[i], active, playerApples));
        }
    }

    private ItemStack buildBoosterItem(BoosterTier tier, Database.BoosterEntry active, long playerApples) {
        boolean isActive = active != null && active.tier() == tier;

        Material mat = switch (tier.level) {
            case 1  -> Material.SUGAR;
            case 2  -> Material.BLAZE_POWDER;
            case 3  -> Material.GLOWSTONE_DUST;
            case 4  -> Material.NETHER_STAR;
            case 5  -> Material.DRAGON_BREATH;
            default -> Material.PAPER;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        String nameColor = isActive ? "<gradient:#fffb00:#ff9800>" : "<gold>";
        meta.displayName(MM.deserialize("<!italic><bold>" + nameColor + "Verstärker " + tier.displayName));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Bonus Äpfel: <white>+" + tier.bonusApples + " / Klick"));
        lore.add(MM.deserialize("<!italic><gray>Max. Nutzungen: <white>" + tier.maxUses));
        lore.add(MM.deserialize("<!italic><gray>Dauer: <white>" + tier.durationDisplay()));
        lore.add(Component.empty());

        if (isActive) {
            lore.add(MM.deserialize("<!italic><gradient:#fffb00:#00ff00>✔ Aktiv"));
            lore.add(MM.deserialize("<!italic><gray>Verbleibend: <white>" + active.remainingUses() + " Nutzungen"));
            long rem = active.expiresAt() - System.currentTimeMillis();
            lore.add(MM.deserialize("<!italic><gray>Zeit: <white>" + formatDuration(rem)));
        } else {
            // Progress bar
            String bar = progressBar(playerApples, tier.appleCost, PROGRESS_BARS);
            lore.add(MM.deserialize("<!italic>" + bar));
            lore.add(MM.deserialize("<!italic><gray>"
                    + FormatUtil.formatAmount(Math.min(playerApples, tier.appleCost))
                    + " <dark_gray>/ <white>" + FormatUtil.formatAmount(tier.appleCost) + " <green>✿"));
            lore.add(Component.empty());
            if (active != null) {
                lore.add(MM.deserialize("<!italic><red>Erst aktiven Verstärker aufbrauchen!"));
            } else if (playerApples >= tier.appleCost) {
                lore.add(MM.deserialize("<!italic><yellow>► Klicken zum Kaufen"));
            } else {
                long needed = tier.appleCost - playerApples;
                lore.add(MM.deserialize("<!italic><dark_gray>Noch <white>"
                        + FormatUtil.formatAmount(needed) + " <green>✿ <dark_gray>benötigt"));
            }
        }
        lore.add(Component.empty());
        meta.lore(lore);

        if (isActive) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildActiveBoosterItem() {
        Database.BoosterEntry active = plugin.getBoosterManager().getActive(player.getUniqueId());
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic><bold><gold>Aktiver Verstärker"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (active != null) {
            lore.add(MM.deserialize("<!italic><gray>Typ: <white>" + active.tier().displayName));
            lore.add(MM.deserialize("<!italic><gray>Bonus: <white>+" + active.tier().bonusApples + " Äpfel / Klick"));
            lore.add(MM.deserialize("<!italic><gray>Nutzungen: <white>" + active.remainingUses()));
            long rem = active.expiresAt() - System.currentTimeMillis();
            lore.add(MM.deserialize("<!italic><gray>Zeit: <white>" + formatDuration(rem)));
        } else {
            lore.add(MM.deserialize("<!italic><dark_gray>Kein aktiver Verstärker."));
        }
        lore.add(Component.empty());
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Balance item ─────────────────────────────────────────────────────────

    private ItemStack buildBalanceItem() {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (skull.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(player);
            meta.displayName(MM.deserialize("<!italic><bold><gradient:#fffb00:#00ff00>" + player.getName()));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            PlayerData pd = EconomyBridge.cached(player.getUniqueId());
            long apples = pd != null ? pd.getApples() : 0;
            long planks = pd != null ? pd.getPlanks() : 0;
            lore.add(MM.deserialize("<!italic><gray>✿ Äpfel: <green>" + FormatUtil.formatAmount(apples)));
            lore.add(MM.deserialize("<!italic><gray>▬ Planks: <#D2691E>" + FormatUtil.formatAmount(planks)));
            lore.add(Component.empty());
            meta.lore(lore);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    // ── Click handling ────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!p.getUniqueId().equals(player.getUniqueId())) return;
        if (!e.getInventory().equals(inv)) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == CLOSE_SLOT) { player.closeInventory(); return; }
        if (purchasing) return; // debounce

        PlankTier[] planks   = PlankTier.values();
        for (int i = 0; i < PLANK_SLOTS.length && i < planks.length; i++) {
            if (slot == PLANK_SLOTS[i]) { handlePlankPurchase(planks[i]); return; }
        }

        BoosterTier[] boosters = BoosterTier.values();
        for (int i = 0; i < BOOSTER_SLOTS.length && i < boosters.length; i++) {
            if (slot == BOOSTER_SLOTS[i]) { handleBoosterPurchase(boosters[i]); return; }
        }
    }

    private void handlePlankPurchase(PlankTier tier) {
        if (tier.isFree()) return;
        PlankTier current = plugin.getTreeUpgradeManager().getCurrentTier(player.getUniqueId());
        if (current == tier) {
            player.sendActionBar(MM.deserialize("<!italic><red>Du hast dieses Upgrade bereits!"));
            return;
        }
        long apples = getApples();
        if (apples < tier.appleCost) {
            player.sendActionBar(MM.deserialize("<!italic><red>Nicht genug ✿! Du brauchst noch "
                    + FormatUtil.formatAmount(tier.appleCost - apples) + " Äpfel."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            return;
        }

        purchasing = true;
        EconomyBridge.removeApples(player.getUniqueId(), tier.appleCost)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getTreeUpgradeManager().upgrade(player.getUniqueId(), tier);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
                    player.sendActionBar(MM.deserialize("<!italic><gradient:#fffb00:#00ff00>Baum-Upgrade auf <white>"
                            + tier.displayName + " <gradient:#fffb00:#00ff00>aktiviert!"));
                    purchasing = false;
                    refresh();
                }));
    }

    private void handleBoosterPurchase(BoosterTier tier) {
        if (plugin.getBoosterManager().getActive(player.getUniqueId()) != null) {
            player.sendActionBar(MM.deserialize("<!italic><red>Du hast bereits einen aktiven Verstärker!"));
            return;
        }
        long apples = getApples();
        if (apples < tier.appleCost) {
            player.sendActionBar(MM.deserialize("<!italic><red>Nicht genug ✿! Du brauchst noch "
                    + FormatUtil.formatAmount(tier.appleCost - apples) + " Äpfel."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            return;
        }

        purchasing = true;
        EconomyBridge.removeApples(player.getUniqueId(), tier.appleCost)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getBoosterManager().activate(player.getUniqueId(), tier);
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.0f);
                    player.sendActionBar(MM.deserialize("<!italic><gradient:#fffb00:#ff9800>Verstärker "
                            + tier.displayName + " <gradient:#fffb00:#ff9800>aktiviert!"));
                    purchasing = false;
                    refresh();
                }));
    }

    private void refresh() {
        if (inv == null) return;
        inv.setItem(BALANCE_SLOT,        buildBalanceItem());
        inv.setItem(CURRENT_TIER_SLOT,   buildCurrentTierItem());
        inv.setItem(ACTIVE_BOOSTER_SLOT, buildActiveBoosterItem());
        fillPlankTiers();
        fillBoosters();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!p.getUniqueId().equals(player.getUniqueId())) return;
        if (!e.getInventory().equals(inv)) return;
        if (registered) { registered = false; HandlerList.unregisterAll(this); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private long getApples() {
        return EconomyBridge.apples(player.getUniqueId());
    }

    private String progressBar(long current, long max, int bars) {
        float ratio  = max <= 0 ? 1f : Math.min(1f, (float) current / max);
        int filled   = (int) (ratio * bars);
        int empty    = bars - filled;
        String color = ratio >= 1f ? "<green>" : ratio >= 0.6f ? "<yellow>" : "<red>";
        return color + "█".repeat(filled) + "<dark_gray>" + "░".repeat(empty);
    }

    private ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack sectionLabel(String miniMsg) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) { meta.displayName(MM.deserialize("<!italic>" + miniMsg)); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack buildCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) { meta.displayName(MM.deserialize("<!italic><red>Schließen")); item.setItemMeta(meta); }
        return item;
    }

    private String formatExpiry(long expiresAt) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(expiresAt));
    }

    private String formatDuration(long millis) {
        if (millis <= 0) return "0s";
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
    }
}
