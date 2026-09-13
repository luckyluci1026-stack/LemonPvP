package de.lemonpvp.antiswear.strikes;

import de.lemonpvp.antiswear.AntiSwear;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Punktekonto je Spieler, mit Verfall: Ohne neuen Verstoss innerhalb von
 * verfall-minuten faellt der Stand wieder auf 0 - eine einzelne
 * Entgleisung vor Wochen soll nicht ewig nachwirken.
 *
 * Rein im Speicher, nicht in einer Datei - ein Server-Neustart loescht
 * offene Punktestaende, genau wie die Kurzzeit-Stummschaltung. Das ist
 * hier bewusst kein Bann-Register wie bei einem echten Ban-Plugin,
 * sondern ein Chat-Filter mit Gedaechtnis fuer den laufenden Betrieb.
 */
public final class StrikeManager {

    private record Konto(int punkte, long zuletzt) {
    }

    private final AntiSwear plugin;
    private final Map<UUID, Konto> punkte = new ConcurrentHashMap<>();
    private final Map<UUID, Long> stummBis = new ConcurrentHashMap<>();

    public StrikeManager(AntiSwear plugin) {
        this.plugin = plugin;
    }

    public boolean istStummgeschaltet(UUID spieler) {
        Long bis = stummBis.get(spieler);
        if (bis == null) {
            return false;
        }
        if (bis <= System.currentTimeMillis()) {
            stummBis.remove(spieler);
            return false;
        }
        return true;
    }

    public long stummRestMillis(UUID spieler) {
        Long bis = stummBis.get(spieler);
        return bis == null ? 0 : Math.max(0, bis - System.currentTimeMillis());
    }

    public void stummschalten(UUID spieler, long dauerMillis) {
        stummBis.put(spieler, System.currentTimeMillis() + dauerMillis);
    }

    /**
     * Traegt einen Verstoss ein (Verfall wird davor beruecksichtigt) und
     * liefert die hoechste Stufe, die genau durch DIESEN Verstoss neu
     * ueberschritten wurde - leer, wenn keine neue Schwelle erreicht wurde.
     */
    public Optional<Stufe> verstoss(Player spieler, int neuePunkte) {
        UUID id = spieler.getUniqueId();
        long jetzt = System.currentTimeMillis();
        long verfallMillis = plugin.verfallMinuten() * 60_000L;

        Konto alt = punkte.get(id);
        int vorher = (alt != null && jetzt - alt.zuletzt() <= verfallMillis) ? alt.punkte() : 0;
        int nachher = vorher + neuePunkte;
        punkte.put(id, new Konto(nachher, jetzt));

        Stufe ausgeloest = null;
        for (Stufe stufe : plugin.stufen()) {
            if (nachher >= stufe.schwelle() && vorher < stufe.schwelle()) {
                ausgeloest = stufe;
            }
        }
        return Optional.ofNullable(ausgeloest);
    }

    public int aktuellePunkte(UUID spieler) {
        Konto konto = punkte.get(spieler);
        if (konto == null) {
            return 0;
        }
        long verfallMillis = plugin.verfallMinuten() * 60_000L;
        return System.currentTimeMillis() - konto.zuletzt() <= verfallMillis ? konto.punkte() : 0;
    }

    public void zuruecksetzen(UUID spieler) {
        punkte.remove(spieler);
        stummBis.remove(spieler);
    }
}
