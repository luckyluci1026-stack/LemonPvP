package de.lemonpvp.smpproxy.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.smpproxy.SMPProxy;
import de.lemonpvp.smpproxy.ban.BanEntry;
import net.kyori.adventure.text.Component;

/**
 * Der eigentliche Riegel: Ein gebannter Spieler kommt gar nicht erst rein.
 *
 * `LoginEvent` laeuft nach der Mojang-Authentifizierung, aber bevor ein
 * einziger Server gewaehlt wird - genau der richtige Zeitpunkt. Wird das
 * Ergebnis hier auf "abgelehnt" gesetzt, sieht der Spieler den
 * Trennbildschirm mit dem Bann-Grund und landet auf KEINEM Server, auch
 * nicht in der Lobby.
 *
 * Weil dieselbe Pruefung bei JEDEM Verbindungsversuch von Neuem laeuft,
 * ist "kann nicht mehr reconnecten" kein Extra-Mechanismus, sondern
 * einfach die Folge davon, dass hier immer wieder derselbe, aktuelle
 * Banzustand gefragt wird.
 */
public final class BanListener {

    private final SMPProxy plugin;

    public BanListener(SMPProxy plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        BanEntry ban = plugin.bans().aktiv(player.getUniqueId()).orElse(null);
        if (ban == null) {
            return;
        }
        event.setResult(ResultedEvent.ComponentResult.denied(banScreen(ban)));
    }

    /**
     * Wird ein online befindlicher Spieler gebannt, muss er sofort raus -
     * nicht erst beim naechsten Versuch, sich zu verbinden.
     */
    public void kickIfOnline(BanEntry ban) {
        plugin.proxy().getPlayer(ban.player()).ifPresent(player ->
                player.disconnect(banScreen(ban)));
    }

    private Component banScreen(BanEntry ban) {
        return plugin.screen(ban.permanent() ? "ban-screen-perm" : "ban-screen-temp",
                "%reason%", ban.reason(),
                "%by%", ban.by(),
                "%remaining%", de.lemonpvp.smpproxy.ban.Durations.humanize(
                        ban.until() - System.currentTimeMillis()),
                "%expires%", de.lemonpvp.smpproxy.ban.Durations.expiry(ban.until(),
                        plugin.config().banPermanentWord()));
    }

    /**
     * Wer sich verbindet, wird sich gemerkt - sonst liesse sich ein
     * Offline-Spieler nur per UUID bannen.
     */
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        plugin.bans().remember(player.getUniqueId(), player.getUsername());
    }
}
