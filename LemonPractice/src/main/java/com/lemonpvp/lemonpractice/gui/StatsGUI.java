package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.managers.EloManager;
import com.lemonpvp.lemonpractice.model.Gamemode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StatsGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;
    private final Player viewer;
    private final UUID targetUuid;
    private final String targetName;
    private Inventory inv;
    private boolean registered;

    // 45-slot layout (5 rows)
    private static final int[] TOP_BORDER    = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] ROW1_SIDES    = {9, 17};
    private static final int[] ROW1_FILL     = {10, 11, 12, 14, 15, 16};
    private static final int   HEAD_SLOT     = 13;
    private static final int[] ROW2_SIDES    = {18, 26};
    private static final int[] STAT_SLOTS    = {19, 20, 21, 22, 23, 24, 25};
    private static final int[] ROW3_SIDES    = {27, 35};
    private static final int[] GM_SLOTS      = {28, 29, 30, 31, 32, 33, 34};
    private static final int[] BOTTOM_BORDER = {36, 37, 38, 39, 40, 41, 42, 43, 44};

    public StatsGUI(LemonPractice plugin, Player viewer, UUID targetUuid, String targetName) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    public void open() {
        inv = Bukkit.createInventory(null, 45,
                MM.deserialize("<!italic><gradient:#fffb00:#00ff00><bold>Sᴛᴀᴛɪsᴛɪᴄs</bold></gradient>"
                        + " <dark_gray>» <white>" + targetName));

        // Lemon-themed frame: yellow corners, lime mid-edge accents, black rim.
        ItemStack black  = pane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack gray   = pane(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack yellow = pane(Material.YELLOW_STAINED_GLASS_PANE);
        ItemStack lime   = pane(Material.LIME_STAINED_GLASS_PANE);

        for (int s : TOP_BORDER)    inv.setItem(s, black);
        for (int s : ROW1_SIDES)    inv.setItem(s, black);
        for (int s : ROW1_FILL)     inv.setItem(s, gray);
        for (int s : ROW2_SIDES)    inv.setItem(s, black);
        for (int s : STAT_SLOTS)    inv.setItem(s, gray);
        for (int s : ROW3_SIDES)    inv.setItem(s, black);
        for (int s : GM_SLOTS)      inv.setItem(s, gray);
        for (int s : BOTTOM_BORDER) inv.setItem(s, black);
        inv.setItem(0, yellow); inv.setItem(8, yellow);
        inv.setItem(36, yellow); inv.setItem(44, yellow);
        inv.setItem(4, lime); inv.setItem(40, lime);
        inv.setItem(12, lime); inv.setItem(14, lime);

        inv.setItem(HEAD_SLOT, buildHead());

        viewer.openInventory(inv);
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);

        if (!registered) {
            registered = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }

        fillStats();
    }

    private void fillStats() {
        int kills = 0, deaths = 0, bestKs = 0;
        long coins = 0;
        double kdr = 0.0;

        org.bukkit.plugin.Plugin lcPlugin = Bukkit.getPluginManager().getPlugin("LemonCore");
        if (lcPlugin instanceof com.lemonpvp.lemoncore.LemonCore lc) {
            com.lemonpvp.lemoncore.managers.PlayerData pd =
                    lc.getPlayerDataManager().getCached(targetUuid);
            if (pd != null) {
                kills  = pd.getKills();
                deaths = pd.getDeaths();
                kdr    = pd.getKDRatio();
                bestKs = pd.getBestKillstreak();
                coins  = pd.getCoins();
            }
        }

        final int fKills = kills, fDeaths = deaths, fBestKs = bestKs;
        final long fCoins = coins;
        final double fKdr = kdr;

        int curStreak = plugin.getStreakManager().getCurrent(targetUuid);
        int bestStreak = plugin.getStreakManager().getBest(targetUuid);

        // Kills
        inv.setItem(STAT_SLOTS[0], statItem(Material.DIAMOND_SWORD,
                "<gradient:#fffb00:#00ff00><!italic>⚔ Kɪʟʟs",
                List.of("<gray>Total: <white>" + fKills,
                        "<gray>Duel streak: <gold>" + curStreak + " <dark_gray>(best " + bestStreak + ")")));

        // Deaths
        inv.setItem(STAT_SLOTS[1], statItem(Material.BONE,
                "<gradient:#ff6b6b:#cc0000><!italic>☠ Dᴇᴀᴛʜs",
                List.of("<gray>Total: <white>" + fDeaths)));

        // K/D ratio
        String kdrHex = fKdr >= 2.0 ? "#00e676" : fKdr >= 1.0 ? "#fffb00" : "#ff5252";
        inv.setItem(STAT_SLOTS[2], statItem(Material.GOLDEN_SWORD,
                "<" + kdrHex + "><!italic>⚖ K/D-Rᴀᴛɪᴏ",
                List.of("<gray>Ratio: <" + kdrHex + ">" + String.format("%.2f", fKdr),
                        bar(fKdr, 2.0, kdrHex))));

        // Best killstreak
        inv.setItem(STAT_SLOTS[3], statItem(Material.BLAZE_POWDER,
                "<gradient:#ff9800:#ff5722><!italic>🔥 Bᴇsᴛ Sᴛʀᴇᴀᴋ",
                List.of("<gray>Best Killstreak: <white>" + fBestKs)));

        // Coins
        String coinsShort = com.lemonpvp.lemoncore.util.TextUtil.formatCoins(fCoins);
        inv.setItem(STAT_SLOTS[4], statItem(Material.GOLD_NUGGET,
                "<gradient:#fffb00:#ff9800><!italic>✦ Cᴏɪɴs",
                List.of("<gray>Balance: <yellow>✦ <gold>" + coinsShort)));

        // ELO per gamemode + an aggregate ranked summary. Load every mode's ELO,
        // then on the main thread fill the per-mode tiles and derive totals.
        List<Gamemode> gamemodes = plugin.getGamemodeManager().getAllGamemodes().stream()
                .filter(Gamemode::isEnabled)
                .limit(GM_SLOTS.length)
                .toList();

        List<CompletableFuture<EloManager.EloData>> futures = new ArrayList<>();
        for (Gamemode gm : gamemodes) {
            futures.add(plugin.getEloManager().loadEloData(targetUuid, gm.getId()));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (inv == null) return;
                    int totalMatches = 0, eloSum = 0, rankedModes = 0, peakElo = 0;
                    int placement = plugin.getEloManager().getPlacementCount();
                    for (int i = 0; i < gamemodes.size(); i++) {
                        EloManager.EloData data = futures.get(i).join();
                        inv.setItem(GM_SLOTS[i], gamemodeItem(gamemodes.get(i), data));
                        int elo = data != null ? data.elo : EloManager.DEFAULT_ELO;
                        int m = data != null ? data.matchesPlayed : 0;
                        totalMatches += m;
                        peakElo = Math.max(peakElo, elo);
                        if (m >= placement) { eloSum += elo; rankedModes++; }
                    }
                    int avgElo = rankedModes > 0 ? eloSum / rankedModes : 0;

                    inv.setItem(STAT_SLOTS[5], statItem(Material.NETHERITE_SWORD,
                            "<gradient:#b388ff:#7c4dff><!italic>🏆 Rᴀɴᴋᴇᴅ Sᴜᴍᴍᴀʀʏ",
                            List.of(
                                    "<gray>Total matches: <white>" + totalMatches,
                                    "<gray>Avg ELO: <white>" + (rankedModes > 0 ? String.valueOf(avgElo) : "—"),
                                    "<gray>Ranked modes: <white>" + rankedModes + "<dark_gray>/" + gamemodes.size())));

                    com.lemonpvp.lemonpractice.model.RankTier peakTier =
                            com.lemonpvp.lemonpractice.model.RankTier.fromElo(peakElo);
                    inv.setItem(STAT_SLOTS[6], statItem(Material.NETHER_STAR,
                            "<gradient:#fffb00:#ff9800><!italic>⭐ Pᴇᴀᴋ Dɪᴠɪsɪᴏɴ",
                            List.of(
                                    "<gray>Peak ELO: <white>" + peakElo,
                                    "<gray>Division: <!italic>" + peakTier.getDisplay())));
                }));
    }

    private ItemStack buildHead() {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (skull.getItemMeta() instanceof SkullMeta meta) {
            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null) meta.setOwningPlayer(target);
            meta.displayName(MM.deserialize(
                    "<!italic><gradient:#fffb00:#00ff00><bold>✦ " + targetName + " ✦"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><dark_gray>» <gray>Player card"));
            int cur = plugin.getStreakManager().getCurrent(targetUuid);
            if (cur > 1) lore.add(MM.deserialize("<!italic><gold>🔥 On a " + cur + " win streak"));
            lore.add(MM.deserialize("<!italic><dark_gray>Divisions & ELO load below…"));
            lore.add(Component.empty());
            meta.lore(lore);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    /** A 10-segment progress bar toward {@code max}, colored with {@code hex}. */
    private String bar(double value, double max, String hex) {
        int filled = (int) Math.round(Math.min(1.0, value / max) * 10);
        return "<" + hex + ">" + "▰".repeat(filled) + "<dark_gray>" + "▱".repeat(10 - filled);
    }

    private ItemStack statItem(Material mat, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + name));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            for (String line : loreLines) lore.add(MM.deserialize("<!italic>" + line));
            lore.add(Component.empty());
            meta.lore(lore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                    org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack gamemodeItem(Gamemode gm, EloManager.EloData data) {
        int elo      = data != null ? data.elo          : EloManager.DEFAULT_ELO;
        int matches  = data != null ? data.matchesPlayed : 0;
        int placement = plugin.getEloManager().getPlacementCount();
        boolean inPlacement = matches < placement;

        String eloColor = elo >= 1800 ? "#ff9800"
                        : elo >= 1500 ? "#fffb00"
                        : elo >= 1200 ? "#69f0ae"
                        : elo >= 1000 ? "#ffffff"
                        :               "#ff5252";

        ItemStack item = new ItemStack(gm.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic><bold><gradient:#fffb00:#00ff00>" + gm.getDisplayName()));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            if (inPlacement) {
                lore.add(MM.deserialize("<!italic><gray>ELO: <yellow>Placement "
                        + "<dark_gray>(" + matches + "/" + placement + ")"));
                lore.add(MM.deserialize("<!italic><gray>Division: <!italic>"
                        + com.lemonpvp.lemonpractice.model.RankTier.UNRANKED.getDisplay()));
            } else {
                com.lemonpvp.lemonpractice.model.RankTier tier =
                        com.lemonpvp.lemonpractice.model.RankTier.fromElo(elo);
                lore.add(MM.deserialize("<!italic><gray>ELO: <" + eloColor + "><bold>" + elo));
                lore.add(MM.deserialize("<!italic><gray>Division: <!italic>" + tier.getDisplay()));
                int nextElo = tier.nextTierElo();
                if (nextElo > 0 && tier.next() != null) {
                    lore.add(MM.deserialize("<!italic><dark_gray>Next: <!italic>"
                            + tier.next().getDisplay() + " <dark_gray>(" + (nextElo - elo) + " ELO)"));
                }
            }
            lore.add(MM.deserialize("<!italic><gray>Matches: <white>" + matches));
            lore.add(Component.empty());
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!p.getUniqueId().equals(viewer.getUniqueId())) return;
        if (!e.getInventory().equals(inv)) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!p.getUniqueId().equals(viewer.getUniqueId())) return;
        if (!e.getInventory().equals(inv)) return;
        if (registered) {
            registered = false;
            HandlerList.unregisterAll(this);
        }
    }
}
