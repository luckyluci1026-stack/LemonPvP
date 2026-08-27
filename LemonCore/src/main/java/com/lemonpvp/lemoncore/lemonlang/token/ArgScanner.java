package com.lemonpvp.lemoncore.lemonlang.token;

import com.lemonpvp.lemoncore.lemonlang.LemonLangError;

import java.util.ArrayList;
import java.util.List;

/**
 * Kleine Hilfsklasse zum Zerlegen von Argumenttext eines Statements.
 *
 * <p>Die Sprache kennt keine Anfuehrungszeichen, daher wird einfach an Whitespace
 * getrennt. Zusaetzlich werden Zeitangaben wie "5s", "10m", "2h", "1d" geparst.
 */
public final class ArgScanner {

    private ArgScanner() {
    }

    /**
     * Zerlegt einen String an beliebigem Whitespace in einzelne Woerter.
     * Mehrfacher und fuehrender/abschliessender Whitespace wird ignoriert.
     * Leere Eingabe ergibt eine leere Liste.
     */
    public static List<String> words(String s) {
        List<String> result = new ArrayList<>();
        if (s == null) {
            return result;
        }
        int len = s.length();
        int i = 0;
        while (i < len) {
            // Whitespace ueberspringen.
            while (i < len && isWhitespace(s.charAt(i))) {
                i++;
            }
            if (i >= len) {
                break;
            }
            int start = i;
            while (i < len && !isWhitespace(s.charAt(i))) {
                i++;
            }
            result.add(s.substring(start, i));
        }
        return result;
    }

    /**
     * Parst eine Zeitangabe ("5s"/"10m"/"2h"/"1d") in Millisekunden.
     *
     * <p>Einheiten: s=Sekunden, m=Minuten, h=Stunden, d=Tage. Ohne gueltige
     * Einheit oder Zahl wird ein {@link LemonLangError} mit deutschem Hinweis geworfen.
     */
    public static long parseDuration(String s, String scriptFile, int line) {
        if (s == null || s.isEmpty()) {
            throw new LemonLangError(
                    scriptFile,
                    line,
                    "Leere Zeitangabe.",
                    "Beispiele: 5s, 10m, 2h, 1d"
            );
        }

        String trimmed = s.trim();
        char unit = trimmed.charAt(trimmed.length() - 1);
        String numberPart = trimmed.substring(0, trimmed.length() - 1);

        long multiplier;
        switch (unit) {
            case 's':
            case 'S':
                multiplier = 1000L;
                break;
            case 'm':
            case 'M':
                multiplier = 60L * 1000L;
                break;
            case 'h':
            case 'H':
                multiplier = 60L * 60L * 1000L;
                break;
            case 'd':
            case 'D':
                multiplier = 24L * 60L * 60L * 1000L;
                break;
            default:
                throw new LemonLangError(
                        scriptFile,
                        line,
                        "Unbekannte Zeiteinheit in '" + trimmed + "'.",
                        "Erlaubte Einheiten: s (Sekunden), m (Minuten), h (Stunden), d (Tage). Beispiel: 5s"
                );
        }

        if (numberPart.isEmpty()) {
            throw new LemonLangError(
                    scriptFile,
                    line,
                    "Fehlende Zahl in Zeitangabe '" + trimmed + "'.",
                    "Beispiele: 5s, 10m, 2h, 1d"
            );
        }

        long value;
        try {
            value = Long.parseLong(numberPart);
        } catch (NumberFormatException ex) {
            throw new LemonLangError(
                    scriptFile,
                    line,
                    "Ungueltige Zahl '" + numberPart + "' in Zeitangabe '" + trimmed + "'.",
                    "Beispiele: 5s, 10m, 2h, 1d"
            );
        }

        if (value < 0) {
            throw new LemonLangError(
                    scriptFile,
                    line,
                    "Negative Zeitangabe '" + trimmed + "' ist nicht erlaubt.",
                    "Bitte eine Zahl groesser oder gleich 0 verwenden. Beispiel: 5s"
            );
        }

        return value * multiplier;
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == 0x0B;
    }
}