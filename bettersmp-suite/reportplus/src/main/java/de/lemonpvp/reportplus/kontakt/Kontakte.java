package de.lemonpvp.reportplus.kontakt;

import de.lemonpvp.reportplus.util.Ziel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

public final class Kontakte {

    private static final long GUELTIG_MILLIS = 2L * 60L * 60L * 1000L;
    private static final int PRO_SPIELER = 5;

    private record Eintrag(Ziel ziel, long zeit) {
    }

    private final Map<UUID, Deque<Eintrag>> nachSpieler = new HashMap<>();
    private final LongSupplier uhr;

    public Kontakte() {
        this(System::currentTimeMillis);
    }

    public Kontakte(LongSupplier uhr) {
        this.uhr = uhr;
    }

    public synchronized void merken(UUID spieler, UUID anderer, String name) {
        if (spieler == null || anderer == null || name == null || name.isBlank() || spieler.equals(anderer)) {
            return;
        }
        Deque<Eintrag> liste = nachSpieler.computeIfAbsent(spieler, id -> new ArrayDeque<>());
        liste.removeIf(eintrag -> eintrag.ziel().id().equals(anderer));
        liste.addFirst(new Eintrag(new Ziel(anderer, name), uhr.getAsLong()));
        while (liste.size() > PRO_SPIELER) {
            liste.removeLast();
        }
    }

    public synchronized List<Ziel> letzte(UUID spieler) {
        Deque<Eintrag> liste = nachSpieler.get(spieler);
        if (liste == null) {
            return List.of();
        }
        long grenze = uhr.getAsLong() - GUELTIG_MILLIS;
        liste.removeIf(eintrag -> eintrag.zeit() < grenze);
        if (liste.isEmpty()) {
            nachSpieler.remove(spieler);
            return List.of();
        }
        List<Ziel> ziele = new ArrayList<>();
        for (Eintrag eintrag : liste) {
            ziele.add(eintrag.ziel());
        }
        return ziele;
    }

    public synchronized Optional<Ziel> finde(String name) {
        long grenze = uhr.getAsLong() - GUELTIG_MILLIS;
        Eintrag bester = null;
        for (Deque<Eintrag> liste : nachSpieler.values()) {
            for (Eintrag eintrag : liste) {
                if (eintrag.zeit() >= grenze && eintrag.ziel().heisst(name) && (bester == null || eintrag.zeit() > bester.zeit())) {
                    bester = eintrag;
                }
            }
        }
        return bester == null ? Optional.empty() : Optional.of(bester.ziel());
    }
}
