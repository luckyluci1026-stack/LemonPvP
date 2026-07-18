package com.lemonpvp.lemonqueue;

import com.google.inject.Inject;
import com.lemonpvp.lemonqueue.command.QueueCommand;
import com.lemonpvp.lemonqueue.config.QueueConfig;
import com.lemonpvp.lemonqueue.listener.ConnectionListener;
import com.lemonpvp.lemonqueue.listener.PingListener;
import com.lemonpvp.lemonqueue.listener.PluginMessageListener;
import com.lemonpvp.lemonqueue.queue.QueueManager;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * LemonQueue – capacity-aware queue + limbo routing for the LemonPvP network.
 *
 * <p>When a target server is full, players are parked in a NanoLimbo backend
 * and shown an animated waiting screen; they are released to the target
 * (priority + FIFO order) as soon as a slot frees up.</p>
 */
@Plugin(
        id = "lemonqueue",
        name = "LemonQueue",
        version = "1.0.0",
        description = "Queue + Limbo routing with gradients for LemonPvP",
        authors = {"LemonPvP"}
)
public class LemonQueue {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDir;

    private QueueConfig config;
    private QueueManager queueManager;

    @Inject
    public LemonQueue(ProxyServer proxy, Logger logger, @DataDirectory Path dataDir) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDir = dataDir;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        this.config = QueueConfig.load(dataDir, logger);
        this.queueManager = new QueueManager(this, proxy, logger, config);

        // Register the lemonpvp:core channel so Velocity forwards plugin messages from backends.
        MinecraftChannelIdentifier lemonChannel =
                MinecraftChannelIdentifier.from("lemonpvp:core");
        proxy.getChannelRegistrar().register(lemonChannel);

        proxy.getEventManager().register(this, new ConnectionListener(this, queueManager));
        proxy.getEventManager().register(this, new com.lemonpvp.lemonqueue.listener.VpnBlockerListener(this));
        proxy.getEventManager().register(this, new PingListener(this));
        proxy.getEventManager().register(this, new PluginMessageListener(queueManager));

        CommandManager cm = proxy.getCommandManager();
        CommandMeta meta = cm.metaBuilder("lemonqueue").aliases("lq", "queue").plugin(this).build();
        cm.register(meta, new QueueCommand(this, queueManager));

        queueManager.start();

        if (proxy.getServer(config.getLimboServer()).isEmpty()) {
            logger.warn("[LemonQueue] Limbo server '{}' is not registered in velocity.toml — "
                    + "queued players cannot be parked until it is added!", config.getLimboServer());
        }
        logger.info("[LemonQueue] Enabled. Limbo='{}', default target='{}'.",
                config.getLimboServer(), config.getDefaultTarget());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (queueManager != null) queueManager.stop();
        logger.info("[LemonQueue] Disabled.");
    }

    public ProxyServer getProxy() { return proxy; }
    public Logger getLogger()     { return logger; }
    public QueueConfig getConfig() { return config; }

    /** Reloads config.yml from disk without stopping or re-queueing any players. */
    public void reload() {
        this.config = QueueConfig.load(dataDir, logger);
        queueManager.reloadConfig(config);
        logger.info("[LemonQueue] Config reloaded. language='{}', limbo='{}', target='{}'.",
                config.getLanguage(), config.getLimboServer(), config.getDefaultTarget());
    }
}
