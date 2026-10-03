package de.lemonpvp.bettersmp.report;

import java.util.UUID;

public record Ziel(UUID id, String name) {

    public boolean heisst(String eingabe) {
        return ohnePunkt(name).equalsIgnoreCase(ohnePunkt(eingabe.trim()));
    }

    private static String ohnePunkt(String text) {
        return text.startsWith(".") ? text.substring(1) : text;
    }
}
