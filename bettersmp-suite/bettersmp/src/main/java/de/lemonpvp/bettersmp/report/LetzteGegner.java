package de.lemonpvp.bettersmp.report;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

public final class LetzteGegner {

    private static final long GUELTIG_MILLIS = 2L * 60L * 60L * 1000L;
    private static final int PRO_SPIELER = 5;

    private record Eintrag(Ziel ziel, long zeit) {
    }

    private final Map<UUID, Deque<Eintrag>> nachSpieler = new HashMap<>();
    private final LongSupplier uhr;

    public LetzteGegner() {
        this(System::currentTimeMillis);
    }

    public LetzteGegner(LongSupplier uhr) {
        this.uhr = uhr;
    }

    public synchronized void merken(UUID spieler, UUID gegner, String name) {
        if (spieler == null || gegner == null || name == null || name.isBlank() || spieler.equals(gegner)) {
            return;
        }
        Deque<Eintrag> liste = nachSpieler.computeIfAbsent(spieler, id -> new ArrayDeque<>());
        liste.removeIf(eintrag -> eintrag.ziel().id().equals(gegner));
        liste.addFirst(new Eintrag(new Ziel(gegner, name), uhr.getAsLong()));
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

    public synchronized Optional<Ziel> finde(UUID melder, String name) {
        for (Ziel ziel : letzte(melder)) {
            if (ziel.heisst(name)) {
                return Optional.of(ziel);
            }
        }
        long grenze = uhr.getAsLong() - GUELTIG_MILLIS;
        Eintrag bester = null;
        for (Deque<Eintrag> liste : nachSpieler.values()) {
            for (Eintrag eintrag : liste) {
                if (eintrag.zeit() >= grenze && eintrag.ziel().heisst(name)
                        && (bester == null || eintrag.zeit() > bester.zeit())) {
                    bester = eintrag;
                }
            }
        }
        return bester == null ? Optional.empty() : Optional.of(bester.ziel());
    }
}
