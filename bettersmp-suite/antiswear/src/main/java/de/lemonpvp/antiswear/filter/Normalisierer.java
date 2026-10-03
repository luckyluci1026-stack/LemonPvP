package de.lemonpvp.antiswear.filter;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class Normalisierer {

    public record Lauf(char zeichen, int anzahl) {
    }

    private static final Pattern FARBCODE = Pattern.compile("[&§](x([&§][0-9a-f]){6}|[0-9a-fk-or])", Pattern.CASE_INSENSITIVE);
    public static final Pattern TRENNER = Pattern.compile("[\\s\\p{Z}]+");
    private static final String LEET = "0123456789@$€(<+!|#¢¥";
    private static final String RAND_LEET = "@$€";
    private static final Map<Integer, String> ZEICHEN = new HashMap<>();

    static {
        abbilden("a", "4@àáâãäåāăąɑαаӓ");
        abbilden("b", "8βвьъб");
        abbilden("c", "(<¢çćĉċčсςϲ");
        abbilden("d", "ďđδдԁ");
        abbilden("e", "3€èéêëēĕėęěεеёэ℮");
        abbilden("f", "ƒφ");
        abbilden("g", "96ĝğġģɡ");
        abbilden("h", "#ĥħнһ");
        abbilden("i", "1!|lìíîïĩīĭįıłιіїℓ");
        abbilden("j", "ĵјʝ");
        abbilden("k", "ķκкқ");
        abbilden("m", "мμ");
        abbilden("n", "ñńņňŋηпπ");
        abbilden("o", "0òóôõöøōŏőοθоσ");
        abbilden("p", "ρр");
        abbilden("r", "ŕŗřгя");
        abbilden("s", "5$śŝşšѕſ");
        abbilden("t", "7+ţťŧτт");
        abbilden("u", "ùúûüũūŭůűųυμц");
        abbilden("w", "ŵωшщ");
        abbilden("x", "χхж");
        abbilden("y", "¥ýÿŷγуў");
        abbilden("z", "2źżžζз");
        ZEICHEN.put((int) 'ß', "ss");
        ZEICHEN.put((int) 'æ', "ae");
        ZEICHEN.put((int) 'œ', "oe");
        for (char c = 'a'; c <= 'z'; c++) {
            ZEICHEN.putIfAbsent((int) c, String.valueOf(c));
        }
    }

    private Normalisierer() {
    }

    private static void abbilden(String ziel, String quellen) {
        quellen.codePoints().forEach(zeichen -> ZEICHEN.put(zeichen, ziel));
    }

    public static List<String> tokens(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        for (String teil : TRENNER.split(text.trim())) {
            if (!teil.isEmpty()) {
                tokens.add(teil);
            }
        }
        return tokens;
    }

    public static String bereinigen(String text) {
        String ohneFarben = FARBCODE.matcher(text).replaceAll("");
        String zerlegt = Normalizer.normalize(ohneFarben, Normalizer.Form.NFKD);
        StringBuilder ergebnis = new StringBuilder(zerlegt.length());
        zerlegt.codePoints().forEach(zeichen -> {
            int typ = Character.getType(zeichen);
            if (typ == Character.NON_SPACING_MARK || typ == Character.ENCLOSING_MARK
                    || typ == Character.COMBINING_SPACING_MARK || typ == Character.FORMAT
                    || typ == Character.CONTROL) {
                return;
            }
            ergebnis.appendCodePoint(Character.toLowerCase(zeichen));
        });
        return ergebnis.toString().toLowerCase(Locale.ROOT);
    }

    public static String skelett(String token) {
        return skelett(token, true);
    }

    public static String skelett(String token, boolean leetspeak) {
        String sauber = randZeichenEntfernen(bereinigen(token));
        StringBuilder buchstaben = new StringBuilder(sauber.length());
        sauber.codePoints().forEach(zeichen -> {
            if (!leetspeak && LEET.indexOf(zeichen) >= 0) {
                return;
            }
            String ersatz = zeichen == 'v' ? "v" : ZEICHEN.get(zeichen);
            if (ersatz != null) {
                buchstaben.append(ersatz);
            }
        });
        String text = buchstaben.toString().replace("vv", "w");
        text = text.replace("ae", "a").replace("oe", "o").replace("ue", "u");
        return text.replace('v', 'u');
    }

    private static String randZeichenEntfernen(String text) {
        int start = 0;
        int ende = text.length();
        while (start < ende && istRandzeichen(text.codePointAt(start))) {
            start += Character.charCount(text.codePointAt(start));
        }
        while (ende > start && istRandzeichen(text.codePointBefore(ende))) {
            ende -= Character.charCount(text.codePointBefore(ende));
        }
        return text.substring(start, ende);
    }

    private static boolean istRandzeichen(int zeichen) {
        return !Character.isLetterOrDigit(zeichen) && RAND_LEET.indexOf(zeichen) < 0;
    }

    public static List<Lauf> laeufe(String skelett) {
        List<Lauf> laeufe = new ArrayList<>();
        int i = 0;
        while (i < skelett.length()) {
            char zeichen = skelett.charAt(i);
            int anzahl = 1;
            while (i + anzahl < skelett.length() && skelett.charAt(i + anzahl) == zeichen) {
                anzahl++;
            }
            laeufe.add(new Lauf(zeichen, anzahl));
            i += anzahl;
        }
        return laeufe;
    }

    public static String zusammenziehen(List<Lauf> laeufe) {
        StringBuilder text = new StringBuilder(laeufe.size());
        for (Lauf lauf : laeufe) {
            text.append(lauf.zeichen());
        }
        return text.toString();
    }
}
