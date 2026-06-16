package com.lemonpvp.lemonquests.managers;

import com.lemonpvp.lemonquests.LemonQuests;
import com.lemonpvp.lemonquests.model.QuestDefinition;
import com.lemonpvp.lemonquests.model.QuestDifficulty;
import com.lemonpvp.lemonquests.model.QuestProgress;
import com.lemonpvp.lemonquests.model.QuestRewardType;
import com.lemonpvp.lemonquests.model.QuestType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class QuestManager {

    private final LemonQuests plugin;

    /** All quest definitions keyed by id. */
    private final Map<String, QuestDefinition> quests = new HashMap<>();

    /** Today's active quest ids. */
    private final List<String> todaysQuestIds = new ArrayList<>();

    /**
     * Progress cache: uuid → (questId → current progress today).
     */
    private final Map<UUID, Map<String, Integer>> progressCache = new ConcurrentHashMap<>();

    /**
     * Completed cache: uuid → set of completed quest ids today.
     */
    private final Map<UUID, Set<String>> completedCache = new ConcurrentHashMap<>();

    /** Difficulty multipliers loaded from quests.yml. */
    private final Map<String, Double> difficultyMultipliers = new HashMap<>();

    /** Rank → minimum level required to unlock, loaded from quests.yml. */
    private final Map<String, Integer> rankLevels = new java.util.LinkedHashMap<>();

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public QuestManager(LemonQuests plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Loading
    // -------------------------------------------------------------------------

    /**
     * Reads quests.yml, populates the quests map and difficultyMultipliers.
     */
    public void loadQuests() {
        quests.clear();
        difficultyMultipliers.clear();
        rankLevels.clear();

        File file = new File(plugin.getDataFolder(), "quests.yml");
        if (!file.exists()) {
            plugin.getLogger().warning("quests.yml not found — no quests loaded.");
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Load rank level thresholds
        ConfigurationSection rankSection = cfg.getConfigurationSection("rank_levels");
        if (rankSection != null) {
            for (String rankKey : rankSection.getKeys(false)) {
                rankLevels.put(rankKey.toLowerCase(), rankSection.getInt(rankKey));
            }
        }

        // Load difficulty multipliers
        ConfigurationSection multSection = cfg.getConfigurationSection("difficulty_multipliers");
        if (multSection != null) {
            for (String key : multSection.getKeys(false)) {
                difficultyMultipliers.put(key.toUpperCase(), multSection.getDouble(key, 1.0));
            }
        }

        // Ensure defaults for any missing difficulty
        for (QuestDifficulty d : QuestDifficulty.values()) {
            difficultyMultipliers.putIfAbsent(d.name(), 1.0);
        }

        // Load quest definitions
        ConfigurationSection questsSection = cfg.getConfigurationSection("quests");
        if (questsSection == null) {
            plugin.getLogger().warning("No 'quests' section found in quests.yml.");
            return;
        }

        for (String id : questsSection.getKeys(false)) {
            ConfigurationSection s = questsSection.getConfigurationSection(id);
            if (s == null) continue;

            try {
                String displayName = s.getString("display_name", id);
                String description = s.getString("description", "");
                QuestType type = QuestType.valueOf(
                        s.getString("type", "KILLS").toUpperCase());
                int target = s.getInt("target", 1);
                QuestRewardType rewardType = QuestRewardType.valueOf(
                        s.getString("reward_type", "XP").toUpperCase());
                int rewardXp = s.getInt("reward_xp", 0);
                int rewardCoins = s.getInt("reward_coins", 0);
                QuestDifficulty difficulty = QuestDifficulty.valueOf(
                        s.getString("difficulty", "EASY").toUpperCase());
                boolean daily = s.getBoolean("daily", true);
                String date = s.getString("date", "");
                boolean enabled = s.getBoolean("enabled", true);

                if (!enabled) continue;

                QuestDefinition def = new QuestDefinition(
                        id, displayName, description, type, target,
                        rewardType, rewardXp, rewardCoins, difficulty, daily, date, enabled);
                quests.put(id, def);

            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Skipping quest '" + id + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + quests.size() + " quest definitions.");
    }

    // -------------------------------------------------------------------------
    // Daily selection
    // -------------------------------------------------------------------------

    /**
     * Selects today's daily quests. Tries to load from DB first; if none saved,
     * generates a new random selection and persists it.
     */
    public CompletableFuture<Void> selectDailyQuests() {
        LocalDate today = LocalDate.now();

        return plugin.getDatabase().loadDailySelection(today).thenCompose(saved -> {
            if (saved != null && !saved.isEmpty()) {
                List<String> valid = new ArrayList<>();
                for (String id : saved) {
                    if (quests.containsKey(id)) valid.add(id);
                }
                if (!valid.isEmpty()) {
                    synchronized (todaysQuestIds) {
                        todaysQuestIds.clear();
                        todaysQuestIds.addAll(valid);
                    }
                    plugin.getLogger().info("Loaded " + valid.size() + " daily quests from DB for " + today);
                    return CompletableFuture.completedFuture(null);
                }
            }

            // Generate new selection
            List<String> generated = generateDailySelection(today);
            synchronized (todaysQuestIds) {
                todaysQuestIds.clear();
                todaysQuestIds.addAll(generated);
            }
            plugin.getLogger().info("Generated " + generated.size() + " daily quests for " + today);
            return plugin.getDatabase().saveDailySelection(today, generated);

        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to load/save daily quest selection", ex);
            return null;
        });
    }

    /**
     * Builds the daily quest list. Always includes date-specific quests that match
     * today, then fills up to daily_quest_count with randomly shuffled daily=true quests.
     */
    private List<String> generateDailySelection(LocalDate today) {
        int count = plugin.getConfig().getInt("daily_quest_count", 3);
        String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

        List<String> result = new ArrayList<>();
        List<String> candidates = new ArrayList<>();

        for (QuestDefinition def : quests.values()) {
            String defDate = def.getDate();
            // Date-specific quests always included when they match today
            if (defDate != null && !defDate.isEmpty() && defDate.equals(todayStr)) {
                result.add(def.getId());
                continue;
            }
            // Pool: daily=true with no specific date
            if (def.isDaily() && (defDate == null || defDate.isEmpty())) {
                candidates.add(def.getId());
            }
        }

        // Shuffle with a deterministic seed derived from the date
        Collections.shuffle(candidates, new Random(today.toEpochDay()));

        for (String id : candidates) {
            if (result.size() >= count) break;
            if (!result.contains(id)) {
                result.add(id);
            }
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public List<QuestDefinition> getTodaysQuests() {
        List<QuestDefinition> list = new ArrayList<>();
        synchronized (todaysQuestIds) {
            for (String id : todaysQuestIds) {
                QuestDefinition def = quests.get(id);
                if (def != null) list.add(def);
            }
        }
        return list;
    }

    public QuestDefinition getQuest(String id) {
        return quests.get(id);
    }

    public Map<String, QuestDefinition> getAllQuests() {
        return Collections.unmodifiableMap(quests);
    }

    public List<String> getTodaysQuestIds() {
        synchronized (todaysQuestIds) {
            return new ArrayList<>(todaysQuestIds);
        }
    }

    public double getDifficultyMultiplier(QuestDifficulty difficulty) {
        return difficultyMultipliers.getOrDefault(difficulty.name(), 1.0);
    }

    /**
     * Returns an unmodifiable view of the rank → minimum-level map.
     */
    public Map<String, Integer> getRankLevels() {
        return Collections.unmodifiableMap(rankLevels);
    }

    // -------------------------------------------------------------------------
    // Progress
    // -------------------------------------------------------------------------

    /**
     * Called by listeners / messaging when a tracked stat changes.
     * Finds matching today's quests and increments progress; completes if target reached.
     */
    public void onStatUpdate(UUID uuid, String statType, int amount) {
        LocalDate today = LocalDate.now();

        for (QuestDefinition def : getTodaysQuests()) {
            if (!def.getType().name().equals(statType)) continue;
            if (isCompleted(uuid, def.getId())) continue;

            plugin.getDatabase().incrementProgress(uuid, def.getId(), today, amount);

            Map<String, Integer> playerCache =
                    progressCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
            int newProgress = playerCache.merge(def.getId(), amount, Integer::sum);

            if (newProgress >= def.getTarget()) {
                completeQuest(uuid, def.getId());
            }
        }
    }

    /**
     * Marks a quest complete in DB, gives rewards, and notifies the player.
     */
    public void completeQuest(UUID uuid, String questId) {
        QuestDefinition def = quests.get(questId);
        if (def == null) return;

        // Guard against double-completion
        Set<String> completed = completedCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
        if (!completed.add(questId)) return;

        LocalDate today = LocalDate.now();
        plugin.getDatabase().markCompleted(uuid, questId, today);

        double mult = getDifficultyMultiplier(def.getDifficulty());
        int xp = def.getEffectiveXp(mult);
        int coins = def.getRewardCoins();
        QuestRewardType rewardType = def.getRewardType();

        // Give XP reward
        CompletableFuture<Void> xpFuture = CompletableFuture.completedFuture(null);
        if (rewardType == QuestRewardType.XP || rewardType == QuestRewardType.BOTH) {
            xpFuture = plugin.getPlayerDataManager().addXp(uuid, xp);
        }

        xpFuture.thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(MM.deserialize(
                        "<green>Quest <yellow>" + def.getDisplayName() + "</yellow> completed! "
                                + "<yellow>+" + xp + " XP</yellow></green>"));
                if ((rewardType == QuestRewardType.COINS || rewardType == QuestRewardType.BOTH) && coins > 0) {
                    player.sendMessage(MM.deserialize("<gold>+" + coins + " Coins</gold>"));
                }
            }
        }));

        // Increment QUESTS_COMPLETED counter (no recursion; uses internal method)
        incrementQuestsCompleted(uuid);
    }

    /**
     * Increments the QUESTS_COMPLETED stat and handles any matching quests,
     * without recursing back into completeQuest for already-handled quests.
     */
    private void incrementQuestsCompleted(UUID uuid) {
        LocalDate today = LocalDate.now();

        for (QuestDefinition def : getTodaysQuests()) {
            if (def.getType() != QuestType.QUESTS_COMPLETED) continue;
            if (isCompleted(uuid, def.getId())) continue;

            plugin.getDatabase().incrementProgress(uuid, def.getId(), today, 1);

            Map<String, Integer> playerCache =
                    progressCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
            int newProgress = playerCache.merge(def.getId(), 1, Integer::sum);

            if (newProgress >= def.getTarget()) {
                // Complete without re-triggering QUESTS_COMPLETED increment
                Set<String> completed = completedCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
                if (completed.add(def.getId())) {
                    plugin.getDatabase().markCompleted(uuid, def.getId(), today);
                    double mult = getDifficultyMultiplier(def.getDifficulty());
                    int xp = def.getEffectiveXp(mult);
                    QuestRewardType rt = def.getRewardType();
                    if (rt == QuestRewardType.XP || rt == QuestRewardType.BOTH) {
                        plugin.getPlayerDataManager().addXp(uuid, xp);
                    }
                    final QuestDefinition fd = def;
                    final int fx = xp;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendMessage(MM.deserialize(
                                    "<green>Quest <yellow>" + fd.getDisplayName() + "</yellow> completed! "
                                            + "<yellow>+" + fx + " XP</yellow></green>"));
                        }
                    });
                }
            }
        }
    }

    /**
     * Returns cached progress today for the given quest; 0 if not cached.
     * Call {@link #loadProgressForPlayer(UUID)} on join to prime the cache.
     */
    public int getProgressToday(UUID uuid, String questId) {
        Map<String, Integer> playerCache = progressCache.get(uuid);
        if (playerCache == null) return 0;
        return playerCache.getOrDefault(questId, 0);
    }

    /**
     * Loads all today's progress rows from the DB into the in-memory cache.
     * Should be called on PlayerJoinEvent.
     */
    public void loadProgressForPlayer(UUID uuid) {
        LocalDate today = LocalDate.now();
        plugin.getDatabase().loadAllProgressToday(uuid, today).thenAccept(progressList -> {
            Map<String, Integer> playerCache =
                    progressCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
            Set<String> completed =
                    completedCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
            for (QuestProgress qp : progressList) {
                playerCache.put(qp.getQuestId(), qp.getProgress());
                if (qp.isCompleted()) {
                    completed.add(qp.getQuestId());
                }
            }
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to load progress for " + uuid, ex);
            return null;
        });
    }

    public boolean isCompleted(UUID uuid, String questId) {
        Set<String> completed = completedCache.get(uuid);
        return completed != null && completed.contains(questId);
    }

    public int getCompletedCountToday(UUID uuid) {
        Set<String> completed = completedCache.get(uuid);
        return completed == null ? 0 : completed.size();
    }

    // -------------------------------------------------------------------------
    // Scheduling
    // -------------------------------------------------------------------------

    /**
     * Schedules a daily reset at the next midnight, then every 24 hours thereafter.
     */
    public void scheduleDailyReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        long msToMidnight = Duration.between(now, midnight).toMillis();
        long ticksToMidnight = msToMidnight / 50L;
        long ticksPerDay = 20L * 60L * 60L * 24L;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            performDailyReset();
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::performDailyReset, 0L, ticksPerDay);
        }, ticksToMidnight);
    }

    /** Removes per-player cache entries when the player disconnects. */
    public void unloadPlayer(UUID uuid) {
        progressCache.remove(uuid);
        completedCache.remove(uuid);
    }

    private void performDailyReset() {
        plugin.getLogger().info("Performing daily quest reset...");
        progressCache.clear();
        completedCache.clear();
        selectDailyQuests().thenRun(() -> plugin.getLogger().info("Daily quests re-selected."));
    }

    /**
     * Starts the async playtime tracking task (fires every 60 seconds).
     */
    public void startPlaytimeTracking() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                onStatUpdate(player.getUniqueId(), "PLAYTIME", 1);
            }
        }, 1200L, 1200L);
    }

    /**
     * Reloads definitions from disk and refreshes the daily selection.
     */
    public void reloadQuests() {
        loadQuests();
        selectDailyQuests().thenRun(() ->
                plugin.getLogger().info("Quests reloaded and daily selection refreshed."));
    }

    // -------------------------------------------------------------------------
    // Admin helpers
    // -------------------------------------------------------------------------

    /**
     * Ensures a quest id appears in today's selection (used by /gquests give).
     */
    public void ensureInTodaysQuests(String questId) {
        synchronized (todaysQuestIds) {
            if (!todaysQuestIds.contains(questId)) {
                todaysQuestIds.add(questId);
            }
        }
    }
}
