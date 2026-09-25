package de.lemonpvp.antiswear.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Erkennt Woerter aus woerter.yml auch durch gaengige Umgehungsversuche
 * hindurch:
 *
 * - Leetspeak (3 statt e, 4 statt a, ...)
 * - Trennzeichen zwischen Buchstaben ("s.c.h.e.i.s.s.e", "s c h e i s s e")
 * - wiederholte Buchstaben ("scheeeiiisse")
 *
 * Woerter UND Chattext durchlaufen dieselbe Normalisierung (siehe
 * normalisiere()) - wichtig, weil sonst z.B. ein Wort mit eigenem
 * Doppelbuchstaben ("scheisse") nach dem Kollabieren von Wiederholungen
 * nicht mehr zum ebenso kollabierten Chat-Token passen wuerde.
 *
 * Fuer die Trennzeichen-Erkennung werden NUR aufeinanderfolgende
 * Einzelbuchstaben-Tokens zusammengefasst, nicht die ganze Nachricht -
 * sonst wuerden aus mehreren normalen, kurzen Woertern hintereinander
 * ("ich sehe dich") leicht falsche Treffer entstehen.
 */
public final class WortFilter {

    private record Eintrag(String original, String normalisiert, int punkte) {
    }

    private static final Map<Character, Character> LEETSPEAK = Map.of(
            '4', 'a', '3', 'e', '1', 'i', '0', 'o', '5', 's', '7', 't', '@', 'a', '$', 's');
    private static final Pattern NICHT_BUCHSTABE = Pattern.compile("[^a-z]");
    private static final Pattern WIEDERHOLUNG = Pattern.compile("(.)\\1+");

    private final List<Eintrag> woerter = new ArrayList<>();
    private boolean leetspeak = true;
    private boolean trennzeichen = true;
    private boolean wiederholungen = true;

    public void konfigurieren(boolean leetspeak, boolean trennzeichen, boolean wiederholungen) {
        this.leetspeak = leetspeak;
        this.trennzeichen = trennzeichen;
        this.wiederholungen = wiederholungen;
    }

    /** Immer erst konfigurieren(), DANACH setWoerter() - die Normalisierung braucht die aktuellen Einstellungen. */
    public void setWoerter(Map<String, Integer> neu) {
        woerter.clear();
        for (Map.Entry<String, Integer> eintrag : neu.entrySet()) {
            String original = eintrag.getKey();
            Integer punkte = eintrag.getValue();
            if (original == null || original.isBlank() || punkte == null) {
                continue;
            }
            String normalisiert = normalisiere(original);
            if (!normalisiert.isEmpty()) {
                woerter.add(new Eintrag(original, normalisiert, punkte));
            }
        }
    }

    public List<String> woerter() {
        return woerter.stream().map(Eintrag::original).toList();
    }

    /** Prueft eine ganze Chatnachricht. Leer, wenn nichts gefunden wurde. */
    public List<Treffer> pruefen(String nachricht) {
        List<Treffer> treffer = new ArrayList<>();
        if (nachricht == null || nachricht.isBlank() || woerter.isEmpty()) {
            return treffer;
        }
        for (String token : normalisierteTokens(nachricht)) {
            for (Eintrag eintrag : woerter) {
                if (token.contains(eintrag.normalisiert())) {
                    treffer.add(new Treffer(eintrag.original(), eintrag.punkte()));
                }
            }
        }
        return treffer;
    }

    /**
     * Ersetzt jedes woertliche Vorkommen (Original-Schreibweise, gross-/
     * kleinschreibungsunabhaengig) im ORIGINALEN Chattext durch Sternchen.
     * Faengt die einfachen Faelle inklusive anderer Gross-/Kleinschreibung,
     * aber keine per Leetspeak/Trennzeichen/Wiederholung verschleierten
     * Varianten - dafuer prueft der Aufrufer das Ergebnis noch einmal mit
     * pruefen() und blockiert im Zweifel lieber die ganze Nachricht, statt
     * schlecht zu zensieren.
     */
    public String zensieren(String nachricht) {
        String ergebnis = nachricht;
        for (Eintrag eintrag : woerter) {
            Pattern pattern = Pattern.compile(Pattern.quote(eintrag.original()), Pattern.CASE_INSENSITIVE);
            ergebnis = pattern.matcher(ergebnis).replaceAll("*".repeat(eintrag.original().length()));
        }
        return ergebnis;
    }

    private List<String> normalisierteTokens(String nachricht) {
        String[] rohTokens = nachricht.trim().split("\\s+");
        List<String> zusammengefasst = new ArrayList<>();
        StringBuilder einzelbuchstaben = new StringBuilder();
        for (String roh : rohTokens) {
            String bereinigt = saeubern(roh);
            if (trennzeichen && bereinigt.length() == 1) {
                einzelbuchstaben.append(bereinigt);
                continue;
            }
            if (!einzelbuchstaben.isEmpty()) {
                zusammengefasst.add(kollabieren(einzelbuchstaben.toString()));
                einzelbuchstaben.setLength(0);
            }
            zusammengefasst.add(kollabieren(bereinigt));
        }
        if (!einzelbuchstaben.isEmpty()) {
            zusammengefasst.add(kollabieren(einzelbuchstaben.toString()));
        }
        return zusammengefasst.stream().filter(s -> !s.isEmpty()).toList();
    }

    /** Gleiche Normalisierung wie fuer Chat-Tokens - fuer ein einzelnes Wort ohne Zusammenfassung noetig. */
    private String normalisiere(String original) {
        return kollabieren(saeubern(original));
    }

    /** Lowercase + Leetspeak + nur Buchstaben. Laeuft immer, unabhaengig von trennzeichen (Satzzeichen an einem Wort sind keine Umgehung, nur normale Tipperei). */
    private String saeubern(String token) {
        String s = ersetzeLeetspeak(token.toLowerCase(Locale.ROOT));
        return NICHT_BUCHSTABE.matcher(s).replaceAll("");
    }

    /** Jede Kette gleicher Buchstaben auf genau einen zusammenstauchen, z.B. "scheeiisse" -> "scheise". */
    private String kollabieren(String token) {
        return wiederholungen ? WIEDERHOLUNG.matcher(token).replaceAll("$1") : token;
    }

    private String ersetzeLeetspeak(String eingabe) {
        if (!leetspeak) {
            return eingabe;
        }
        StringBuilder sb = new StringBuilder(eingabe.length());
        for (char c : eingabe.toCharArray()) {
            sb.append(LEETSPEAK.getOrDefault(c, c));
        }
        return sb.toString();
    }
}
