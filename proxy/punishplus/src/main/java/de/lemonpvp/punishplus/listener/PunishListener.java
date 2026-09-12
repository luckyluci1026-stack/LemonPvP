package de.lemonpvp.punishplus.listener;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.punishplus.PunishPlus;
import de.lemonpvp.punishplus.store.PunishRecord;
import de.lemonpvp.punishplus.util.Durations;
import net.kyori.adventure.text.Component;

/**
 * Der eigentliche Riegel: Ein gesperrter Spieler kommt gar nicht erst rein.
 *
 * `LoginEvent` laeuft nach der Mojang-Authentifizierung, aber bevor ein
 * einziger Server gewaehlt wird (gleiches Vorbild wie SMPProxys
 * BanListener) - wird das Ergebnis hier auf "abgelehnt" gesetzt, landet
 * der Spieler auf KEINEM Server, auch nicht in der Lobby.
 *
 * Weil dieselbe Pruefung bei JEDEM Verbindungsversuch von Neuem laeuft,
 * ist "kann nicht mehr reconnecten" kein Extra-Mechanismus, sondern
 * einfach die Folge davon, dass hier immer wieder derselbe, aktuelle
 * Sperrzustand gefragt wird.
 */
public final class PunishListener {

    private final PunishPlus plugin;

    public PunishListener(PunishPlus plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        plugin.store().aktiv(player.getUniqueId()).ifPresent(record ->
                event.setResult(ResultedEvent.ComponentResult.denied(bildschirm(record))));
    }

    private Component bildschirm(PunishRecord record) {
        if (record.dauerhaft()) {
            return plugin.screen("kick-perm", "%grund%", record.grundText(), "%bis%", "");
        }
        String bis = Durations.humanize(record.bis() - System.currentTimeMillis());
        return plugin.screen("kick-temp", "%grund%", record.grundText(), "%bis%", bis);
    }
}
