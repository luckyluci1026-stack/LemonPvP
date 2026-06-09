package com.lemonpvp.lemoncore.config;

import com.lemonpvp.lemoncore.LemonCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FilterManager {

    private final LemonCore plugin;
    private FileConfiguration filterConfig;
    private final List<Pattern> nwordPatterns = new ArrayList<>();
    private final List<Pattern> slurPatterns = new ArrayList<>();
    private final List<String> whitelist = new ArrayList<>();

    public FilterManager(LemonCore plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "filter.yml");
        if (!file.exists()) {
            plugin.saveResource("filter.yml", false);
        }
        filterConfig = YamlConfiguration.loadConfiguration(file);
        buildPatterns();
    }

    private void buildPatterns() {
        nwordPatterns.clear();
        slurPatterns.clear();
        whitelist.clear();

        whitelist.addAll(filterConfig.getStringList("whitelist"));

        for (String word : filterConfig.getStringList("nword-patterns")) {
            try { nwordPatterns.add(buildPattern(word)); }
            catch (Exception e) { plugin.getLogger().warning("[Filter] Failed to compile nword pattern '" + word + "': " + e.getMessage()); }
        }
        for (String word : filterConfig.getStringList("slurs")) {
            try { slurPatterns.add(buildPattern(word)); }
            catch (Exception e) { plugin.getLogger().warning("[Filter] Failed to compile slur pattern '" + word + "': " + e.getMessage()); }
        }
    }

    /**
     * Builds a regex Pattern that catches the given word even when:
     * - Letters are separated by spaces, dots, underscores, dashes, asterisks, etc.
     * - Common leetspeak substitutions are used (a→4/@, e→3, i→1/!, o→0, s→$, ...)
     * - Characters are repeated (fuuuuck, niiiig...)
     */
    private Pattern buildPattern(String word) {
        // Between consecutive letters we allow any number of non-alphanumeric separators
        // (catches "f.u.c.k", "f_u_c_k", "f u c k", "f*ck", ...)
        String sep = "[^a-zA-Z0-9]*";

        StringBuilder sb = new StringBuilder("(?i)");
        boolean first = true;

        for (char c : word.toCharArray()) {
            char lc = Character.toLowerCase(c);

            if (lc == ' ') {
                // Space within a multi-word phrase — require at least one separator
                sb.append("[\\s._\\-!@#$%^&*]+");
                first = true; // next char is "first" after the word-gap
                continue;
            }

            if (!first) sb.append(sep);
            first = false;

            // Each character is matched as one-or-more (+) to handle repetition (fuuuck)
            switch (lc) {
                case 'a' -> sb.append("[a@4áä]+");
                case 'e' -> sb.append("[e3é]+");
                case 'i' -> sb.append("[i1!|íï]+");
                case 'o' -> sb.append("[o0óö]+");
                case 'u' -> sb.append("[uüú]+");
                case 's' -> sb.append("[s$5ß]+");
                case 'g' -> sb.append("[g9]+");
                case 'b' -> sb.append("[b8]+");
                case 't' -> sb.append("[t7+]+");
                case 'c' -> sb.append("[cç]+");
                case 'n' -> sb.append("[nñ]+");
                default  -> sb.append(Pattern.quote(String.valueOf(lc)) + "+");
            }
        }
        return Pattern.compile(sb.toString());
    }

    public boolean containsNword(String message) {
        String clean = clean(message);
        if (isWhitelisted(clean)) return false;
        for (Pattern p : nwordPatterns) {
            if (p.matcher(clean).find()) return true;
        }
        return false;
    }

    public boolean containsSlur(String message) {
        String clean = clean(message);
        if (isWhitelisted(clean)) return false;
        for (Pattern p : slurPatterns) {
            if (p.matcher(clean).find()) return true;
        }
        return false;
    }

    private boolean isWhitelisted(String message) {
        String lower = message.toLowerCase();
        for (String w : whitelist) {
            if (lower.contains(w.toLowerCase())) return true;
        }
        return false;
    }

    private String clean(String message) {
        // Remove zero-width, invisible, and directional Unicode characters
        return message
                .replaceAll("[\\u200B-\\u200D\\uFEFF\\u00AD\\u200E\\u200F\\u202A-\\u202E\\u2060-\\u2069\\u206A-\\u206F]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
