package de.lemonpvp.fastshop.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Icons aus dem SMP-Texturepack.
 *
 * Im Text werden sie als <code>%g:coin%</code> geschrieben und hier durch das
 * jeweilige Zeichen ersetzt. Ist das Texturepack nicht installiert, kann man
 * <code>glyphs.enabled: false</code> setzen - dann verschwinden die Platzhalter
 * rueckstandslos und alles bleibt lesbar.
 */
public final class Glyphs {

    private static final Pattern TOKEN = Pattern.compile("%g:([a-z_]+)%");

    private final JavaPlugin plugin;
    private final Map<String, String> glyphs = new HashMap<>();
    private boolean enabled;

    public Glyphs(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        glyphs.clear();
        this.enabled = plugin.getConfig().getBoolean("glyphs.enabled", true);
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("glyphs");
        if (sec == null) {
            return;
        }
        for (String key : sec.getKeys(false)) {
            if (key.equals("enabled")) {
                continue;
            }
            String value = sec.getString(key);
            if (value != null) {
                glyphs.put(key, value);
            }
        }
    }

    /** Einzelnes Icon (leer, wenn deaktiviert oder unbekannt). */
    public String get(String name) {
        return enabled ? glyphs.getOrDefault(name, "") : "";
    }

    /** Ersetzt alle %g:name%-Platzhalter in einem Text. */
    public String apply(String text) {
        if (text == null || text.indexOf("%g:") < 0) {
            return text;
        }
        Matcher m = TOKEN.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(get(m.group(1))));
        }
        m.appendTail(out);
        return out.toString();
    }
}
