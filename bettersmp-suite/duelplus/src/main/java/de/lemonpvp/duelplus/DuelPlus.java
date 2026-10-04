package de.lemonpvp.duelplus;

import de.lemonpvp.duelplus.arena.ArenaManager;
import de.lemonpvp.duelplus.arena.RollbackTracker;
import de.lemonpvp.duelplus.command.DrawCommand;
import de.lemonpvp.duelplus.command.DuelCommand;
import de.lemonpvp.duelplus.command.ReplayCommand;
import de.lemonpvp.duelplus.db.DuelDatabase;
import de.lemonpvp.duelplus.item.DuelItemListener;
import de.lemonpvp.duelplus.presence.InventarSperre;
import de.lemonpvp.duelplus.presence.PresenceService;
import de.lemonpvp.duelplus.presence.StammInventarService;
import de.lemonpvp.duelplus.replay.ReplayManager;
import de.lemonpvp.duelplus.request.AnfrageManager;
import de.lemonpvp.duelplus.request.AnfragePollTask;
import de.lemonpvp.duelplus.session.ArenaGuardListener;
import de.lemonpvp.duelplus.session.PortalSperre;
import de.lemonpvp.duelplus.session.DuellChat;
import de.lemonpvp.duelplus.session.DuellSessionManager;
import de.lemonpvp.duelplus.session.TrefferWaechter;
import de.lemonpvp.duelplus.util.KampfPruefung;
import de.lemonpvp.duelplus.util.Msgs;
import de.lemonpvp.duelplus.util.ProxyBridge;
import de.lemonpvp.duelplus.util.ProxySperre;
import de.lemonpvp.duelplus.zuschauer.ZuschauerManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Duell-System mit echtem SMP-Loot ueber drei Server (SMP, Lobby,
 * Duels) - EIN Plugin, dessen Verhalten sich ueber server-name /
 * ist-arena-server in der config.yml je Server unterscheidet.
 *
 * Bewusst ein eigenes, neues Plugin statt Erweiterungen von SMPLobby
 * oder SMPProxy - bestehende Plugins werden nicht angefasst. Fuer den
 * Serverwechsel reicht der Standard-"BungeeCord"-Kanal (siehe
 * ProxyBridge), ein eigenes Velocity-Plugin ist dafuer nicht noetig -
 * nur ein Eintrag fuer den Duels-Server in der velocity.toml.
 */
public final class DuelPlus extends JavaPlugin {

    private Msgs msgs;
    private DuelDatabase db;
    private ProxyBridge bridge;
    private AnfrageManager anfragen;
    private ArenaManager arenaManager;
    private DuellSessionManager sessionManager;
    private TrefferWaechter trefferWaechter;
    private RollbackTracker rollback;
    private ZuschauerManager zuschauer;
    private final KampfPruefung kampf = new KampfPruefung();
    private InventarSperre inventarSperre;
    private ProxySperre proxySperre;
    private DuellChat duellChat;
    private ReplayManager replays;
    private boolean reportPlusKontakte;
    private boolean betterSmpKontakte;
    private StammInventarService stammInventar;

    private String serverName;
    private boolean istArenaServer;
    private String arenaServerName;
    private boolean istLootQuelle;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.msgs = new Msgs(this);
        this.serverName = getConfig().getString("server-name", "SMP");
        this.istArenaServer = getConfig().getBoolean("ist-arena-server", false);
        this.arenaServerName = getConfig().getString("arena-server-name", "Duels");
        this.istLootQuelle = getConfig().getBoolean("ist-loot-quelle", false);

        this.db = new DuelDatabase(this);
        db.init();
        if (!db.bereit()) {
            getLogger().severe("DuelPlus bleibt auf diesem Server inaktiv, bis die MariaDB-Verbindung klappt "
                    + "(siehe config.yml -> database). /duelplus reload versucht es erneut.");
        }

        this.bridge = new ProxyBridge(this);
        this.proxySperre = new ProxySperre(this);
        this.inventarSperre = new InventarSperre(this);
        this.anfragen = new AnfrageManager(this);
        // Auf JEDEM Server instanziiert (anfordern() muss serveruebergreifend
        // per DB funktionieren, siehe ZuschauerManager), aber ihr
        // PlayerJoinEvent-Handler wird bewusst nur unten, im istArenaServer-
        // Zweig, als Listener registriert - angewendet wird ein Zuschauer-
        // Status ohnehin nur dort, wo die Arena-Welten tatsaechlich existieren.
        this.zuschauer = new ZuschauerManager(this);
        this.replays = new ReplayManager(this);
        replays.meldungenAbhoeren();

        getServer().getPluginManager().registerEvents(new PresenceService(this), this);
        if (istLootQuelle) {
            getServer().getPluginManager().registerEvents(inventarSperre, this);
            this.stammInventar = new StammInventarService(this);
            getServer().getPluginManager().registerEvents(stammInventar, this);
        }
        new AnfragePollTask(this).starten();

        if (istArenaServer) {
            this.rollback = new RollbackTracker();
            getServer().getPluginManager().registerEvents(rollback, this);
            this.arenaManager = new ArenaManager(this);
            arenaManager.arenenVorbereiten();
            this.trefferWaechter = new TrefferWaechter(this);
            getServer().getPluginManager().registerEvents(trefferWaechter, this);
            this.sessionManager = new DuellSessionManager(this);
            sessionManager.starten();
            getServer().getPluginManager().registerEvents(new ArenaGuardListener(this), this);
            getServer().getPluginManager().registerEvents(new PortalSperre(this), this);
            this.duellChat = new DuellChat(this);
            getServer().getPluginManager().registerEvents(duellChat, this);
            getServer().getPluginManager().registerEvents(zuschauer, this);
            replays.aufArenaServerStarten();
        } else {
            getServer().getPluginManager().registerEvents(new DuelItemListener(this), this);
        }

        DuelCommand command = new DuelCommand(this);
        var duelCmd = getCommand("duel");
        if (duelCmd != null) {
            duelCmd.setExecutor(command);
            duelCmd.setTabCompleter(command);
        }
        var replayCmd = getCommand("replay");
        if (replayCmd != null) {
            ReplayCommand replayBefehl = new ReplayCommand(this);
            replayCmd.setExecutor(replayBefehl);
            replayCmd.setTabCompleter(replayBefehl);
        }
        var drawCmd = getCommand("draw");
        if (drawCmd != null) {
            drawCmd.setExecutor(new DrawCommand(this));
        }
        var adminCmd = getCommand("duelplus");
        if (adminCmd != null) {
            adminCmd.setExecutor((sender, cmd, label, args) -> {
                db.shutdown();
                db.init();
                reloadConfig();
                this.serverName = getConfig().getString("server-name", "SMP");
                this.istArenaServer = getConfig().getBoolean("ist-arena-server", false);
                this.arenaServerName = getConfig().getString("arena-server-name", "Duels");
                this.istLootQuelle = getConfig().getBoolean("ist-loot-quelle", false);
                msgs.send(sender, "reloaded");
                return true;
            });
        }

        reportPlusKontakte = getServer().getPluginManager().getPlugin("ReportPlus") != null;
        betterSmpKontakte = getServer().getPluginManager().getPlugin("BetterSMP") != null;
        if (istArenaServer && !serverName.equalsIgnoreCase(arenaServerName)) {
            getLogger().warning("DuelPlus: server-name ist \"" + serverName + "\", arena-server-name \"" + arenaServerName
                    + "\" - auf dem Duels-Server muessen beide gleich sein (so, wie der Server in der velocity.toml heisst).");
        }
        getLogger().info("DuelPlus aktiviert (Server: " + serverName
                + ", Rolle: " + (istArenaServer ? "Arena" : "Herkunft")
                + (istArenaServer ? "" : ", Loot-Quelle: " + (istLootQuelle ? "JA" : "nein"))
                + "). Genau EIN Server im Netzwerk (der mit dem echten Loot, i.d.R. SMP) braucht "
                + "'Loot-Quelle: JA' - alle anderen (Lobby, Duels) bleiben bei 'nein'.");
    }

    @Override
    public void onDisable() {
        if (replays != null) {
            replays.stoppen();
        }
        if (stammInventar != null && db != null && db.bereit()) {
            stammInventar.allesSichern();
        }
        if (db != null) {
            db.shutdown();
        }
    }

    public Msgs msgs() {
        return msgs;
    }

    public DuelDatabase db() {
        return db;
    }

    public ProxyBridge bridge() {
        return bridge;
    }

    public AnfrageManager anfragen() {
        return anfragen;
    }

    public ArenaManager arenaManager() {
        return arenaManager;
    }

    public DuellSessionManager sessionManager() {
        return sessionManager;
    }

    public TrefferWaechter trefferWaechter() {
        return trefferWaechter;
    }

    public RollbackTracker rollback() {
        return rollback;
    }

    public ZuschauerManager zuschauer() {
        return zuschauer;
    }

    public String serverName() {
        return serverName;
    }

    public boolean istArenaServer() {
        return istArenaServer;
    }

    public String arenaServerName() {
        return arenaServerName;
    }

    public boolean istLootQuelle() {
        return istLootQuelle;
    }

    public KampfPruefung kampf() {
        return kampf;
    }

    public InventarSperre inventarSperre() {
        return inventarSperre;
    }

    public ProxySperre proxySperre() {
        return proxySperre;
    }

    public DuellChat duellChat() {
        return duellChat;
    }

    public ReplayManager replays() {
        return replays;
    }

    public void gegnerMerken(java.util.UUID spieler, java.util.UUID gegner, String gegnerName) {
        if (reportPlusKontakte) {
            try {
                de.lemonpvp.duelplus.util.ReportPlusKontakt.merken(spieler, gegner, gegnerName);
            } catch (LinkageError | RuntimeException fehler) {
                reportPlusKontakte = false;
                getLogger().warning("ReportPlus ist zu alt fuer 'letzte Duell-Gegner' - bitte die aktuelle ReportPlus-Jar einspielen.");
            }
        }
        if (betterSmpKontakte) {
            try {
                de.lemonpvp.duelplus.util.BetterSmpKontakt.merken(spieler, gegner, gegnerName);
            } catch (LinkageError | RuntimeException fehler) {
                betterSmpKontakte = false;
                getLogger().warning("BetterSMP ist zu alt fuer 'letzte Duell-Gegner' - bitte die aktuelle BetterSMP-Jar einspielen.");
            }
        }
    }
}
