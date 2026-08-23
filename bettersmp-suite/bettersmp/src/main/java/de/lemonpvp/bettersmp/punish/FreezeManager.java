package de.lemonpvp.bettersmp.punish;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wer gerade eingefroren ist.
 *
 * Bewusst nur im Arbeitsspeicher: Ein Einfrieren soll die Ausnahme fuer
 * die naechsten paar Minuten sein, waehrend ein Teammitglied sich einen
 * Verdachtsfall ansieht - keine Strafe, die einen Serverneustart
 * ueberleben muesste. Nach einem Neustart ist jeder wieder frei, und
 * das ist richtig so.
 */
public final class FreezeManager {

    private final Set<UUID> eingefroren = ConcurrentHashMap.newKeySet();

    public boolean istEingefroren(UUID spieler) {
        return eingefroren.contains(spieler);
    }

    /** @return true, wenn danach eingefroren ist (vorher war er es nicht) */
    public boolean umschalten(UUID spieler) {
        if (eingefroren.remove(spieler)) {
            return false;
        }
        eingefroren.add(spieler);
        return true;
    }

    public void vergessen(UUID spieler) {
        eingefroren.remove(spieler);
    }
}
