package de.lemonpvp.smpproxy.ban;

import java.util.UUID;

/**
 * Ein Netzwerkbann: Nicht "von diesem einen Server geflogen", sondern
 * "kommt beim Proxy gar nicht mehr rein" - egal welche Domain, egal
 * welcher Server.
 *
 * @param until   Unix-Millisekunden, ab wann der Bann ablaeuft. 0 = fuer
 *                immer. Absichtlich kein `Duration` gespeichert: Eine
 *                Dauer muesste ab dem Laden neu gerechnet werden, ein
 *                fester Zeitpunkt bleibt richtig, auch wenn der Proxy
 *                zwischendurch neu startet.
 */
public record BanEntry(UUID player, String name, String reason, String by,
                       long at, long until) {

    public boolean active() {
        return until <= 0 || until > System.currentTimeMillis();
    }

    public boolean permanent() {
        return until <= 0;
    }
}
