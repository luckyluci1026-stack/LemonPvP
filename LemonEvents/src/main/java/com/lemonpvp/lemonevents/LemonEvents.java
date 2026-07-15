package com.lemonpvp.lemonevents;

import com.lemonpvp.lemonevents.commands.*;
import com.lemonpvp.lemonevents.database.EventDatabase;
import com.lemonpvp.lemonevents.listeners.EventPlayerListener;
import com.lemonpvp.lemonevents.managers.*;
import com.lemonpvp.lemonevents.messaging.EventMessaging;
import com.lemonpvp.lemonevents.model.EventStatus;
import com.lemonpvp.lemonevents.model.EventType;
import com.lemonpvp.lemonevents.model.GameEvent;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LemonEvents extends JavaPlugin {

    private EventDatabase database;
    private EventManager eventManager;
    private AnnouncementManager announcementManager;
    private LootManager lootManager;
    private MapManager mapManager;
    private EventMessaging messaging;
    private HostedEventManager hostedEventManager;
    private TournamentManager tournamentManager;
    private com.lemonpvp.lemonevents.util.ChatInputManager chatInputManager;

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
        hostedEventManager = new HostedEventManager(this);
        tournamentManager = new TournamentManager(this);
        chatInputManager = new com.lemonpvp.lemonevents.util.ChatInputManager(this);
        getServer().getPluginManager().registerEvents(chatInputManager, this);

        messaging.register();
        lootManager.load();
        eventManager.loadAll();
        tournamentManager.loadAll();
        tournamentManager.start();
        announcementManager.start();

        getServer().getPluginManager().registerEvents(new EventPlayerListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.lemonpvp.lemonevents.listeners.HostedEventListener(this), this);

        registerCommands();

        getLogger().info("LemonEvents enabled.");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) eventManager.endAllActiveGames();
        if (hostedEventManager != null) hostedEventManager.shutdown();
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

        var hostCmd = getCommand("host");
        if (hostCmd != null) {
            var hostExec = new HostCommand(this);
            hostCmd.setExecutor(hostExec);
            hostCmd.setTabCompleter(hostExec);
        }
        var tournamentCmd = getCommand("tournament");
        if (tournamentCmd != null) {
            var tExec = new TournamentCommand(this);
            tournamentCmd.setExecutor(tExec);
            tournamentCmd.setTabCompleter(tExec);
        }
        var panelCmd = getCommand("eventpanel");
        if (panelCmd != null) panelCmd.setExecutor(new EventPanelCommand(this));

        // Tab completion: /aowcreateevent <name> <type> ..., others take an event name
        if (create != null) create.setTabCompleter((TabCompleter) (s, c, l, a) ->
                a.length == 2 ? filterEnum(EventType.values(), a[1]) : List.of());
        if (join  != null) join.setTabCompleter((TabCompleter) (s, c, l, a) ->
                a.length == 1 ? eventNames(EventStatus.WAITING, a[0]) : List.of());
        if (start != null) start.setTabCompleter((TabCompleter) (s, c, l, a) ->
                a.length == 1 ? eventNames(EventStatus.WAITING, a[0]) : List.of());
        if (end   != null) end.setTabCompleter((TabCompleter) (s, c, l, a) ->
                a.length == 1 ? eventNames(EventStatus.ACTIVE, a[0]) : List.of());
        if (ffa   != null) ffa.setTabCompleter((TabCompleter) (s, c, l, a) ->
                a.length == 1 ? eventNames(null, a[0]) : List.of());
    }

    /** Suggests event names, optionally filtered to a status, matching the typed prefix. */
    private List<String> eventNames(EventStatus status, String prefix) {
        if (eventManager == null) return List.of();
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (GameEvent e : eventManager.getAllEvents()) {
            if (status != null && e.getStatus() != status) continue;
            if (e.getName().toLowerCase(Locale.ROOT).startsWith(lower)) out.add(e.getName());
        }
        return out;
    }

    private static List<String> filterEnum(Enum<?>[] values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (Enum<?> v : values) if (v.name().toLowerCase(Locale.ROOT).startsWith(lower)) out.add(v.name());
        return out;
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
    public HostedEventManager getHostedEventManager() { return hostedEventManager; }
    public TournamentManager getTournamentManager() { return tournamentManager; }
    public com.lemonpvp.lemonevents.util.ChatInputManager getChatInputManager() { return chatInputManager; }

    /** Resolves a player name to a UUID (online first, then LemonCore's store). */
    public java.util.concurrent.CompletableFuture<java.util.UUID> getPlayerUuid(String name) {
        org.bukkit.entity.Player p = getServer().getPlayerExact(name);
        if (p != null) return java.util.concurrent.CompletableFuture.completedFuture(p.getUniqueId());
        org.bukkit.plugin.Plugin lc = getServer().getPluginManager().getPlugin("LemonCore");
        if (lc instanceof com.lemonpvp.lemoncore.LemonCore core) {
            return core.getPlayerDataManager().findUUIDByName(name);
        }
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }
    public FileConfiguration getServersConfig() { return serversConfig; }
    private void loadServersConfig() {
        java.io.File f = new java.io.File(getDataFolder(), "servers.yml");
        if (!f.exists()) saveResource("servers.yml", false);
        serversConfig = YamlConfiguration.loadConfiguration(f);
    }

    public FileConfiguration getEventsConfig() { return eventsConfig; }
    public FileConfiguration getLootConfig() { return lootConfig; }
}
