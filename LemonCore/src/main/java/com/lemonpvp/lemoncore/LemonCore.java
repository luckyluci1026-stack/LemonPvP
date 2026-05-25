package com.lemonpvp.lemoncore;

import com.lemonpvp.lemoncore.commands.admin.*;
import com.lemonpvp.lemoncore.commands.user.*;
import com.lemonpvp.lemoncore.discord.DiscordWebhookManager;
import com.lemonpvp.lemoncore.managers.PlayerTracker;
import com.lemonpvp.lemoncore.config.ConfigManager;
import com.lemonpvp.lemoncore.config.FilterManager;
import com.lemonpvp.lemoncore.config.MessagesManager;
import com.lemonpvp.lemoncore.database.DatabaseManager;
import com.lemonpvp.lemoncore.gui.SettingsGUI;
import com.lemonpvp.lemoncore.listeners.AdvancementListener;
import com.lemonpvp.lemoncore.listeners.ChatListener;
import com.lemonpvp.lemoncore.listeners.PlayerJoinQuitListener;
import com.lemonpvp.lemoncore.listeners.PlayerTrackerListener;
import com.lemonpvp.lemoncore.managers.*;
import com.lemonpvp.lemoncore.scoreboard.ScoreboardManager;
import com.lemonpvp.lemoncore.util.RandomNameUtil;
import com.lemonpvp.lemoncore.velocity.VelocityMessaging;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class LemonCore extends JavaPlugin {

    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private FilterManager filterManager;
    private PlayerDataManager playerDataManager;
    private BanManager banManager;
    private MuteManager muteManager;
    private CodeManager codeManager;
    private StatsManager statsManager;
    private ReportManager reportManager;
    private OffenseManager offenseManager;
    private RestartManager restartManager;
    private ListenerManager listenerManager;
    private ScoreboardManager scoreboardManager;
    private VelocityMessaging velocityMessaging;
    private RandomNameUtil randomNameUtil;
    private PlayerTracker playerTracker;
    private DiscordLinkManager discordLinkManager;
    private DiscordWebhookManager discordWebhookManager;
    private MaintenanceManager maintenanceManager;
    private LuckPerms luckPerms;
    private org.bukkit.configuration.file.FileConfiguration serversConfig;

    @Override
    public void onEnable() {
        // Save default configs
        saveDefaultConfig();
        loadServersConfig();

        // Init config managers
        configManager = new ConfigManager(this);
        messagesManager = new MessagesManager(this);
        filterManager = new FilterManager(this);
        randomNameUtil = new RandomNameUtil(this);

        // Connect to database
        databaseManager = new DatabaseManager(this);
        try {
            databaseManager.connect();
            getLogger().info("Connected to MySQL database.");
        } catch (Exception e) {
            getLogger().severe("Failed to connect to MySQL: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Hook LuckPerms
        RegisteredServiceProvider<LuckPerms> lpProvider =
                getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (lpProvider != null) {
            luckPerms = lpProvider.getProvider();
            getLogger().info("Hooked into LuckPerms.");
        } else {
            getLogger().warning("LuckPerms not found! Rank features will be limited.");
        }

        // Init maintenance (before other managers so state is ready at join)
        maintenanceManager = new MaintenanceManager(this);
        maintenanceManager.load();

        // Init managers
        playerDataManager = new PlayerDataManager(this);
        banManager = new BanManager(this);
        muteManager = new MuteManager(this);
        codeManager = new CodeManager(this);
        statsManager = new StatsManager(this);
        reportManager = new ReportManager(this);
        offenseManager = new OffenseManager(this);
        restartManager = new RestartManager(this);
        listenerManager = new ListenerManager(this);
        scoreboardManager = new ScoreboardManager(this);
        velocityMessaging = new VelocityMessaging(this);
        playerTracker = new PlayerTracker();
        discordLinkManager = new DiscordLinkManager(this);
        discordLinkManager.start();
        discordWebhookManager = new DiscordWebhookManager(this);

        // Register plugin messaging
        velocityMessaging.register();

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new AdvancementListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerTrackerListener(this), this);

        // Register commands
        registerCommands();

        // Start scoreboard
        scoreboardManager.startUpdating();

        getLogger().info("LemonCore enabled successfully.");
    }

    @Override
    public void onDisable() {
        // Save all online players
        for (Player p : Bukkit.getOnlinePlayers()) {
            playerDataManager.savePlayer(p.getUniqueId()).join();
        }

        // Stop link polling and webhook scheduler
        if (discordLinkManager != null) discordLinkManager.stop();
        if (discordWebhookManager != null) discordWebhookManager.shutdown();

        // Disconnect database
        if (databaseManager != null) databaseManager.disconnect();

        // Unregister messaging
        if (velocityMessaging != null) velocityMessaging.unregister();

        getLogger().info("LemonCore disabled.");
    }

    private void registerCommands() {
        // Admin commands
        getCommand("grank").setExecutor(luckPerms != null ? new GRankCommand(this, luckPerms) : (s, c, l, a) -> { s.sendMessage("LuckPerms not available."); return true; });
        getCommand("gcoins").setExecutor(new GCoinsCommand(this));
        getCommand("gcoinsall").setExecutor(new GCoinsAllCommand(this));
        getCommand("aowcode").setExecutor(new AowCodeCommand(this));
        getCommand("gban").setExecutor(new GBanCommand(this));
        getCommand("gunban").setExecutor(new GUnbanCommand(this));
        getCommand("gmute").setExecutor(new GMuteCommand(this));
        getCommand("gunmute").setExecutor(new GUnmuteCommand(this));
        getCommand("gkick").setExecutor(new GKickCommand(this));
        getCommand("ghistory").setExecutor(new GHistoryCommand(this));
        getCommand("greport").setExecutor(new GReportCommand(this));
        getCommand("gbug").setExecutor(new GBugCommand(this));
        getCommand("gmreport").setExecutor(new GMReportCommand(this));
        getCommand("gwipe").setExecutor(new GWipeCommand(this));
        getCommand("gunwipe").setExecutor(new GUnwipeCommand(this));
        getCommand("gspec").setExecutor(new GSpecCommand(this));
        getCommand("gsm").setExecutor(new GSmCommand(this));
        getCommand("gtp").setExecutor(new GTPCommand(this));
        getCommand("gspawn").setExecutor(new GSpawnCommand(this));
        getCommand("aowsetlobby").setExecutor(new AowSetLobbyCommand(this));
        getCommand("restart").setExecutor(new RestartCommand(this));
        getCommand("gpop").setExecutor(new GPopCommand(this));
        getCommand("gcheck").setExecutor(new GCheckCommand(this));
        getCommand("linked").setExecutor(new LinkedCommand(this));
        getCommand("aowm").setExecutor(new AOWMCommand(this));

        // User commands
        getCommand("rank").setExecutor(luckPerms != null ? new RankCommand(this, luckPerms) : (s, c, l, a) -> { s.sendMessage("LuckPerms not available."); return true; });
        getCommand("coins").setExecutor(new CoinsCommand(this));
        getCommand("code").setExecutor(luckPerms != null ? new CodeCommand(this, luckPerms) : new CodeCommand(this, null));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("settings").setExecutor(new SettingsCommand(this));
        getCommand("friend").setExecutor(new FriendCommand(this));
        getCommand("recording").setExecutor(new RecordingCommand(this));
        getCommand("nick").setExecutor(new NickCommand(this));
        getCommand("hide").setExecutor(new HideCommand(this));
        getCommand("lobby").setExecutor(new LobbyCommand(this));
        getCommand("fly").setExecutor(new FlyCommand(this));
        getCommand("link").setExecutor(new LinkCommand(this));
        getCommand("unlink").setExecutor(new UnlinkCommand(this));
        getCommand("report").setExecutor(new ReportCommand(this));
        getCommand("bugreport").setExecutor(new BugReportCommand(this));
        getCommand("mreport").setExecutor(new MReportCommand(this));
    }

    public void teleportToLobby(Player player) {
        String worldName = getConfig().getString("lobby.spawn.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(messagesManager.get("teleport.spawn"));
            return;
        }
        double x = getConfig().getDouble("lobby.spawn.x", 0.5);
        double y = getConfig().getDouble("lobby.spawn.y", 64.0);
        double z = getConfig().getDouble("lobby.spawn.z", 0.5);
        float yaw = (float) getConfig().getDouble("lobby.spawn.yaw", 0.0);
        float pitch = (float) getConfig().getDouble("lobby.spawn.pitch", 0.0);
        player.teleport(new Location(world, x, y, z, yaw, pitch));
    }

    // Getters
    public org.bukkit.configuration.file.FileConfiguration getServersConfig() { return serversConfig; }
    private void loadServersConfig() {
        java.io.File f = new java.io.File(getDataFolder(), "servers.yml");
        if (!f.exists()) saveResource("servers.yml", false);
        serversConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
    }

    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public MessagesManager getMessagesManager() { return messagesManager; }
    public FilterManager getFilterManager() { return filterManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public BanManager getBanManager() { return banManager; }
    public MuteManager getMuteManager() { return muteManager; }
    public CodeManager getCodeManager() { return codeManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public ReportManager getReportManager() { return reportManager; }
    public OffenseManager getOffenseManager() { return offenseManager; }
    public RestartManager getRestartManager() { return restartManager; }
    public ListenerManager getListenerManager() { return listenerManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public VelocityMessaging getVelocityMessaging() { return velocityMessaging; }
    public RandomNameUtil getRandomNameUtil() { return randomNameUtil; }
    public PlayerTracker getPlayerTracker() { return playerTracker; }
    public DiscordLinkManager getDiscordLinkManager() { return discordLinkManager; }
    public DiscordWebhookManager getDiscordWebhookManager() { return discordWebhookManager; }
    public MaintenanceManager getMaintenanceManager() { return maintenanceManager; }
    public LuckPerms getLuckPerms() { return luckPerms; }
}
