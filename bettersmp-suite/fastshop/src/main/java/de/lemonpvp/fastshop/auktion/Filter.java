package de.lemonpvp.fastshop.auktion;

import org.bukkit.Material;

import java.util.Set;

public enum Filter {

    ALLE("Alle Items", Material.CHEST),
    BLOECKE("Blöcke", Material.GRASS_BLOCK),
    WERKZEUGE("Werkzeuge", Material.DIAMOND_PICKAXE),
    KAMPF("Kampf & Rüstung", Material.DIAMOND_SWORD),
    ESSEN("Essen", Material.COOKED_BEEF),
    TRAENKE_BUECHER("Tränke & Bücher", Material.ENCHANTED_BOOK),
    SONSTIGES("Sonstiges", Material.ENDER_PEARL);

    private static final Set<Material> KAMPF_EINZELN = Set.of(
            Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.MACE, Material.SHIELD,
            Material.TOTEM_OF_UNDYING, Material.ARROW, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW,
            Material.END_CRYSTAL, Material.ELYTRA, Material.TURTLE_HELMET);

    private static final Set<Material> WERKZEUGE_EINZELN = Set.of(
            Material.SHEARS, Material.FISHING_ROD, Material.FLINT_AND_STEEL, Material.BRUSH,
            Material.COMPASS, Material.RECOVERY_COMPASS, Material.CLOCK, Material.SPYGLASS, Material.LEAD);

    private static final Set<Material> TRAENKE_BUECHER_EINZELN = Set.of(
            Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION, Material.ENCHANTED_BOOK,
            Material.BOOK, Material.WRITABLE_BOOK, Material.WRITTEN_BOOK, Material.EXPERIENCE_BOTTLE);

    private final String anzeige;
    private final Material symbol;

    Filter(String anzeige, Material symbol) {
        this.anzeige = anzeige;
        this.symbol = symbol;
    }

    public String anzeige() {
        return anzeige;
    }

    public Material symbol() {
        return symbol;
    }

    public Filter naechster() {
        Filter[] alle = values();
        return alle[(ordinal() + 1) % alle.length];
    }

    public boolean passt(Material material) {
        return this == ALLE || this == von(material);
    }

    public static Filter von(Material material) {
        String name = material.name();
        if (KAMPF_EINZELN.contains(material) || name.endsWith("_SWORD") || name.endsWith("_SPEAR")
                || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")) {
            return KAMPF;
        }
        if (WERKZEUGE_EINZELN.contains(material) || name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE") || name.endsWith("_AXE")) {
            return WERKZEUGE;
        }
        if (TRAENKE_BUECHER_EINZELN.contains(material)) {
            return TRAENKE_BUECHER;
        }
        if (material.isEdible()) {
            return ESSEN;
        }
        if (material.isBlock()) {
            return BLOECKE;
        }
        return SONSTIGES;
    }
}
