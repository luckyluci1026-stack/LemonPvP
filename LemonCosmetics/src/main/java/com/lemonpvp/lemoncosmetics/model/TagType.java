package com.lemonpvp.lemoncosmetics.model;

import java.util.Optional;

public enum TagType {

    DEVELOPING("developing", "Developing", -1, '', 0x00FFFF, 0x0044FF),
    MONEY      ("money",     "Money",       500, '', 0x00FF88, 0x006600),
    WINNER     ("winner",    "Winner",     1000, '', 0xFFEE00, 0xFF8800),
    VETERAN    ("veteran",   "Veteran",     750, '', 0xAAAAAA, 0xFFFFFF),
    LEMON      ("lemon",     "Lemon",       300, '', 0xFFFB00, 0x00FF00),
    ELITE      ("elite",     "Elite",      2000, '', 0xFFD700, 0xAA6600),
    GHOST      ("ghost",     "Ghost",       400, '', 0xFFFFFF, 0xAADDFF),
    TOXIC      ("toxic",     "Toxic",       400, '', 0x44FF00, 0x003300),
    NOVA       ("nova",      "Nova",        800, '', 0xCC00FF, 0xFF00AA),
    FROST      ("frost",     "Frost",       600, '', 0xAAEEFF, 0x003366),
    INFERNO    ("inferno",   "Inferno",     600, '', 0xFF4400, 0x660000),
    COSMIC     ("cosmic",    "Cosmic",      900, '', 0x000066, 0x6600CC),
    STAFF      ("staff",     "Staff",        -1, '', 0xFF0000, 0x880000),
    HOLZI      ("holzi",    "Holzi",        -1, '', 0xC8A060, 0x5C3010),
    WICHTIG    ("wichtig",  "Wichtig",      -1, '', 0x00CFFF, 0xFF00CC),
    HUGO       ("hugo",     "Hugo",         -1, '', 0x00CCFF, 0x9900FF);

    public final String id;
    public final String displayName;
    /** Coin price; -1 = permission-only, no purchase. */
    public final int price;
    public final char emoji;
    public final int colorStart; // ARGB hex
    public final int colorEnd;

    TagType(String id, String displayName, int price, char emoji, int colorStart, int colorEnd) {
        this.id = id;
        this.displayName = displayName;
        this.price = price;
        this.emoji = emoji;
        this.colorStart = colorStart;
        this.colorEnd = colorEnd;
    }

    public boolean isPermissionOnly() {
        return price < 0;
    }

    public String permission() {
        return "lemoncosmetics.tag." + id;
    }

    public static Optional<TagType> fromId(String id) {
        if (id == null) return Optional.empty();
        for (TagType t : values()) {
            if (t.id.equalsIgnoreCase(id)) return Optional.of(t);
        }
        return Optional.empty();
    }
}
