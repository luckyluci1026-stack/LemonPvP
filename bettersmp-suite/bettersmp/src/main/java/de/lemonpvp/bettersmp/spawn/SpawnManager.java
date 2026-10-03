package de.lemonpvp.bettersmp.spawn;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

/**
 * Der Spawn dieses SMPs - ein fester Punkt, an dem jeder respawnt und
 * an den /spawn bringt, statt am zufaelligen Bett oder Weltspawn.
 *
 * Eigene spawn.yml statt eines Wertes in der config.yml: /setspawn
 * schreibt sie neu, und eine Datei, die das Plugin selbst ueberschreibt,
 * soll nicht dieselbe sein, in der der Admin von Hand herumeditiert.
 *
 * Fast identisch mit SMPLobbys eigenem SpawnManager - bewusst noch
 * einmal hier statt geteilt: BetterSMP und SMPLobby sind zwei Plugins
 * mit eigenem Lebenszyklus, jedes auf einem anderen Server installiert.
 */
public final class SpawnManager {

    private final BetterSMP plugin;
    private final File datei;

    private Location spawn;

    public SpawnManager(BetterSMP plugin) {
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

    /** @return false, wenn kein Spawn gesetzt ist */
    public boolean bringe(Player spieler) {
        if (spawn == null) {
            return false;
        }
        spieler.teleport(spawn.clone());
        return true;
    }
}
