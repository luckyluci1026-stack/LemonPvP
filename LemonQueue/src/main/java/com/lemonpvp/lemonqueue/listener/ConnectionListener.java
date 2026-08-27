package com.lemonpvp.lemonqueue.listener;

import com.lemonpvp.lemonqueue.LemonQueue;
import com.lemonpvp.lemonqueue.config.QueueConfig;
import com.lemonpvp.lemonqueue.queue.QueueManager;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Optional;

/**
 * Routes players based on capacity:
 * <ul>
 *   <li>On join, sends them straight to the target if a slot is free,
 *       otherwise parks them in the limbo and enqueues them.</li>
 *   <li>If a backend kicks them because it is full, redirects to the limbo
 *       and enqueues instead of disconnecting them.</li>
 *   <li>Cleans up queue state on disconnect.</li>
 * </ul>
 * Players with the bypass permission always skip the queue.
 *
 * <p>Config is read live from {@link LemonQueue#getConfig()} on every event so
 * that {@code /lq reload} takes effect immediately for routing decisions.</p>
 */
public class ConnectionListener {

    private final LemonQueue plugin;
    private final ProxyServer proxy;
    private final QueueManager queues;

    public ConnectionListener(LemonQueue plugin, QueueManager queues) {
        this.plugin = plugin;
        this.proxy = plugin.getProxy();
        this.queues = queues;
    }

    private QueueConfig config() {
        return plugin.getConfig();
    }

    /**
     * Denies banned players during the proxy handshake — the ban screen shows
     * at connection time and they never reach a backend (mirrors how
     * maintenance kicks fully disconnect). The cache is fed by LemonCore's
     * PlayerBanning plugin message; after a proxy restart the first attempt
     * falls through to the backend kick, which re-populates the cache.
     */
    @Subscribe
    public void onLogin(LoginEvent event) {
        String screen = queues.getActiveBanScreen(event.getPlayer().getUniqueId());
        if (screen != null) {
            event.setResult(ResultedEvent.ComponentResult.denied(
                    MiniMessage.miniMessage().deserialize(screen)));
        }
    }

    @Subscribe
    public void onChooseInitialServer(PlayerChooseInitialServerEvent event) {
        QueueConfig config = config();
        Player player = event.getPlayer();
        String target = config.getDefaultTarget();

        // Staff/owner bypass: go straight to the target, even if "full".
        if (player.hasPermission(config.getBypassPermission()) || queues.hasFreeSlot(target)) {
            proxy.getServer(target).ifPresent(event::setInitialServer);
            return;
        }

        // Full → park in limbo, then enqueue for the real target.
        // Only use limbo if it is registered AND currently reachable (no log spam).
        Optional<RegisteredServer> limbo = proxy.getServer(config.getLimboServer());
        if (limbo.isPresent() && queues.isLimboOnline()) {
            event.setInitialServer(limbo.get());
            queues.enqueue(player, target);
        } else {
            // Limbo offline or unconfigured → send directly to target.
            // If target is also full the player will just see the normal full-server screen.
            proxy.getServer(target).ifPresent(event::setInitialServer);
        }
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        QueueConfig config = config();
        Player player = event.getPlayer();
        String kicked = event.getServer().getServerInfo().getName();

        // Never re-queue someone leaving the limbo itself.
        if (kicked.equalsIgnoreCase(config.getLimboServer())) return;
        if (player.hasPermission(config.getBypassPermission())) return;
        // Ban-kick or admin/maintenance kick: LemonCore signalled us to fully
        // disconnect the player (show the kick screen) instead of re-routing to
        // limbo. Consume-once so a later unrelated kick is handled normally.
        if (queues.consumeBanning(player.getUniqueId()) || queues.consumeKicking(player.getUniqueId())) {
            queues.dequeue(player.getUniqueId());
            Component reason = event.getServerKickReason().orElse(Component.empty());
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(reason));
            return;
        }

        // Only redirect to limbo when it is actually reachable. With no limbo the
        // player fully disconnects — translate technical reasons (netty/Java
        // exceptions) into something a normal player understands.
        Optional<RegisteredServer> limbo = proxy.getServer(config.getLimboServer());
        if (limbo.isEmpty() || !queues.isLimboOnline()) {
            Component raw = event.getServerKickReason().orElse(Component.empty());
            Component friendly = com.lemonpvp.lemonqueue.util.FriendlyErrors.translate(raw);
            if (friendly != raw) {
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(friendly));
            }
            return;
        }

        // Redirect to limbo + queue instead of dropping the player.
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(
                limbo.get(),
                config.getMessages().kickFull(kicked)));
        queues.enqueue(player, kicked);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        queues.dequeue(event.getPlayer().getUniqueId());
    }
}
