package dev.lemonpvp.runtime.paper;

import dev.lemonpvp.runtime.RuntimeVersion;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginBase;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;

/**
 * Owner of all Bukkit registrations (listeners, scheduler tasks) made on behalf of
 * runtime modules.
 *
 * <p>Bukkit requires a {@link org.bukkit.plugin.Plugin} as owner for listeners and tasks.
 * This internal owner is <em>not</em> registered in the plugin manager (it does not
 * appear in {@code /plugins} and is not touched by plugin enable/disable), which mirrors
 * how Paper itself runs internal components. It is enabled exactly while the runtime is
 * running, so Paper's event bus skips module listeners and the scheduler rejects new
 * module tasks as soon as the runtime stops. Per-module cleanup is done by the runtime;
 * owning everything through one host additionally allows a final
 * {@code HandlerList.unregisterAll(host)} / {@code cancelTasks(host)} safety net.</p>
 */
final class RuntimeHostPlugin extends PluginBase {

    /** Name shown in Bukkit messages about module listeners and tasks. */
    static final String NAME = "LemonRuntime";

    private final Server server;
    private final Logger logger;
    private final PluginDescriptionFile description;
    private final FileConfiguration config = new YamlConfiguration();
    private volatile boolean enabled;
    private boolean naggable = true;

    RuntimeHostPlugin(Server server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.description = new PluginDescriptionFile(NAME, RuntimeVersion.VERSION, RuntimeHostPlugin.class.getName());
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public File getDataFolder() {
        return new File("lemon");
    }

    @Override
    public PluginDescriptionFile getDescription() {
        return this.description;
    }

    @Override
    public PluginMeta getPluginMeta() {
        return this.description;
    }

    @Override
    public FileConfiguration getConfig() {
        return this.config;
    }

    @Override
    public InputStream getResource(String filename) {
        return null;
    }

    @Override
    public void saveConfig() {
    }

    @Override
    public void saveDefaultConfig() {
    }

    @Override
    public void saveResource(String resourcePath, boolean replace) {
    }

    @Override
    public void reloadConfig() {
    }

    @Override
    public PluginLoader getPluginLoader() {
        throw new UnsupportedOperationException(NAME + " is an internal component and has no plugin loader");
    }

    @Override
    public Server getServer() {
        return this.server;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public boolean isNaggable() {
        return this.naggable;
    }

    @Override
    public void setNaggable(boolean canNag) {
        this.naggable = canNag;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return null;
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(String worldName, String id) {
        return null;
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public LifecycleEventManager<org.bukkit.plugin.Plugin> getLifecycleManager() {
        return handlerConfiguration -> {
            throw new UnsupportedOperationException(NAME + " does not take part in Paper lifecycle events");
        };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
