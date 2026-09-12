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
 * @param extras      seltener gebrauchte Einstellungen
 * @param furniture   Möbel-Einstellungen, sonst null
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
        int experience,
        Extras extras,
        Furniture furniture
) {

    /** Ein Möbelstück wird als Anzeige-Objekt gesetzt, nicht als Notenblock. */
    public boolean isFurniture() {
        return furniture != null;
    }

    /**
     * Zusatzeinstellungen, die nicht jeder braucht.
     *
     * @param slot         Wo Attribute wirken ("head", "chest", "legs", "feet",
     *                     "mainhand", "offhand", "armor", "any").
     *                     Leer = passend zum Material geraten.
     * @param rarity       common, uncommon, rare, epic (färbt den Namen)
     * @param maxStack     eigene Stapelgröße (0 = unverändert)
     * @param requiresTool nötiges Werkzeug zum Abbauen ("pickaxe", "axe",
     *                     "shovel", "hoe"), sonst gibt es keine Drops
     * @param placeSound   Ton beim Setzen (leer = Standard)
     * @param breakSound   Ton beim Abbauen (leer = Standard)
     */
    public record Extras(String slot, String rarity, int maxStack,
                         String requiresTool, String placeSound, String breakSound) {

        public static final Extras NONE = new Extras("", "", 0, "", "", "");
    }

    /**
     * Möbel: kein Notenblock, sondern ein Anzeige-Objekt auf einem
     * unsichtbaren Platzhalterblock.
     *
     * Damit gibt es <b>keine Obergrenze</b> mehr - Note-Block-Zustände sind
     * irgendwann alle, Anzeige-Objekte nie. Außerdem kann sich ein Möbelstück
     * so nach Blickrichtung drehen, was ein Notenblock nicht kann.
     *
     * @param solid   true = man kann darauf stehen und stößt dagegen
     * @param rotate  Drehschritte beim Setzen (0 = nie drehen, 4 = Himmels-
     *                richtungen, 8 = auch diagonal, 16 = ganz fein)
     * @param seat    true = man kann sich draufsetzen
     * @param seatY   Sitzhöhe über der Blockunterkante
     * @param scale   Größe des Modells (1.0 = normal)
     * @param height  Höhe des Klickbereichs
     * @param width   Breite des Klickbereichs
     * @param light   Lichtstärke 0-15, die das Möbel abgibt
     */
    public record Furniture(boolean solid, int rotate, boolean seat, double seatY,
                            double scale, double height, double width, int light) {
    }

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
