package com.lemonpvp.lemoncosmetics.model;

import org.bukkit.Material;

/**
 * A selectable chat/tablist tag shown as a suffix after the player's name.
 *
 * <p>Tags with {@code price > 0} are purchasable with coins. Tags with
 * {@code price == 0} are permission-only and can only be equipped by players
 * who hold {@code lemoncosmetics.tag.<id>}. Every tag may also be granted for
 * free via that same permission node.</p>
 *
 * <p>{@code render} is the MiniMessage representation of the tag (no brackets,
 * no italics) and is what gets pushed to LemonCore for chat/tablist display.</p>
 */
public enum TagType {

    // --- Purchasable -------------------------------------------------------
    MONEY     ("money",     "Money",     "<gradient:#fff176:#f9a825>ᴍᴏɴᴇʏ",   Material.GOLD_INGOT,         500),
    WINNER    ("winner",    "Winner",    "<gradient:#ffd700:#ff8f00>ᴡɪɴɴᴇʀ",  Material.GOLDEN_APPLE,     1000),
    VETERAN   ("veteran",   "Veteran",   "<gradient:#b0bec5:#546e7a>ᴠᴇᴛᴇʀᴀɴ", Material.IRON_INGOT,        750),
    LEMON     ("lemon",     "Lemon",     "<gradient:#fff176:#aeea00>ʟᴇᴍᴏɴ",   Material.LIME_DYE,          300),
    ELITE     ("elite",     "Elite",     "<gradient:#40c4ff:#2962ff>ᴇʟɪᴛᴇ",   Material.DIAMOND,          2000),
    GHOST     ("ghost",     "Ghost",     "<gradient:#eceff1:#90a4ae>ɢʜᴏsᴛ",   Material.GHAST_TEAR,        400),
    TOXIC     ("toxic",     "Toxic",     "<gradient:#76ff03:#33691e>ᴛᴏxɪᴄ",   Material.SLIME_BALL,        400),
    NOVA      ("nova",      "Nova",      "<gradient:#ff9100:#d500f9>ɴᴏᴠᴀ",    Material.FIRE_CHARGE,       800),
    FROST     ("frost",     "Frost",     "<gradient:#e1f5fe:#00b0ff>ꜰʀᴏsᴛ",   Material.PRISMARINE_CRYSTALS, 600),
    INFERNO   ("inferno",   "Inferno",   "<gradient:#ff6d00:#dd2c00>ɪɴꜰᴇʀɴᴏ", Material.BLAZE_POWDER,      600),
    COSMIC    ("cosmic",    "Cosmic",    "<gradient:#e040fb:#7c4dff>ᴄᴏsᴍɪᴄ",  Material.AMETHYST_SHARD,    900),

    // --- Permission-only (price 0) ----------------------------------------
    STAFF     ("staff",     "Staff",     "<gradient:#ff5252:#b71c1c>sᴛᴀꜰꜰ",   Material.NETHER_STAR,         0),
    DEVELOPING("developing","Developing","<gradient:#40c4ff:#0091ea>ᴅᴇᴠ",     Material.COMMAND_BLOCK,       0),
    HOLZI     ("holzi",     "Holzi",     "<gradient:#8d6e63:#4e342e>ʜᴏʟᴢɪ",   Material.OAK_LOG,            0),
    WICHTIG   ("wichtig",   "Wichtig",   "<gradient:#ffd740:#ff6f00>ᴡɪᴄʜᴛɪɢ", Material.PAPER,             0),
    HUGO      ("hugo",      "Hugo",      "<gradient:#80d8ff:#0288d1>ʜᴜɢᴏ",    Material.HEART_OF_THE_SEA,   0);

    public final String id;
    public final String displayName;
    public final String render;
    public final Material icon;
    public final int price;

    TagType(String id, String displayName, String render, Material icon, int price) {
        this.id = id;
        this.displayName = displayName;
        this.render = render;
        this.icon = icon;
        this.price = price;
    }

    /** {@code true} if this tag can be bought with coins. */
    public boolean isBuyable() {
        return price > 0;
    }

    /** {@code true} if this tag can only be unlocked through a permission. */
    public boolean isPermissionOnly() {
        return price <= 0;
    }

    /** The permission node that grants this tag for free. */
    public String permission() {
        return "lemoncosmetics.tag." + id;
    }

    public static java.util.Optional<TagType> fromId(String id) {
        for (TagType t : values()) if (t.id.equals(id)) return java.util.Optional.of(t);
        return java.util.Optional.empty();
    }
}
