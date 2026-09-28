package de.lemonpvp.fastshop.auktion;

import java.util.Comparator;

public enum Sortierung {

    NEUESTE("Neueste zuerst", Comparator.comparingLong(Angebot::erstellt).reversed()),
    GUENSTIGSTE("Günstigste zuerst", Comparator.comparingDouble(Angebot::preis).thenComparing(Comparator.comparingLong(Angebot::erstellt).reversed())),
    TEUERSTE("Teuerste zuerst", Comparator.comparingDouble(Angebot::preis).reversed().thenComparing(Comparator.comparingLong(Angebot::erstellt).reversed())),
    ENDET_BALD("Endet bald", Comparator.comparingLong(Angebot::endet));

    private final String anzeige;
    private final Comparator<Angebot> reihenfolge;

    Sortierung(String anzeige, Comparator<Angebot> reihenfolge) {
        this.anzeige = anzeige;
        this.reihenfolge = reihenfolge;
    }

    public String anzeige() {
        return anzeige;
    }

    public Comparator<Angebot> reihenfolge() {
        return reihenfolge;
    }

    public Sortierung naechste() {
        Sortierung[] alle = values();
        return alle[(ordinal() + 1) % alle.length];
    }
}
