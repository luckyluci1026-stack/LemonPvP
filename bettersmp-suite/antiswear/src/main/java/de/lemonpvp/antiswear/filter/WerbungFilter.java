package de.lemonpvp.antiswear.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WerbungFilter {

    private static final String PUNKT = "(?:\\.|\\s+\\.\\s+|\\s*[\\(\\[\\{]\\s*(?:\\.|dot|punkt)\\s*[\\)\\]\\}]\\s*|\\s+(?:dot|punkt)\\s+)";
    private static final String TLDS = "com|net|org|de|eu|gg|io|me|xyz|tk|ml|ga|cf|gq|info|biz|co|us|uk|at|ch|fr|pl|ru|tv"
            + "|cc|ws|pw|top|club|online|site|fun|games|host|live|pro|app|dev|nl|es|it|to|ly|link|shop|store|world"
            + "|network|mc|one|space|lol|gl|ovh|su|be|dk|se|no|fi|cz|sk|hu|ro|tr|br|ca|au|nz|in|jp|cn|kr|vip|icu"
            + "|win|bet|cloud|digital|email|social|team|zone";
    private static final Pattern IP = Pattern.compile("(?<![\\d.])(\\d{1,3})" + PUNKT + "(\\d{1,3})" + PUNKT
            + "(\\d{1,3})" + PUNKT + "(\\d{1,3})(?!\\d)");
    private static final Pattern DOMAIN = Pattern.compile("(?<![a-z0-9-])((?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"
            + PUNKT + ")+)(" + TLDS + ")(?![a-z0-9-])");
    private static final Pattern DISCORD = Pattern.compile("discord(?:app)?" + PUNKT + "(?:gg|com\\s*/\\s*invite|io|me)"
            + "|dsc" + PUNKT + "gg|discord\\s*/\\s*[a-z0-9]{4,}");
    private static final Pattern NUR_PUNKTE = Pattern.compile("[\\s\\(\\)\\[\\]\\{\\}]*(?:dot|punkt|\\.)[\\s\\(\\)\\[\\]\\{\\}]*");

    private final List<String> erlaubt = new ArrayList<>();

    public void setErlaubt(Collection<String> domains) {
        erlaubt.clear();
        for (String domain : domains) {
            if (domain != null && !domain.isBlank()) {
                erlaubt.add(domain.trim().toLowerCase(Locale.ROOT));
            }
        }
    }

    public String finden(String nachricht) {
        if (nachricht == null || nachricht.isBlank()) {
            return null;
        }
        String text = Normalisierer.bereinigen(nachricht);
        Matcher discord = DISCORD.matcher(text);
        if (discord.find()) {
            return discord.group();
        }
        Matcher ip = IP.matcher(text);
        while (ip.find()) {
            if (gueltigeIp(ip)) {
                return ip.group(1) + "." + ip.group(2) + "." + ip.group(3) + "." + ip.group(4);
            }
        }
        Matcher domain = DOMAIN.matcher(text);
        while (domain.find()) {
            String name = NUR_PUNKTE.matcher(domain.group(1)).replaceAll(".") + domain.group(2);
            if (!istErlaubt(name)) {
                return name;
            }
        }
        return null;
    }

    private static boolean gueltigeIp(Matcher ip) {
        for (int gruppe = 1; gruppe <= 4; gruppe++) {
            if (Integer.parseInt(ip.group(gruppe)) > 255) {
                return false;
            }
        }
        return true;
    }

    private boolean istErlaubt(String domain) {
        for (String erlaubteDomain : erlaubt) {
            if (domain.equals(erlaubteDomain) || domain.endsWith("." + erlaubteDomain)) {
                return true;
            }
        }
        return false;
    }
}
