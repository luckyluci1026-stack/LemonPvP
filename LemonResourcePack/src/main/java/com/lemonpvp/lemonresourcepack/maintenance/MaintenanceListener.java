package com.lemonpvp.lemonresourcepack.maintenance;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class MaintenanceListener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final AtomicBoolean enabled;
    private final Set<UUID> whitelist;
    private final String discordLink;

    public MaintenanceListener(AtomicBoolean enabled, Set<UUID> whitelist, String discordLink) {
        this.enabled = enabled;
        this.whitelist = whitelist;
        this.discordLink = discordLink;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPostLogin(PostLoginEvent event) {
        if (!enabled.get()) return;
        UUID uuid = event.getPlayer().getUniqueId();
        if (!whitelist.contains(uuid)) {
            Component msg = MM.deserialize(
                "<bold><gradient:#fffb00:#00ff00>LemonPvP</gradient></bold>\n" +
                "<red>The server is currently under maintenance.</red>\n" +
                "<gray>Follow our Discord for updates:</gray>\n" +
                "<aqua>" + discordLink + "</aqua>"
            );
            event.getPlayer().disconnect(msg);
        }
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        if (!enabled.get()) return;
        Component motd = MM.deserialize(
            "<bold><gradient:#fffb00:#00ff00>LemonPvP</gradient></bold>\n" +
            "<red>⚙ Under Maintenance</red>"
        );
        event.setPing(event.getPing().asBuilder().description(motd).build());
    }
}
