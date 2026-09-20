package de.lemonpvp.duelplus.arena;

import org.bukkit.Location;
import org.bukkit.World;

/** Eine einzelne Arena: eigene Welt, zwei Startpunkte darin, volle Worldborder-Groesse (zum Zuruecksetzen nach dem Schrumpfen). */
public record Arena(String name, World world, Location spawnA, Location spawnB, double vollGroesse) {
}
