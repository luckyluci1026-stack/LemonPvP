package de.lemonpvp.antiswear.filter;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatPruefung {

    public enum Kategorie { SPAM, DATEN, WERBUNG, WORT }

    public enum Aktion { DURCHLASSEN, GEAENDERT, BLOCKIERT }

    public record Ergebnis(Aktion aktion, String text, Kategorie kategorie, List<Treffer> treffer, int punkte,
                           String hinweis, String fund) {

        static Ergebnis durchlassen(String text) {
            return new Ergebnis(Aktion.DURCHLASSEN, text, null, List.of(), 0, null, null);
        }

        public boolean verstoss() {
            return kategorie != null;
        }

        public Ergebnis ohneHinweis() {
            return new Ergebnis(aktion, text, kategorie, treffer, punkte, null, fund);
        }
    }

    public record Einstellungen(boolean ersetzen, boolean spam, int spamPunkte, boolean werbung, int werbungPunkte,
                                boolean daten, int datenPunkte, boolean caps, int capsMinBuchstaben, double capsAnteil,
                                int zeichenMax) {

        public static Einstellungen standard() {
            return new Einstellungen(true, true, 1, true, 2, true, 0, true, 8, 0.7, 4);
        }
    }

    private final WortFilter woerter;
    private final SpamSchutz spam;
    private final WerbungFilter werbung;
    private final DatenFilter daten;
    private volatile Einstellungen einstellungen = Einstellungen.standard();

    public ChatPruefung(WortFilter woerter, SpamSchutz spam, WerbungFilter werbung, DatenFilter daten) {
        this.woerter = woerter;
        this.spam = spam;
        this.werbung = werbung;
        this.daten = daten;
    }

    public void setEinstellungen(Einstellungen einstellungen) {
        this.einstellungen = einstellungen;
    }

    public Einstellungen einstellungen() {
        return einstellungen;
    }

    public Ergebnis pruefen(UUID spieler, String text, boolean spamPruefen, long jetzt) {
        Einstellungen e = einstellungen;
        if (text == null || text.isBlank()) {
            return Ergebnis.durchlassen(text);
        }
        if (spamPruefen && e.spam() && spieler != null) {
            SpamSchutz.Ergebnis spamErgebnis = spam.pruefen(spieler, text, jetzt);
            if (spamErgebnis != SpamSchutz.Ergebnis.OK) {
                String hinweis = spamErgebnis == SpamSchutz.Ergebnis.ZU_SCHNELL ? "spam-schnell" : "spam-wiederholung";
                return new Ergebnis(Aktion.BLOCKIERT, text, Kategorie.SPAM, List.of(), e.spamPunkte(), hinweis, null);
            }
        }
        if (e.daten()) {
            String fund = daten.finden(text);
            if (fund != null) {
                return new Ergebnis(Aktion.BLOCKIERT, text, Kategorie.DATEN, List.of(), e.datenPunkte(), "daten", fund);
            }
        }
        if (e.werbung()) {
            String fund = werbung.finden(text);
            if (fund != null) {
                return new Ergebnis(Aktion.BLOCKIERT, text, Kategorie.WERBUNG, List.of(), e.werbungPunkte(), "werbung", fund);
            }
        }
        String geglaettet = glaetten(text, e);
        List<Treffer> treffer = woerter.pruefen(geglaettet);
        if (!treffer.isEmpty()) {
            int punkte = treffer.stream().mapToInt(Treffer::punkte).sum();
            if (e.ersetzen()) {
                String zensiert = woerter.zensieren(geglaettet);
                if (woerter.pruefen(zensiert).isEmpty()) {
                    return new Ergebnis(Aktion.GEAENDERT, zensiert, Kategorie.WORT, treffer, punkte, null, null);
                }
            }
            return new Ergebnis(Aktion.BLOCKIERT, text, Kategorie.WORT, treffer, punkte, "blockiert", null);
        }
        return geglaettet.equals(text) ? Ergebnis.durchlassen(text)
                : new Ergebnis(Aktion.GEAENDERT, geglaettet, null, List.of(), 0, null, null);
    }

    public Ergebnis pruefenOhneSpam(String text) {
        return pruefen(null, text, false, 0L);
    }

    public void vergessen(UUID spieler) {
        spam.vergessen(spieler);
    }

    static String glaetten(String text, Einstellungen e) {
        String ergebnis = text;
        if (e.zeichenMax() > 0) {
            ergebnis = zeichenKuerzen(ergebnis, e.zeichenMax());
        }
        if (e.caps()) {
            ergebnis = capsDaempfen(ergebnis, e.capsMinBuchstaben(), e.capsAnteil());
        }
        return ergebnis;
    }

    static String capsDaempfen(String text, int minBuchstaben, double anteil) {
        int buchstaben = 0;
        int gross = 0;
        for (int i = 0; i < text.length(); ) {
            int zeichen = text.codePointAt(i);
            if (Character.isLetter(zeichen)) {
                buchstaben++;
                if (Character.isUpperCase(zeichen)) {
                    gross++;
                }
            }
            i += Character.charCount(zeichen);
        }
        if (buchstaben >= minBuchstaben && gross >= anteil * buchstaben) {
            String klein = text.toLowerCase(Locale.GERMAN);
            return klein.isEmpty() ? klein : klein.substring(0, 1).toUpperCase(Locale.GERMAN) + klein.substring(1);
        }
        return text;
    }

    static String zeichenKuerzen(String text, int max) {
        Matcher matcher = Pattern.compile("(.)\\1{" + max + ",}").matcher(text);
        StringBuilder ergebnis = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(ergebnis, Matcher.quoteReplacement(matcher.group(1).repeat(max)));
        }
        matcher.appendTail(ergebnis);
        return ergebnis.toString();
    }
}
