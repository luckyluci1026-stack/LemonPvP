package de.lemonpvp.bettersmp.home;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public record Home(int platz, String name, String welt, UUID weltId, double x, double y, double z, float yaw, float pitch) {

    public static Home von(int platz, String name, Location ort) {
        World w = ort.getWorld();
        return new Home(platz, name, w.getName(), w.getUID(), ort.getX(), ort.getY(), ort.getZ(), ort.getYaw(), ort.getPitch());
    }

    public World weltFinden() {
        World w = weltId == null ? null : Bukkit.getWorld(weltId);
        if (w == null && welt != null) {
            w = Bukkit.getWorld(welt);
        }
        return w;
    }

    public Location ort() {
        World w = weltFinden();
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }

    public Home mitPlatz(int neuerPlatz) {
        return new Home(neuerPlatz, name, welt, weltId, x, y, z, yaw, pitch);
    }

    public boolean heisst(String eingabe) {
        return name != null && name.equalsIgnoreCase(eingabe);
    }
}
