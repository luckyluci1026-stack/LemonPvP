package com.lemonpvp.lemonfailover;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * While the proxy is in STANDBY (primary healthy) and {@code standby.deny-logins}
 * is on, refuses logins so players don't split across proxies. In ACTIVE
 * (failover) everyone is accepted. Players with the bypass permission always
 * get through.
 */
public final class LoginGateListener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonFailover plugin;

    public LoginGateListener(LemonFailover plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        if (plugin.getMonitor().getState() == FailoverState.ACTIVE) return;

        FailoverConfig cfg = plugin.getConfig();
        if (!cfg.isDenyLogins()) return;

        Player player = event.getPlayer();
        if (player.hasPermission(cfg.getBypassPermission())) return;

        event.setResult(ResultedEvent.ComponentResult.denied(
                MM.deserialize(cfg.getDenyStandby())));
    }
}
