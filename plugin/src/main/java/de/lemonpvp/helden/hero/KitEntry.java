package de.lemonpvp.helden.hero;

import org.bukkit.Material;

/**
 * Ein Eintrag im Startkit eines Helden.
 *
 * @param itemId   Artefakt-ID aus items.yml, oder {@code null}
 * @param material Vanilla-Material, wenn kein Artefakt gesetzt ist
 * @param amount   Stapelgroesse
 * @param slot     0-35 Inventar, 36 Schuhe, 37 Hose, 38 Brust, 39 Helm,
 *                 40 Zweithand, -1 = naechster freier Platz
 */
public record KitEntry(String itemId, Material material, int amount, int slot) {

    public boolean isCustomItem() {
        return itemId != null && !itemId.isEmpty();
    }
}
