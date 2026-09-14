/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.config;

import ac.grim.grimac.manager.AcBanDuration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Build-time verification of every yml BuckSMPAC ships.
 *
 * <p>These files are only read on a running server, and most of what can go
 * wrong in them fails late and quietly: an unclosed MiniMessage tag throws
 * while rendering a ban screen, a duplicated key silently drops whichever copy
 * loses, and a ladder handing out a permanent ban is only noticed by the
 * player it hit. All three are decidable here.</p>
 */
class BundledResourceAuditTest {

    private static Path resources;

    @BeforeAll
    static void setUp() {
        resources = Path.of("src/main/resources").toAbsolutePath();
        assertTrue(Files.isDirectory(resources), "expected to run from the common module dir");
    }

    // ------------------------------------------------------------ structure

    @Test
    void everyBundledYamlParses() throws IOException {
        List<String> failures = new ArrayList<>();
        int files = 0;

        for (Path file : ymlFiles()) {
            files++;
            try {
                load(file);
            } catch (Exception e) {
                failures.add(rel(file) + ": " + e.getMessage());
            }
        }

        assertTrue(files > 50, "resource scan looks broken; found only " + files + " yml files");
        if (!failures.isEmpty()) fail(String.join("\n", failures));
    }

    // -------------------------------------------------------- MiniMessage

    /**
     * Tag names MiniMessage resolves. An opening tag with one of these names
     * that is never closed with {@code >} is a typo, not literal text.
     *
     * <p>Names outside this list are left alone on purpose: a help line
     * reading {@code /acban <player> [reason]} is not a broken tag, and
     * MiniMessage renders it as the literal text it is meant to be.</p>
     */
    private static final Set<String> RESOLVED_TAGS = Set.of(
            "gradient", "rainbow", "hover", "click", "key", "lang", "insert", "transition",
            "newline", "br", "reset", "font", "color", "colour", "shadow_color",
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white",
            "bold", "b", "italic", "i", "underlined", "u", "strikethrough", "st",
            "obfuscated", "obf");

    @Test
    void everyMiniMessageStringDeserializes() throws IOException {
        // Catches what MiniMessage itself rejects: a quote left open inside a
        // hover argument, a malformed click action. Unknown tags are NOT an
        // error here - MiniMessage renders those as text, deliberately.
        List<String> failures = new ArrayList<>();
        int checked = 0;

        for (Path file : ymlFiles()) {
            for (Map.Entry<String, String> entry : stringsIn(load(file)).entrySet()) {
                if (entry.getValue().indexOf('<') < 0) continue;
                checked++;
                try {
                    MiniMessage.miniMessage().deserialize(fillPlaceholders(entry.getValue()));
                } catch (RuntimeException e) {
                    failures.add(rel(file) + " -> " + entry.getKey() + ": " + e.getMessage());
                }
            }
        }

        assertTrue(checked > 20, "MiniMessage extraction looks broken; checked only " + checked + " strings");
        if (!failures.isEmpty()) fail(String.join("\n", failures));
    }

    @Test
    void noKnownTagIsLeftUnclosed() throws IOException {
        // The failure this exists for: "<gradient:#6C5CE7:#00D4FF" with the
        // closing bracket missing. MiniMessage does not complain - it renders
        // the raw text to the player, so a ban screen shows its own markup.
        List<String> failures = new ArrayList<>();
        int tags = 0;

        for (Path file : ymlFiles()) {
            for (Map.Entry<String, String> entry : stringsIn(load(file)).entrySet()) {
                String value = entry.getValue();
                for (String unclosed : unclosedTags(value)) {
                    failures.add(rel(file) + " -> " + entry.getKey() + ": <" + unclosed + " is never closed");
                }
                tags += countResolvedTags(value);
            }
        }

        assertTrue(tags > 50, "tag scan looks broken; found only " + tags + " known tags");
        if (!failures.isEmpty()) fail(String.join("\n", failures));
    }

    /** Names of known tags in {@code value} whose {@code >} is missing. */
    private static List<String> unclosedTags(String value) {
        List<String> unclosed = new ArrayList<>();
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != '<') continue;

            int nameEnd = i + 1;
            if (nameEnd < value.length() && value.charAt(nameEnd) == '/') nameEnd++; // closing tag
            int nameStart = nameEnd;
            while (nameEnd < value.length() && isTagNameChar(value.charAt(nameEnd))) nameEnd++;

            String name = value.substring(nameStart, nameEnd).toLowerCase(Locale.ROOT);
            if (!RESOLVED_TAGS.contains(name)) continue;
            if (closingBracket(value, nameEnd) < 0) unclosed.add(name);
        }
        return unclosed;
    }

    private static int countResolvedTags(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != '<') continue;
            int nameEnd = i + 1;
            if (nameEnd < value.length() && value.charAt(nameEnd) == '/') nameEnd++;
            int nameStart = nameEnd;
            while (nameEnd < value.length() && isTagNameChar(value.charAt(nameEnd))) nameEnd++;
            if (RESOLVED_TAGS.contains(value.substring(nameStart, nameEnd).toLowerCase(Locale.ROOT))) count++;
        }
        return count;
    }

    /**
     * Index of the {@code >} closing a tag that starts at {@code from}, or -1.
     *
     * <p>Quoted arguments are skipped over, because a hover's text may contain
     * brackets of its own, and another {@code <} ends the search: a tag cannot
     * span the start of the next one.</p>
     */
    private static int closingBracket(String value, int from) {
        boolean inQuotes = false;
        for (int i = from; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"' || c == '\'') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && c == '>') {
                return i;
            } else if (!inQuotes && c == '<') {
                return -1;
            }
        }
        return -1;
    }

    private static boolean isTagNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }

    /** Placeholders are substituted before rendering, so render them filled. */
    private static String fillPlaceholders(String value) {
        return value.replaceAll("%[a-z_0-9]+%", "x");
    }

    // -------------------------------------------------------------- policy

    @Test
    void noBundledLadderHandsOutAPermanentBan() throws IOException {
        List<String> failures = new ArrayList<>();
        int commands = 0;

        for (Path file : ymlFiles()) {
            for (Map.Entry<String, String> entry : stringsIn(load(file)).entrySet()) {
                String duration = acbanDurationIn(entry.getValue());
                if (duration == null) continue;
                commands++;
                Long parsed = AcBanDuration.parse(duration);
                if (parsed != null && parsed == AcBanDuration.PERMANENT) {
                    failures.add(rel(file) + " -> " + entry.getKey() + ": " + entry.getValue());
                }
            }
        }

        assertTrue(commands > 5, "acban command extraction looks broken; found only " + commands);
        if (!failures.isEmpty()) {
            fail("BuckSMP hands out 7-14 day bans, never permanent ones:\n" + String.join("\n", failures));
        }
    }

    @Test
    void theConfiguredBanLengthsAreUnderstoodAndTemporary() throws IOException {
        List<String> failures = new ArrayList<>();

        for (Path file : ymlFiles()) {
            for (Map.Entry<String, String> entry : stringsIn(load(file)).entrySet()) {
                String key = entry.getKey();
                if (!key.endsWith("acban-default-duration") && !key.endsWith("acban-max-duration")) continue;

                Long parsed = AcBanDuration.parse(entry.getValue());
                if (parsed == null) {
                    failures.add(rel(file) + " -> " + key + ": \"" + entry.getValue() + "\" is not a duration");
                } else if (parsed == AcBanDuration.PERMANENT) {
                    failures.add(rel(file) + " -> " + key + ": permanent, but bans are meant to lift themselves");
                }
            }
        }

        if (!failures.isEmpty()) fail(String.join("\n", failures));
    }

    /**
     * The duration out of {@code "14:0 acban %player% 14d reason"}, or null
     * when the line is not an acban command or names no duration (in which
     * case the configured default applies and is checked separately).
     */
    private static String acbanDurationIn(String value) {
        int acban = value.indexOf("acban ");
        if (acban < 0) return null;
        // Only when acban is the command itself, not a word inside a reason.
        if (acban > 0 && !Character.isWhitespace(value.charAt(acban - 1))) return null;

        String[] tokens = value.substring(acban).split("\\s+");
        // acban <player> <maybe duration>
        return tokens.length >= 3 ? tokens[2] : null;
    }

    // ------------------------------------------------------------ versions

    @Test
    void theEnglishAndGermanVersionMarkersAgree() throws IOException {
        // The operator files are never migrated automatically; the marker is
        // what tells somebody their copy is behind. en and de are the two this
        // fork edits together, so they are the pair that can drift by mistake.
        //
        // The other translations are deliberately not compared: several still
        // carry upstream's older config-version, which is a translation
        // backlog rather than something a build should block on.
        List<String> failures = new ArrayList<>();

        for (String group : new String[]{"punishments", "bans", "config", "messages"}) {
            Path dir = resources.resolve(group);
            if (!Files.isDirectory(dir)) continue;

            Map<String, Object> english = versionMarkers(dir.resolve("en.yml"));
            Map<String, Object> german = versionMarkers(dir.resolve("de.yml"));
            if (english.isEmpty() || german.isEmpty()) continue;

            for (Map.Entry<String, Object> entry : english.entrySet()) {
                Object other = german.get(entry.getKey());
                if (!entry.getValue().equals(other)) {
                    failures.add(group + "/: en.yml has " + entry.getKey() + " " + entry.getValue()
                            + " but de.yml has " + other);
                }
            }
        }

        if (!failures.isEmpty()) fail(String.join("\n", failures));
    }

    /** Top-level {@code *-version} keys of one file, empty when it has none. */
    private static Map<String, Object> versionMarkers(Path file) throws IOException {
        if (!Files.isRegularFile(file)) return Map.of();

        Map<String, Object> out = new TreeMap<>();
        load(file).forEach((key, value) -> {
            if (key.endsWith("-version") && value != null) out.put(key, value);
        });
        return out;
    }

    // ------------------------------------------------------------- helpers

    private static List<Path> ymlFiles() throws IOException {
        try (Stream<Path> files = Files.walk(resources)) {
            return files.filter(p -> p.toString().endsWith(".yml")).sorted().toList();
        }
    }

    private static String rel(Path file) {
        return resources.relativize(file).toString().replace('\\', '/');
    }

    /**
     * Loads with duplicate keys rejected: a key written twice silently loses
     * one of the two settings, which is the kind of thing that is only ever
     * noticed as "the config does nothing".
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> load(Path file) throws IOException {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);

        Yaml yaml = new Yaml(new SafeConstructor(options));
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object root = yaml.load(reader);
            return root instanceof Map ? (Map<String, Object>) root : Map.of();
        }
    }

    /** Every string in the tree, keyed by its dotted path. */
    private static Map<String, String> stringsIn(Map<String, Object> root) {
        Map<String, String> out = new LinkedHashMap<>();
        flatten(root).forEach((key, value) -> {
            if (value instanceof String s) out.put(key, s);
        });
        return out;
    }

    private static Map<String, Object> flatten(Map<String, Object> root) {
        Map<String, Object> out = new LinkedHashMap<>();
        walk("", root, out);
        return out;
    }

    private static void walk(String prefix, Object node, Map<String, Object> out) {
        if (node instanceof Map<?, ?> map) {
            map.forEach((key, value) -> walk(join(prefix, String.valueOf(key)), value, out));
        } else if (node instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) walk(prefix + "[" + i + "]", list.get(i), out);
        } else {
            out.put(prefix, node);
        }
    }

    private static String join(String prefix, String key) {
        return prefix.isEmpty() ? key : prefix + "." + key;
    }
}
