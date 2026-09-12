package de.lemonpvp.punishplus.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dauern parsen und anzeigen: 30m, 12h, 7d, 2w | perm/0 = fuer immer.
 *
 * Dieselbe Schreibweise wie bei SMPProxys eigenem /netban (siehe dessen
 * ban/Durations.java) - bewusst noch einmal hier und nicht als
 * gemeinsame Abhaengigkeit: Zwei eigenstaendige Velocity-Plugins in
 * getrennten Modulen mit je eigenem Classloader. Ein gemeinsames drittes
 * Modul nur fuer diese paar Zeilen waere mehr Umstand als die Dopplung.
 */
public final class Durations {

    private static final Pattern TOKEN = Pattern.compile("(\\d+)\\s*([smhdw])", Pattern.CASE_INSENSITIVE);
    private static final SimpleDateFormat DATE = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY);

    private Durations() {
    }

    /** @return Millisekunden, oder 0 fuer permanent/nicht erkannt. */
    public static long parse(String input) {
        if (input == null) {
            return 0;
        }
        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.equals("perm") || s.equals("permanent") || s.equals("0")) {
            return 0;
        }
        long total = 0;
        Matcher m = TOKEN.matcher(s);
        boolean found = false;
        while (m.find()) {
            found = true;
            long value = Long.parseLong(m.group(1));
            total += switch (Character.toLowerCase(m.group(2).charAt(0))) {
                case 's' -> value * 1000L;
                case 'm' -> value * 60_000L;
                case 'h' -> value * 3_600_000L;
                case 'd' -> value * 86_400_000L;
                case 'w' -> value * 604_800_000L;
                default -> 0L;
            };
        }
        return found ? total : 0;
    }

    /** Menschliche Restdauer, z.B. "2d 3h 15m". */
    public static String humanize(long millis) {
        if (millis <= 0) {
            return "0m";
        }
        long days = millis / 86_400_000L;
        long hours = (millis % 86_400_000L) / 3_600_000L;
        long minutes = (millis % 3_600_000L) / 60_000L;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || sb.isEmpty()) {
            sb.append(Math.max(1, minutes)).append("m");
        }
        return sb.toString().trim();
    }

    public static String expiry(long expiresEpochMillis, String permanentWord) {
        if (expiresEpochMillis <= 0) {
            return permanentWord;
        }
        return DATE.format(new Date(expiresEpochMillis));
    }
}
