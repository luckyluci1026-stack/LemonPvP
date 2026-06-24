package com.lemonpvp.lemonlobby;

import com.lemonpvp.lemonlobby.commands.ApplesCommand;
import com.lemonpvp.lemonlobby.commands.GApplesCommand;
import com.lemonpvp.lemonlobby.commands.GPlanksCommand;
import com.lemonpvp.lemonlobby.commands.LobbyAdminCommand;
import com.lemonpvp.lemonlobby.commands.PlanksCommand;
import com.lemonpvp.lemonlobby.commands.ShopCommand;
import com.lemonpvp.lemonlobby.database.Database;
import com.lemonpvp.lemonlobby.gui.TrainingGUI;
import com.lemonpvp.lemonlobby.listeners.AppleTreeListener;
import com.lemonpvp.lemonlobby.listeners.PlayerListener;
import com.lemonpvp.lemonlobby.managers.BoosterManager;
import com.lemonpvp.lemonlobby.managers.HotbarManager;
import com.lemonpvp.lemonlobby.managers.RestartManager;
import com.lemonpvp.lemonlobby.managers.TreeUpgradeManager;
import com.lemonpvp.lemonlobby.messaging.LobbyMessaging;
import org.bukkit.plugin.java.JavaPlugin;

public final class LemonLobby extends JavaPlugin {

    private Database database;
    private HotbarManager hotbarManager;
    private LobbyMessaging lobbyMessaging;
    private TrainingGUI trainingGUI;
    private RestartManager restartManager;
    private BoosterManager boosterManager;
    private TreeUpgradeManager treeUpgradeManager;
    private AppleTreeListener appleTreeListener;
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
        hotbarManager      = new HotbarManager(this);
        lobbyMessaging     = new LobbyMessaging(this);
        trainingGUI        = new TrainingGUI(this, lobbyMessaging);
        restartManager     = new RestartManager(this);
        boosterManager     = new BoosterManager(this);
        treeUpgradeManager = new TreeUpgradeManager(this);
        appleTreeListener  = new AppleTreeListener(this);

        // BungeeCord plugin messaging
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Register listeners
        getServer().getPluginManager().registerEvents(
                new PlayerListener(this, hotbarManager, lobbyMessaging, trainingGUI), this);
        getServer().getPluginManager().registerEvents(appleTreeListener, this);

        // Register commands
        var shopCmd = getCommand("shop");
        if (shopCmd != null) shopCmd.setExecutor(new ShopCommand(this));
        var applesCmd = getCommand("apples");
        if (applesCmd != null) applesCmd.setExecutor(new ApplesCommand(this));
        var planksCmd = getCommand("planks");
        if (planksCmd != null) planksCmd.setExecutor(new PlanksCommand(this));
        var gapplesCmd = new GApplesCommand(this);
        var gapplesBukkitCmd = getCommand("gapples");
        if (gapplesBukkitCmd != null) { gapplesBukkitCmd.setExecutor(gapplesCmd); gapplesBukkitCmd.setTabCompleter(gapplesCmd); }
        var gplanksCmd = new GPlanksCommand(this);
        var gplanksBukkitCmd = getCommand("gplanks");
        if (gplanksBukkitCmd != null) { gplanksBukkitCmd.setExecutor(gplanksCmd); gplanksBukkitCmd.setTabCompleter(gplanksCmd); }
        var llobbyCmd = getCommand("llobby");
        if (llobbyCmd != null) {
            var llobbyAdmin = new LobbyAdminCommand(this);
            llobbyCmd.setExecutor(llobbyAdmin);
            llobbyCmd.setTabCompleter(llobbyAdmin);
        }

        // Booster bossbar tick (every second)
        getServer().getScheduler().runTaskTimer(this,
                () -> boosterManager.tickBossBars(), 20L, 20L);

        // Start daily restart scheduler
        restartManager.start();

        getLogger().info("LemonLobby enabled.");
    }

    @Override
    public void onDisable() {
        if (restartManager != null) restartManager.stop();
        if (database != null) database.close();
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
        getLogger().info("LemonLobby disabled.");
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Database getDatabase()                     { return database; }
    public HotbarManager getHotbarManager()           { return hotbarManager; }
    public LobbyMessaging getLobbyMessaging()         { return lobbyMessaging; }
    public TrainingGUI getTrainingGUI()               { return trainingGUI; }
    public BoosterManager getBoosterManager()         { return boosterManager; }
    public TreeUpgradeManager getTreeUpgradeManager() { return treeUpgradeManager; }
    public AppleTreeListener getAppleTreeListener()   { return appleTreeListener; }
    public org.bukkit.configuration.file.FileConfiguration getServersConfig() { return serversConfig; }
    public RestartManager getRestartManager()         { return restartManager; }

    public void loadServersConfig() {
        java.io.File f = new java.io.File(getDataFolder(), "servers.yml");
        if (!f.exists()) saveResource("servers.yml", false);
        serversConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
    }
}
