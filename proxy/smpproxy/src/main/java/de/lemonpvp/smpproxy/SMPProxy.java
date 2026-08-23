package de.lemonpvp.smpproxy;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.lemonpvp.smpproxy.ban.BanStore;
import de.lemonpvp.smpproxy.command.HubCommand;
import de.lemonpvp.smpproxy.command.NetworkBanCommand;
import de.lemonpvp.smpproxy.command.RtpCommand;
import de.lemonpvp.smpproxy.command.ProxyCommand;
import de.lemonpvp.smpproxy.command.ServerCommand;
import de.lemonpvp.smpproxy.config.ProxyConfig;
import de.lemonpvp.smpproxy.health.HomeTracker;
import de.lemonpvp.smpproxy.health.ServerWatcher;
import de.lemonpvp.smpproxy.listener.BanListener;
import de.lemonpvp.smpproxy.listener.ConnectListener;
import de.lemonpvp.smpproxy.util.Msg;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * SMPProxy - schickt Spieler anhand der benutzten Domain direkt auf den
 * richtigen Server und fängt Abstürze mit einem Limbo-Server ab.
 */
@Plugin(
        id = "smpproxy",
        name = "SMPProxy",
        version = "1.0.0",
        description = "Domain-Routing und Limbo-Ausweichserver",
        authors = {"SMPProxy"}
)
public final class SMPProxy {

    private final ProxyServer proxy;
    private final Logger log;

    private final ProxyConfig config;
    private final ServerWatcher watcher;
    private final HomeTracker tracker = new HomeTracker();
    private final BanStore bans;
    private final BanListener banListener;

    private final List<ScheduledTask> tasks = new ArrayList<>();

    @Inject
    public SMPProxy(ProxyServer proxy, Logger log, @DataDirectory Path folder) {
        this.proxy = proxy;
        this.log = log;
        this.config = new ProxyConfig(folder, log);
        this.watcher = new ServerWatcher(proxy, config, log);
        this.bans = new BanStore(folder, log);
        this.banListener = new BanListener(this);
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        config.load();
        bans.ensureFiles();
        bans.load();
        proxy.getEventManager().register(this, new ConnectListener(this));
        proxy.getEventManager().register(this, banListener);
        startTasks();
        registerCommands();
        logRoutes();
    }

    // ------------------------------------------------------------------
    //  Aufgaben
    // ------------------------------------------------------------------

    private void startTasks() {
        cancelTasks();

        if (config.pingOnStart()) {
            watcher.tick();
        }
        tasks.add(proxy.getScheduler()
                .buildTask(this, watcher::tick)
                .delay(config.pingInterval(), TimeUnit.SECONDS)
                .repeat(config.pingInterval(), TimeUnit.SECONDS)
                .schedule());

        if (config.autoReturn()) {
            tasks.add(proxy.getScheduler()
                    .buildTask(this, this::bringPlayersHome)
                    .delay(config.pingInterval(), TimeUnit.SECONDS)
                    .repeat(config.pingInterval(), TimeUnit.SECONDS)
                    .schedule());
        }
    }

    private void cancelTasks() {
        for (ScheduledTask task : tasks) {
            task.cancel();
        }
        tasks.clear();
    }

    /** Holt Spieler aus dem Limbo zurück, sobald ihr Server wieder stabil läuft. */
    private void bringPlayersHome() {
        String limbo = config.limbo();
        if (limbo.isEmpty()) {
            return;
        }
        for (Player player : proxy.getAllPlayers()) {
            Optional<RegisteredServer> current = player.getCurrentServer()
                    .map(connection -> connection.getServer());
            if (current.isEmpty() || !current.get().getServerInfo().getName().equals(limbo)) {
                continue;
            }
            String home = tracker.home(player.getUniqueId());
            if (home == null || home.equals(limbo) || !watcher.isStable(home)) {
                continue;
            }
            if (!tracker.tryAttempt(player.getUniqueId(), config.returnCooldown() * 1000L)) {
                continue;
            }
            if (config.returnNotice()) {
                player.sendMessage(message("return-soon", "%server%", home));
            }
            sendHome(player, home);
        }
    }

    private void sendHome(Player player, String home) {
        Optional<RegisteredServer> target = proxy.getServer(home);
        if (target.isEmpty()) {
            return;
        }
        player.createConnectionRequest(target.get()).connect().whenComplete((result, error) -> {
            if (error == null && result != null && result.isSuccessful()) {
                tracker.clearCooldown(player.getUniqueId());
                player.sendMessage(message("return-ok", "%server%", home));
            } else if (config.returnNotice()) {
                player.sendMessage(message("return-failed", "%server%", home));
            }
        });
    }

    // ------------------------------------------------------------------
    //  Befehle
    // ------------------------------------------------------------------

    private void registerCommands() {
        CommandManager commands = proxy.getCommandManager();

        commands.register(commands.metaBuilder("smpproxy").aliases("proxy").build(),
                new ProxyCommand(this));

        commands.register(commands.metaBuilder("netban").build(),
                new NetworkBanCommand(this, NetworkBanCommand.Modus.BAN));
        commands.register(commands.metaBuilder("netunban").build(),
                new NetworkBanCommand(this, NetworkBanCommand.Modus.UNBAN));
        commands.register(commands.metaBuilder("netbans").aliases("netbanlist").build(),
                new NetworkBanCommand(this, NetworkBanCommand.Modus.LIST));
        commands.register(commands.metaBuilder("netbaninfo").build(),
                new NetworkBanCommand(this, NetworkBanCommand.Modus.INFO));

        if (config.rtpEnabled() && !config.rtpRedirectServer().isEmpty()) {
            commands.register(commands.metaBuilder("rtp").build(), new RtpCommand(this));
        }

        if (config.hubEnabled() && !config.limbo().isEmpty()) {
            List<String> aliases = config.hubAliases();
            if (!aliases.isEmpty()) {
                String main = aliases.get(0);
                String[] rest = aliases.subList(1, aliases.size()).toArray(new String[0]);
                commands.register(commands.metaBuilder(main).aliases(rest).build(),
                        new HubCommand(this));
            }
        }

        if (config.serverShortcuts()) {
            for (String server : config.domains().values().stream().distinct().toList()) {
                if (server.equals(config.limbo())) {
                    continue;
                }
                commands.register(commands.metaBuilder(server).build(), new ServerCommand(this, server));
            }
        }
    }

    private void logRoutes() {
        log.info("SMPProxy läuft.");
        config.domains().forEach((host, server) -> log.info("  {} -> {}", host, server));
        if (config.limbo().isEmpty()) {
            log.warn("  Kein Limbo eingetragen - bei einem Absturz werden Spieler getrennt.");
        } else {
            log.info("  Ausweichserver: {}", config.limbo());
        }
    }

    /** Von /smpproxy reload aufgerufen. Befehle bleiben registriert. */
    public void reload() {
        config.load();
        bans.load();
        watcher.reset();
        tracker.clear();
        startTasks();
    }

    // ------------------------------------------------------------------
    //  Gemeinsam genutzt
    // ------------------------------------------------------------------

    /** Verbindet einen Spieler und meldet zurück, ob es geklappt hat. */
    public void connect(Player player, String server, boolean quiet) {
        Optional<RegisteredServer> target = proxy.getServer(server);
        if (target.isEmpty()) {
            player.sendMessage(message("unknown-server", "%server%", server));
            return;
        }
        player.createConnectionRequest(target.get()).connect().whenComplete((result, error) -> {
            boolean ok = error == null && result != null && result.isSuccessful();
            if (!ok && !quiet) {
                player.sendMessage(message("switch-failed", "%server%", server));
            }
        });
    }

    public Component message(String key, String... placeholders) {
        return Msg.of(config.message(key), config.prefix(), placeholders);
    }

    /** Nachricht ohne Prefix - für Trenn-Bildschirme. */
    public Component screen(String key, String... placeholders) {
        return Msg.of(config.message(key), "", placeholders);
    }

    public ProxyServer proxy() {
        return proxy;
    }

    public ProxyConfig config() {
        return config;
    }

    public ServerWatcher watcher() {
        return watcher;
    }

    public HomeTracker tracker() {
        return tracker;
    }

    public BanStore bans() {
        return bans;
    }

    public BanListener banListener() {
        return banListener;
    }

    public Logger log() {
        return log;
    }
}
