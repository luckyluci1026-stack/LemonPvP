package de.lemonpvp.smplobby;

import de.lemonpvp.smplobby.board.LobbyBoard;
import de.lemonpvp.smplobby.command.LobbyCommand;
import de.lemonpvp.smplobby.hide.HideModule;
import de.lemonpvp.smplobby.items.ItemListener;
import de.lemonpvp.smplobby.items.LobbyItems;
import de.lemonpvp.smplobby.join.JoinListener;
import de.lemonpvp.smplobby.jump.DoubleJump;
import de.lemonpvp.smplobby.protect.ProtectListener;
import de.lemonpvp.smplobby.proxy.ProxyBridge;
import de.lemonpvp.smplobby.selector.SelectorGui;
import de.lemonpvp.smplobby.selector.SelectorListener;
import de.lemonpvp.smplobby.spawn.SpawnManager;
import de.lemonpvp.smplobby.util.Msgs;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * SMPLobby - das Plugin fuer den Hub eines Netzwerks.
 *
 * Es gehoert auf den LOBBY-Server, nicht auf die SMPs und nicht auf den
 * Proxy. Der Proxy (SMPProxy) schickt die Spieler hierher, alles Weitere
 * passiert hier: Schutz, Spawn, Schnellleiste, Serverauswahl, Tafel.
 *
 * Zum Weiterschicken redet es ueber den Kanal "BungeeCord" mit dem
 * Proxy - siehe ProxyBridge. Der Lobby-Server muss dafuer hinter dem
 * Proxy stehen; ohne ihn funktioniert alles ausser der Serverauswahl.
 */
public final class SMPLobby extends JavaPlugin {

    private Msgs msgs;
    private SpawnManager spawn;
    private LobbyItems items;
    private SelectorGui waehler;
    private ProxyBridge proxy;
    private HideModule hide;
    private LobbyBoard board;
    private DoubleJump doppelsprung;
    private JoinListener ankunft;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        this.msgs = new Msgs(this);
        this.spawn = new SpawnManager(this);
        this.items = new LobbyItems(this);
        this.proxy = new ProxyBridge(this);
        this.waehler = new SelectorGui(this);
        this.hide = new HideModule(this);
        this.board = new LobbyBoard(this);
        this.doppelsprung = new DoubleJump(this);
        this.ankunft = new JoinListener(this);

        proxy.start();

        Bukkit.getPluginManager().registerEvents(ankunft, this);
        Bukkit.getPluginManager().registerEvents(new ProtectListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ItemListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SelectorListener(this), this);
        Bukkit.getPluginManager().registerEvents(doppelsprung, this);

        befehl("spawn");
        befehl("setspawn");
        befehl("smplobby");

        board.start();
        haltZeitAn();
        frageZahlenRegelmaessig();

        // Beim /reload eines laufenden Servers sind schon Spieler da. Ohne
        // das saessen sie ohne Schnellleiste und ohne Tafel in der Lobby,
        // bis sie sich einmal neu verbinden.
        for (Player spieler : Bukkit.getOnlinePlayers()) {
            ankunft.bereite(spieler);
            board.baueAuf(spieler);
        }

        if (!spawn.gesetzt()) {
            getLogger().warning("Es ist noch kein Spawn gesetzt. "
                    + "Stell dich hin und tipp /setspawn.");
        }
        getLogger().info("SMPLobby aktiviert.");
    }

    @Override
    public void onDisable() {
        if (board != null) {
            board.stop();
        }
        if (hide != null) {
            hide.alleVergessen();
        }
        if (proxy != null) {
            proxy.stop();
        }
    }

    /**
     * Einen Befehl verdrahten - und sagen, wenn er in der plugin.yml fehlt.
     *
     * getCommand() gibt dort null zurueck, und ein blindes
     * `.setExecutor(...)` waere eine NullPointerException mitten im Start,
     * nach der das halbe Plugin nicht mehr laeuft.
     */
    private void befehl(String name) {
        PluginCommand befehl = getCommand(name);
        if (befehl == null) {
            getLogger().severe("Befehl \"" + name + "\" fehlt in der plugin.yml.");
            return;
        }
        LobbyCommand ausfuehrer = new LobbyCommand(this);
        befehl.setExecutor(ausfuehrer);
        befehl.setTabCompleter(ausfuehrer);
    }

    /** Feste Tageszeit, damit die Lobby immer gleich aussieht. */
    private void haltZeitAn() {
        long zeit = getConfig().getLong("schutz.uhrzeit", -1);
        if (zeit < 0) {
            return;
        }
        for (World welt : Bukkit.getWorlds()) {
            welt.setTime(zeit);
            // Ohne das laeuft die Zeit weiter und die Lobby wird trotzdem
            // dunkel - nur eben ab einem anderen Startpunkt.
            //
            // Die typisierte Form, nicht setGameRuleValue(String, String):
            // die alte ist seit 1.13 veraltet, und veraltete Bukkit-Methoden
            // verschwinden irgendwann wirklich.
            welt.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            welt.setGameRule(GameRule.DO_WEATHER_CYCLE,
                    getConfig().getBoolean("schutz.wetter", false));
        }
    }

    /**
     * Die Spielerzahlen beim Proxy nachfragen.
     *
     * Alle fuenf Sekunden reicht: Die Zahl steht in der Serverauswahl und
     * auf der Tafel, und niemand merkt, ob sie zwei Sekunden alt ist.
     * Oefter zu fragen belastet nur die Leitung.
     */
    private void frageZahlenRegelmaessig() {
        Bukkit.getScheduler().runTaskTimer(this,
                () -> proxy.frageZahlen(waehler.serverNamen()), 40L, 100L);
    }

    // ---------------------------------------------------------- Zugriff

    public Msgs msgs() {
        return msgs;
    }

    public SpawnManager spawn() {
        return spawn;
    }

    public LobbyItems items() {
        return items;
    }

    public SelectorGui waehler() {
        return waehler;
    }

    public ProxyBridge proxy() {
        return proxy;
    }

    public HideModule hide() {
        return hide;
    }

    public LobbyBoard board() {
        return board;
    }

    public DoubleJump doppelsprung() {
        return doppelsprung;
    }
}
