package com.lemonpvp.lemonqueue.listener;

import com.lemonpvp.lemonqueue.LemonQueue;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Overrides the proxy MOTD shown in the Minecraft server list.
 * The text is configured via {@code motd:} in LemonQueue's config.yml and
 * reloaded live with {@code /lq reload} — no velocity.toml edit needed.
 *
 * <p>If {@code motd} is empty or blank, this listener does nothing and Velocity
 * uses whatever is set in velocity.toml.</p>
 */
public class PingListener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonQueue plugin;

    public PingListener(LemonQueue plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPing(ProxyPingEvent event) {
        String motd = plugin.getConfig().getMotd();
        if (motd == null || motd.isBlank()) return;

        ServerPing original = event.getPing();
        event.setPing(original.asBuilder()
                .description(MM.deserialize(motd))
                .build());
    }
}
