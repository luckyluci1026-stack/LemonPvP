package de.lemonpvp.antiswear.filter;

/** Ein erkanntes Wort aus woerter.yml samt seiner Punktzahl. */
public record Treffer(String wort, int punkte) {
}
