package de.lemonpvp.smplobby.spawn;

import de.lemonpvp.smplobby.SMPLobby;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

/**
 * Wo die Lobby anfaengt.
 *
 * Der Punkt steht in einer eigenen spawn.yml und nicht in der config.yml:
 * Die config.yml darf der Admin von Hand bearbeiten, die spawn.yml wird
 * vom Plugin geschrieben. Beides in einer Datei hiesse, dass /setspawn
 * die Kommentare des Admins wegschreibt - und das faellt erst auf, wenn
 * man sie braucht.
 */
public final class SpawnManager {

    private final SMPLobby plugin;
    private final File datei;

    private Location spawn;

    public SpawnManager(SMPLobby plugin) {
        this.plugin = plugin;
        this.datei = new File(plugin.getDataFolder(), "spawn.yml");
        lade();
    }

    public void lade() {
        if (!datei.exists()) {
            this.spawn = null;
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(datei);
        String weltName = yml.getString("welt", "");
        World welt = weltName.isEmpty() ? null : Bukkit.getWorld(weltName);
        if (welt == null) {
            // Kommt vor, wenn die Lobbywelt umbenannt oder noch nicht
            // geladen ist. Kein Grund zu poltern - aber sagen muss man es,
            // sonst sucht der Admin die Ursache am falschen Ende.
            if (!weltName.isEmpty()) {
                plugin.getLogger().warning("Spawn zeigt auf die Welt \"" + weltName
                        + "\", die es hier nicht gibt. /setspawn setzt ihn neu.");
            }
            this.spawn = null;
            return;
        }
        this.spawn = new Location(welt,
                yml.getDouble("x"), yml.getDouble("y"), yml.getDouble("z"),
                (float) yml.getDouble("gier"), (float) yml.getDouble("nick"));
    }

    public void setze(Location ort) {
        this.spawn = ort.clone();
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("welt", ort.getWorld().getName());
        yml.set("x", ort.getX());
        yml.set("y", ort.getY());
        yml.set("z", ort.getZ());
        yml.set("gier", ort.getYaw());
        yml.set("nick", ort.getPitch());
        try {
            yml.save(datei);
        } catch (IOException fehler) {
            plugin.getLogger().severe("spawn.yml liess sich nicht schreiben: " + fehler.getMessage());
        }
    }

    public boolean gesetzt() {
        return spawn != null;
    }

    public Location ort() {
        return spawn == null ? null : spawn.clone();
    }

    /**
     * Zum Spawn bringen - oder sagen, dass es keinen gibt.
     *
     * @return false, wenn kein Spawn gesetzt ist
     */
    public boolean bringe(Player spieler) {
        if (spawn == null) {
            return false;
        }
        spieler.teleport(spawn.clone());
        return true;
    }
}
