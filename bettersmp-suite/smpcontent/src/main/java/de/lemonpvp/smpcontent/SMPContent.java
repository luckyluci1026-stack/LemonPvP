package de.lemonpvp.smpcontent;

import de.lemonpvp.smpcontent.ability.Abilities;
import de.lemonpvp.smpcontent.command.ContentCommand;
import de.lemonpvp.smpcontent.content.ContentRegistry;
import de.lemonpvp.smpcontent.content.CustomEntry;
import de.lemonpvp.smpcontent.gui.ContentGui;
import de.lemonpvp.smpcontent.listener.AbilityListener;
import de.lemonpvp.smpcontent.listener.BlockListener;
import de.lemonpvp.smpcontent.listener.GuiListener;
import de.lemonpvp.smpcontent.pack.PackGenerator;
import de.lemonpvp.smpcontent.util.ConfigProblem;
import de.lemonpvp.smpcontent.util.Msgs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SMPContent - eigene Blöcke und Items passend zum SMP-Texturepack.
 *
 * Blöcke werden über feste Note-Block-Zustände umgesetzt; das Plugin hält
 * diese Zustände stabil und sorgt für die richtigen Drops. Items bekommen
 * ein item_model (Java) und CustomModelData (Geyser/Bedrock).
 */
public final class SMPContent extends JavaPlugin {

    /** Zusatzdateien mit eigenen Inhalten, die beim ersten Start angelegt werden. */
    private static final String[] CONTENT_FILES = {
            "laserschwerter.yml", "platzhalter-bloecke.yml", "platzhalter-items.yml",
    };

    private Msgs msgs;
    private ContentRegistry registry;
    private PackGenerator pack;
    private Abilities abilities;
    private ContentGui gui;

    private YamlConfiguration config;
    private ConfigProblem.Report configProblem;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        saveExtras();
        this.msgs = new Msgs(this);
        this.abilities = new Abilities(this);
        this.registry = new ContentRegistry(this);
        this.pack = new PackGenerator(this);
        this.gui = new ContentGui(this);
        pack.ensureFolders();

        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AbilityListener(this), this);
        getCommand("smpcontent").setExecutor(new ContentCommand(this));
        startHeldTask();

        getLogger().info("SMPContent aktiviert.");
    }

    /**
     * Der Auslöser "held" wirkt, solange man das Item in der Hand hat.
     * Der Zeitgeber läuft nur, wenn es überhaupt so eine Fähigkeit gibt.
     */
    private void startHeldTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!abilities.hasTrigger("held")) {
                return;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                String id = registry.idOf(player.getInventory().getItemInMainHand());
                if (id == null) {
                    continue;
                }
                CustomEntry entry = registry.get(id);
                if (entry != null && !entry.block()) {
                    abilities.run(player, entry, "held", null);
                }
            }
        }, 20L, 20L);
    }

    /** Legt content/ und animationen.yml beim ersten Start an. */
    private void saveExtras() {
        try {
            Files.createDirectories(contentDir());
        } catch (IOException ex) {
            getLogger().warning("content/ nicht anlegbar: " + ex.getMessage());
            return;
        }
        for (String name : CONTENT_FILES) {
            copyIfAbsent("content/" + name, contentDir().resolve(name));
        }
        copyIfAbsent("animationen.yml", getDataFolder().toPath().resolve("animationen.yml"));
        copyIfAbsent("FAEHIGKEITEN.txt", getDataFolder().toPath().resolve("FAEHIGKEITEN.txt"));
    }

    private void copyIfAbsent(String resource, Path target) {
        if (Files.exists(target)) {
            return;
        }
        try (InputStream in = getResource(resource)) {
            if (in == null) {
                return;
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target);
            getLogger().info("Angelegt: " + getDataFolder().toPath().relativize(target));
        } catch (IOException ex) {
            getLogger().warning(resource + " nicht anlegbar: " + ex.getMessage());
        }
    }

    /** plugins/SMPContent/content/ - hier liegen die Zusatzdateien. */
    public Path contentDir() {
        return getDataFolder().toPath().resolve("content");
    }

    public Abilities abilities() {
        return abilities;
    }

    public ContentGui gui() {
        return gui;
    }

    // ------------------------------------------------------------------
    //  Konfiguration
    //
    //  Bukkit wirft bei einem Tippfehler in der config.yml einfach alles
    //  weg und arbeitet mit einer leeren Datei weiter - dann sind plötzlich
    //  alle eigenen Blöcke und Items verschwunden. Hier bleibt stattdessen
    //  der letzte funktionierende Stand aktiv, und in der Konsole steht,
    //  in welcher Zeile der Fehler steckt.
    // ------------------------------------------------------------------

    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    @Override
    public void reloadConfig() {
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            saveDefaultConfig();
        }

        ConfigProblem.Result result = ConfigProblem.load(file);
        if (result.ok()) {
            YamlConfiguration fresh = result.config();
            fresh.setDefaults(bundledConfig());
            config = fresh;
            configProblem = null;
            return;
        }

        configProblem = result.problem();
        ConfigProblem.log(getLogger(), configProblem);
        if (config == null) {
            // Erster Start und die Datei ist schon kaputt: wenigstens mit den
            // mitgelieferten Standardinhalten weiterarbeiten.
            config = bundledConfig();
            getLogger().warning("Es werden vorerst die mitgelieferten Standardinhalte benutzt.");
        }
    }

    private YamlConfiguration bundledConfig() {
        try (InputStream in = getResource("config.yml")) {
            if (in == null) {
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return new YamlConfiguration();
        }
    }

    /** Null, solange die config.yml in Ordnung ist. */
    public ConfigProblem.Report configProblem() {
        return configProblem;
    }

    public Msgs msgs() {
        return msgs;
    }

    public ContentRegistry registry() {
        return registry;
    }

    public PackGenerator pack() {
        return pack;
    }
}
