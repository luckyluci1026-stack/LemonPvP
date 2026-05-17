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
            try {
                nwordPatterns.add(buildPattern(word));
            } catch (Exception ignored) {}
        }

        for (String word : filterConfig.getStringList("slurs")) {
            try {
                slurPatterns.add(buildPattern(word));
            } catch (Exception ignored) {}
        }
    }

    private Pattern buildPattern(String word) {
        // Build a flexible pattern that handles leetspeak and common substitutions
        StringBuilder sb = new StringBuilder("(?i)");
        for (char c : word.toCharArray()) {
            switch (Character.toLowerCase(c)) {
                case 'a' -> sb.append("[a@4]");
                case 'e' -> sb.append("[e3]");
                case 'i' -> sb.append("[i1!|]");
                case 'o' -> sb.append("[o0]");
                case 's' -> sb.append("[s$5]");
                case 'g' -> sb.append("[g9]");
                case 'b' -> sb.append("[b8]");
                case 't' -> sb.append("[t7+]");
                case ' ' -> sb.append("[\\s._-]*");
                default -> sb.append(Pattern.quote(String.valueOf(c)));
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
        // Remove zero-width and special chars, normalize spaces
        return message.replaceAll("[\\u200B-\\u200D\\uFEFF]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
