package de.lemonpvp.dbwipe;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Notfallwerkzeug: /dbwipe leert nach drei Warnungen und einem
 * Passwort ALLE Tabellen der konfigurierten Datenbank - vorher wird
 * ein vollstaendiges .tar.gz-Backup ALLER Tabellen erstellt (jedes
 * installierten Plugins, nicht nur eines einzelnen). Nur ueber die
 * Serverkonsole nutzbar, siehe WipeCommand.
 *
 * Eigenstaendig, ohne andere Plugins zu beruehren - verbindet sich nur
 * selbst mit derselben Datenbank wie der Rest der Suite.
 */
public final class DBWipe extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        var command = getCommand("dbwipe");
        if (command != null) {
            command.setExecutor(new WipeCommand(this));
        }
        getLogger().info("DBWipe aktiviert - nur ueber die Serverkonsole nutzbar (/dbwipe).");
    }
}
