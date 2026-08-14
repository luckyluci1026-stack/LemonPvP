package de.lemonpvp.smpcontent.content;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * Ein eigenes Item, Werkzeug, eine Waffe oder ein Block.
 *
 * @param id          eindeutiger Schlüssel aus der config.yml
 * @param block       true = Block (wird als Note-Block platziert)
 * @param material    Basis-Material des Items
 * @param name        Anzeigename (MiniMessage)
 * @param lore        Beschreibung (MiniMessage)
 * @param modelData   CustomModelData für Geyser/Bedrock
 * @param state       Blockzustand (nur bei Blöcken), sonst null
 * @param attributes  Attribut-Name -> Wert (z.B. attack_damage: 9)
 * @param maxDamage   eigene Haltbarkeit (0 = unverändert)
 * @param unbreakable Item nutzt sich nicht ab
 * @param enchants    Verzauberung -> Stufe
 * @param glow        Leucht-Effekt ohne echte Verzauberung
 * @param drops       was beim Abbauen fällt (leer = der Block selbst)
 * @param experience  Erfahrungspunkte beim Abbauen
 */
public record CustomEntry(
        String id,
        boolean block,
        Material material,
        String name,
        List<String> lore,
        int modelData,
        String state,
        Map<String, Double> attributes,
        int maxDamage,
        boolean unbreakable,
        Map<String, Integer> enchants,
        boolean glow,
        List<Drop> drops,
        int experience
) {

    /**
     * Ein möglicher Drop eines Blocks.
     *
     * @param item    Vanilla-Material oder "smp:id"
     * @param min     kleinste Menge
     * @param max     größte Menge
     * @param chance  0.0 bis 1.0
     * @param fortune ob Glück die Menge erhöht
     */
    public record Drop(String item, int min, int max, double chance, boolean fortune) {
    }
}
