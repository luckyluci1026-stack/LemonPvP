package de.lemonpvp.smplobby.util;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Lädt messages.yml (mit Jar-Fallback) und rendert MiniMessage-Nachrichten. */
public final class Msgs {

    private final JavaPlugin plugin;
    private YamlConfiguration messages;
    private YamlConfiguration defaults;
    private ConfigProblem.Report problem;

    public Msgs(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        var res = plugin.getResource("messages.yml");
        this.defaults = res == null
                ? new YamlConfiguration()
                : YamlConfiguration.loadConfiguration(new InputStreamReader(res, StandardCharsets.UTF_8));

        // Bei einem Tippfehler bleiben die bisherigen Texte stehen, statt dass
        // plötzlich überall nur noch die Schlüsselnamen im Chat auftauchen.
        ConfigProblem.Result result = ConfigProblem.load(file);
        if (result.ok()) {
            this.messages = result.config();
            this.problem = null;
            return;
        }
        this.problem = result.problem();
        ConfigProblem.log(plugin.getLogger(), problem);
        if (this.messages == null) {
            this.messages = new YamlConfiguration();
        }
    }

    /** Null, solange die messages.yml in Ordnung ist. */
    public ConfigProblem.Report problem() {
        return problem;
    }

    public String raw(String path) {
        String value = messages.getString(path, defaults.getString(path));
        return value == null ? path : value;
    }

    /**
     * Eine Liste von Zeilen - z.B. die Regeln.
     *
     * Faellt auf die Fassung aus der Jar zurueck, damit ein Tippfehler in
     * der messages.yml nicht dazu fuehrt, dass gar nichts mehr kommt.
     */
    public java.util.List<String> liste(String path) {
        java.util.List<String> aus = messages.getStringList(path);
        return aus.isEmpty() ? defaults.getStringList(path) : aus;
    }

    public Component format(String path, String... replacements) {
        String text = raw(path);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        text = text.replace("%prefix%", raw("prefix"));
        return Text.mm(text);
    }

    public void send(CommandSender to, String path, String... replacements) {
        to.sendMessage(format(path, replacements));
    }

    public void broadcast(String path, String... replacements) {
        org.bukkit.Bukkit.getServer().sendMessage(format(path, replacements));
    }
}
