package de.lemonpvp.antiswear.strikes;

import de.lemonpvp.antiswear.AntiSwear;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StrikeManager {

    private record Konto(int punkte, long zuletzt) {
    }

    private static final long FUER_IMMER = Long.MAX_VALUE;

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
            stummBis.remove(spieler, bis);
            return false;
        }
        return true;
    }

    public long stummRestMillis(UUID spieler) {
        Long bis = stummBis.get(spieler);
        if (bis == null) {
            return 0;
        }
        return bis == FUER_IMMER ? -1 : Math.max(0, bis - System.currentTimeMillis());
    }

    public void stummschalten(UUID spieler, long dauerMillis) {
        stummBis.put(spieler, dauerMillis < 0 ? FUER_IMMER : System.currentTimeMillis() + dauerMillis);
    }

    public void stummVomNetzwerk(UUID spieler, long restMillis) {
        if (restMillis == 0) {
            stummBis.remove(spieler);
        } else {
            stummschalten(spieler, restMillis);
        }
    }

    public Optional<Stufe> verstoss(UUID spieler, int neuePunkte) {
        long jetzt = System.currentTimeMillis();
        long verfallMillis = plugin.verfallMinuten() * 60_000L;
        Konto alt = punkte.get(spieler);
        int vorher = alt != null && jetzt - alt.zuletzt() <= verfallMillis ? alt.punkte() : 0;
        int nachher = vorher + neuePunkte;
        punkte.put(spieler, new Konto(nachher, jetzt));
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
