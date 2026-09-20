package de.lemonpvp.duelplus.arena;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Eine einzelne Arena: eigene Welt, zwei Startpunkte darin, volle
 * Worldborder-Groesse (zum Zuruecksetzen nach dem Schrumpfen) und die
 * Y-Hoehe, auf der die Plattform TATSAECHLICH gebaut wurde.
 *
 * plattformHoehe wird bewusst HIER gespeichert statt spaeter live aus
 * arenen.plattform-hoehe in der config.yml nachgelesen: die Plattform
 * wird nur beim ALLERERSTEN Erzeugen der Welt gebaut (siehe
 * ArenaManager) - aendert sich der Config-Wert danach (z.B. durch einen
 * spaeteren Server-Neustart mit angepasster config.yml, ohne die
 * Weltordner zu loeschen), wuerde ein live nachgelesener Wert nicht
 * mehr zur tatsaechlich gebauten Plattform passen. Das hat frueher dazu
 * gefuehrt, dass ArenaGuardListener.beimAbsturzUnterDieArena mit einer
 * falschen Schwelle rechnete und Spieler, die ganz normal auf der
 * Plattform standen, sofort als "abgestuerzt" behandelt wurden.
 */
public record Arena(String name, World world, Location spawnA, Location spawnB, double vollGroesse, int plattformHoehe) {
}
