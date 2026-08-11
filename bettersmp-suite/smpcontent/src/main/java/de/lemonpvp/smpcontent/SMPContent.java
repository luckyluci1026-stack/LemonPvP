package de.lemonpvp.smpcontent;

import de.lemonpvp.smpcontent.command.ContentCommand;
import de.lemonpvp.smpcontent.content.ContentRegistry;
import de.lemonpvp.smpcontent.listener.BlockListener;
import de.lemonpvp.smpcontent.listener.GuiListener;
import de.lemonpvp.smpcontent.pack.PackGenerator;
import de.lemonpvp.smpcontent.util.ConfigProblem;
import de.lemonpvp.smpcontent.util.Msgs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * SMPContent - eigene Blöcke und Items passend zum SMP-Texturepack.
 *
 * Blöcke werden über feste Note-Block-Zustände umgesetzt; das Plugin hält
 * diese Zustände stabil und sorgt für die richtigen Drops. Items bekommen
 * ein item_model (Java) und CustomModelData (Geyser/Bedrock).
 */
public final class SMPContent extends JavaPlugin {

    private Msgs msgs;
    private ContentRegistry registry;
    private PackGenerator pack;

    private YamlConfiguration config;
    private ConfigProblem.Report configProblem;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        this.msgs = new Msgs(this);
        this.registry = new ContentRegistry(this);
        this.pack = new PackGenerator(this);
        pack.ensureFolders();

        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        getCommand("smpcontent").setExecutor(new ContentCommand(this));

        getLogger().info("SMPContent aktiviert.");
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
