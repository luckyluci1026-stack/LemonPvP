package de.lemonpvp.smpproxy.health;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Merkt sich pro Spieler, wo er eigentlich hingehört.
 *
 * Wer über smp2.lemon-servers.de kommt, soll bei einem Absturz auch wieder
 * auf smp2 landen - und nicht auf smp1.
 */
public final class HomeTracker {

    private final Map<UUID, String> home = new ConcurrentHashMap<>();
    private final Map<UUID, Long> nextAttempt = new ConcurrentHashMap<>();

    public void setHome(UUID player, String server) {
        if (server != null && !server.isEmpty()) {
            home.put(player, server);
        }
    }

    public String home(UUID player) {
        return home.get(player);
    }

    public void forget(UUID player) {
        home.remove(player);
        nextAttempt.remove(player);
    }

    /** Darf jetzt ein Rückhol-Versuch laufen? Setzt gleich den nächsten Termin. */
    public boolean tryAttempt(UUID player, long cooldownMillis) {
        long now = System.currentTimeMillis();
        Long next = nextAttempt.get(player);
        if (next != null && next > now) {
            return false;
        }
        nextAttempt.put(player, now + cooldownMillis);
        return true;
    }

    public void clearCooldown(UUID player) {
        nextAttempt.remove(player);
    }

    public void clear() {
        home.clear();
        nextAttempt.clear();
    }
}
