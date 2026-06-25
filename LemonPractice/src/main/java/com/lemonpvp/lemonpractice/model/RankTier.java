package com.lemonpvp.lemonpractice.model;

import org.bukkit.Material;

/**
 * ELO-based competitive divisions, FoxPvP/FlowPvP style.
 *
 * <p>Each tier carries:
 * <ul>
 *   <li>{@code minElo} — inclusive lower ELO bound for the tier.</li>
 *   <li>{@code display} — a MiniMessage gradient label in small caps
 *       (no texture pack required).</li>
 *   <li>{@code symbol} — a compact one-glyph badge for scoreboard/tablist.</li>
 *   <li>{@code icon} — the GUI material representing the tier.</li>
 * </ul>
 *
 * <p>Use {@link #fromElo(int)} to resolve a tier from a rating. Players still
 * in placement should be shown as {@link #UNRANKED} regardless of their raw ELO.
 */
public enum RankTier {

    UNRANKED ("Unranked",    Integer.MIN_VALUE, "<gradient:#9e9e9e:#616161>ᴜɴʀᴀɴᴋᴇᴅ",   "✫", Material.GRAY_DYE),
    BRONZE   ("Bronze",      0,                 "<gradient:#bcaaa4:#8d6e63>ʙʀᴏɴᴢᴇ",     "❶", Material.COPPER_INGOT),
    SILVER   ("Silver",      900,               "<gradient:#eceff1:#b0bec5>sɪʟᴠᴇʀ",     "❷", Material.IRON_INGOT),
    GOLD     ("Gold",        1100,              "<gradient:#ffe082:#ffb300>ɢᴏʟᴅ",       "❸", Material.GOLD_INGOT),
    PLATINUM ("Platinum",    1300,              "<gradient:#80deea:#00acc1>ᴘʟᴀᴛɪɴᴜᴍ",   "❹", Material.DIAMOND),
    DIAMOND  ("Diamond",     1500,              "<gradient:#40c4ff:#2962ff>ᴅɪᴀᴍᴏɴᴅ",    "❺", Material.DIAMOND_BLOCK),
    MASTER   ("Master",      1800,              "<gradient:#e040fb:#7c4dff>ᴍᴀsᴛᴇʀ",     "❻", Material.AMETHYST_CLUSTER),
    LEGEND   ("Legend",      2100,              "<gradient:#ff8a65:#dd2c00>ʟᴇɢᴇɴᴅ",     "❼", Material.NETHER_STAR);

    private final String displayName;
    private final int minElo;
    private final String display;
    private final String symbol;
    private final Material icon;

    RankTier(String displayName, int minElo, String display, String symbol, Material icon) {
        this.displayName = displayName;
        this.minElo = minElo;
        this.display = display;
        this.symbol = symbol;
        this.icon = icon;
    }

    public String getDisplayName() { return displayName; }
    public int getMinElo() { return minElo; }
    /** MiniMessage gradient label (small caps). Prefix with {@code <!italic>} when rendering in lore. */
    public String getDisplay() { return display; }
    public String getSymbol() { return symbol; }
    public Material getIcon() { return icon; }

    /** Compact colored badge for scoreboard/tablist: gradient tag + symbol glyph. */
    public String badge() {
        // display starts with the MiniMessage color/gradient tag; extract it.
        int end = display.indexOf('>');
        String colorPrefix = end >= 0 ? display.substring(0, end + 1) : "<gray>";
        return colorPrefix + symbol;
    }

    /**
     * Resolves the highest tier whose {@code minElo} does not exceed {@code elo}.
     * {@link #UNRANKED} is never returned here (its bound is MIN_VALUE only as a
     * sentinel) — callers should special-case placement players themselves.
     */
    public static RankTier fromElo(int elo) {
        RankTier result = BRONZE;
        for (RankTier tier : values()) {
            if (tier == UNRANKED) continue;
            if (elo >= tier.minElo) result = tier;
        }
        return result;
    }

    /** ELO needed to reach the next tier, or -1 if already at the top. */
    public int nextTierElo() {
        RankTier[] values = values();
        int idx = ordinal() + 1;
        if (idx >= values.length) return -1;
        return values[idx].minElo;
    }

    /** The tier directly above this one, or null if already at the top. */
    public RankTier next() {
        RankTier[] values = values();
        int idx = ordinal() + 1;
        return idx < values.length ? values[idx] : null;
    }
}
