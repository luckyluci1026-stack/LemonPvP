package de.lemonpvp.fastshop.shop;

import org.bukkit.Material;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class WertRechner {

    public record Rezept(Material ergebnis, int menge, List<List<Material>> zutaten) {
    }

    public record Wert(double wert, boolean berechnet) {
    }

    private static final int MAX_RUNDEN = 200;

    private WertRechner() {
    }

    public static Map<Material, Wert> berechnen(Map<Material, Double> fest, Set<Material> gesperrt, List<Rezept> rezepte,
                                                Collection<Material> alle, double standard, double faktor) {
        Set<Material> ergebnisse = new HashSet<>();
        for (Rezept rezept : rezepte) {
            ergebnisse.add(rezept.ergebnis());
        }
        Map<Material, Double> basis = new HashMap<>(fest);
        for (Material material : alle) {
            if (!basis.containsKey(material) && !gesperrt.contains(material) && !ergebnisse.contains(material)) {
                basis.put(material, standard);
            }
        }
        Map<Material, Double> kosten = new HashMap<>();
        entspannen(basis, kosten, gesperrt, rezepte);
        boolean nachgetragen = false;
        for (Material material : ergebnisse) {
            if (!basis.containsKey(material) && !kosten.containsKey(material) && !gesperrt.contains(material)) {
                kosten.put(material, standard);
                nachgetragen = true;
            }
        }
        if (nachgetragen) {
            entspannen(basis, kosten, gesperrt, rezepte);
        }
        Map<Material, Wert> werte = new HashMap<>();
        for (Material material : alle) {
            if (gesperrt.contains(material) || fest.containsKey(material)) {
                continue;
            }
            Double kost = kosten.get(material);
            if (kost != null && !basis.containsKey(material)) {
                werte.put(material, new Wert(abgerundet(kost * faktor), true));
            } else {
                werte.put(material, new Wert(basis.getOrDefault(material, standard), false));
            }
        }
        behaelterAnpassen(werte, fest);
        angleichen(werte, fest, gesperrt, rezepte);
        return werte;
    }

    private static void entspannen(Map<Material, Double> basis, Map<Material, Double> kosten, Set<Material> gesperrt,
                                   List<Rezept> rezepte) {
        for (int runde = 0; runde < MAX_RUNDEN; runde++) {
            boolean geaendert = false;
            for (Rezept rezept : rezepte) {
                Material ergebnis = rezept.ergebnis();
                if (basis.containsKey(ergebnis) || gesperrt.contains(ergebnis) || rezept.menge() <= 0) {
                    continue;
                }
                double summe = summe(rezept, material -> basis.containsKey(material) ? basis.get(material) : kosten.get(material),
                        gesperrt);
                if (summe < 0) {
                    continue;
                }
                double kandidat = summe / rezept.menge();
                Double bisher = kosten.get(ergebnis);
                if (bisher == null || kandidat < bisher - 1e-9) {
                    kosten.put(ergebnis, kandidat);
                    geaendert = true;
                }
            }
            if (!geaendert) {
                return;
            }
        }
    }

    private static void angleichen(Map<Material, Wert> werte, Map<Material, Double> fest, Set<Material> gesperrt,
                                   List<Rezept> rezepte) {
        for (int runde = 0; runde < MAX_RUNDEN; runde++) {
            boolean geaendert = false;
            for (Rezept rezept : rezepte) {
                Wert ergebnis = werte.get(rezept.ergebnis());
                if (ergebnis == null || !ergebnis.berechnet() || rezept.menge() <= 0) {
                    continue;
                }
                double summe = summe(rezept, material -> {
                    if (fest.containsKey(material)) {
                        return fest.get(material);
                    }
                    Wert wert = werte.get(material);
                    return wert == null ? null : wert.wert();
                }, gesperrt);
                if (summe < 0) {
                    continue;
                }
                double hoechstens = abgerundet(summe / rezept.menge());
                if (ergebnis.wert() > hoechstens + 1e-9) {
                    werte.put(rezept.ergebnis(), new Wert(hoechstens, true));
                    geaendert = true;
                }
            }
            if (!geaendert) {
                return;
            }
        }
    }

    private static double summe(Rezept rezept, Function<Material, Double> wertVon, Set<Material> gesperrt) {
        double summe = 0;
        for (List<Material> optionen : rezept.zutaten()) {
            double billigste = Double.MAX_VALUE;
            for (Material option : optionen) {
                Double wert = gesperrt.contains(option) ? null : wertVon.apply(option);
                if (wert != null) {
                    billigste = Math.min(billigste, wert);
                }
            }
            if (billigste == Double.MAX_VALUE) {
                return -1;
            }
            summe += billigste;
        }
        return summe;
    }

    private static void behaelterAnpassen(Map<Material, Wert> werte, Map<Material, Double> fest) {
        double eimer = wertVon(Material.BUCKET, werte, fest);
        double flasche = wertVon(Material.GLASS_BOTTLE, werte, fest);
        for (Map.Entry<Material, Wert> eintrag : werte.entrySet()) {
            Material material = eintrag.getKey();
            double mindestens = 0;
            if (material.name().endsWith("_BUCKET")) {
                mindestens = eimer;
            } else if (material == Material.POTION || material == Material.SPLASH_POTION
                    || material == Material.LINGERING_POTION || material == Material.OMINOUS_BOTTLE) {
                mindestens = flasche;
            }
            if (mindestens > eintrag.getValue().wert()) {
                eintrag.setValue(new Wert(mindestens, false));
            }
        }
    }

    private static double wertVon(Material material, Map<Material, Wert> werte, Map<Material, Double> fest) {
        if (fest.containsKey(material)) {
            return fest.get(material);
        }
        Wert wert = werte.get(material);
        return wert == null ? 0 : wert.wert();
    }

    static double abgerundet(double wert) {
        if (wert <= 0) {
            return 0;
        }
        return Math.floor(wert * 100.0 + 1e-6) / 100.0;
    }
}
