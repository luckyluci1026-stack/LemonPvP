package de.lemonpvp.bettersmp;

import de.lemonpvp.bettersmp.api.BetterSMPApi;
import de.lemonpvp.bettersmp.chat.ChatModule;
import de.lemonpvp.bettersmp.combat.CombatListener;
import de.lemonpvp.bettersmp.combat.CombatManager;
import de.lemonpvp.bettersmp.command.BetterSMPCommand;
import de.lemonpvp.bettersmp.command.SettingsCommand;
import de.lemonpvp.bettersmp.gui.SettingsListener;
import de.lemonpvp.bettersmp.hook.LuckPermsHook;
import de.lemonpvp.bettersmp.hook.PapiHook;
import de.lemonpvp.bettersmp.join.JoinModule;
import de.lemonpvp.bettersmp.setup.ConfigDeployer;
import de.lemonpvp.bettersmp.setup.Installer;
import de.lemonpvp.bettersmp.setup.RankSetup;
import de.lemonpvp.bettersmp.util.Msgs;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * BetterSMP - SMP-Kernplugin fuer LemonPvP.
 *
 * Features:
 *  - LPC-MiniMessage-Chat mit LuckPerms-Prefixen + PlaceholderAPI
 *  - NoChatReports (Chat als System-Nachricht + server.properties-Patch)
 *  - AntiCombatLog mit CombatLog-API-Event (fuer Lifesteal+)
 *  - Join/Quit/MOTD/Erst-Join-Titel
 *  - Auto-Installer + fertige Configs fuer EssentialsX, LuckPerms, Vault,
 *    PlaceholderAPI und TAB
 *  - /settings-GUI zum Live-Umschalten aller Module
 */
public final class BetterSMP extends JavaPlugin {

    private Msgs msgs;
    private LuckPermsHook luckPerms;
    private PapiHook papi;
    private CombatManager combat;
    private Installer installer;
    private ConfigDeployer configDeployer;
    private RankSetup rankSetup;

    private ChatModule chatModule;
    private CombatListener combatListener;
    private JoinModule joinModule;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.msgs = new Msgs(this);
        this.luckPerms = new LuckPermsHook();
        this.papi = new PapiHook();
        this.combat = new CombatManager(this);
        this.installer = new Installer(this);
        this.configDeployer = new ConfigDeployer(this);
        this.rankSetup = new RankSetup(this);

        BetterSMPApi.init(combat);
        combat.start();

        // Module registrieren
        this.chatModule = new ChatModule(this);
        this.combatListener = new CombatListener(this, combat);
        this.joinModule = new JoinModule(this);
        Bukkit.getPluginManager().registerEvents(chatModule, this);
        Bukkit.getPluginManager().registerEvents(combatListener, this);
        Bukkit.getPluginManager().registerEvents(joinModule, this);
        Bukkit.getPluginManager().registerEvents(new SettingsListener(this), this);

        // Befehle
        getCommand("bettersmp").setExecutor(new BetterSMPCommand(this));
        getCommand("settings").setExecutor(new SettingsCommand(this));

        logHooks();

        // Erst-Einrichtung nach dem vollstaendigen Serverstart
        Bukkit.getScheduler().runTaskLater(this, this::firstRunSetup, 40L);

        getLogger().info("BetterSMP aktiviert - viel Spass auf LemonPvP!");
    }

    @Override
    public void onDisable() {
        if (combat != null) {
            combat.stop();
        }
    }

    private void logHooks() {
        getLogger().info("LuckPerms: " + (luckPerms.isAvailable() ? "verbunden" : "nicht gefunden"));
        getLogger().info("PlaceholderAPI: " + (papi.isAvailable() ? "verbunden" : "nicht gefunden"));
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

    /** Registriert Module neu (nach Config-Reload / Toggle). Listener bleiben aktiv,
     *  ihr Verhalten haengt live an der Config. */
    public void reloadModules() {
        // Module lesen ihre Schalter direkt aus der Config - hier nichts weiter noetig.
        // Methode existiert als klarer Hook fuer zukuenftige Erweiterungen.
    }

    public void setupRanks(CommandSender feedback) {
        rankSetup.run(feedback);
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

    public CombatManager combat() {
        return combat;
    }

    public Installer installer() {
        return installer;
    }

    public ConfigDeployer configDeployer() {
        return configDeployer;
    }
}
