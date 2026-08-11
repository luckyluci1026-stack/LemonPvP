package de.lemonpvp.smpproxy.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.lemonpvp.smpproxy.SMPProxy;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Regelt, wo ein Spieler landet.
 *
 * 1. Beim Verbinden entscheidet die benutzte Domain über den Server.
 * 2. Ist der Server aus, geht es in den Limbo statt auf einen Trennbildschirm.
 * 3. Fliegt jemand mitten im Spiel raus, wird geprüft, ob der Server noch
 *    antwortet: Antwortet er, war es ein echter Kick (z.B. ein Bann) und der
 *    Spieler soll den Grund sehen. Antwortet er nicht, ist er abgestürzt und
 *    der Spieler wandert in den Limbo.
 */
public final class ConnectListener {

    /** So lange wird beim Rauswurf höchstens auf den Ping gewartet. */
    private static final long KICK_CHECK_MILLIS = 1500L;

    private final SMPProxy plugin;

    public ConnectListener(SMPProxy plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    //  1. Domain -> Server
    // ------------------------------------------------------------------

    @Subscribe
    public void onChooseServer(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        String host = player.getVirtualHost().map(InetSocketAddress::getHostString).orElse(null);
        String target = plugin.config().serverFor(host);

        if (target == null || target.isEmpty()) {
            return;
        }
        plugin.tracker().setHome(player.getUniqueId(), target);

        Optional<RegisteredServer> server = plugin.proxy().getServer(target);
        if (server.isEmpty()) {
            plugin.log().warn("Domain '{}' zeigt auf '{}' - diesen Server gibt es in der velocity.toml nicht.",
                    host, target);
            return;
        }

        // Zielserver läuft: direkt hin.
        if (plugin.watcher().isOnline(target)) {
            event.setInitialServer(server.get());
            return;
        }

        // Zielserver ist aus: Limbo, falls vorhanden.
        String limbo = plugin.config().limbo();
        Optional<RegisteredServer> fallback = limbo.isEmpty()
                ? Optional.empty()
                : plugin.proxy().getServer(limbo);

        if (fallback.isPresent() && plugin.watcher().isOnline(limbo)) {
            event.setInitialServer(fallback.get());
            notifyLater(player, plugin.message("server-offline", "%server%", target));
        } else {
            // Kein Limbo da - es wird trotzdem versucht, den Rest macht der Kick-Handler.
            event.setInitialServer(server.get());
        }
    }

    // ------------------------------------------------------------------
    //  2. Wo ist der Spieler gerade wirklich?
    // ------------------------------------------------------------------

    @Subscribe
    public void onConnected(ServerConnectedEvent event) {
        String name = event.getServer().getServerInfo().getName();
        if (!name.equals(plugin.config().limbo())) {
            // Der letzte echte Server ist ab jetzt sein Zuhause.
            plugin.tracker().setHome(event.getPlayer().getUniqueId(), name);
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        plugin.tracker().forget(event.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------------
    //  3. Rauswurf: Absturz oder echter Kick?
    // ------------------------------------------------------------------

    @Subscribe
    public void onKicked(KickedFromServerEvent event) {
        Player player = event.getPlayer();
        String from = event.getServer().getServerInfo().getName();
        String limbo = plugin.config().limbo();

        // Antwortet der Server noch? Dann war es ein echter Kick (Bann, /kick, ...)
        // und der Spieler soll den Grund zu sehen bekommen.
        boolean stillUp = plugin.watcher().pingNow(from, KICK_CHECK_MILLIS);
        if (stillUp) {
            return;
        }

        // Ab hier: der Server ist weg.
        if (from.equals(limbo)) {
            // Sogar der Warteraum ist tot - mehr können wir nicht tun.
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(
                    plugin.screen("limbo-crashed")));
            return;
        }

        Optional<RegisteredServer> fallback = limbo.isEmpty()
                ? Optional.empty()
                : plugin.proxy().getServer(limbo);

        if (fallback.isEmpty() || !plugin.watcher().isOnline(limbo)) {
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(
                    plugin.screen("no-limbo", "%server%", from)));
            return;
        }

        plugin.tracker().setHome(player.getUniqueId(), from);
        Component note = event.kickedDuringServerConnect()
                ? plugin.message("server-offline", "%server%", from)
                : plugin.message("server-crashed", "%server%", from);
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(fallback.get(), note));
    }

    // ------------------------------------------------------------------

    /**
     * Nachrichten während des Verbindens gehen gerne verloren, darum kurz
     * warten, bis der Spieler wirklich im Spiel ist.
     */
    private void notifyLater(Player player, Component message) {
        plugin.proxy().getScheduler()
                .buildTask(plugin, () -> {
                    if (player.getCurrentServer().isPresent()) {
                        player.sendMessage(message);
                    }
                })
                .delay(1, TimeUnit.SECONDS)
                .schedule();
    }
}
