package de.lemonpvp.bettersmp;

import de.lemonpvp.bettersmp.api.BetterSMPApi;
import de.lemonpvp.bettersmp.board.BoardListener;
import de.lemonpvp.bettersmp.board.BoardManager;
import de.lemonpvp.bettersmp.chat.ChatModule;
import de.lemonpvp.bettersmp.combat.CombatListener;
import de.lemonpvp.bettersmp.combat.CombatManager;
import de.lemonpvp.bettersmp.command.BetterSMPCommand;
import de.lemonpvp.bettersmp.command.SettingsCommand;
import de.lemonpvp.bettersmp.gui.SettingsListener;
import de.lemonpvp.bettersmp.hook.EconomyHook;
import de.lemonpvp.bettersmp.hook.LuckPermsHook;
import de.lemonpvp.bettersmp.hook.PapiHook;
import de.lemonpvp.bettersmp.join.JoinModule;
import de.lemonpvp.bettersmp.punish.PunishmentCommands;
import de.lemonpvp.bettersmp.punish.PunishmentConfig;
import de.lemonpvp.bettersmp.punish.PunishmentListener;
import de.lemonpvp.bettersmp.punish.PunishmentManager;
import de.lemonpvp.bettersmp.respawn.DeathRedirectListener;
import de.lemonpvp.bettersmp.setup.ConfigDeployer;
import de.lemonpvp.bettersmp.setup.Installer;
import de.lemonpvp.bettersmp.setup.RankSetup;
import de.lemonpvp.bettersmp.stats.StatsCommand;
import de.lemonpvp.bettersmp.stats.StatsListener;
import de.lemonpvp.bettersmp.stats.StatsManager;
import de.lemonpvp.bettersmp.storage.Database;
import de.lemonpvp.bettersmp.util.Msgs;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * BetterSMP - SMP-Kernplugin.
 *
 * Chat, NoChatReports, AntiCombatLog, Ban/Mute-System mit Screen, Stats
 * (MariaDB/SQLite), Ränge mit Gradient-Prefixen, Nametags und Scoreboard -
 * plus Auto-Installer für die Begleit-Plugins.
 */
public final class BetterSMP extends JavaPlugin {

    private Msgs msgs;
    private LuckPermsHook luckPerms;
    private PapiHook papi;
    private EconomyHook economy;
    private CombatManager combat;
    private Installer installer;
    private ConfigDeployer configDeployer;
    private RankSetup rankSetup;

    private Database database;
    private PunishmentConfig punishConfig;
    private PunishmentManager punishments;
    private StatsManager stats;
    private BoardManager board;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.msgs = new Msgs(this);
        this.luckPerms = new LuckPermsHook();
        this.papi = new PapiHook();
        this.economy = new EconomyHook();
        this.combat = new CombatManager(this);
        this.installer = new Installer(this);
        this.configDeployer = new ConfigDeployer(this);
        this.rankSetup = new RankSetup(this);

        // Datenbank + darauf aufbauende Systeme
        this.database = new Database(this);
        database.init();
        this.punishConfig = new PunishmentConfig(this);
        this.punishments = new PunishmentManager(this, punishConfig);
        this.stats = new StatsManager(this);
        this.board = new BoardManager(this);

        BetterSMPApi.init(combat);
        combat.start();
        stats.start();
        board.start();

        // Listener
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new ChatModule(this), this);
        pm.registerEvents(new CombatListener(this, combat), this);
        pm.registerEvents(new JoinModule(this), this);
        pm.registerEvents(new SettingsListener(this), this);
        pm.registerEvents(new PunishmentListener(this), this);
        pm.registerEvents(new StatsListener(this), this);
        pm.registerEvents(new BoardListener(this), this);
        pm.registerEvents(new DeathRedirectListener(this), this);

        // Fuer die Tod-Umleitung - unabhaengig vom Schalter registriert,
        // damit ein spaeteres Einschalten per /bettersmp reload sofort
        // funktioniert und nicht erst nach einem vollen Serverneustart.
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Befehle
        getCommand("bettersmp").setExecutor(new BetterSMPCommand(this));
        getCommand("settings").setExecutor(new SettingsCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        PunishmentCommands punishmentCommands = new PunishmentCommands(this);
        for (String cmd : new String[]{"gban", "gunban", "gmute", "gunmute"}) {
            getCommand(cmd).setExecutor(punishmentCommands);
        }

        logHooks();
        Bukkit.getScheduler().runTaskLater(this, this::firstRunSetup, 40L);
        getLogger().info("BetterSMP aktiviert.");
    }

    @Override
    public void onDisable() {
        if (combat != null) combat.stop();
        if (stats != null) stats.stop();
        if (board != null) board.stop();
        if (database != null) database.shutdown();
    }

    private void logHooks() {
        getLogger().info("LuckPerms: " + (luckPerms.isAvailable() ? "verbunden" : "nicht gefunden"));
        getLogger().info("PlaceholderAPI: " + (papi.isAvailable() ? "verbunden" : "nicht gefunden"));
        getLogger().info("Vault-Economy: " + (economy.isEnabled() ? "verbunden" : "nicht gefunden"));
    }

    private void firstRunSetup() {
        CommandSender console = Bukkit.getConsoleSender();
        if (getConfig().getBoolean("patch-server-properties", true)) {
            configDeployer.patchServerProperties(console);
        }
        if (getConfig().getBoolean("installer.deploy-configs", true)) {
            configDeployer.deployAll(console);
        }
        if (getConfig().getBoolean("installer.enabled", true)
                && getConfig().getBoolean("installer.auto-install-on-start", true)) {
            installer.installAsync(console);
        }
    }

    public void reloadModules() {
        punishConfig.reload();
        board.loadBoardConfig();
    }

    public void setupRanks(CommandSender feedback) {
        rankSetup.run(feedback);
    }

    /** Server-Name aus der Config (Platzhalter %brand%). */
    public String brand() {
        return getConfig().getString("brand", "SMP");
    }

    public Msgs msgs() {
        return msgs;
    }

    public LuckPermsHook luckPerms() {
        return luckPerms;
    }

    public PapiHook papi() {
        return papi;
    }

    public EconomyHook economy() {
        return economy;
    }

    public CombatManager combat() {
        return combat;
    }

    public Installer installer() {
        return installer;
    }

    public ConfigDeployer configDeployer() {
        return configDeployer;
    }

    public Database database() {
        return database;
    }

    public PunishmentManager punishments() {
        return punishments;
    }

    public StatsManager stats() {
        return stats;
    }

    public BoardManager board() {
        return board;
    }
}
