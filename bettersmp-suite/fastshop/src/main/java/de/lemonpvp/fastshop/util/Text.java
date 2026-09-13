package de.lemonpvp.fastshop.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** MiniMessage-Helfer mit zusätzlicher Legacy-Farbcode-Unterstützung. */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern HEX = Pattern.compile("[&§]#([0-9a-fA-F]{6})");

    private static final Map<Character, String> CODES = Map.ofEntries(
            Map.entry('0', "<black>"), Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"), Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"), Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"), Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"), Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"), Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"), Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"), Map.entry('f', "<white>"),
            Map.entry('k', "<obfuscated>"), Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"), Map.entry('n', "<underlined>"),
            Map.entry('o', "<italic>"), Map.entry('r', "<reset>"));

    private Text() {
    }

    public static Component mm(String input) {
        return MM.deserialize(legacyToMini(input));
    }

    public static Component mm(String input, TagResolver... resolvers) {
        return MM.deserialize(legacyToMini(input), resolvers);
    }

    public static String legacyToMini(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        Matcher hex = HEX.matcher(s);
        s = hex.replaceAll(mr -> "<#" + mr.group(1) + ">");
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < s.length()) {
                String tag = CODES.get(Character.toLowerCase(s.charAt(i + 1)));
                if (tag != null) {
                    out.append(tag);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }
}
