package de.lemonpvp.helden;

import de.lemonpvp.helden.combat.CombatManager;
import de.lemonpvp.helden.command.AbilityCommand;
import de.lemonpvp.helden.command.AdminCommand;
import de.lemonpvp.helden.command.BaseCommand;
import de.lemonpvp.helden.command.HeldCommand;
import de.lemonpvp.helden.command.LivesCommand;
import de.lemonpvp.helden.command.ReviveCommand;
import de.lemonpvp.helden.command.ShopCommand;
import de.lemonpvp.helden.command.StatsCommand;
import de.lemonpvp.helden.command.TeamCommand;
import de.lemonpvp.helden.config.ConfigFile;
import de.lemonpvp.helden.config.Messages;
import de.lemonpvp.helden.config.Settings;
import de.lemonpvp.helden.economy.EconomyManager;
import de.lemonpvp.helden.gameevent.GameEventManager;
import de.lemonpvp.helden.hero.HeroManager;
import de.lemonpvp.helden.hero.ability.AbilityManager;
import de.lemonpvp.helden.hud.HudManager;
import de.lemonpvp.helden.item.ItemRegistry;
import de.lemonpvp.helden.listener.CombatListener;
import de.lemonpvp.helden.listener.ConnectionListener;
import de.lemonpvp.helden.listener.DeathListener;
import de.lemonpvp.helden.listener.InteractListener;
import de.lemonpvp.helden.listener.MenuListener;
import de.lemonpvp.helden.lives.LivesManager;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.player.ProfileManager;
import de.lemonpvp.helden.storage.Storage;
import de.lemonpvp.helden.storage.YamlStorage;
import de.lemonpvp.helden.team.HeldenTeam;
import de.lemonpvp.helden.team.TeamManager;
import de.lemonpvp.helden.util.BedrockSupport;
import de.lemonpvp.helden.util.Keys;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Helden 3 - das LemonPvP YouTuber-Projekt.
 *
 * <p>Ein Paper-/Spigot-Plugin, das Java- und Bedrock-Spieler gleich behandelt:
 * die Oberflaechen sind Kisten-GUIs (Geyser uebersetzt sie automatisch), die
 * Farben werden fuer Bedrock reduziert und die Artefakte haben neben der
 * Java-CustomModelData auch einen Bedrock-Identifier fuer das Geyser-Mapping.</p>
 */
public final class HeldenPlugin extends JavaPlugin {

    private Settings settings;
    private BedrockSupport bedrock;
    private Messages messages;
    private ConfigFile shopFile;

    private ItemRegistry items;
    private HeroManager heroes;
    private AbilityManager abilities;
    private ProfileManager profiles;
    private TeamManager teams;
    private CombatManager combat;
    private EconomyManager economy;
    private LivesManager lives;
    private HudManager hud;
    private GameEventManager events;

    private BukkitTask autosaveTask;
    private BukkitTask passiveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Keys.init(this);

        settings = new Settings();
        settings.load(getConfig());

        bedrock = new BedrockSupport(getLogger());
        bedrock.configure(settings.detectFloodgate(), settings.bedrockUsernamePrefix(), settings.stripHexColors());

        messages = new Messages(this, settings, bedrock);
        messages.reload();

        shopFile = new ConfigFile(this, "shop.yml");
        shopFile.reload();

        items = new ItemRegistry(this);
        items.reload();

        abilities = new AbilityManager(this);
        heroes = new HeroManager(this);
        heroes.reload();

        Storage storage = new YamlStorage(this);
        profiles = new ProfileManager(storage, settings);
        profiles.loadAll();

        teams = new TeamManager(this, profiles);
        teams.reload();

        combat = new CombatManager(this);
        economy = new EconomyManager(this);
        lives = new LivesManager(this);
        hud = new HudManager(this);
        events = new GameEventManager(this);

        abilities.registerDefaults();
        events.registerDefaults();

        registerListeners();
        registerCommands();
        startTasks();

        // Nach einem /reload sind schon Spieler online - die bekommen alles nachgereicht.
        for (Player player : getServer().getOnlinePlayers()) {
            profiles.getOrCreate(player);
            hud.setup(player);
        }

        getLogger().info("Helden 3 ist bereit. Viel Spass beim Dreh!");
    }

    @Override
    public void onDisable() {
        stopTasks();
        if (hud != null) {
            hud.stop();
            for (Player player : getServer().getOnlinePlayers()) {
                hud.remove(player);
            }
        }
        if (events != null) {
            events.stopTask();
        }
        if (profiles != null) {
            profiles.saveAll();
            getLogger().info("Spielerdaten gespeichert: " + profiles.size());
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
    }

    private void registerCommands() {
        register("held", new HeldCommand(this));
        register("faehigkeit", new AbilityCommand(this));
        register("team", new TeamCommand(this));
        register("leben", new LivesCommand(this));
        register("wiederbeleben", new ReviveCommand(this));
        register("stats", new StatsCommand(this));
        register("shop", new ShopCommand(this));
        register("helden3", new AdminCommand(this));
    }

    private void register(String name, BaseCommand command) {
        PluginCommand pluginCommand = getCommand(name);
        if (pluginCommand == null) {
            getLogger().warning("Befehl '" + name + "' fehlt in der plugin.yml.");
            return;
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }

    private void startTasks() {
        stopTasks();

        long autosaveTicks = settings.autosaveSeconds() * 20L;
        autosaveTask = getServer().getScheduler().runTaskTimerAsynchronously(
                this, () -> profiles.saveAll(), autosaveTicks, autosaveTicks);

        long passiveTicks = settings.passiveRefreshSeconds() * 20L;
        passiveTask = getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                HeldenProfile profile = profiles.get(player);
                if (profile != null && !profile.fallen()) {
                    heroes.applyPassives(player);
                }
            }
        }, passiveTicks, passiveTicks);

        hud.start();
        events.start();
    }

    private void stopTasks() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        if (passiveTask != null) {
            passiveTask.cancel();
            passiveTask = null;
        }
    }

    /** Laedt alle Konfigurationen neu und baut die laufenden Aufgaben neu auf. */
    public void reloadAll() {
        reloadConfig();
        settings.load(getConfig());
        bedrock.configure(settings.detectFloodgate(), settings.bedrockUsernamePrefix(), settings.stripHexColors());
        messages.reload();
        shopFile.reload();
        items.reload();
        heroes.reload();
        teams.reload();

        hud.stop();
        for (Player player : getServer().getOnlinePlayers()) {
            hud.remove(player);
        }
        events.stopTask();
        startTasks();
        for (Player player : getServer().getOnlinePlayers()) {
            hud.setup(player);
        }
    }

    /** Teamspawn, sonst Projektspawn, sonst Weltspawn. */
    public Location spawnFor(Player player) {
        HeldenTeam team = teams.of(player);
        if (team != null && team.hasSpawn()) {
            return team.spawn();
        }
        return settings.spawnOrDefault();
    }

    public FileConfiguration shopConfig() {
        return shopFile.get();
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public BedrockSupport bedrock() {
        return bedrock;
    }

    public ItemRegistry items() {
        return items;
    }

    public HeroManager heroes() {
        return heroes;
    }

    public AbilityManager abilities() {
        return abilities;
    }

    public ProfileManager profiles() {
        return profiles;
    }

    public TeamManager teams() {
        return teams;
    }

    public CombatManager combat() {
        return combat;
    }

    public EconomyManager economy() {
        return economy;
    }

    public LivesManager lives() {
        return lives;
    }

    public HudManager hud() {
        return hud;
    }

    public GameEventManager events() {
        return events;
    }
}
