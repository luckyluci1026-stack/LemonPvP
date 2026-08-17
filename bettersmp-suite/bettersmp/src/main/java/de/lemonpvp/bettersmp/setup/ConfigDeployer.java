package de.lemonpvp.bettersmp.setup;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Legt fertige, hübsche Configs für EssentialsX und TAB an - aber nur, wenn
 * dort noch keine eigene Config existiert (nichts wird überschrieben).
 * Außerdem: server.properties-Patch für NoChatReports.
 */
public final class ConfigDeployer {

    private final BetterSMP plugin;

    public ConfigDeployer(BetterSMP plugin) {
        this.plugin = plugin;
    }

    /**
     * plugins/ - immer als absoluter Pfad.
     *
     * getDataFolder() liefert bei Paper einen relativen Pfad
     * ("plugins/BetterSMP"). Auf dem Elternteil davon ist getParentFile()
     * dann {@code null}, und das war der Absturz beim ersten Start.
     */
    private File pluginsDir() {
        return plugin.getDataFolder().getAbsoluteFile().getParentFile();
    }

    /** Der Ordner, in dem der Server läuft - dort liegt die server.properties. */
    private File serverDir() {
        File oben = pluginsDir().getParentFile();
        return oben != null ? oben : new File(".").getAbsoluteFile();
    }

    /** Kopiert eine Jar-Ressource in eine Zieldatei, falls diese noch nicht existiert. */
    private boolean deploy(String resource, File target) {
        if (target.exists()) {
            return false;
        }
        try (InputStream in = plugin.getResource(resource)) {
            if (in == null) {
                return false;
            }
            target.getParentFile().mkdirs();
            Files.copy(in, target.toPath());
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte " + resource + " nicht anlegen: " + e.getMessage());
            return false;
        }
    }

    /** Legt EssentialsX- und TAB-Configs an; gibt eine Liste der Ziele zurück. */
    public java.util.List<String> deployAll(CommandSender feedback) {
        java.util.List<String> done = new java.util.ArrayList<>();

        File essDir = new File(pluginsDir(), "Essentials");
        if (deploy("deploy/essentials-config.yml", new File(essDir, "config.yml"))) {
            done.add("EssentialsX/config.yml");
        }
        if (deploy("deploy/essentials-messages_de.properties",
                new File(essDir, "messages_de.properties"))) {
            done.add("EssentialsX/messages_de.properties");
        }

        File tabDir = new File(pluginsDir(), "TAB");
        if (deploy("deploy/tab-config.yml", new File(tabDir, "config.yml"))) {
            done.add("TAB/config.yml");
        }
        if (deploy("deploy/tab-groups.yml", new File(tabDir, "groups.yml"))) {
            done.add("TAB/groups.yml");
        }

        if (!done.isEmpty() && feedback != null) {
            plugin.msgs().send(feedback, "installer.configs-deployed", "target", String.join(", ", done));
        }
        return done;
    }

    /**
     * Setzt enforce-secure-profile=false in server.properties (NoChatReports).
     * Wirkt nach dem nächsten Neustart.
     */
    public boolean patchServerProperties(CommandSender feedback) {
        Path props = serverDir().toPath().resolve("server.properties");
        if (!Files.exists(props)) {
            return false;
        }
        try {
            String content = Files.readString(props, StandardCharsets.UTF_8);
            if (content.contains("enforce-secure-profile=false")) {
                return false;
            }
            String patched;
            if (content.contains("enforce-secure-profile=")) {
                patched = content.replaceAll("enforce-secure-profile=\\w+",
                        "enforce-secure-profile=false");
            } else {
                patched = content + System.lineSeparator() + "enforce-secure-profile=false"
                        + System.lineSeparator();
            }
            Files.writeString(props, patched, StandardCharsets.UTF_8);
            if (feedback != null) {
                plugin.msgs().send(feedback, "installer.properties-patched");
            }
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("server.properties-Patch fehlgeschlagen: " + e.getMessage());
            return false;
        }
    }

    // Reserviert für zukünftige binäre Patches
    @SuppressWarnings("unused")
    private void touch(RandomAccessFile ignored) {
    }
}
