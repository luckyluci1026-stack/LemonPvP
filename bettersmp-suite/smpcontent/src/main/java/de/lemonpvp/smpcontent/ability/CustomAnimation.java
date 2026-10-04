package de.lemonpvp.smpcontent.ability;

import java.util.Locale;
import java.util.Map;

/**
 * Eine selbst gebaute Animation aus der animationen.yml.
 *
 * Pro Tick werden {@code points} Teilchen gesetzt; wo sie landen, sagen die
 * drei Formeln. Verfügbare Variablen:
 *
 * <pre>
 *   t       Tick, 0 bis ticks-1
 *   p       Fortschritt 0..1
 *   i       Nummer des Teilchens in diesem Tick, 0 bis points-1
 *   n       points
 *   a       Winkel für dieses Teilchen (2*pi*i/n) - spart cos(i*...)
 *   ticks   Gesamtdauer
 *   radius  Wert "radius" aus der Fähigkeit
 *   length  Wert "length" aus der Fähigkeit
 * </pre>
 */
public record CustomAnimation(int points, Formula x, Formula y, Formula z,
                              Frame frame, boolean follow) {

    /** Worauf sich die Koordinaten beziehen. */
    public enum Frame {
        /** Füße des Spielers, Achsen wie in der Welt */
        PLAYER,
        /** Augenhöhe, Achsen wie in der Welt */
        EYES,
        /** Augenhöhe, z = Blickrichtung, x = rechts, y = oben */
        LOOK;

        static Frame of(String value) {
            if (value == null) {
                return PLAYER;
            }
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "eyes", "augen" -> EYES;
                case "look", "blick", "blickrichtung" -> LOOK;
                default -> PLAYER;
            };
        }
    }

    public static CustomAnimation from(Map<String, Object> raw) {
        return new CustomAnimation(
                Math.max(1, Math.min(400, (int) Ability.number(raw, "points", 16))),
                Formula.compile(Ability.string(raw, "x", "0")),
                Formula.compile(Ability.string(raw, "y", "0")),
                Formula.compile(Ability.string(raw, "z", "0")),
                Frame.of(Ability.string(raw, "relative-to", "player")),
                Ability.flag(raw, "follow", false));
    }
}
