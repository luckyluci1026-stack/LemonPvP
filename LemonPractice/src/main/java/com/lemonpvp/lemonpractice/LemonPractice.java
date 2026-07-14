package com.lemonpvp.lemonpractice;

import com.lemonpvp.lemonpractice.database.PracticeDatabase;
import com.lemonpvp.lemonpractice.managers.ArenaManager;
import com.lemonpvp.lemonpractice.managers.DuelManager;
import com.lemonpvp.lemonpractice.managers.DuelInviteManager;
import com.lemonpvp.lemonpractice.managers.EloManager;
import com.lemonpvp.lemonpractice.managers.FFAManager;
import com.lemonpvp.lemonpractice.managers.KitManager;
import com.lemonpvp.lemonpractice.managers.LobbyHotbarManager;
import com.lemonpvp.lemonpractice.managers.PartyManager;
import com.lemonpvp.lemonpractice.managers.QueueManager;
import com.lemonpvp.lemonpractice.managers.SpectatorManager;
import com.lemonpvp.lemonpractice.game.zone.ZonePracticeManager;
import com.lemonpvp.lemonpractice.velocity.VelocityMessaging;
import com.lemonpvp.lemonpractice.commands.AowArenaCommand;
import com.lemonpvp.lemonpractice.commands.AowBuildSpawnCommand;
import com.lemonpvp.lemonpractice.commands.GEloCommand;
import com.lemonpvp.lemonpractice.commands.LPracticeCommand;
import com.lemonpvp.lemonpractice.commands.DuelCommand;
import com.lemonpvp.lemonpractice.commands.PartyCommand;
import com.lemonpvp.lemonpractice.commands.StatsCommand;
import com.lemonpvp.lemonpractice.commands.ZoneCommand;
import com.lemonpvp.lemonpractice.commands.TopCommand;
import com.lemonpvp.lemonpractice.listeners.DuelListener;
import com.lemonpvp.lemonpractice.listeners.DuelInviteListener;
import com.lemonpvp.lemonpractice.listeners.FFAListener;
import com.lemonpvp.lemonpractice.listeners.KitEditorListener;
import com.lemonpvp.lemonpractice.listeners.LobbyListener;
import com.lemonpvp.lemonpractice.listeners.PartyListener;
import com.lemonpvp.lemonpractice.listeners.ZoneListener;
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
    private com.lemonpvp.lemonpractice.managers.ArenaRollbackManager arenaRollbackManager;
    private com.lemonpvp.lemonpractice.managers.DuelWorldManager duelWorldManager;
    private KitManager kitManager;
    private com.lemonpvp.lemonpractice.managers.KitAdminManager kitAdminManager;
    private EloManager eloManager;
    private com.lemonpvp.lemonpractice.managers.StreakManager streakManager;
    private QueueManager queueManager;
    private DuelManager duelManager;
    private com.lemonpvp.lemonpractice.managers.BotDuelManager botDuelManager;
    private DuelInviteManager duelInviteManager;
    private SpectatorManager spectatorManager;
    private FFAManager ffaManager;
    private LobbyHotbarManager lobbyHotbarManager;
    private PartyManager partyManager;
    private ZonePracticeManager zonePracticeManager;
    private com.lemonpvp.lemonpractice.managers.ReplayManager replayManager;
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
        loadDuelWorldsConfig();
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

        arenaRollbackManager = new com.lemonpvp.lemonpractice.managers.ArenaRollbackManager(this);
        // Vanilla biome duel worlds: load + register one arena per world (DUELS only).
        if (serverType.equals("DUELS")) {
            duelWorldManager = new com.lemonpvp.lemonpractice.managers.DuelWorldManager(this);
            duelWorldManager.loadAndRegister();
            getServer().getPluginManager().registerEvents(
                    new com.lemonpvp.lemonpractice.listeners.ArenaRollbackListener(this), this);
        }

        kitManager = new KitManager(this);
        kitManager.preloadAdminKits(); // load admin-edited presets so they override kits.yml
        kitManager.startAdminKitSync(); // network-wide realtime sync of preset edits
        kitAdminManager = new com.lemonpvp.lemonpractice.managers.KitAdminManager(this);
        eloManager = new EloManager(this);
        streakManager = new com.lemonpvp.lemonpractice.managers.StreakManager(this);
        queueManager = new QueueManager(this);
        queueManager.startTasks();
        duelManager = new DuelManager(this);
        botDuelManager = new com.lemonpvp.lemonpractice.managers.BotDuelManager(this);
        duelInviteManager = new DuelInviteManager(this);
        spectatorManager = new SpectatorManager(this);
        ffaManager = new FFAManager(this);
        ffaManager.loadAll();
        ffaManager.startTasks();
        lobbyHotbarManager = new LobbyHotbarManager(this);
        partyManager = new PartyManager(this);
        zonePracticeManager = new ZonePracticeManager(this);
        replayManager = new com.lemonpvp.lemonpractice.managers.ReplayManager(this);

        // 5. Register VelocityMessaging
        velocityMessaging = new VelocityMessaging(this);
        velocityMessaging.register();

        // 6. Register all listeners on both servers (listeners check serverType internally)
        getServer().getPluginManager().registerEvents(new LobbyListener(this), this);
        getServer().getPluginManager().registerEvents(new KitEditorListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.lemonpvp.lemonpractice.listeners.KitAdminListener(this), this);
        getServer().getPluginManager().registerEvents(new DuelListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.lemonpvp.lemonpractice.listeners.BotDuelListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.lemonpvp.lemonpractice.listeners.PearlCooldownListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.lemonpvp.lemonpractice.listeners.PendingQueueListener(this), this);
        // Reliable bot hit-reg needs PacketEvents to redirect attacks on the fake
        // player-model to the real hitbox. Guarded so the server runs without it.
        if (getServer().getPluginManager().isPluginEnabled("packetevents")) {
            try {
                com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager()
                        .registerListener(new com.lemonpvp.lemonpractice.listeners.BotHitPacketListener(this));
            } catch (Throwable t) {
                getLogger().warning("[Bot] Could not register hit-redirect packet listener: " + t.getMessage());
            }
        }
        getServer().getPluginManager().registerEvents(new FFAListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.lemonpvp.lemonpractice.listeners.KitPreloadListener(this), this);
        getServer().getPluginManager().registerEvents(new DuelInviteListener(this), this);
        getServer().getPluginManager().registerEvents(new PartyListener(this), this);
        getServer().getPluginManager().registerEvents(new ZoneListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.lemonpvp.lemonpractice.listeners.ReplayHitListener(this), this);

        // 7. Register commands
        AowArenaCommand arenaCmd = new AowArenaCommand(this);
        getCommand("aowarena").setExecutor(arenaCmd);
        getCommand("aowarena").setTabCompleter(arenaCmd);
        getCommand("aowbuildspawn").setExecutor(new AowBuildSpawnCommand(this));
        GEloCommand eloCmd = new GEloCommand(this);
        getCommand("gelo").setExecutor(eloCmd);
        getCommand("gelo").setTabCompleter(eloCmd);
        LPracticeCommand lpCmd = new LPracticeCommand(this);
        getCommand("lpractice").setExecutor(lpCmd);
        getCommand("lpractice").setTabCompleter(lpCmd);
        DuelCommand duelCmd = new DuelCommand(this);
        getCommand("duel").setExecutor(duelCmd);
        getCommand("duel").setTabCompleter(duelCmd);
        StatsCommand statsCmd = new StatsCommand(this);
        getCommand("stats").setExecutor(statsCmd);
        getCommand("stats").setTabCompleter(statsCmd);
        TopCommand topCmd = new TopCommand(this);
        getCommand("top").setExecutor(topCmd);
        var leaderboardCmd = getCommand("leaderboard");
        if (leaderboardCmd != null) leaderboardCmd.setExecutor(topCmd);
        PartyCommand partyCmd = new PartyCommand(this);
        getCommand("party").setExecutor(partyCmd);
        getCommand("party").setTabCompleter(partyCmd);
        getCommand("p").setExecutor(partyCmd);
        getCommand("p").setTabCompleter(partyCmd);
        getCommand("partychat").setExecutor(partyCmd);
        getCommand("partychat").setTabCompleter(partyCmd);
        ZoneCommand zoneCmd = new ZoneCommand(this);
        getCommand("zone").setExecutor(zoneCmd);
        getCommand("zone").setTabCompleter(zoneCmd);
        var ffaCmd = new com.lemonpvp.lemonpractice.commands.FfaCommand(this);
        var ffaCommand = getCommand("ffa");
        if (ffaCommand != null) {
            ffaCommand.setExecutor(ffaCmd);
            ffaCommand.setTabCompleter(ffaCmd);
        }
        var replayCmd = getCommand("replay");
        if (replayCmd != null) {
            var replayExec = new com.lemonpvp.lemonpractice.commands.ReplayCommand(this);
            replayCmd.setExecutor(replayExec);
            replayCmd.setTabCompleter(replayExec);
        }
        var greplayCmd = getCommand("greplay");
        if (greplayCmd != null) {
            var greplayExec = new com.lemonpvp.lemonpractice.commands.GReplayCommand(this);
            greplayCmd.setExecutor(greplayExec);
            greplayCmd.setTabCompleter(greplayExec);
        }
        var kitEditorCmd = getCommand("kiteditor");
        if (kitEditorCmd != null) kitEditorCmd.setExecutor(new com.lemonpvp.lemonpractice.commands.KitEditorCommand(this));
        var kitAdminCmd = getCommand("kitadmin");
        if (kitAdminCmd != null) {
            var exec = new com.lemonpvp.lemonpractice.commands.KitAdminCommand(this);
            kitAdminCmd.setExecutor(exec);
            kitAdminCmd.setTabCompleter(exec);
        }

        // Replay retention cleanup (every 6 hours; first run after 1 min)
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> replayManager.purgeExpired(), 1200L, 6L * 60L * 60L * 20L);

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
        if (botDuelManager != null) {
            botDuelManager.shutdown();
        }
        if (arenaRollbackManager != null) {
            arenaRollbackManager.shutdown(); // heal any in-progress arenas before stopping
        }
        if (zonePracticeManager != null) {
            zonePracticeManager.shutdown();
        }
        if (ffaManager != null) {
            ffaManager.shutdown();
        }
        if (replayManager != null) {
            replayManager.shutdown();
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
    public com.lemonpvp.lemonpractice.managers.ArenaRollbackManager getArenaRollbackManager() { return arenaRollbackManager; }
    public com.lemonpvp.lemonpractice.managers.DuelWorldManager getDuelWorldManager() { return duelWorldManager; }
    public KitManager getKitManager() { return kitManager; }
    public com.lemonpvp.lemonpractice.managers.KitAdminManager getKitAdminManager() { return kitAdminManager; }
    public EloManager getEloManager() { return eloManager; }
    public com.lemonpvp.lemonpractice.managers.StreakManager getStreakManager() { return streakManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public DuelManager getDuelManager() { return duelManager; }
    public com.lemonpvp.lemonpractice.managers.BotDuelManager getBotDuelManager() { return botDuelManager; }
    public DuelInviteManager getDuelInviteManager() { return duelInviteManager; }
    public SpectatorManager getSpectatorManager() { return spectatorManager; }
    public FFAManager getFfaManager() { return ffaManager; }
    public LobbyHotbarManager getLobbyHotbarManager() { return lobbyHotbarManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public ZonePracticeManager getZonePracticeManager() { return zonePracticeManager; }
    public com.lemonpvp.lemonpractice.managers.ReplayManager getReplayManager() { return replayManager; }
    public FileConfiguration getServersConfig() { return serversConfig; }
    private void loadServersConfig() {
        java.io.File f = new java.io.File(getDataFolder(), "servers.yml");
        if (!f.exists()) saveResource("servers.yml", false);
        serversConfig = YamlConfiguration.loadConfiguration(f);
    }

    private FileConfiguration duelWorldsConfig;
    /** Vanilla duel-world list — its own file so no DB credentials sit in it. */
    public FileConfiguration getDuelWorldsConfig() { return duelWorldsConfig; }
    private void loadDuelWorldsConfig() {
        java.io.File f = new java.io.File(getDataFolder(), "duel-worlds.yml");
        if (!f.exists()) saveResource("duel-worlds.yml", false);
        duelWorldsConfig = YamlConfiguration.loadConfiguration(f);
    }

    public VelocityMessaging getVelocityMessaging() { return velocityMessaging; }
    public String getServerType() { return serverType; }

    public FileConfiguration getKitsConfig() { return kitsConfig; }
    public FileConfiguration getGamemodesConfig() { return gamemodesConfig; }
    public FileConfiguration getArenasConfig() { return arenasConfig; }
    public FileConfiguration getMessagesConfig() { return messagesConfig; }
}
