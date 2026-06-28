package com.lemonpvp.lemonquests.gui;

import com.lemonpvp.lemonquests.LemonQuests;
import com.lemonpvp.lemonquests.model.PlayerData;
import com.lemonpvp.lemonquests.model.QuestDefinition;
import com.lemonpvp.lemonquests.model.QuestDifficulty;
import com.lemonpvp.lemonquests.model.QuestRewardType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int SIZE = 45; // 9×5
    private static final int PROFILE_SLOT = 4;
    private static final int CLOCK_SLOT = 40;

    /** Centered quest slots in rows 2 and 3 (avoids the bordered edges). */
    private static final int[] QUEST_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    private static final ItemStack BLACK_FILLER = filler(Material.BLACK_STAINED_GLASS_PANE);
    private static final ItemStack GRAY_FILLER  = filler(Material.GRAY_STAINED_GLASS_PANE);

    private final LemonQuests plugin;
    private final Player player;

    private Inventory inventory;
    private BukkitTask refreshTask;
    private boolean registered = false;

    public QuestGUI(LemonQuests plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    // -------------------------------------------------------------------------
    // Open
    // -------------------------------------------------------------------------

    public void open() {
        inventory = Bukkit.createInventory(null, SIZE,
                MM.deserialize("<!italic><gradient:#fffb00:#00ff00>ǫᴜᴇsᴛs</gradient>"));

        render();

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::render, 20L, 20L);

        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        player.openInventory(inventory);
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    private void render() {
        // Border: black panes around the edges, gray everywhere inside.
        for (int i = 0; i < SIZE; i++) {
            if (isBorder(i)) {
                inventory.setItem(i, BLACK_FILLER);
            } else {
                inventory.setItem(i, GRAY_FILLER);
            }
        }

        inventory.setItem(PROFILE_SLOT, buildProfileItem());

        List<QuestDefinition> quests = plugin.getQuestManager().getTodaysQuests();
        for (int i = 0; i < quests.size() && i < QUEST_SLOTS.length; i++) {
            inventory.setItem(QUEST_SLOTS[i], buildQuestItem(quests.get(i)));
        }

        inventory.setItem(CLOCK_SLOT, buildClockItem());
    }

    private boolean isBorder(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row == 0 || row == 4 || col == 0 || col == 8;
    }

    // -------------------------------------------------------------------------
    // Item builders
    // -------------------------------------------------------------------------

    private ItemStack buildProfileItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;

        meta.setOwningPlayer(player);
        meta.displayName(MM.deserialize("<!italic><gradient:#fffb00:#00ff00>" + player.getName() + "</gradient>"));

        PlayerData pd = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        int level = (pd != null) ? pd.getLevel() : 1;
        int currentXp = (pd != null) ? pd.getCurrentLevelXp() : 0;

        // Next rank lookup
        Map<String, Integer> rankLevels = plugin.getPlayerDataManager().getRankLevels();
        String nextRank = null;
        int nextRankLevel = 0;
        for (Map.Entry<String, Integer> entry : rankLevels.entrySet()) {
            if (level < entry.getValue()) {
                nextRank = entry.getKey();
                nextRankLevel = entry.getValue();
                break;
            }
        }

        // Daily completion count
        List<QuestDefinition> todays = plugin.getQuestManager().getTodaysQuests();
        int doneToday = 0;
        for (QuestDefinition def : todays) {
            if (plugin.getQuestManager().isCompleted(player.getUniqueId(), def.getId())) doneToday++;
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Level: <white>" + level));
        lore.add(MM.deserialize("<!italic><gray>Coins: <gold>" + formatNumber(getCachedCoins()) + " ⭐"));
        lore.add(Component.empty());

        String xpBar = buildBar(currentXp, 100, 20, "#00ff00", "#444444");
        lore.add(MM.deserialize("<!italic>" + xpBar));
        lore.add(MM.deserialize("<!italic><gray>XP: <green>" + currentXp + "<gray>/<white>100"));
        lore.add(Component.empty());

        if (nextRank != null) {
            lore.add(MM.deserialize("<!italic><gray>Next Rank: <yellow>" + nextRank));
            lore.add(MM.deserialize("<!italic><gray>at Level <white>" + nextRankLevel));
        } else {
            lore.add(MM.deserialize("<!italic><gold>Highest rank reached!"));
        }
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Completed today: <green>" + doneToday
                + "<gray>/<white>" + todays.size()));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildQuestItem(QuestDefinition def) {
        Material mat = difficultyMaterial(def.getDifficulty());
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String diffColor = difficultyColor(def.getDifficulty());
        meta.displayName(MM.deserialize("<!italic><" + diffColor + "><bold>" + def.getDisplayName() + "</bold>"));

        int current = plugin.getQuestManager().getProgressToday(player.getUniqueId(), def.getId());
        int target = def.getTarget();
        boolean completed = plugin.getQuestManager().isCompleted(player.getUniqueId(), def.getId());

        double mult = plugin.getQuestManager().getDifficultyMultiplier(def.getDifficulty());
        int finalXp = def.getEffectiveXp(mult);
        int coins = def.getRewardCoins();
        QuestRewardType rewardType = def.getRewardType();

        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic><dark_gray>" + capitalize(def.getDifficulty().name()) + " Quest"));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>" + def.getDescription()));
        lore.add(Component.empty());

        int clamped = Math.min(current, target);
        String progressBar = buildBar(clamped, target, 20,
                completed ? "#00ff00" : "#fffb00", "#444444");
        lore.add(MM.deserialize("<!italic>" + progressBar));
        lore.add(MM.deserialize("<!italic><gray>Progress: <white>" + clamped + "<gray>/<white>" + target
                + " <dark_gray>(" + percent(clamped, target) + "%)"));
        lore.add(Component.empty());

        lore.add(MM.deserialize("<!italic><gray>Rewards:"));
        if (rewardType == QuestRewardType.XP || rewardType == QuestRewardType.BOTH) {
            lore.add(MM.deserialize("<!italic>  <yellow>+ " + finalXp + " XP"));
        }
        if ((rewardType == QuestRewardType.COINS || rewardType == QuestRewardType.BOTH) && coins > 0) {
            lore.add(MM.deserialize("<!italic>  <gold>+ " + coins + " Coins ⭐"));
        }
        lore.add(Component.empty());

        if (completed) {
            lore.add(MM.deserialize("<!italic><green><bold>✔ COMPLETED</bold>"));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            lore.add(MM.deserialize("<!italic><yellow>➜ In progress..."));
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildClockItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic><gradient:#4fc3f7:#0288d1>Daily Reset</gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>New quests in:"));
        lore.add(MM.deserialize("<!italic><white>" + formatTimeUntilMidnight()));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><dark_gray>Quests reset at"));
        lore.add(MM.deserialize("<!italic><dark_gray>midnight."));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private long getCachedCoins() {
        org.bukkit.plugin.Plugin lcPlugin = Bukkit.getPluginManager().getPlugin("LemonCore");
        if (lcPlugin instanceof com.lemonpvp.lemoncore.LemonCore core) {
            com.lemonpvp.lemoncore.managers.PlayerData pd =
                    core.getPlayerDataManager().getCached(player.getUniqueId());
            return pd != null ? pd.getCoins() : 0;
        }
        return 0;
    }

    private String formatNumber(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fk", n / 1_000.0);
        return String.valueOf(n);
    }

    private int percent(int current, int max) {
        if (max <= 0) return 100;
        return Math.min(100, (int) Math.round((double) current / max * 100));
    }

    /**
     * Builds a progress bar string using MiniMessage color tags.
     */
    private String buildBar(int current, int max, int length, String fillColor, String emptyColor) {
        int filled = (max <= 0) ? length : Math.min(length, (int) Math.round((double) current / max * length));
        int empty = length - filled;

        StringBuilder sb = new StringBuilder();
        if (filled > 0) {
            sb.append("<color:").append(fillColor).append(">")
              .append("|".repeat(filled))
              .append("</color>");
        }
        if (empty > 0) {
            sb.append("<color:").append(emptyColor).append(">")
              .append("|".repeat(empty))
              .append("</color>");
        }
        return sb.toString();
    }

    private Material difficultyMaterial(QuestDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> Material.LIME_DYE;
            case MEDIUM -> Material.YELLOW_DYE;
            case HARD -> Material.ORANGE_DYE;
            case EXPERT -> Material.RED_DYE;
        };
    }

    private String difficultyColor(QuestDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> "green";
            case MEDIUM -> "yellow";
            case HARD -> "gold";
            case EXPERT -> "red";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private String formatTimeUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        long seconds = Duration.between(now, midnight).getSeconds();

        long hh = seconds / 3600;
        long mm = (seconds % 3600) / 60;
        long ss = seconds % 60;

        return String.format("%02d:%02d:%02d", hh, mm, ss);
    }

    private static ItemStack filler(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    // -------------------------------------------------------------------------
    // Listeners
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!(event.getPlayer() instanceof Player)) return;
        cleanup();
    }

    private void cleanup() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            refreshTask.cancel();
            refreshTask = null;
        }
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }
}
