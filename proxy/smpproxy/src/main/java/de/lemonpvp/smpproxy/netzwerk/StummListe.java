package de.lemonpvp.smpproxy.netzwerk;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StummListe {

    private final Map<UUID, Long> bis = new ConcurrentHashMap<>();

    public void setzen(UUID spieler, long restMillis) {
        if (restMillis == 0) {
            bis.remove(spieler);
            return;
        }
        bis.put(spieler, restMillis < 0 ? Long.MAX_VALUE : System.currentTimeMillis() + restMillis);
    }

    public boolean stumm(UUID spieler) {
        return restMillis(spieler) != 0;
    }

    public long restMillis(UUID spieler) {
        Long ende = bis.get(spieler);
        if (ende == null) {
            return 0;
        }
        if (ende == Long.MAX_VALUE) {
            return -1;
        }
        long rest = ende - System.currentTimeMillis();
        if (rest <= 0) {
            bis.remove(spieler, ende);
            return 0;
        }
        return rest;
    }
}
