package de.lemonpvp.punishplus.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dauer-Strings wie "30m 12h 7d 2w" - dieselbe Schreibweise wie im Rest
 * der Suite (BetterSMP, SMPProxy). "perm" oder "0" = dauerhaft. Absichtlich
 * eine eigene, kleine Kopie statt einer Abhaengigkeit zu einem anderen Modul.
 */
public final class Durations {

    private static final Pattern TEIL = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);

    private Durations() {
    }

    /** @return Millisekunden, 0 = dauerhaft. */
    public static long parse(String text) {
        if (text == null) {
            return 0L;
        }
        String getrimmt = text.trim();
        if (getrimmt.equalsIgnoreCase("perm") || getrimmt.equals("0")) {
            return 0L;
        }
        long millis = 0L;
        Matcher m = TEIL.matcher(getrimmt);
        while (m.find()) {
            long menge = Long.parseLong(m.group(1));
            millis += menge * einheitInMillis(m.group(2).toLowerCase(Locale.ROOT));
        }
        return millis;
    }

    private static long einheitInMillis(String einheit) {
        return switch (einheit) {
            case "s" -> 1_000L;
            case "m" -> 60_000L;
            case "h" -> 3_600_000L;
            case "d" -> 86_400_000L;
            case "w" -> 604_800_000L;
            default -> 0L;
        };
    }

    public static String humanize(long millis) {
        if (millis <= 0) {
            return "0m";
        }
        long tage = millis / 86_400_000L;
        long stunden = (millis % 86_400_000L) / 3_600_000L;
        long minuten = (millis % 3_600_000L) / 60_000L;
        StringBuilder sb = new StringBuilder();
        if (tage > 0) {
            sb.append(tage).append("d ");
        }
        if (stunden > 0) {
            sb.append(stunden).append("h ");
        }
        if (minuten > 0 || sb.isEmpty()) {
            sb.append(minuten).append("m");
        }
        return sb.toString().trim();
    }
}
