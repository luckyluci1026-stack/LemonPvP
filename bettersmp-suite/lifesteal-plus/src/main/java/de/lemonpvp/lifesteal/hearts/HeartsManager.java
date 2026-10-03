package de.lemonpvp.lifesteal.hearts;

import de.lemonpvp.lifesteal.LifestealPlus;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Verwaltet Herzen über die echte MAX_HEALTH-Eigenschaft und persistiert
 * sie in data.yml (auch für Offline-Spieler und Elimination-Status).
 */
public final class HeartsManager {

    private final LifestealPlus plugin;
    private final File dataFile;
    private YamlConfiguration data;

    public HeartsManager(LifestealPlus plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Konnte data.yml nicht anlegen: " + e.getMessage());
            }
        }
        this.data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public int starting() {
        return plugin.getConfig().getInt("hearts.starting", 10);
    }

    public int maximum() {
        return plugin.getConfig().getInt("hearts.maximum", 20);
    }

    public int eliminateAt() {
        return plugin.getConfig().getInt("hearts.eliminate-at", 0);
    }

    private String path(UUID uuid) {
        return "players." + uuid + ".hearts";
    }

    private String elimPath(UUID uuid) {
        return "players." + uuid + ".eliminated";
    }

    /** Herzen aus dem Speicher (Standard: starting). */
    public int getHearts(UUID uuid) {
        return data.getInt(path(uuid), starting());
    }

    public boolean isEliminated(UUID uuid) {
        return data.getBoolean(elimPath(uuid), false);
    }

    /** Setzt Herzen (geklemmt auf [0, maximum]) und aktualisiert Attribut + Speicher. */
    public void setHearts(UUID uuid, int hearts) {
        int clamped = Math.max(0, Math.min(maximum(), hearts));
        data.set(path(uuid), clamped);
        save();
        applyToPlayer(uuid, clamped);
    }

    public void addHearts(UUID uuid, int delta) {
        setHearts(uuid, getHearts(uuid) + delta);
    }

    /** Wendet die Herzzahl live auf das MAX_HEALTH-Attribut an. */
    public void applyToPlayer(UUID uuid, int hearts) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        double value = Math.max(1.0, hearts * 2.0);
        attribute.setBaseValue(value);
        if (player.getHealth() > value) {
            player.setHealth(value);
        }
    }

    /** Beim Beitritt: Attribut mit gespeicherter Herzzahl synchronisieren. */
    public void syncOnJoin(Player player) {
        UUID uuid = player.getUniqueId();
        if (!data.contains(path(uuid))) {
            data.set(path(uuid), starting());
            save();
        }
        applyToPlayer(uuid, getHearts(uuid));
    }

    public void setEliminated(UUID uuid, boolean eliminated) {
        data.set(elimPath(uuid), eliminated);
        save();
    }

    public OfflinePlayer offline(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }

    public synchronized void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte data.yml nicht speichern: " + e.getMessage());
        }
    }

    public void reload() {
        this.data = YamlConfiguration.loadConfiguration(dataFile);
    }
}
