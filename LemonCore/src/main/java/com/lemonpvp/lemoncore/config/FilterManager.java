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
     * - It is misspelled: one letter omitted or two adjacent letters swapped
     *   (config {@code fuzzy-misspellings}, words of 4+ letters; prefix a word
     *   with {@code =} in filter.yml to force exact matching for that word)
     */
    private Pattern buildPattern(String word) {
        boolean fuzzy = filterConfig.getBoolean("fuzzy-misspellings", true);
        if (word.startsWith("=")) {
            word = word.substring(1);
            fuzzy = false;
        }

        // Multi-word phrases keep the simple linear build (no fuzzing across gaps).
        if (word.indexOf(' ') >= 0) {
            StringBuilder sb = new StringBuilder("(?i)");
            boolean first = true;
            for (char c : word.toCharArray()) {
                char lc = Character.toLowerCase(c);
                if (lc == ' ') { sb.append("[\\s._\\-!@#$%^&*]+"); first = true; continue; }
                if (!first) sb.append(SEP);
                first = false;
                sb.append(charClass(lc));
            }
            return Pattern.compile(sb.toString());
        }

        List<String> classes = new ArrayList<>();
        for (char c : word.toCharArray()) classes.add(charClass(Character.toLowerCase(c)));

        List<String> variants = new ArrayList<>();
        variants.add(join(classes, -1, -1));
        if (fuzzy && classes.size() >= 4) {
            // One INTERIOR letter omitted (catches "niga"-style typos) — first and
            // last letter always stay required, otherwise "uck"-style stubs would
            // match harmless words like "luck".
            for (int skip = 1; skip < classes.size() - 1; skip++) variants.add(join(classes, skip, -1));
            // Or two adjacent letters swapped (catches "ngiga"-style typos).
            for (int swap = 0; swap < classes.size() - 1; swap++) variants.add(join(classes, -1, swap));
        }
        return Pattern.compile("(?i)(?:" + String.join("|", variants) + ")");
    }

    /** Between consecutive letters any number of non-alphanumeric separators is allowed. */
    private static final String SEP = "[^a-zA-Z0-9]*";

    /** Joins the char classes with separators, optionally skipping index or swapping index/index+1. */
    private String join(List<String> classes, int skipIdx, int swapIdx) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < classes.size(); i++) {
            int idx = i;
            if (swapIdx >= 0) {
                if (i == swapIdx) idx = swapIdx + 1;
                else if (i == swapIdx + 1) idx = swapIdx;
            }
            if (idx == skipIdx) continue;
            if (!first) sb.append(SEP);
            first = false;
            sb.append(classes.get(idx));
        }
        return sb.toString();
    }

    /** Leet/diacritic-tolerant class for one letter, one-or-more for repetition. */
    private String charClass(char lc) {
        return switch (lc) {
            case 'a' -> "[a@4áä]+";
            case 'e' -> "[e3é]+";
            case 'i' -> "[i1!|íï]+";
            case 'o' -> "[o0óö]+";
            case 'u' -> "[uüú]+";
            case 's' -> "[s$5ß]+";
            case 'g' -> "[g9]+";
            case 'b' -> "[b8]+";
            case 't' -> "[t7+]+";
            case 'c' -> "[cç]+";
            case 'n' -> "[nñ]+";
            default  -> Pattern.quote(String.valueOf(lc)) + "+";
        };
    }

    public boolean containsNword(String message) {
        String clean = stripWhitelisted(clean(message));
        for (Pattern p : nwordPatterns) {
            if (p.matcher(clean).find()) return true;
        }
        return false;
    }

    public boolean containsSlur(String message) {
        String clean = stripWhitelisted(clean(message));
        for (Pattern p : slurPatterns) {
            if (p.matcher(clean).find()) return true;
        }
        return false;
    }

    /**
     * Neutralizes only the benign whitelisted words (e.g. "classic", "assassin")
     * before pattern matching. The old behaviour returned "whitelisted" — and thus
     * skipped the WHOLE filter — if the message merely contained a whitelisted
     * substring, giving a trivial one-word bypass ("classic <slur>"). Stripping the
     * benign token instead exempts the false positive without disarming the line.
     */
    private String stripWhitelisted(String message) {
        String result = message;
        for (String w : whitelist) {
            if (w == null || w.isEmpty()) continue;
            result = result.replaceAll("(?i)" + Pattern.quote(w), " ");
        }
        return result;
    }

    private String clean(String message) {
        // Remove zero-width, invisible, and directional Unicode characters
        return message
                .replaceAll("[\\u200B-\\u200D\\uFEFF\\u00AD\\u200E\\u200F\\u202A-\\u202E\\u2060-\\u2069\\u206A-\\u206F]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
