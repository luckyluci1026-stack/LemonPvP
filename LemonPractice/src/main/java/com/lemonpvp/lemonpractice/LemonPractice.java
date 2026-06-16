package com.lemonpvp.lemonpractice;

import com.lemonpvp.lemonpractice.database.PracticeDatabase;
import com.lemonpvp.lemonpractice.managers.ArenaManager;
import com.lemonpvp.lemonpractice.managers.DuelManager;
import com.lemonpvp.lemonpractice.managers.EloManager;
import com.lemonpvp.lemonpractice.managers.FFAManager;
import com.lemonpvp.lemonpractice.managers.KitManager;
import com.lemonpvp.lemonpractice.managers.LobbyHotbarManager;
import com.lemonpvp.lemonpractice.managers.QueueManager;
import com.lemonpvp.lemonpractice.managers.SpectatorManager;
import com.lemonpvp.lemonpractice.velocity.VelocityMessaging;
import com.lemonpvp.lemonpractice.commands.AowArenaCommand;
import com.lemonpvp.lemonpractice.commands.AowBuildSpawnCommand;
import com.lemonpvp.lemonpractice.commands.GEloCommand;
import com.lemonpvp.lemonpractice.listeners.DuelListener;
import com.lemonpvp.lemonpractice.listeners.FFAListener;
import com.lemonpvp.lemonpractice.listeners.KitEditorListener;
import com.lemonpvp.lemonpractice.listeners.LobbyListener;
import com.lemonpvp.lemonpractice.config.GamemodeManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class LemonPractice extends JavaPlugin {

    private PracticeDatabase db;
    private GamemodeManager gamemodeManager;
    private ArenaManager arenaManager;
    private KitManager kitManager;
    private EloManager eloManager;
    private QueueManager queueManager;
    private DuelManager duelManager;
    private SpectatorManager spectatorManager;
    private FFAManager ffaManager;
    private LobbyHotbarManager lobbyHotbarManager;
    private VelocityMessaging velocityMessaging;
    private String serverType;

    private FileConfiguration kitsConfig;
    private FileConfiguration gamemodesConfig;
    private FileConfiguration arenasConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration serversConfig;

    @Override
    public void onEnable() {
        // 1. Save default resource files
        loadServersConfig();
        saveDefaultConfig();
        saveResource("gamemodes.yml", false);
        saveResource("kits.yml", false);
        saveResource("arenas.yml", false);
        saveResource("messages.yml", false);

        loadSecondaryConfigs();

        // 2. Connect database
        db = new PracticeDatabase(this);
        try {
            db.connect();
            getLogger().info("[LemonPractice] Database connected successfully.");
        } catch (Exception e) {
            getLogger().severe("[LemonPractice] Failed to connect to database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Read server type
        serverType = getConfig().getString("server-type", "LOBBY").toUpperCase();

        // 4. Init managers in order
        gamemodeManager = new GamemodeManager(this);
        gamemodeManager.loadGamemodes();

        arenaManager = new ArenaManager(this);
        arenaManager.loadAll();

        kitManager = new KitManager(this);
        eloManager = new EloManager(this);
        queueManager = new QueueManager(this);
        queueManager.startTasks();
        duelManager = new DuelManager(this);
        spectatorManager = new SpectatorManager(this);
        ffaManager = new FFAManager(this);
        ffaManager.loadAll();
        lobbyHotbarManager = new LobbyHotbarManager(this);

        // 5. Register VelocityMessaging
        velocityMessaging = new VelocityMessaging(this);
        velocityMessaging.register();

        // 6. Register all listeners on both servers (listeners check serverType internally)
        getServer().getPluginManager().registerEvents(new LobbyListener(this), this);
        getServer().getPluginManager().registerEvents(new KitEditorListener(this), this);
        getServer().getPluginManager().registerEvents(new DuelListener(this), this);
        getServer().getPluginManager().registerEvents(new FFAListener(this), this);

        // 7. Register commands
        AowArenaCommand arenaCmd = new AowArenaCommand(this);
        getCommand("aowarena").setExecutor(arenaCmd);
        getCommand("aowarena").setTabCompleter(arenaCmd);
        getCommand("aowbuildspawn").setExecutor(new AowBuildSpawnCommand(this));
        GEloCommand eloCmd = new GEloCommand(this);
        getCommand("gelo").setExecutor(eloCmd);
        getCommand("gelo").setTabCompleter(eloCmd);

        // 8. If LOBBY: set up hotbars for all currently online players (reload case)
        if (serverType.equals("LOBBY")) {
            for (Player player : getServer().getOnlinePlayers()) {
                lobbyHotbarManager.setupHotbar(player);
            }
        }

        // 9. Done
        getLogger().info("[LemonPractice] Enabled on " + serverType + " server.");
    }

    @Override
    public void onDisable() {
        if (queueManager != null) {
            queueManager.stopTasks();
            queueManager.clearQueue();
        }
        if (duelManager != null) {
            duelManager.endAllDuels();
        }
        if (db != null) {
            db.disconnect();
        }
        if (velocityMessaging != null) {
            velocityMessaging.unregister();
        }
        getLogger().info("[LemonPractice] Disabled.");
    }

    // -------------------------------------------------------------------------
    // Secondary config loaders
    // -------------------------------------------------------------------------

    private void loadSecondaryConfigs() {
        kitsConfig = loadConfig("kits.yml");
        gamemodesConfig = loadConfig("gamemodes.yml");
        arenasConfig = loadConfig("arenas.yml");
        messagesConfig = loadConfig("messages.yml");
    }

    private FileConfiguration loadConfig(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) saveResource(fileName, false);
        return YamlConfiguration.loadConfiguration(file);
    }

    public void reloadSecondaryConfigs() {
        reloadConfig();
        loadSecondaryConfigs();
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public PracticeDatabase getDatabase() { return db; }
    public GamemodeManager getGamemodeManager() { return gamemodeManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public KitManager getKitManager() { return kitManager; }
    public EloManager getEloManager() { return eloManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public DuelManager getDuelManager() { return duelManager; }
    public SpectatorManager getSpectatorManager() { return spectatorManager; }
    public FFAManager getFfaManager() { return ffaManager; }
    public LobbyHotbarManager getLobbyHotbarManager() { return lobbyHotbarManager; }
    public FileConfiguration getServersConfig() { return serversConfig; }
    private void loadServersConfig() {
        java.io.File f = new java.io.File(getDataFolder(), "servers.yml");
        if (!f.exists()) saveResource("servers.yml", false);
        serversConfig = YamlConfiguration.loadConfiguration(f);
    }

    public VelocityMessaging getVelocityMessaging() { return velocityMessaging; }
    public String getServerType() { return serverType; }

    public FileConfiguration getKitsConfig() { return kitsConfig; }
    public FileConfiguration getGamemodesConfig() { return gamemodesConfig; }
    public FileConfiguration getArenasConfig() { return arenasConfig; }
    public FileConfiguration getMessagesConfig() { return messagesConfig; }
}
