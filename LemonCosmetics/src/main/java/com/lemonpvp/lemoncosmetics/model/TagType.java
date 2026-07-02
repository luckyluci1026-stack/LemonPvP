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
    KING      ("king",      "King",      "<gradient:#ffd700:#ff8f00>ᴋɪɴɢ",    Material.GOLD_BLOCK,       1500),
    LEGEND    ("legend",    "Legend",    "<gradient:#ff6f00:#ffca28>ʟᴇɢᴇɴᴅ",  Material.NETHERITE_INGOT,  2500),
    MYTHIC    ("mythic",    "Mythic",    "<gradient:#e040fb:#311b92>ᴍʏᴛʜɪᴄ",  Material.ECHO_SHARD,       3000),
    SAVAGE    ("savage",    "Savage",    "<gradient:#ff1744:#7f0000>sᴀᴠᴀɢᴇ",  Material.IRON_AXE,          800),
    PRO       ("pro",       "Pro",       "<gradient:#00e676:#1b5e20>ᴘʀᴏ",     Material.DIAMOND_SWORD,     700),
    NINJA     ("ninja",     "Ninja",     "<gradient:#616161:#212121>ɴɪɴᴊᴀ",   Material.LEATHER_BOOTS,     900),
    WIZARD    ("wizard",    "Wizard",    "<gradient:#7c4dff:#311b92>ᴡɪᴢᴀʀᴅ",  Material.ENCHANTING_TABLE,  900),
    PHOENIX   ("phoenix",   "Phoenix",   "<gradient:#ff6d00:#ffd54f>ᴘʜᴏᴇɴɪx", Material.BLAZE_POWDER,     1200),
    SHADOW    ("shadow",    "Shadow",    "<gradient:#424242:#000000>sʜᴀᴅᴏᴡ",  Material.COAL,              800),
    GALAXY    ("galaxy",    "Galaxy",    "<gradient:#311b92:#d500f9>ɢᴀʟᴀxʏ",  Material.NETHER_STAR,      2000),
    THUNDER   ("thunder",   "Thunder",   "<gradient:#fff176:#01579b>ᴛʜᴜɴᴅᴇʀ", Material.LIGHTNING_ROD,    1000),
    VENOM     ("venom",     "Venom",     "<gradient:#76ff03:#1b5e20>ᴠᴇɴᴏᴍ",   Material.SPIDER_EYE,        700),
    ROYAL     ("royal",     "Royal",     "<gradient:#ffd740:#6a1b9a>ʀᴏʏᴀʟ",   Material.GOLDEN_HELMET,    1800),
    ANGEL     ("angel",     "Angel",     "<gradient:#ffffff:#fff59d>ᴀɴɢᴇʟ",   Material.FEATHER,          1000),
    DEMON     ("demon",     "Demon",     "<gradient:#d50000:#311b92>ᴅᴇᴍᴏɴ",   Material.WITHER_SKELETON_SKULL, 1200),
    CHAMPION  ("champion",  "Champion",  "<gradient:#ffd700:#c62828>ᴄʜᴀᴍᴘɪᴏɴ",Material.GOLDEN_APPLE,     2200),
    REAPER    ("reaper",    "Reaper",    "<gradient:#9e9e9e:#212121>ʀᴇᴀᴘᴇʀ",  Material.IRON_HOE,         1100),
    VOID      ("void",      "Void",      "<gradient:#4a148c:#000000>ᴠᴏɪᴅ",    Material.OBSIDIAN,         1300),
    SAMURAI   ("samurai",   "Samurai",   "<gradient:#ff1744:#eceff1>sᴀᴍᴜʀᴀɪ", Material.IRON_SWORD,       1000),
    PIRATE    ("pirate",    "Pirate",    "<gradient:#8d6e63:#263238>ᴘɪʀᴀᴛᴇ",  Material.SPYGLASS,          800),
    VIKING    ("viking",    "Viking",    "<gradient:#90a4ae:#37474f>ᴠɪᴋɪɴɢ",  Material.IRON_AXE,          900),
    GLITCH    ("glitch",    "Glitch",    "<gradient:#00e5ff:#d500f9>ɢʟɪᴛᴄʜ",  Material.REDSTONE_TORCH,   1200),
    RADIANT   ("radiant",   "Radiant",   "<gradient:#ffff8d:#ffab00>ʀᴀᴅɪᴀɴᴛ", Material.SUNFLOWER,        1000),
    ONYX      ("onyx",      "Onyx",      "<gradient:#455a64:#000000>ᴏɴʏx",    Material.POLISHED_BLACKSTONE, 900),
    AURORA    ("aurora",    "Aurora",    "<gradient:#69f0ae:#40c4ff>ᴀᴜʀᴏʀᴀ",  Material.PRISMARINE_SHARD, 1400),
    BANDIT    ("bandit",    "Bandit",    "<gradient:#a1887f:#3e2723>ʙᴀɴᴅɪᴛ",  Material.CROSSBOW,          700),
    TITAN     ("titan",     "Titan",     "<gradient:#b0bec5:#ff6f00>ᴛɪᴛᴀɴ",   Material.ANVIL,            1600),
    CLUTCH    ("clutch",    "Clutch",    "<gradient:#ff4081:#ffea00>ᴄʟᴜᴛᴄʜ",  Material.TOTEM_OF_UNDYING, 1500),
    BLOSSOM   ("blossom",   "Blossom",   "<gradient:#f8bbd0:#ec407a>ʙʟᴏssᴏᴍ", Material.CHERRY_SAPLING,    600),
    SNIPER    ("sniper",    "Sniper",    "<gradient:#c8e6c9:#1b5e20>sɴɪᴘᴇʀ",  Material.BOW,               800),

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
