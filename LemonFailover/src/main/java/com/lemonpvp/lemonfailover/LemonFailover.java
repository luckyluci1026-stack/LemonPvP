package com.lemonpvp.lemonfailover;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * LemonFailover — runs on the FALLBACK Velocity proxy.
 *
 * <p>Continuously monitors the PRIMARY proxy. While the primary is reachable this
 * proxy stays in {@link FailoverState#STANDBY} (and, by default, refuses logins
 * so all players stay on the primary). If the primary becomes unreachable it
 * switches to {@link FailoverState#ACTIVE} — accepting players and routing them
 * to the backend servers configured in velocity.toml — and alerts staff.</p>
 */
@Plugin(
        id = "lemonfailover",
        name = "LemonFailover",
        version = "1.0.0",
        description = "Standby/failover controller for the backup proxy",
        authors = {"LemonPvP"}
)
public class LemonFailover {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDir;

    private volatile FailoverConfig config;
    private WebhookNotifier webhook;
    private PrimaryMonitor monitor;

    @Inject
    public LemonFailover(ProxyServer proxy, Logger logger, @DataDirectory Path dataDir) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDir = dataDir;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        this.config = FailoverConfig.load(dataDir, logger);
        this.webhook = new WebhookNotifier(logger);
        this.monitor = new PrimaryMonitor(this);

        proxy.getEventManager().register(this, new LoginGateListener(this));
        proxy.getEventManager().register(this, new MotdListener(this));

        CommandManager cm = proxy.getCommandManager();
        CommandMeta meta = cm.metaBuilder("lemonfailover")
                .aliases("failover", "lfo")
                .plugin(this)
                .build();
        cm.register(meta, new FailoverCommand(this));

        monitor.start();
        logger.info("[LemonFailover] Enabled. Watching primary {}:{}.",
                config.getPrimaryHost(), config.getPrimaryPort());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (monitor != null) monitor.stop();
        logger.info("[LemonFailover] Disabled.");
    }

    /** Reloads config.yml and re-schedules the monitor with any new interval. */
    public void reload() {
        this.config = FailoverConfig.load(dataDir, logger);
        if (monitor != null) monitor.reschedule();
        logger.info("[LemonFailover] Config reloaded. Primary={}:{}, deny-logins={}.",
                config.getPrimaryHost(), config.getPrimaryPort(), config.isDenyLogins());
    }

    public ProxyServer getProxy()     { return proxy; }
    public Logger getLogger()         { return logger; }
    public FailoverConfig getConfig() { return config; }
    public PrimaryMonitor getMonitor(){ return monitor; }
    public WebhookNotifier getWebhook(){ return webhook; }
}
