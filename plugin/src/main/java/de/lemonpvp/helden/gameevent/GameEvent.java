package de.lemonpvp.helden.gameevent;

import de.lemonpvp.helden.HeldenPlugin;
import org.bukkit.configuration.ConfigurationSection;

/** Ein Projekt-Event wie Blutmond oder Zitronenregen. */
public abstract class GameEvent {

    protected final HeldenPlugin plugin;
    private final String id;

    protected GameEvent(HeldenPlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Anzeigename aus messages.yml. */
    public String displayName() {
        return plugin.messages().raw("events." + id + "-name");
    }

    /** Beschreibung fuer die Startmeldung. */
    public String description() {
        return plugin.messages().raw("events." + id + "-description");
    }

    public abstract int durationSeconds();

    /** Eigener Abschnitt in der config.yml unter {@code events.<id>}. */
    protected ConfigurationSection section() {
        return plugin.settings().eventSection(id);
    }

    protected int option(String key, int fallback) {
        ConfigurationSection section = section();
        return section == null ? fallback : section.getInt(key, fallback);
    }

    protected boolean option(String key, boolean fallback) {
        ConfigurationSection section = section();
        return section == null ? fallback : section.getBoolean(key, fallback);
    }

    /**
     * Wird beim Start aufgerufen.
     *
     * @return {@code false}, wenn das Event nicht starten kann (dann wird es
     *         uebersprungen)
     */
    public boolean onStart() {
        return true;
    }

    /** Jede Sekunde waehrend der Laufzeit. */
    public void onTick(int secondsElapsed) {
        // Standardmaessig passiert nichts.
    }

    public void onStop() {
        // Standardmaessig passiert nichts.
    }
}
