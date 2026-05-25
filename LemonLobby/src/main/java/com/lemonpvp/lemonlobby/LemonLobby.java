package com.lemonpvp.lemonlobby;

import com.lemonpvp.lemonlobby.database.Database;
import com.lemonpvp.lemonlobby.gui.TrainingGUI;
import com.lemonpvp.lemonlobby.listeners.PlayerListener;
import com.lemonpvp.lemonlobby.managers.HotbarManager;
import com.lemonpvp.lemonlobby.managers.RestartManager;
import com.lemonpvp.lemonlobby.messaging.LobbyMessaging;
import org.bukkit.plugin.java.JavaPlugin;

public final class LemonLobby extends JavaPlugin {

    private Database database;
    private HotbarManager hotbarManager;
    private LobbyMessaging lobbyMessaging;
    private TrainingGUI trainingGUI;
    private RestartManager restartManager;
    private org.bukkit.configuration.file.FileConfiguration serversConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadServersConfig();

        // Database setup
        database = new Database(this);
        try {
            database.connect();
        } catch (Exception e) {
            getLogger().severe("Failed to connect to database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Managers and messaging
        hotbarManager  = new HotbarManager(this);
        lobbyMessaging = new LobbyMessaging(this);
        trainingGUI    = new TrainingGUI(this, lobbyMessaging);
        restartManager = new RestartManager(this);

        // Register BungeeCord plugin messaging channels
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "lemonlobby:training");

        // Register listeners
        getServer().getPluginManager().registerEvents(
                new PlayerListener(this, hotbarManager, lobbyMessaging, trainingGUI), this);

        // Start daily restart scheduler
        restartManager.start();

        getLogger().info("LemonLobby enabled.");
    }

    @Override
    public void onDisable() {
        if (restartManager != null) restartManager.stop();
        if (database != null) database.close();
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getLogger().info("LemonLobby disabled.");
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Database getDatabase()           { return database; }
    public HotbarManager getHotbarManager() { return hotbarManager; }
    public LobbyMessaging getLobbyMessaging(){ return lobbyMessaging; }
    public TrainingGUI getTrainingGUI()     { return trainingGUI; }
        public org.bukkit.configuration.file.FileConfiguration getServersConfig() { return serversConfig; }
    private void loadServersConfig() {
        java.io.File f = new java.io.File(getDataFolder(), "servers.yml");
        if (!f.exists()) saveResource("servers.yml", false);
        serversConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
    }

    public RestartManager getRestartManager(){ return restartManager; }
}
