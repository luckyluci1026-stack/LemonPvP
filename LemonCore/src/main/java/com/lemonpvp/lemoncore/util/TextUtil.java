package com.lemonpvp.lemoncore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TextUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static Component parse(String text) {
        if (text == null) return Component.empty();
        return MM.deserialize(text);
    }

    public static Component parse(String text, Map<String, String> placeholders) {
        if (text == null) return Component.empty();
        List<TagResolver> resolvers = new ArrayList<>();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolvers.add(Placeholder.parsed(entry.getKey(), entry.getValue()));
        }
        return MM.deserialize(text, TagResolver.resolver(resolvers));
    }

    public static String stripTags(String text) {
        if (text == null) return "";
        Component parsed = MM.deserialize(text);
        return PlainTextComponentSerializer.plainText().serialize(parsed);
    }

    public static String escapeTags(String text) {
        if (text == null) return "";
        return MM.escapeTags(text);
    }

    public static String formatDuration(long seconds) {
        if (seconds <= 0) return "Permanent";
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (mins > 0) sb.append(mins).append("m ");
        if (secs > 0) sb.append(secs).append("s");
        return sb.toString().trim();
    }

    public static long parseDuration(String input) {
        if (input == null || input.equals("0") || input.equalsIgnoreCase("permanent")) return 0;
        long total = 0;
        StringBuilder num = new StringBuilder();
        for (char c : input.toLowerCase().toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                if (num.length() == 0) continue;
                long val = Long.parseLong(num.toString());
                num = new StringBuilder();
                switch (c) {
                    case 's' -> total += val;
                    case 'm' -> total += val * 60;
                    case 'h' -> total += val * 3600;
                    case 'd' -> total += val * 86400;
                    case 'w' -> total += val * 604800;
                    case 'y' -> total += val * 31536000L;
                }
            }
        }
        return total;
    }

    public static String generateId(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static String generateAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
