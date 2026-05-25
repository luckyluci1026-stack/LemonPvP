package com.lemonpvp.lemonquests;

import com.lemonpvp.lemonquests.commands.GLevelCommand;
import com.lemonpvp.lemonquests.commands.GQuestsCommand;
import com.lemonpvp.lemonquests.commands.GXpCommand;
import com.lemonpvp.lemonquests.commands.QuestsCommand;
import com.lemonpvp.lemonquests.database.QuestsDatabase;
import com.lemonpvp.lemonquests.listeners.PlayerListener;
import com.lemonpvp.lemonquests.listeners.QuestProgressListener;
import com.lemonpvp.lemonquests.managers.PlayerDataManager;
import com.lemonpvp.lemonquests.managers.QuestManager;
import com.lemonpvp.lemonquests.velocity.QuestsMessaging;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class LemonQuests extends JavaPlugin {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private QuestsDatabase database;
    private QuestManager questManager;
    private PlayerDataManager playerDataManager;
    private QuestsMessaging messaging;

    /** Cached messages.yml configuration. */
    private FileConfiguration messagesConfig;

    // Listener instance kept so setupHotbar can be called for online players on reload
    private PlayerListener playerListener;
    private FileConfiguration serversConfig;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onEnable() {
        // 1. Save default resource files
        saveDefaultConfig();
        loadServersConfig();
        saveResource("quests.yml", false);
        saveResource("messages.yml", false);
        reloadMessagesConfig();

        // 2. Connect to database; disable plugin on failure
        database = new QuestsDatabase(this);
        try {
            database.connect();
            getLogger().info("Database connected successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to connect to the database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Initialise managers (PlayerDataManager loads rank levels in its constructor)
        playerDataManager = new PlayerDataManager(this);
        questManager = new QuestManager(this);
        questManager.loadQuests();

        // 4. Select daily quests asynchronously, then start the reset timer and
        //    playtime tracking once the selection is ready
        questManager.selectDailyQuests().thenRun(() -> {
            questManager.scheduleDailyReset();
            questManager.startPlaytimeTracking();
            getLogger().info("Daily quests selected; reset + playtime tracking scheduled.");
        });

        // 5. Velocity / BungeeCord plugin-message channel
        messaging = new QuestsMessaging(this);
        messaging.register();

        // 6. Register listeners
        playerListener = new PlayerListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(new QuestProgressListener(this), this);

        // 7. Register commands
        registerCommand("quests", new QuestsCommand(this));
        registerTabCommand("gquests", new GQuestsCommand(this));
        registerTabCommand("gxp", new GXpCommand(this));
        registerTabCommand("glevel", new GLevelCommand(this));

        // 8. Handle already-online players (covers the /reload scenario)
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerDataManager.loadPlayer(player.getUniqueId())
                    .thenRun(() -> questManager.loadProgressForPlayer(player.getUniqueId()));
            playerListener.setupHotbar(player);
        }

        getLogger().info("LemonQuests enabled successfully.");
    }

    @Override
    public void onDisable() {
        // Unregister plugin-message channel
        if (messaging != null) {
            messaging.unregister();
        }

        // Save all online players' data before shutdown
        for (Player player : Bukkit.getOnlinePlayers()) {
            // saveAndUnload is async; we just fire-and-forget here since
            // the server is shutting down and the pool will drain before closing
            playerDataManager.saveAndUnload(player.getUniqueId());
        }

        // Close the connection pool
        if (database != null) {
            database.disconnect();
            getLogger().info("Database disconnected.");
        }

        getLogger().info("LemonQuests disabled.");
    }

    // -------------------------------------------------------------------------
    // Servers config
    // -------------------------------------------------------------------------

    public FileConfiguration getServersConfig() { return serversConfig; }
    private void loadServersConfig() {
        java.io.File f = new java.io.File(getDataFolder(), "servers.yml");
        if (!f.exists()) saveResource("servers.yml", false);
        serversConfig = YamlConfiguration.loadConfiguration(f);
    }

    // -------------------------------------------------------------------------
    // Messages config
    // -------------------------------------------------------------------------

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    /** Reloads messages.yml from disk into the cache. */
    public void reloadMessagesConfig() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) saveResource("messages.yml", false);
        messagesConfig = YamlConfiguration.loadConfiguration(file);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public QuestsDatabase getDatabase() {
        return database;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public QuestsMessaging getMessaging() {
        return messaging;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
        } else {
            getLogger().warning("Command '" + name + "' not found in plugin.yml!");
        }
    }

    private void registerTabCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter tc) {
                cmd.setTabCompleter(tc);
            }
        } else {
            getLogger().warning("Command '" + name + "' not found in plugin.yml!");
        }
    }
}
