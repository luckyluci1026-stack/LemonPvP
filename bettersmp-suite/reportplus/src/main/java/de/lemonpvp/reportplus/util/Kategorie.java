package de.lemonpvp.reportplus.util;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Eine Grund-/Kategorie-Option aus config.yml ({id, icon, name}). */
public record Kategorie(String id, Material icon, String name) {

    public static List<Kategorie> laden(JavaPlugin plugin, String configPfad) {
        List<Kategorie> raus = new ArrayList<>();
        for (Map<?, ?> roh : plugin.getConfig().getMapList(configPfad)) {
            Object idRoh = roh.get("id");
            if (idRoh == null) {
                continue;
            }
            String id = String.valueOf(idRoh);
            Material icon = Material.matchMaterial(String.valueOf(roh.get("icon")));
            Object nameRoh = roh.get("name");
            String name = nameRoh == null ? id : String.valueOf(nameRoh);
            raus.add(new Kategorie(id, icon == null ? Material.PAPER : icon, name));
        }
        return raus;
    }
}
