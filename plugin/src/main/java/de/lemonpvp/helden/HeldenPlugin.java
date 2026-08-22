package de.lemonpvp.helden;

import de.lemonpvp.helden.combat.CombatManager;
import de.lemonpvp.helden.combat.DummyManager;
import de.lemonpvp.helden.command.AdminCommand;
import de.lemonpvp.helden.command.BaseCommand;
import de.lemonpvp.helden.command.HerzenCommand;
import de.lemonpvp.helden.command.PlayerListCommand;
import de.lemonpvp.helden.command.StatsCommand;
import de.lemonpvp.helden.config.Messages;
import de.lemonpvp.helden.config.Settings;
import de.lemonpvp.helden.game.GameManager;
import de.lemonpvp.helden.heart.HeartManager;
import de.lemonpvp.helden.heart.LinkHeartManager;
import de.lemonpvp.helden.hud.HudManager;
import de.lemonpvp.helden.item.ItemRegistry;
import de.lemonpvp.helden.listener.CombatListener;
import de.lemonpvp.helden.listener.ConnectionListener;
import de.lemonpvp.helden.listener.DeathListener;
import de.lemonpvp.helden.listener.DummyListener;
import de.lemonpvp.helden.listener.InteractListener;
import de.lemonpvp.helden.listener.MenuListener;
import de.lemonpvp.helden.player.ProfileManager;
import de.lemonpvp.helden.storage.Storage;
import de.lemonpvp.helden.storage.YamlStorage;
import de.lemonpvp.helden.util.BedrockSupport;
import de.lemonpvp.helden.util.Keys;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Helden 3 - Herzen-Projekt nach dem Regelwerk von Minecraft Helden.
 *
 * <p>Jeder startet mit drei eigenen Herzen plus einem geteilten Link-Herz.
 * Herzen gehen ausschliesslich an andere Spieler verloren; wer im Kampf
 * ausloggt, hinterlaesst eine Puppe mit seiner Ausruestung. Bei null Herzen ist
 * man raus - der Letzte im Rennen gewinnt.</p>
 *
 * <p>Java- und Bedrock-Spieler werden gleich behandelt: die Oberflaechen sind
 * Kisten-GUIs (Geyser uebersetzt sie automatisch) und Farben werden fuer
 * Bedrock-Clients auf Legacy-Farben reduziert.</p>
 */
public final class HeldenPlugin extends JavaPlugin {

    private Settings settings;
    private BedrockSupport bedrock;
    private Messages messages;

    private ItemRegistry items;
    private ProfileManager profiles;
    private HeartManager hearts;
    private LinkHeartManager links;
    private GameManager game;
    private CombatManager combat;
    private DummyManager dummies;
    private HudManager hud;

    private BukkitTask autosaveTask;

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

        items = new ItemRegistry(this);
        items.reload();

        Storage storage = new YamlStorage(this);
        profiles = new ProfileManager(storage, settings);
        profiles.loadAll();

        hearts = new HeartManager(this);
        links = new LinkHeartManager(this);
        game = new GameManager(this);
        combat = new CombatManager(this);
        dummies = new DummyManager(this);
        hud = new HudManager(this);

        registerListeners();
        registerCommands();
        startTasks();

        // Puppen, die einen Absturz ueberlebt haben, gehoeren nicht in die Welt.
        int orphans = dummies.removeOrphans();
        if (orphans > 0) {
            getLogger().info("Verwaiste Dummies entfernt: " + orphans);
        }

        // Nach einem /reload sind schon Spieler online.
        for (Player player : getServer().getOnlinePlayers()) {
            profiles.getOrCreate(player);
            hud.setup(player);
            game.enforceState(player);
        }

        getLogger().info("Helden 3 bereit - " + settings.totalStartHearts() + " Herzen pro Spieler"
                + (settings.linkHeartEnabled() ? " (inkl. Link-Herz)" : "") + ".");
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
        if (dummies != null) {
            dummies.stop();
            dummies.removeAll();
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
        getServer().getPluginManager().registerEvents(new DummyListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
    }

    private void registerCommands() {
        register("herzen", new HerzenCommand(this));
        register("teilnehmer", new PlayerListCommand(this));
        register("stats", new StatsCommand(this));
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
        hud.start();
        dummies.start();
    }

    private void stopTasks() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    /** Laedt alle Konfigurationen neu und baut die laufenden Aufgaben neu auf. */
    public void reloadAll() {
        reloadConfig();
        settings.load(getConfig());
        bedrock.configure(settings.detectFloodgate(), settings.bedrockUsernamePrefix(), settings.stripHexColors());
        messages.reload();
        items.reload();

        hud.stop();
        for (Player player : getServer().getOnlinePlayers()) {
            hud.remove(player);
        }
        dummies.stop();
        startTasks();
        for (Player player : getServer().getOnlinePlayers()) {
            hud.setup(player);
            hearts.apply(player);
        }
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

    public ProfileManager profiles() {
        return profiles;
    }

    public HeartManager hearts() {
        return hearts;
    }

    public LinkHeartManager links() {
        return links;
    }

    public GameManager game() {
        return game;
    }

    public CombatManager combat() {
        return combat;
    }

    public DummyManager dummies() {
        return dummies;
    }

    public HudManager hud() {
        return hud;
    }
}
