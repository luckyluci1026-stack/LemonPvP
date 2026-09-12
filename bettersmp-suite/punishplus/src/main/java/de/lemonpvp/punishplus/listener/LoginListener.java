package de.lemonpvp.punishplus.listener;

import de.lemonpvp.punishplus.PunishPlus;
import de.lemonpvp.punishplus.store.PunishRecord;
import de.lemonpvp.punishplus.util.Durations;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

/**
 * Sperrt das Reconnecten: Jeder Verbindungsversuch prueft neu gegen
 * gesperrt.yml - laeuft bereits vor dem eigentlichen Beitritt, damit der
 * Kick-Screen sauber (auch fuer Bedrock) angezeigt wird.
 */
public final class LoginListener implements Listener {

    private final PunishPlus plugin;

    public LoginListener(PunishPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        plugin.store().aktiv(event.getUniqueId()).ifPresent(record -> {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, bildschirm(record));
        });
    }

    private net.kyori.adventure.text.Component bildschirm(PunishRecord record) {
        if (record.dauerhaft()) {
            return plugin.msgs().format("kick.perm", "grund", record.grundText());
        }
        String bis = Durations.humanize(record.bis() - System.currentTimeMillis());
        return plugin.msgs().format("kick.temp", "grund", record.grundText(), "bis", bis);
    }
}
