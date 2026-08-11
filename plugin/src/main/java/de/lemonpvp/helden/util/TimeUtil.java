package de.lemonpvp.helden.util;

public final class TimeUtil {

    private TimeUtil() {
    }

    /** Formatiert Sekunden als "1h 5m", "5m 30s" oder "42s". */
    public static String format(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder builder = new StringBuilder();
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0) {
            builder.append(minutes).append("m ");
        }
        if (secs > 0 && hours == 0) {
            builder.append(secs).append("s");
        }
        return builder.toString().trim();
    }

    /** Formatiert Sekunden als mm:ss - kompakt genug fuer das Scoreboard. */
    public static String clock(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    /** Rundet Millisekunden auf volle Sekunden auf. */
    public static long toSecondsCeil(long millis) {
        return (millis + 999L) / 1000L;
    }
}
