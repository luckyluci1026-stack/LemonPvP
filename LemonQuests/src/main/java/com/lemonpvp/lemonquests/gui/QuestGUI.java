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
import org.bukkit.NamespacedKey;
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
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int SIZE = 45; // 9×5
    private static final int PROGRESS_ITEM_SLOT = 4;
    private static final int CLOCK_SLOT = 40;
    private static final int QUEST_START_SLOT = 9;

    /** Gray glass pane used for background filler. */
    private static final ItemStack FILLER;
    static {
        FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = FILLER.getItemMeta();
        meta.displayName(Component.empty());
        FILLER.setItemMeta(meta);
    }

    private final LemonQuests plugin;
    private final Player player;

    private Inventory inventory;
    private BukkitTask refreshTask;

    public QuestGUI(LemonQuests plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    // -------------------------------------------------------------------------
    // Open
    // -------------------------------------------------------------------------

    /**
     * Creates the inventory, renders all slots, registers this listener, starts
     * the refresh task, and opens the GUI for the player.
     */
    public void open() {
        inventory = Bukkit.createInventory(null, SIZE,
                MM.deserialize("<gradient:#fffb00:#00ff00>Quests</gradient>"));

        render();

        // Register this instance as a Bukkit listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Refresh every second (20 ticks)
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::render, 20L, 20L);

        player.openInventory(inventory);
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    /**
     * Fills all slots with their correct contents. Called on open and every second.
     */
    private void render() {
        // Background filler: all 45 slots
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, FILLER.clone());
        }

        // Slot 4: player progress
        inventory.setItem(PROGRESS_ITEM_SLOT, buildProgressItem());

        // Quest items starting at slot 9
        List<QuestDefinition> quests = plugin.getQuestManager().getTodaysQuests();
        for (int i = 0; i < quests.size(); i++) {
            int slot = QUEST_START_SLOT + i;
            if (slot >= SIZE) break; // safety guard
            inventory.setItem(slot, buildQuestItem(quests.get(i)));
        }

        // Slot 40: clock
        inventory.setItem(CLOCK_SLOT, buildClockItem());
    }

    // -------------------------------------------------------------------------
    // Item builders
    // -------------------------------------------------------------------------

    private ItemStack buildProgressItem() {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(MM.deserialize("<gradient:#fffb00:#00ff00>Your Progress</gradient>"));

        PlayerData pd = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        int level = (pd != null) ? pd.getLevel() : 1;
        int currentXp = (pd != null) ? pd.getCurrentLevelXp() : 0;

        // Find next rank
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

        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<white>Level: " + level + "</white>"));
        lore.add(MM.deserialize("<white>XP: " + currentXp + "/100</white>"));

        if (nextRank != null) {
            lore.add(MM.deserialize("<gray>Next rank at level " + nextRankLevel
                    + ": " + nextRank + "</gray>"));
        } else {
            lore.add(Component.empty());
        }

        // XP bar: 10 chars
        String xpBar = buildBar(currentXp, 100, 10, "#00ff00", "#555555");
        lore.add(MM.deserialize(xpBar + " <white>" + currentXp + "/100</white>"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildQuestItem(QuestDefinition def) {
        Material mat = difficultyMaterial(def.getDifficulty());
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(MM.deserialize("<bold><white>" + def.getDisplayName() + "</white></bold>"));

        int current = plugin.getQuestManager()
                .getProgressToday(player.getUniqueId(), def.getId());
        int target = def.getTarget();
        boolean completed = plugin.getQuestManager()
                .isCompleted(player.getUniqueId(), def.getId());

        double mult = plugin.getQuestManager()
                .getDifficultyMultiplier(def.getDifficulty());
        int finalXp = def.getEffectiveXp(mult);
        int coins = def.getRewardCoins();
        QuestRewardType rewardType = def.getRewardType();

        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<gray>" + def.getDescription() + "</gray>"));
        lore.add(Component.empty());

        int clamped = Math.min(current, target);
        lore.add(MM.deserialize("<white>Progress: " + clamped + "/" + target + "</white>"));

        String progressBar = buildBar(clamped, target, 10, "#00ff00", "#555555");
        lore.add(MM.deserialize(progressBar));
        lore.add(Component.empty());

        if (rewardType == QuestRewardType.XP || rewardType == QuestRewardType.BOTH) {
            lore.add(MM.deserialize("<yellow>+" + finalXp + " XP</yellow>"));
        }
        if (rewardType == QuestRewardType.COINS || rewardType == QuestRewardType.BOTH) {
            lore.add(MM.deserialize("<gold>+" + coins + " Coins</gold>"));
        }
        lore.add(Component.empty());

        if (completed) {
            lore.add(MM.deserialize("<bold><green>✔ COMPLETED</green></bold>"));
            // Enchantment glint
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            lore.add(MM.deserialize("<gray>In Progress...</gray>"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildClockItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize("<white>Daily Reset</white>"));

        String timeStr = formatTimeUntilMidnight();
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<gray>Resets in: " + timeStr + "</gray>"));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a 10-character progress bar string using MiniMessage color tags.
     *
     * @param current   current value
     * @param max       maximum value
     * @param length    total bar characters
     * @param fillColor hex color for filled portion (e.g. "#00ff00")
     * @param emptyColor hex color for empty portion (e.g. "#555555")
     */
    private String buildBar(int current, int max, int length, String fillColor, String emptyColor) {
        int filled = (max <= 0) ? length : Math.min(length, (int) Math.round((double) current / max * length));
        int empty = length - filled;

        StringBuilder sb = new StringBuilder();
        if (filled > 0) {
            sb.append("<color:").append(fillColor).append(">")
              .append("█".repeat(filled))
              .append("</color>");
        }
        if (empty > 0) {
            sb.append("<color:").append(emptyColor).append(">")
              .append("░".repeat(empty))
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

    private String formatTimeUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        long seconds = Duration.between(now, midnight).getSeconds();

        long hh = seconds / 3600;
        long mm = (seconds % 3600) / 60;
        long ss = seconds % 60;

        return String.format("%02d:%02d:%02d", hh, mm, ss);
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
        HandlerList.unregisterAll(this);
    }
}
