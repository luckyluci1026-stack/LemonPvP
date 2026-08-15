package de.lemonpvp.smpcontent.vehicle;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/**
 * Ein Fahrzeugtyp aus der {@code fahrzeuge.yml}.
 *
 * @param id      Schlüssel aus der Datei
 * @param kind    auto, jet oder zug
 * @param item    Id des eigenen Items, mit dem man es hinstellt
 * @param model   Id des eigenen Items, das als Modell gezeigt wird
 * @param speed   Höchstgeschwindigkeit in Blöcken pro Tick
 * @param power   wie schnell es beschleunigt
 * @param turn    Grad pro Tick beim Lenken
 * @param seats   wie viele mitfahren können (Fahrer eingerechnet)
 * @param scale   Größe des Modells
 * @param height  Höhe des Klickfelds zum Einsteigen
 * @param width   Breite des Klickfelds
 * @param sound   Fahrgeräusch (leer = keins)
 * @param fuel    Item, das als Sprit verbraucht wird (leer = kein Sprit nötig)
 * @param range   wie viele Blöcke ein Spritstück reicht
 */
public record VehicleType(String id, Kind kind, String item, String model,
                          double speed, double power, double turn, int seats,
                          double scale, double height, double width,
                          String sound, String fuel, int range) {

    /** Was für ein Fahrzeug es ist - danach richtet sich das Fahrverhalten. */
    public enum Kind {
        /** Fährt auf dem Boden, fällt, kann eine Stufe hochfahren. */
        CAR,
        /** Fliegt frei; Blickrichtung steuert, Springen hoch, Schleichen runter. */
        JET,
        /** Folgt Schienen und fährt nur dort. */
        TRAIN;

        static Kind of(String raw) {
            return switch (raw == null ? "" : raw.toLowerCase(Locale.ROOT).trim()) {
                case "jet", "flugzeug", "plane", "flieger" -> JET;
                case "zug", "train", "lok", "bahn" -> TRAIN;
                default -> CAR;
            };
        }
    }

    public static VehicleType read(String id, ConfigurationSection section) {
        return new VehicleType(
                id,
                Kind.of(section.getString("art", section.getString("type", "auto"))),
                section.getString("item", id),
                section.getString("model", section.getString("item", id)),
                section.getDouble("speed", 0.55),
                section.getDouble("power", 0.06),
                section.getDouble("turn", 4.5),
                Math.max(1, section.getInt("seats", 1)),
                section.getDouble("scale", 1.0),
                section.getDouble("height", 1.4),
                section.getDouble("width", 1.8),
                section.getString("sound", ""),
                section.getString("fuel", ""),
                Math.max(1, section.getInt("range", 400)));
    }
}
