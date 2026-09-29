package de.lemonpvp.antiswear.filter;

import de.lemonpvp.antiswear.filter.Normalisierer.Lauf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WortFilter {

    public enum Modus { TEIL, WORT }

    public record Eintrag(String wort, int punkte, Modus modus, List<Lauf> muster, int laenge) {
    }

    public record Definition(String wort, int punkte, Modus modus) {
    }

    private record Token(String skelett, List<Lauf> laeufe) {
    }

    private record Fund(Eintrag eintrag, int von, int bis) {
    }

    private static final Set<String> ENDUNGEN = Set.of("", "e", "en", "er", "ern", "es", "s", "n", "in", "inen",
            "i", "is", "y", "ie", "ies", "ed", "ing", "t", "st", "te", "ten", "et");
    private static final int TEIL_AB = 6;
    private static final int FENSTER_AB = 8;
    private static final int MAX_FENSTER = 3;
    private static final Pattern TOKEN = Pattern.compile("[^\\s\\p{Z}]+");

    private final List<Eintrag> woerter = new ArrayList<>();
    private final List<List<Lauf>> ausnahmen = new ArrayList<>();
    private boolean leetspeak = true;
    private boolean trennzeichen = true;
    private boolean wiederholungen = true;

    public void konfigurieren(boolean leetspeak, boolean trennzeichen, boolean wiederholungen) {
        this.leetspeak = leetspeak;
        this.trennzeichen = trennzeichen;
        this.wiederholungen = wiederholungen;
    }

    public void setWoerter(Map<String, Integer> neu) {
        List<Definition> definitionen = new ArrayList<>();
        neu.forEach((wort, punkte) -> {
            if (punkte != null) {
                definitionen.add(new Definition(wort, punkte, null));
            }
        });
        setDefinitionen(definitionen, List.of());
    }

    public void setDefinitionen(Collection<Definition> definitionen, Collection<String> ausnahmeWoerter) {
        woerter.clear();
        ausnahmen.clear();
        for (Definition definition : definitionen) {
            if (definition.wort() == null || definition.wort().isBlank()) {
                continue;
            }
            String skelett = Normalisierer.skelett(definition.wort().replace(" ", ""), leetspeak);
            if (skelett.isEmpty()) {
                continue;
            }
            List<Lauf> muster = Normalisierer.laeufe(skelett);
            Modus modus = definition.modus() != null ? definition.modus()
                    : skelett.length() >= TEIL_AB ? Modus.TEIL : Modus.WORT;
            woerter.add(new Eintrag(definition.wort().toLowerCase(Locale.ROOT), Math.max(0, definition.punkte()),
                    modus, muster, skelett.length()));
        }
        for (String ausnahme : ausnahmeWoerter) {
            String skelett = ausnahme == null ? "" : Normalisierer.skelett(ausnahme.replace(" ", ""), leetspeak);
            if (!skelett.isEmpty()) {
                ausnahmen.add(Normalisierer.laeufe(skelett));
            }
        }
    }

    public List<String> woerter() {
        return woerter.stream().map(Eintrag::wort).toList();
    }

    public List<Eintrag> eintraege() {
        return List.copyOf(woerter);
    }

    public int anzahlAusnahmen() {
        return ausnahmen.size();
    }

    public List<Treffer> pruefen(String nachricht) {
        Map<String, Treffer> eindeutig = new LinkedHashMap<>();
        for (Fund fund : finden(tokens(nachricht))) {
            eindeutig.putIfAbsent(fund.eintrag().wort(), new Treffer(fund.eintrag().wort(), fund.eintrag().punkte()));
        }
        return new ArrayList<>(eindeutig.values());
    }

    public String zensieren(String nachricht) {
        if (nachricht == null || nachricht.isBlank()) {
            return nachricht;
        }
        List<Token> tokens = tokens(nachricht);
        List<Fund> funde = finden(tokens);
        if (funde.isEmpty()) {
            return nachricht;
        }
        boolean[] weg = new boolean[tokens.size()];
        for (Fund fund : funde) {
            for (int i = fund.von(); i <= fund.bis(); i++) {
                weg[i] = true;
            }
        }
        StringBuilder ergebnis = new StringBuilder(nachricht.length());
        Matcher teile = TOKEN.matcher(nachricht);
        int letzte = 0;
        int index = 0;
        while (teile.find()) {
            ergebnis.append(nachricht, letzte, teile.start());
            String teil = teile.group();
            ergebnis.append(index < weg.length && weg[index] ? "*".repeat(teil.codePointCount(0, teil.length())) : teil);
            letzte = teile.end();
            index++;
        }
        ergebnis.append(nachricht.substring(letzte));
        return ergebnis.toString();
    }

    private List<Token> tokens(String nachricht) {
        List<Token> tokens = new ArrayList<>();
        if (nachricht == null) {
            return tokens;
        }
        for (String roh : Normalisierer.tokens(nachricht)) {
            String skelett = Normalisierer.skelett(roh, leetspeak);
            tokens.add(new Token(skelett, Normalisierer.laeufe(skelett)));
        }
        return tokens;
    }

    private List<Fund> finden(List<Token> tokens) {
        List<Fund> funde = new ArrayList<>();
        if (woerter.isEmpty() || tokens.isEmpty()) {
            return funde;
        }
        for (int i = 0; i < tokens.size(); i++) {
            if (!tokens.get(i).skelett().isEmpty()) {
                pruefeBereich(tokens.get(i).laeufe(), i, i, false, funde);
            }
        }
        if (!trennzeichen) {
            return funde;
        }
        int start = -1;
        int einzelne = 0;
        for (int i = 0; i <= tokens.size(); i++) {
            String skelett = i < tokens.size() ? tokens.get(i).skelett() : null;
            if (skelett != null && skelett.length() <= 1) {
                if (skelett.length() == 1) {
                    if (start < 0) {
                        start = i;
                    }
                    einzelne++;
                }
                continue;
            }
            if (einzelne >= 2) {
                int ende = i - 1;
                while (tokens.get(ende).skelett().isEmpty()) {
                    ende--;
                }
                pruefeBereich(Normalisierer.laeufe(verbinden(tokens, start, ende)), start, ende, false, funde);
            }
            start = -1;
            einzelne = 0;
        }
        for (int breite = 2; breite <= MAX_FENSTER; breite++) {
            for (int von = 0; von + breite <= tokens.size(); von++) {
                int bis = von + breite - 1;
                if (tokens.get(von).skelett().isEmpty() || tokens.get(bis).skelett().isEmpty()) {
                    continue;
                }
                pruefeBereich(Normalisierer.laeufe(verbinden(tokens, von, bis)), von, bis, true, funde);
            }
        }
        return funde;
    }

    private static String verbinden(List<Token> tokens, int von, int bis) {
        StringBuilder text = new StringBuilder();
        for (int i = von; i <= bis; i++) {
            text.append(tokens.get(i).skelett());
        }
        return text.toString();
    }

    private void pruefeBereich(List<Lauf> laeufe, int von, int bis, boolean fenster, List<Fund> funde) {
        for (Eintrag eintrag : woerter) {
            if (fenster && (eintrag.modus() != Modus.TEIL || eintrag.laenge() < FENSTER_AB
                    || schonGefunden(funde, eintrag, von, bis))) {
                continue;
            }
            boolean treffer = eintrag.modus() == Modus.WORT ? wortPasst(laeufe, eintrag.muster()) : teilPasst(laeufe, eintrag.muster());
            if (treffer) {
                funde.add(new Fund(eintrag, von, bis));
            }
        }
    }

    private static boolean schonGefunden(List<Fund> funde, Eintrag eintrag, int von, int bis) {
        for (Fund fund : funde) {
            if (fund.eintrag() == eintrag && fund.von() <= bis && fund.bis() >= von) {
                return true;
            }
        }
        return false;
    }

    private boolean wortPasst(List<Lauf> text, List<Lauf> muster) {
        if (text.size() < muster.size() || !passtAb(text, muster, 0)) {
            return false;
        }
        StringBuilder rest = new StringBuilder();
        for (int i = muster.size(); i < text.size(); i++) {
            rest.append(String.valueOf(text.get(i).zeichen()).repeat(text.get(i).anzahl()));
        }
        return ENDUNGEN.contains(rest.toString());
    }

    private boolean teilPasst(List<Lauf> text, List<Lauf> muster) {
        for (int j = 0; j + muster.size() <= text.size(); j++) {
            if (passtAb(text, muster, j) && !entschuldigt(text, j, j + muster.size() - 1)) {
                return true;
            }
        }
        return false;
    }

    private boolean entschuldigt(List<Lauf> text, int von, int bis) {
        for (List<Lauf> ausnahme : ausnahmen) {
            for (int p = Math.max(0, bis - ausnahme.size() + 1); p <= von && p + ausnahme.size() <= text.size(); p++) {
                if (passtAb(text, ausnahme, p)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean passtAb(List<Lauf> text, List<Lauf> muster, int ab) {
        for (int i = 0; i < muster.size(); i++) {
            Lauf t = text.get(ab + i);
            Lauf m = muster.get(i);
            if (t.zeichen() != m.zeichen()) {
                return false;
            }
            if (wiederholungen ? t.anzahl() < m.anzahl() : t.anzahl() != m.anzahl()) {
                return false;
            }
        }
        return true;
    }
}
