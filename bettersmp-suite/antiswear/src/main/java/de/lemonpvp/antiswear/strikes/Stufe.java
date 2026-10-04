package de.lemonpvp.antiswear.strikes;

/** Eine Punkte-Schwelle aus config.yml (strikes.stufen) samt der Aktion, die beim Ueberschreiten passiert. */
public record Stufe(int schwelle, Aktion aktion, long dauerMillis, String befehl) {

    public enum Aktion { WARNEN, STUMMSCHALTEN, BEFEHL }
}
