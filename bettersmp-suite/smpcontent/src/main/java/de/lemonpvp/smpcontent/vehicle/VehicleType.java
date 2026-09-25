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
 * @param hover   nur beim Fliegen: bleibt ohne Schub in der Luft stehen
 *                (Hubschrauber), statt langsam durchzusacken (Flugzeug)
 * @param eject   Schleudersitz und Fallschirm
 * @param crash   was passiert, wenn man gegen etwas fährt
 * @param hud     Tacho über der Hotbar während der Fahrt
 * @param rotor   Höhe eines drehenden Rotorblatts über dem Fahrzeug
 *                (0 = keins). Hubschrauber und Propellermaschinen.
 * @param horn    Ton beim Drücken der Leertaste (leer = keine Hupe)
 */
public record VehicleType(String id, Kind kind, String item, String model,
                          double speed, double power, double turn, int seats,
                          double scale, double height, double width,
                          String sound, String fuel, int range, boolean hover,
                          Eject eject, Crash crash, boolean hud, double rotor,
                          String horn) {

    /**
     * Der Aufprall.
     *
     * Erkannt wird er daran, dass das Fahrzeug viel weniger weit gekommen
     * ist, als es wollte - also gegen etwas gefahren ist. Was dann passiert,
     * stellst du selbst ein: vom harmlosen Rempler bis zum Feuerball.
     *
     * @param enabled     ob dieses Fahrzeug überhaupt verunglücken kann
     * @param minSpeed    ab welchem Tempo es kracht (langsames Anstoßen nicht)
     * @param damage      Schaden für alle Insassen
     * @param explosion   Stärke der Explosion (0 = keine)
     * @param breakBlocks ob die Explosion Blöcke zerstört
     * @param destroy     ob das Fahrzeug dabei kaputtgeht
     * @param sound       Ton beim Aufprall
     * @param ram         Schaden für den, den man umfährt (0 = niemand)
     * @param hardLanding ab welcher Sinkgeschwindigkeit ein Flieger beim
     *                    Aufsetzen zu Bruch geht (0 = nie)
     */
    public record Crash(boolean enabled, double minSpeed, double damage,
                        double explosion, boolean breakBlocks, boolean destroy,
                        String sound, double ram, double hardLanding) {

        public static final Crash NONE =
                new Crash(false, 0.5, 6.0, 0, false, true,
                        "entity.generic.explode", 0, 0);

        static Crash read(ConfigurationSection section) {
            if (section == null) {
                return NONE;
            }
            return new Crash(
                    section.getBoolean("enabled", true),
                    Math.max(0.05, section.getDouble("min-speed", 0.5)),
                    Math.max(0, section.getDouble("damage", 6.0)),
                    Math.max(0, section.getDouble("explosion", 0)),
                    section.getBoolean("break-blocks", false),
                    section.getBoolean("destroy", true),
                    section.getString("sound", "entity.generic.explode"),
                    Math.max(0, section.getDouble("ram", 0)),
                    Math.max(0, section.getDouble("hard-landing", 0)));
        }
    }

    /**
     * Der Schleudersitz.
     *
     * Zweimal schnell schleichen, und es schießt dich aus dem Flugzeug -
     * darüber geht der Fallschirm auf. Jeder Wert steht in der
     * fahrzeuge.yml, du kannst dir das also selbst zusammenstellen.
     *
     * @param enabled   ob dieses Fahrzeug einen Schleudersitz hat
     * @param window    wie schnell die zwei Schleicher aufeinander folgen
     *                  müssen, in Sekunden
     * @param power     wie hoch es dich schleudert
     * @param forward   wieviel davon nach vorne geht
     * @param sound     Ton beim Auswurf
     * @param parachute Id des eigenen Items, das als Schirm gezeigt wird
     *                  (leer = kein Schirm, dann fällst du einfach)
     * @param openAfter Ticks, bis der Schirm aufgeht
     * @param fallSpeed wie schnell man am Schirm sinkt
     * @param drift     wie gut man am Schirm lenken kann
     */
    public record Eject(boolean enabled, double window, double power, double forward,
                        String sound, String parachute, int openAfter,
                        double fallSpeed, double drift) {

        /** Kein Schleudersitz - Standard für alles, was am Boden bleibt. */
        public static final Eject NONE =
                new Eject(false, 1.0, 1.4, 0.3, "", "", 8, 0.18, 0.08);

        static Eject read(ConfigurationSection section) {
            if (section == null) {
                return NONE;
            }
            return new Eject(
                    section.getBoolean("enabled", true),
                    Math.max(0.1, section.getDouble("window", 1.0)),
                    section.getDouble("power", 1.4),
                    section.getDouble("forward", 0.3),
                    section.getString("sound", "entity.firework_rocket.launch"),
                    section.getString("parachute", ""),
                    Math.max(0, section.getInt("open-after", 8)),
                    Math.max(0.01, section.getDouble("fall-speed", 0.18)),
                    Math.max(0, section.getDouble("drift", 0.08)));
        }
    }

    /** Was für ein Fahrzeug es ist - danach richtet sich das Fahrverhalten. */
    public enum Kind {
        /** Fährt auf dem Boden, fällt, kann eine Stufe hochfahren. */
        CAR,
        /** Fliegt frei; Blickrichtung steuert, Springen hoch, Schleichen runter. */
        JET,
        /** Folgt Schienen und fährt nur dort. */
        TRAIN,
        /** Schwimmt auf dem Wasser und kommt an Land nicht weit. */
        BOAT;

        static Kind of(String raw) {
            return switch (raw == null ? "" : raw.toLowerCase(Locale.ROOT).trim()) {
                case "jet", "flugzeug", "plane", "flieger" -> JET;
                case "zug", "train", "lok", "bahn" -> TRAIN;
                case "boot", "schiff", "boat", "ship", "yacht" -> BOAT;
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
                Math.max(1, section.getInt("range", 400)),
                section.getBoolean("hover", false),
                Eject.read(section.getConfigurationSection("eject")),
                Crash.read(section.getConfigurationSection("crash")),
                section.getBoolean("hud", true),
                Math.max(0, section.getDouble("rotor", 0)),
                section.getString("horn", ""));
    }
}
