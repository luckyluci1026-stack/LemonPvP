package com.lemonpvp.lemonevents.model;

public enum EventType {
    LemonRoyale,
    HungerGames,
    Cinema,
    PvP,
    Horror;

    public static EventType fromString(String s) {
        for (EventType t : values()) {
            if (t.name().equalsIgnoreCase(s)) return t;
        }
        return null;
    }
}
