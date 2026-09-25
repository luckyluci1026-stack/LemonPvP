package de.lemonpvp.bettersmp.killstreak;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serien-Ansagen im PvP - reine Stimmung, keine Belohnung.
 *
 * Absichtlich nur im Arbeitsspeicher: Eine Serie ist per Definition
 * etwas, das gerade laeuft. Nach einem Serverneustart faengt jeder bei
 * 0 an - das ist kein Verlust, sondern genau richtig.
 */
public final class KillstreakListener implements Listener {

    private final BetterSMP plugin;
    private final Map<UUID, Integer> serien = new ConcurrentHashMap<>();

    public KillstreakListener(BetterSMP plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("killstreak.enabled", true);
    }

    /** Laufende Serie eines Spielers - fuer Scoreboard/Anzeige, 0 wenn keine laeuft. */
    public int serie(UUID spieler) {
        return serien.getOrDefault(spieler, 0);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!enabled()) {
            return;
        }
        Player opfer = event.getEntity();
        int beendeteSerie = serien.getOrDefault(opfer.getUniqueId(), 0);
        serien.remove(opfer.getUniqueId());

        Player toeter = opfer.getKiller();
        if (toeter == null || toeter.equals(opfer)) {
            return;
        }

        int neueSerie = serien.merge(toeter.getUniqueId(), 1, Integer::sum);

        int endeSchwelle = plugin.getConfig().getInt("killstreak.end-announce-threshold", 5);
        if (beendeteSerie >= endeSchwelle) {
            plugin.msgs().broadcast("killstreak.ended",
                    "opfer", opfer.getName(), "serie", String.valueOf(beendeteSerie),
                    "toeter", toeter.getName());
        }

        List<Integer> meilensteine = plugin.getConfig().getIntegerList("killstreak.milestones");
        if (meilensteine.contains(neueSerie)) {
            plugin.msgs().broadcast("killstreak.milestone",
                    "spieler", toeter.getName(), "serie", String.valueOf(neueSerie));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Wer geht, nimmt seine laufende Serie nicht als Statistik mit -
        // aber auch nicht als "beendet durch Verlassen"-Meldung, das
        // waere kein richtiger Kill gegen ihn gewesen.
        serien.remove(event.getPlayer().getUniqueId());
    }
}
