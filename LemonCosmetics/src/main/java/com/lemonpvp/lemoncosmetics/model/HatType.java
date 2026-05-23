package com.lemonpvp.lemoncosmetics.model;

public enum HatType {
    LEMON_CROWN("lemon_crown", "Lemon Crown",   2001, 500),
    TROPICAL_LEAF("tropical_leaf", "Tropical Leaf", 2002, 500),
    BAMBOO_HAT("bamboo_hat",    "Bamboo Hat",    2003, 500),
    GOLD_HALO("gold_halo",      "Gold Halo",     2004, 500),
    PIRATE_HAT("pirate_hat",    "Pirate Hat",    2005, 500),
    PARTY_HAT("party_hat",      "Party Hat",     2006, 500);

    public final String id;
    public final String displayName;
    public final int customModelData;
    public final int price;

    HatType(String id, String displayName, int customModelData, int price) {
        this.id = id;
        this.displayName = displayName;
        this.customModelData = customModelData;
        this.price = price;
    }

    public static java.util.Optional<HatType> fromId(String id) {
        for (HatType h : values()) if (h.id.equals(id)) return java.util.Optional.of(h);
        return java.util.Optional.empty();
    }
}
