package de.lemonpvp.antiswear.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DatenFilter {

    private static final Pattern EMAIL = Pattern.compile("[a-z0-9._%+-]+\\s*(?:@|\\(\\s*at\\s*\\)|\\[\\s*at\\s*\\])\\s*"
            + "[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\s*(?:\\.|\\(\\s*(?:dot|punkt)\\s*\\)|\\[\\s*(?:dot|punkt)\\s*\\])\\s*[a-z]{2,}");
    private static final Pattern HANDY = Pattern.compile("(?<![\\d])(?:(?:\\+|00)\\s*49[\\s\\-/]*(?:\\(\\s*0\\s*\\)[\\s\\-/]*)?1[5-7]"
            + "|0[\\s\\-/]*1[5-7])[\\s\\-/]*\\d(?:[\\s\\-/]*\\d){6,9}(?![\\d])");

    public String finden(String nachricht) {
        if (nachricht == null || nachricht.isBlank()) {
            return null;
        }
        String text = Normalisierer.bereinigen(nachricht);
        Matcher email = EMAIL.matcher(text);
        if (email.find()) {
            return email.group();
        }
        Matcher handy = HANDY.matcher(text);
        if (handy.find()) {
            return handy.group();
        }
        return null;
    }
}
