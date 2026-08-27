package com.lemonpvp.lemonfailover;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Shows a state-dependent MOTD in the server list: the standby MOTD while the
 * primary is healthy, the failover MOTD while ACTIVE. Blank config values keep
 * whatever velocity.toml provides.
 */
public final class MotdListener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonFailover plugin;

    public MotdListener(LemonFailover plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPing(ProxyPingEvent event) {
        FailoverConfig cfg = plugin.getConfig();
        String motd = plugin.getMonitor().getState() == FailoverState.ACTIVE
                ? cfg.getMotdActive()
                : cfg.getMotdStandby();
        if (motd == null || motd.isBlank()) return;

        ServerPing original = event.getPing();
        event.setPing(original.asBuilder()
                .description(MM.deserialize(motd))
                .build());
    }
}
