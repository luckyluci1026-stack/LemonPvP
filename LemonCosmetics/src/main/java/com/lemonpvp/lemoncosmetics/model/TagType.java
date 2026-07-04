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
    ACE       ("ace",       "Ace",       "<gradient:#ffffff:#ffd700>ᴀᴄᴇ",     Material.PAPER,             600),
    ALPHA     ("alpha",     "Alpha",     "<gradient:#ff1744:#ff9100>ᴀʟᴘʜᴀ",   Material.BLAZE_POWDER,      900),
    APEX      ("apex",      "Apex",      "<gradient:#00e5ff:#00695c>ᴀᴘᴇx",    Material.PRISMARINE_SHARD,  900),
    ARCANE    ("arcane",    "Arcane",    "<gradient:#b388ff:#4a148c>ᴀʀᴄᴀɴᴇ",  Material.ENCHANTED_BOOK,    800),
    ASSASSIN  ("assassin",  "Assassin",  "<gradient:#616161:#b71c1c>ᴀssᴀssɪɴ",Material.IRON_SWORD,       1000),
    BEAST     ("beast",     "Beast",     "<gradient:#ff6f00:#4e342e>ʙᴇᴀsᴛ",   Material.BONE,              700),
    BLAZE     ("blaze",     "Blaze",     "<gradient:#ffab00:#dd2c00>ʙʟᴀᴢᴇ",   Material.BLAZE_ROD,         700),
    BLITZ     ("blitz",     "Blitz",     "<gradient:#ffff00:#0091ea>ʙʟɪᴛᴢ",   Material.LIGHTNING_ROD,     800),
    BOUNTY    ("bounty",    "Bounty",    "<gradient:#ffd700:#795548>ʙᴏᴜɴᴛʏ",  Material.GOLD_NUGGET,       700),
    BRAWLER   ("brawler",   "Brawler",   "<gradient:#ff8a65:#bf360c>ʙʀᴀᴡʟᴇʀ", Material.LEATHER_CHESTPLATE,600),
    BRUTAL    ("brutal",    "Brutal",    "<gradient:#d50000:#212121>ʙʀᴜᴛᴀʟ",  Material.NETHERITE_AXE,    1100),
    BULLET    ("bullet",    "Bullet",    "<gradient:#cfd8dc:#455a64>ʙᴜʟʟᴇᴛ",  Material.IRON_NUGGET,       500),
    CHAOS     ("chaos",     "Chaos",     "<gradient:#d500f9:#ff1744>ᴄʜᴀᴏs",   Material.TNT,               900),
    CHIEF     ("chief",     "Chief",     "<gradient:#ffd740:#e65100>ᴄʜɪᴇꜰ",   Material.GOLDEN_CHESTPLATE,1000),
    COBRA     ("cobra",     "Cobra",     "<gradient:#aeea00:#33691e>ᴄᴏʙʀᴀ",   Material.FERMENTED_SPIDER_EYE,700),
    COMET     ("comet",     "Comet",     "<gradient:#80d8ff:#fffde7>ᴄᴏᴍᴇᴛ",   Material.FIRE_CHARGE,       800),
    CRIMSON   ("crimson",   "Crimson",   "<gradient:#ff1744:#4a0000>ᴄʀɪᴍsᴏɴ", Material.CRIMSON_FUNGUS,    700),
    CRUSADER  ("crusader",  "Crusader",  "<gradient:#eceff1:#c62828>ᴄʀᴜsᴀᴅᴇʀ",Material.SHIELD,            900),
    CYBER     ("cyber",     "Cyber",     "<gradient:#00e5ff:#651fff>ᴄʏʙᴇʀ",   Material.OBSERVER,          900),
    DAGGER    ("dagger",    "Dagger",    "<gradient:#b0bec5:#263238>ᴅᴀɢɢᴇʀ",  Material.FLINT,             600),
    DAWN      ("dawn",      "Dawn",      "<gradient:#ffcc80:#ff4081>ᴅᴀᴡɴ",    Material.SUNFLOWER,         600),
    DEADLY    ("deadly",    "Deadly",    "<gradient:#ff5252:#1a1a1a>ᴅᴇᴀᴅʟʏ",  Material.WITHER_ROSE,       900),
    DRAGON    ("dragon",    "Dragon",    "<gradient:#7c4dff:#d500f9>ᴅʀᴀɢᴏɴ",  Material.DRAGON_HEAD,      1600),
    DREAD     ("dread",     "Dread",     "<gradient:#37474f:#000000>ᴅʀᴇᴀᴅ",   Material.SKELETON_SKULL,    900),
    DUKE      ("duke",      "Duke",      "<gradient:#ce93d8:#4a148c>ᴅᴜᴋᴇ",    Material.GOLDEN_HELMET,     900),
    ECLIPSE   ("eclipse",   "Eclipse",   "<gradient:#ffd54f:#1a1a1a>ᴇᴄʟɪᴘsᴇ", Material.BLACK_STAINED_GLASS,1000),
    EMBER     ("ember",     "Ember",     "<gradient:#ff8a65:#bf360c>ᴇᴍʙᴇʀ",   Material.CAMPFIRE,          600),
    EMPIRE    ("empire",    "Empire",    "<gradient:#ffd700:#4a148c>ᴇᴍᴘɪʀᴇ",  Material.GOLD_BLOCK,       1400),
    FALCON    ("falcon",    "Falcon",    "<gradient:#90a4ae:#3e2723>ꜰᴀʟᴄᴏɴ",  Material.FEATHER,           700),
    FATAL     ("fatal",     "Fatal",     "<gradient:#ff1744:#880e4f>ꜰᴀᴛᴀʟ",   Material.SPIDER_EYE,        800),
    FLASH     ("flash",     "Flash",     "<gradient:#ffff8d:#ff6f00>ꜰʟᴀsʜ",   Material.GLOWSTONE_DUST,    700),
    FURY      ("fury",      "Fury",      "<gradient:#ff3d00:#b71c1c>ꜰᴜʀʏ",    Material.MAGMA_CREAM,       800),
    GLADIATOR ("gladiator", "Gladiator", "<gradient:#ffb300:#6d4c41>ɢʟᴀᴅɪᴀᴛᴏʀ",Material.IRON_CHESTPLATE, 1000),
    GOAT      ("goat",      "Goat",      "<gradient:#ffd700:#ffffff>ɢᴏᴀᴛ",    Material.GOAT_HORN,        2000),
    GRIM      ("grim",      "Grim",      "<gradient:#9e9e9e:#000000>ɢʀɪᴍ",    Material.IRON_HOE,          800),
    HAWK      ("hawk",      "Hawk",      "<gradient:#8d6e63:#fbc02d>ʜᴀᴡᴋ",    Material.SPYGLASS,          700),
    HERO      ("hero",      "Hero",      "<gradient:#40c4ff:#ffd700>ʜᴇʀᴏ",    Material.GOLDEN_APPLE,      900),
    HUNTER    ("hunter",    "Hunter",    "<gradient:#8bc34a:#33691e>ʜᴜɴᴛᴇʀ",  Material.CROSSBOW,          700),
    HYDRA     ("hydra",     "Hydra",     "<gradient:#00bfa5:#004d40>ʜʏᴅʀᴀ",   Material.PRISMARINE_CRYSTALS,1000),
    ICY       ("icy",       "Icy",       "<gradient:#e0f7fa:#4fc3f7>ɪᴄʏ",     Material.PACKED_ICE,        500),
    IMMORTAL  ("immortal",  "Immortal",  "<gradient:#ffd700:#00e5ff>ɪᴍᴍᴏʀᴛᴀʟ",Material.TOTEM_OF_UNDYING, 2500),
    JESTER    ("jester",    "Jester",    "<gradient:#ff4081:#7c4dff>ᴊᴇsᴛᴇʀ",  Material.NOTE_BLOCK,        600),
    KARMA     ("karma",     "Karma",     "<gradient:#b2ff59:#f50057>ᴋᴀʀᴍᴀ",   Material.AMETHYST_CLUSTER,  800),
    KNIGHT    ("knight",    "Knight",    "<gradient:#cfd8dc:#37474f>ᴋɴɪɢʜᴛ",  Material.IRON_HELMET,       800),
    KRAKEN    ("kraken",    "Kraken",    "<gradient:#26c6da:#0d47a1>ᴋʀᴀᴋᴇɴ",  Material.INK_SAC,          1100),
    LION      ("lion",      "Lion",      "<gradient:#ffb300:#e65100>ʟɪᴏɴ",    Material.ORANGE_WOOL,       900),
    LUCKY     ("lucky",     "Lucky",     "<gradient:#69f0ae:#ffd700>ʟᴜᴄᴋʏ",   Material.RABBIT_FOOT,       777),
    LUNAR     ("lunar",     "Lunar",     "<gradient:#e1f5fe:#5c6bc0>ʟᴜɴᴀʀ",   Material.QUARTZ,            800),
    MACHINE   ("machine",   "Machine",   "<gradient:#b0bec5:#546e7a>ᴍᴀᴄʜɪɴᴇ", Material.PISTON,            700),
    MANIAC    ("maniac",    "Maniac",    "<gradient:#ff1744:#d500f9>ᴍᴀɴɪᴀᴄ",  Material.TNT_MINECART,      800),
    MENACE    ("menace",    "Menace",    "<gradient:#ff5252:#37474f>ᴍᴇɴᴀᴄᴇ",  Material.CACTUS,            800),
    METEOR    ("meteor",    "Meteor",    "<gradient:#ff9100:#3e2723>ᴍᴇᴛᴇᴏʀ",  Material.MAGMA_BLOCK,       900),
    MYSTIC    ("mystic",    "Mystic",    "<gradient:#80deea:#7c4dff>ᴍʏsᴛɪᴄ",  Material.AMETHYST_SHARD,    800),
    NEON      ("neon",      "Neon",      "<gradient:#76ff03:#00e5ff>ɴᴇᴏɴ",    Material.SEA_LANTERN,       800),
    NIGHTMARE ("nightmare", "Nightmare", "<gradient:#4a148c:#000000>ɴɪɢʜᴛᴍᴀʀᴇ",Material.PHANTOM_MEMBRANE,1000),
    NOMAD     ("nomad",     "Nomad",     "<gradient:#d7ccc8:#6d4c41>ɴᴏᴍᴀᴅ",   Material.LEATHER_BOOTS,     500),
    OMEGA     ("omega",     "Omega",     "<gradient:#ff6d00:#212121>ᴏᴍᴇɢᴀ",   Material.NETHERITE_INGOT,  1800),
    ORACLE    ("oracle",    "Oracle",    "<gradient:#ba68c8:#311b92>ᴏʀᴀᴄʟᴇ",  Material.ENDER_EYE,         900),
    OUTLAW    ("outlaw",    "Outlaw",    "<gradient:#a1887f:#212121>ᴏᴜᴛʟᴀᴡ",  Material.IRON_BARS,         700),
    PANDA     ("panda",     "Panda",     "<gradient:#ffffff:#212121>ᴘᴀɴᴅᴀ",   Material.BAMBOO,            600),
    PHANTOM   ("phantom",   "Phantom",   "<gradient:#b39ddb:#311b92>ᴘʜᴀɴᴛᴏᴍ", Material.PHANTOM_MEMBRANE,  800),
    PIXEL     ("pixel",     "Pixel",     "<gradient:#00e676:#2979ff>ᴘɪxᴇʟ",   Material.PAINTING,          600),
    PLAGUE    ("plague",    "Plague",    "<gradient:#9e9d24:#33691e>ᴘʟᴀɢᴜᴇ",  Material.POISONOUS_POTATO,  800),
    PRIME     ("prime",     "Prime",     "<gradient:#ffd700:#ff1744>ᴘʀɪᴍᴇ",   Material.BEACON,           1500),
    PRODIGY   ("prodigy",   "Prodigy",   "<gradient:#40c4ff:#00c853>ᴘʀᴏᴅɪɢʏ", Material.WRITABLE_BOOK,     800),
    QUAKE     ("quake",     "Quake",     "<gradient:#8d6e63:#3e2723>ǫᴜᴀᴋᴇ",   Material.COBBLESTONE,       700),
    RAGE      ("rage",      "Rage",      "<gradient:#ff1744:#ff6d00>ʀᴀɢᴇ",    Material.FIRE_CORAL,        700),
    RAPID     ("rapid",     "Rapid",     "<gradient:#00e5ff:#ffffff>ʀᴀᴘɪᴅ",   Material.SUGAR,             600),
    RAVEN     ("raven",     "Raven",     "<gradient:#455a64:#000000>ʀᴀᴠᴇɴ",   Material.BLACK_DYE,         700),
    REBEL     ("rebel",     "Rebel",     "<gradient:#ff5252:#ffab00>ʀᴇʙᴇʟ",   Material.CHAIN,             700),
    ROGUE     ("rogue",     "Rogue",     "<gradient:#78909c:#212121>ʀᴏɢᴜᴇ",   Material.LEATHER_HELMET,    700),
    RUTHLESS  ("ruthless",  "Ruthless",  "<gradient:#b71c1c:#000000>ʀᴜᴛʜʟᴇss",Material.NETHERITE_SWORD,  1200),
    SCAR      ("scar",      "Scar",      "<gradient:#ef9a9a:#7f0000>sᴄᴀʀ",    Material.PINK_TULIP,        600),
    SERPENT   ("serpent",   "Serpent",   "<gradient:#66bb6a:#1b5e20>sᴇʀᴘᴇɴᴛ", Material.VINE,              700),
    SLAYER    ("slayer",    "Slayer",    "<gradient:#ff1744:#37474f>sʟᴀʏᴇʀ",  Material.DIAMOND_AXE,       900),
    SPARK     ("spark",     "Spark",     "<gradient:#fff176:#00e5ff>sᴘᴀʀᴋ",   Material.COPPER_INGOT,      600),
    SPECTRE   ("spectre",   "Spectre",   "<gradient:#eceff1:#607d8b>sᴘᴇᴄᴛʀᴇ", Material.GLASS,             800),
    STORM     ("storm",     "Storm",     "<gradient:#4fc3f7:#263238>sᴛᴏʀᴍ",   Material.CONDUIT,          1000),
    SWIFT     ("swift",     "Swift",     "<gradient:#b2ff59:#00bfa5>sᴡɪꜰᴛ",   Material.RABBIT_HIDE,       600),
    TEMPEST   ("tempest",   "Tempest",   "<gradient:#00b0ff:#6a1b9a>ᴛᴇᴍᴘᴇsᴛ", Material.TRIDENT,          1100),

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
