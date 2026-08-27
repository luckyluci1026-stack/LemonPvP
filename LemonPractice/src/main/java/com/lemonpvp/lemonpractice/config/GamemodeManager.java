package com.lemonpvp.lemonpractice.config;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Gamemode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class GamemodeManager {

    private final LemonPractice plugin;
    private final Map<String, Gamemode> gamemodes = new LinkedHashMap<>();

    public GamemodeManager(LemonPractice plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        gamemodes.clear();
        File file = new File(plugin.getDataFolder(), "gamemodes.yml");
        if (!file.exists()) {
            plugin.saveResource("gamemodes.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (!cfg.isConfigurationSection("gamemodes")) return;

        for (String id : cfg.getConfigurationSection("gamemodes").getKeys(false)) {
            String path = "gamemodes." + id;
            String name = cfg.getString(path + ".name", id);
            String displayName = cfg.getString(path + ".display-name", name);
            String matName = cfg.getString(path + ".material", "STONE");
            Material material;
            try {
                material = Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException e) {
                material = Material.STONE;
            }
            boolean enabled = cfg.getBoolean(path + ".enabled", true);
            int slot = cfg.getInt(path + ".slot", gamemodes.size());
            gamemodes.put(id.toLowerCase(), new Gamemode(id.toLowerCase(), name, displayName, material, slot, enabled));
        }
    }

    public Gamemode getGamemode(String id) {
        return gamemodes.get(id.toLowerCase());
    }

    public Collection<Gamemode> getAllGamemodes() {
        return gamemodes.values();
    }

    public List<Gamemode> getEnabledGamemodes() {
        List<Gamemode> list = new ArrayList<>();
        for (Gamemode gm : gamemodes.values()) {
            if (gm.isEnabled()) list.add(gm);
        }
        return list;
    }

    public boolean exists(String id) {
        return gamemodes.containsKey(id.toLowerCase());
    }

    public void loadGamemodes() {
        load();
    }
}
