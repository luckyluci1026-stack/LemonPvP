package de.lemonpvp.easybedrock;

import de.lemonpvp.easybedrock.command.EasyBedrockCommand;
import de.lemonpvp.easybedrock.setup.BedrockInstaller;
import de.lemonpvp.easybedrock.setup.GeyserOptimizer;
import de.lemonpvp.easybedrock.util.Msgs;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * EasyBedrock - Bedrock-Crossplay in einem Schritt.
 *
 * Installiert Geyser + Floodgate (offizielle Downloads) und stellt per
 * Direktverbindung sicher, dass ein Bedrock-Spieler etwa so viele Ressourcen
 * braucht wie ein Java-Spieler.
 */
public final class EasyBedrock extends JavaPlugin {

    private Msgs msgs;
    private BedrockInstaller installer;
    private GeyserOptimizer optimizer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.msgs = new Msgs(this);
        this.installer = new BedrockInstaller(this);
        this.optimizer = new GeyserOptimizer(this);

        getCommand("easybedrock").setExecutor(new EasyBedrockCommand(this));

        // Optimierung zuerst: falls Geyser-Config schon existiert, VOR dem
        // Geyser-Start patchen (loadbefore in plugin.yml sorgt für die Reihenfolge).
        if (getConfig().getBoolean("optimize.enabled", true)) {
            int changes = optimizer.optimize();
            if (changes > 0) {
                getLogger().info("Geyser ressourcenschonend optimiert (" + changes + " Änderung(en)).");
            }
        }

        if (getConfig().getBoolean("installer.auto-install-on-start", true)) {
            installer.installAsync(Bukkit.getConsoleSender());
        }

        if (getConfig().getBoolean("log-status", true)) {
            boolean direct = optimizer.isDirectConnection();
            getLogger().info("Bedrock-Direktverbindung (Java-Last pro Bedrock-Spieler): "
                    + (direct ? "AKTIV" : "noch nicht - nach Geyser-Erststart /easybedrock optimize"));
        }

        getLogger().info("EasyBedrock aktiviert.");
    }

    public Msgs msgs() {
        return msgs;
    }

    public BedrockInstaller installer() {
        return installer;
    }

    public GeyserOptimizer optimizer() {
        return optimizer;
    }
}
