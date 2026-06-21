package com.lemonpvp.lemonevents;

import com.lemonpvp.lemonevents.commands.*;
import com.lemonpvp.lemonevents.database.EventDatabase;
import com.lemonpvp.lemonevents.listeners.EventPlayerListener;
import com.lemonpvp.lemonevents.managers.*;
import com.lemonpvp.lemonevents.messaging.EventMessaging;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class LemonEvents extends JavaPlugin {

    private EventDatabase database;
    private EventManager eventManager;
    private AnnouncementManager announcementManager;
    private LootManager lootManager;
    private MapManager mapManager;
    private EventMessaging messaging;

    private FileConfiguration eventsConfig;
    private FileConfiguration lootConfig;
    private FileConfiguration serversConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadServersConfig();
        saveResourceIfAbsent("events.yml");
        saveResourceIfAbsent("lootTables.yml");
        loadExtraConfigs();

        database = new EventDatabase(this);
        try {
            database.connect();
        } catch (Exception e) {
            getLogger().severe("Failed to connect to database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        messaging        = new EventMessaging(this);
        mapManager       = new MapManager(this);
        lootManager      = new LootManager(this);
        eventManager     = new EventManager(this);
        announcementManager = new AnnouncementManager(this);

        messaging.register();
        lootManager.load();
        eventManager.loadAll();
        announcementManager.start();

        getServer().getPluginManager().registerEvents(new EventPlayerListener(this), this);

        registerCommands();

        getLogger().info("LemonEvents enabled.");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) eventManager.endAllActiveGames();
        if (announcementManager != null) announcementManager.stop();
        if (messaging != null) messaging.unregister();
        if (database != null) database.disconnect();
        getLogger().info("LemonEvents disabled.");
    }

    private void registerCommands() {
        var create = getCommand("aowcreateevent");
        var join   = getCommand("joinevent");
        var start  = getCommand("aowstartevent");
        var end    = getCommand("aowendevent");
        var ffa    = getCommand("aowffaevent");
        var effa   = getCommand("aoweventffa");

        if (create != null) create.setExecutor(new CreateEventCommand(this));
        if (join   != null) join.setExecutor(new JoinEventCommand(this));
        if (start  != null) start.setExecutor(new StartEventCommand(this));
        if (end    != null) end.setExecutor(new EndEventCommand(this));
        if (ffa    != null) ffa.setExecutor(new FFAEventCommand(this));
        if (effa   != null) effa.setExecutor(new EventFFACommand(this));
    }

    private void loadExtraConfigs() {
        File eventsFile = new File(getDataFolder(), "events.yml");
        eventsConfig = YamlConfiguration.loadConfiguration(eventsFile);

        File lootFile = new File(getDataFolder(), "lootTables.yml");
        lootConfig = YamlConfiguration.loadConfiguration(lootFile);
    }

    private void saveResourceIfAbsent(String name) {
        File f = new File(getDataFolder(), name);
        if (!f.exists()) saveResource(name, false);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public EventDatabase getDatabase() { return database; }
    public EventManager getEventManager() { return eventManager; }
    public AnnouncementManager getAnnouncementManager() { return announcementManager; }
    public LootManager getLootManager() { return lootManager; }
    public MapManager getMapManager() { return mapManager; }
    public EventMessaging getMessaging() { return messaging; }
    public FileConfiguration getServersConfig() { return serversConfig; }
    private void loadServersConfig() {
        java.io.File f = new java.io.File(getDataFolder(), "servers.yml");
        if (!f.exists()) saveResource("servers.yml", false);
        serversConfig = YamlConfiguration.loadConfiguration(f);
    }

    public FileConfiguration getEventsConfig() { return eventsConfig; }
    public FileConfiguration getLootConfig() { return lootConfig; }
}
