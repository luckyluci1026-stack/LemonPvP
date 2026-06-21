package com.lemonpvp.lemonquests.managers;

import com.lemonpvp.lemonquests.LemonQuests;
import com.lemonpvp.lemonquests.model.PlayerData;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerDataManager {

    private final LemonQuests plugin;

    /** In-memory player data cache. */
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    /** LuckPerms API reference; may be null if LP is not installed. */
    private LuckPerms luckPerms;

    /**
     * Rank name → minimum level required. Stored in declaration order
     * (iron → gold → emerald → diamond → netherite → pro → master).
     */
    private final LinkedHashMap<String, Integer> rankLevels = new LinkedHashMap<>();

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Canonical rank order — also used by QuestGUI for "next rank" look-up.
    private static final String[] RANK_ORDER =
            {"iron", "gold", "emerald", "diamond", "netherite", "pro", "master"};

    public PlayerDataManager(LemonQuests plugin) {
        this.plugin = plugin;

        // Obtain LuckPerms if available
        try {
            RegisteredServiceProvider<LuckPerms> provider =
                    Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                this.luckPerms = provider.getProvider();
                plugin.getLogger().info("LuckPerms hooked successfully.");
            } else {
                plugin.getLogger().warning("LuckPerms not found — rank unlocks disabled.");
            }
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().warning("LuckPerms class not found — rank unlocks disabled.");
        }

        loadRankLevels();
    }

    // -------------------------------------------------------------------------
    // Config loading
    // -------------------------------------------------------------------------

    /**
     * Reads the rank_levels section from quests.yml. Should be called on enable
     * and on /gquests reload.
     */
    public void loadRankLevels() {
        rankLevels.clear();

        File file = new File(plugin.getDataFolder(), "quests.yml");
        if (!file.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("rank_levels");
        if (section == null) return;

        for (String rank : RANK_ORDER) {
            if (section.contains(rank)) {
                rankLevels.put(rank, section.getInt(rank));
            }
        }
        // Pick up any extra ranks not in the hardcoded list
        for (String key : section.getKeys(false)) {
            rankLevels.putIfAbsent(key, section.getInt(key));
        }
    }

    public LinkedHashMap<String, Integer> getRankLevels() {
        return rankLevels;
    }

    // -------------------------------------------------------------------------
    // Cache
    // -------------------------------------------------------------------------

    /** Returns the cached PlayerData, or null if not loaded. */
    public PlayerData getPlayer(UUID uuid) {
        return cache.get(uuid);
    }

    /** Removes the player from cache without saving. */
    public void unloadPlayer(UUID uuid) {
        cache.remove(uuid);
    }

    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    // -------------------------------------------------------------------------
    // Load / save
    // -------------------------------------------------------------------------

    /**
     * Loads a player's data from the DB and puts it in cache.
     * DB contract: int[0]=level, int[1]=xp (XP within current level).
     * We store totalXp = (level-1)*100 + currentLevelXp.
     */
    public CompletableFuture<PlayerData> loadPlayer(UUID uuid) {
        return plugin.getDatabase().loadPlayerData(uuid).thenApply(data -> {
            int dbLevel = Math.max(1, data[0]);
            int dbXp    = Math.max(0, data[1]);
            int totalXp = (dbLevel - 1) * 100 + dbXp;

            PlayerData pd = new PlayerData(uuid, totalXp);
            cache.put(uuid, pd);
            return pd;
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, ex);
            PlayerData fallback = new PlayerData(uuid, 0);
            cache.put(uuid, fallback);
            return fallback;
        });
    }

    /**
     * Saves the player's data to the DB. Does NOT remove from cache.
     */
    public CompletableFuture<Void> savePlayer(UUID uuid) {
        PlayerData pd = cache.get(uuid);
        if (pd == null) return CompletableFuture.completedFuture(null);
        return persistPlayerData(uuid, pd);
    }

    /**
     * Saves and removes the player from cache.
     */
    public CompletableFuture<Void> saveAndUnload(UUID uuid) {
        PlayerData pd = cache.remove(uuid);
        if (pd == null) return CompletableFuture.completedFuture(null);
        return persistPlayerData(uuid, pd);
    }

    private CompletableFuture<Void> persistPlayerData(UUID uuid, PlayerData pd) {
        return plugin.getDatabase()
                .savePlayerData(uuid, pd.getLevel(), pd.getCurrentLevelXp())
                .exceptionally(ex -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + uuid, ex);
                    return null;
                });
    }

    // -------------------------------------------------------------------------
    // XP manipulation
    // -------------------------------------------------------------------------

    /**
     * Adds XP, fires level-up effects for each level gained, saves, and
     * checks rank unlocks.
     */
    public CompletableFuture<Void> addXp(UUID uuid, int amount) {
        return ensureLoaded(uuid).thenCompose(pd -> {
            int oldLevel = pd.getLevel();
            pd.addXp(amount);
            int newLevel = pd.getLevel();

            // Fire level-up effects on the main thread for each new level
            if (newLevel > oldLevel) {
                for (int lvl = oldLevel + 1; lvl <= newLevel; lvl++) {
                    final int fLvl = lvl;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) fireLevelUpEffect(player, fLvl);
                    });
                }
            }

            CompletableFuture<Void> saveFuture = persistPlayerData(uuid, pd);

            if (newLevel > oldLevel) {
                return saveFuture.thenCompose(v -> checkRankUnlocks(uuid));
            }
            return saveFuture;
        });
    }

    /**
     * Subtracts XP from the player (floors at 0), recomputes level, and saves.
     */
    public CompletableFuture<Void> subtractXp(UUID uuid, int amount) {
        return ensureLoaded(uuid).thenCompose(pd -> {
            pd.subtractXp(amount);
            return persistPlayerData(uuid, pd);
        });
    }

    /**
     * Sets the player's total XP to an exact value, recomputes level, saves,
     * and checks rank unlocks.
     */
    public CompletableFuture<Void> setXp(UUID uuid, int amount) {
        return ensureLoaded(uuid).thenCompose(pd -> {
            pd.subtractXp(pd.getTotalXp()); // zero out
            pd.addXp(Math.max(0, amount));
            return persistPlayerData(uuid, pd)
                    .thenCompose(v -> checkRankUnlocks(uuid));
        });
    }

    // -------------------------------------------------------------------------
    // Level manipulation
    // -------------------------------------------------------------------------

    /**
     * Sets the player to the given level (preserving 0 XP within the level),
     * saves, and checks rank unlocks.
     */
    public CompletableFuture<Void> setLevel(UUID uuid, int level) {
        return ensureLoaded(uuid).thenCompose(pd -> {
            int targetXp = (Math.max(1, level) - 1) * 100;
            pd.subtractXp(pd.getTotalXp());
            pd.addXp(targetXp);
            return persistPlayerData(uuid, pd)
                    .thenCompose(v -> checkRankUnlocks(uuid));
        });
    }

    /**
     * Adds the given number of levels, fires effects, saves, and checks rank unlocks.
     */
    public CompletableFuture<Void> addLevel(UUID uuid, int amount) {
        return ensureLoaded(uuid).thenCompose(pd -> {
            int oldLevel = pd.getLevel();
            int newLevel = oldLevel + amount;
            // Keep XP within current level intact
            int targetXp = (newLevel - 1) * 100 + pd.getCurrentLevelXp();
            pd.subtractXp(pd.getTotalXp());
            pd.addXp(targetXp);

            for (int lvl = oldLevel + 1; lvl <= newLevel; lvl++) {
                final int fLvl = lvl;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) fireLevelUpEffect(player, fLvl);
                });
            }

            return persistPlayerData(uuid, pd)
                    .thenCompose(v -> checkRankUnlocks(uuid));
        });
    }

    /**
     * Subtracts the given number of levels (floors at level 1), saves.
     */
    public CompletableFuture<Void> subtractLevel(UUID uuid, int amount) {
        return ensureLoaded(uuid).thenCompose(pd -> {
            int newLevel = Math.max(1, pd.getLevel() - amount);
            int targetXp = (newLevel - 1) * 100 + pd.getCurrentLevelXp();
            pd.subtractXp(pd.getTotalXp());
            pd.addXp(targetXp);
            return persistPlayerData(uuid, pd);
        });
    }

    /**
     * Resets the player to level 1, 0 XP, and saves.
     */
    public CompletableFuture<Void> resetPlayer(UUID uuid) {
        return ensureLoaded(uuid).thenCompose(pd -> {
            pd.subtractXp(pd.getTotalXp());
            return persistPlayerData(uuid, pd);
        });
    }

    // -------------------------------------------------------------------------
    // Rank unlocks
    // -------------------------------------------------------------------------

    /**
     * Checks each rank threshold. For any rank the player qualifies for that
     * hasn't been unlocked yet, grants it via LuckPerms and records it in the DB.
     */
    public CompletableFuture<Void> checkRankUnlocks(UUID uuid) {
        PlayerData pd = cache.get(uuid);
        if (pd == null) return CompletableFuture.completedFuture(null);

        int playerLevel = pd.getLevel();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : rankLevels.entrySet()) {
            String rank = entry.getKey();
            int required = entry.getValue();

            if (playerLevel >= required) {
                CompletableFuture<Void> f = plugin.getDatabase()
                        .hasRankUnlocked(uuid, rank)
                        .thenCompose(alreadyUnlocked -> {
                            if (!alreadyUnlocked) {
                                grantRank(uuid, rank);
                                plugin.getDatabase().recordRankUnlock(uuid, rank);

                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    Player p = Bukkit.getPlayer(uuid);
                                    if (p != null) p.sendMessage(MM.deserialize(
                                            ">> <gradient:#fffb00:#00ff00>Rank Unlocked: "
                                                    + rank + "</gradient> <<"));
                                });
                            }
                            return CompletableFuture.<Void>completedFuture(null);
                        })
                        .exceptionally(ex -> {
                            plugin.getLogger().log(Level.SEVERE,
                                    "Error checking rank unlock for " + rank, ex);
                            return (Void) null;
                        });
                futures.add(f);
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Grants the named rank via LuckPerms using InheritanceNode (parent add).
     * NEVER uses parent set — always adds inheritance.
     */
    public void grantRank(UUID uuid, String rank) {
        if (luckPerms == null) return;

        luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
            user.data().add(InheritanceNode.builder(rank).build());
            luckPerms.getUserManager().saveUser(user);
            plugin.getLogger().info("Granted rank '" + rank + "' to " + uuid);
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to grant rank '" + rank + "' to " + uuid, ex);
            return null;
        });
    }

    // -------------------------------------------------------------------------
    // Level-up visual
    // -------------------------------------------------------------------------

    /**
     * Plays the level-up sound and shows a gradient title to the player.
     */
    public void fireLevelUpEffect(Player player, int newLevel) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        Title title = Title.title(
                MM.deserialize("<bold><gradient:#fffb00:#00ff00>Level Up!</gradient></bold>"),
                MM.deserialize("<white>Level " + newLevel + "</white>"));
        player.showTitle(title);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the cached PlayerData for the UUID, loading from DB if necessary.
     */
    private CompletableFuture<PlayerData> ensureLoaded(UUID uuid) {
        PlayerData cached = cache.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return loadPlayer(uuid);
    }
}
