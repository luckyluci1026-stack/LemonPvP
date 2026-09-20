package de.lemonpvp.duelplus.presence;

import de.lemonpvp.duelplus.DuelPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Haelt duelplus_presence aktuell: welcher Spieler ist gerade auf
 * WELCHEM Server. Noetig, damit /duel <Name> auch serveruebergreifend
 * funktioniert (Herausforderer auf dem SMP, Ziel in der Lobby).
 *
 * Ein reiner Eintrag bei Join/Quit wuerde bei einem Serverabsturz ohne
 * sauberes PlayerQuitEvent fuer immer veraltet stehen bleiben - deshalb
 * zusaetzlich ein Herzschlag, der die Zeile alle 30 Sekunden fuer jeden
 * noch online befindlichen Spieler neu schreibt. Leser (presenceServerVon)
 * verwerfen alles, was aelter als ein paar Herzschlaege ist.
 */
public final class PresenceService implements Listener {

    private static final long HERZSCHLAG_TICKS = 30L * 20L;

    private final DuelPlus plugin;

    public PresenceService(DuelPlus plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::herzschlag, HERZSCHLAG_TICKS, HERZSCHLAG_TICKS);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimJoin(PlayerJoinEvent event) {
        aktualisieren(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimVerlassen(PlayerQuitEvent event) {
        plugin.db().presenceEntfernen(event.getPlayer().getUniqueId());
    }

    private void herzschlag() {
        for (Player spieler : Bukkit.getOnlinePlayers()) {
            aktualisieren(spieler);
        }
    }

    private void aktualisieren(Player spieler) {
        plugin.db().presenceSetzen(spieler.getUniqueId(), spieler.getName(), plugin.serverName());
    }
}
