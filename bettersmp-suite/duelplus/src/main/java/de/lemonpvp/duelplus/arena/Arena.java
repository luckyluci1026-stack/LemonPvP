package de.lemonpvp.duelplus.arena;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Eine einzelne Arena: eigene Welt, zwei Startpunkte darin, volle
 * Worldborder-Groesse (zum Zuruecksetzen nach dem Schrumpfen) und die
 * Y-Hoehe, auf der die Plattform TATSAECHLICH gebaut wurde.
 *
 * plattformHoehe wird bewusst HIER gespeichert (aus arena-meta.yml,
 * siehe ArenaManager) statt spaeter live aus der config.yml berechnet:
 * die Plattform wird nur beim ALLERERSTEN Erzeugen der Welt gebaut -
 * aendern sich die Terrain-Tiefen in der config.yml danach (z.B. durch
 * einen spaeteren Server-Neustart, ohne die Weltordner zu loeschen),
 * wuerde ein live neu berechneter Wert nicht mehr zur tatsaechlich
 * gebauten Plattform passen (falsche Spawn-Hoehe, falsche Worldborder-
 * Mitte). Der Sturz-Check in ArenaGuardListener haengt inzwischen NICHT
 * mehr von plattformHoehe ab (siehe arenen.todeslinie-y) - dieses Feld
 * bleibt trotzdem aus genau diesem Grund persistiert, nicht live gelesen.
 */
public record Arena(String name, World world, Location spawnA, Location spawnB, double vollGroesse, int plattformHoehe) {
}
