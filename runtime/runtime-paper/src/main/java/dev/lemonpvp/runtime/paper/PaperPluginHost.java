package dev.lemonpvp.runtime.paper;

import dev.lemonpvp.runtime.plugins.InstalledPlugin;
import dev.lemonpvp.runtime.plugins.PluginHost;
import dev.lemonpvp.runtime.plugins.PluginInfo;
import io.papermc.paper.plugin.configuration.PluginMeta;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoadOrder;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * {@link PluginHost} on Paper. Paper's {@code disablePlugin} already calls
 * {@code onDisable}, cancels the plugin's tasks (Bukkit, async and global region
 * schedulers), unregisters listeners, services, lifecycle handlers and plugin channels and
 * closes the plugin class loader. {@link #unload} additionally removes what Paper keeps: the
 * plugin's commands, the permissions from its descriptor and its plugin-manager entry.
 */
final class PaperPluginHost implements PluginHost {

    private final Server server;

    PaperPluginHost(Server server) {
        this.server = server;
    }

    @Override
    public List<InstalledPlugin> installed() {
        List<InstalledPlugin> out = new ArrayList<>();
        for (Plugin p : this.server.getPluginManager().getPlugins()) {
            out.add(new InstalledPlugin(info(p), p.isEnabled()));
        }
        return out;
    }

    private static PluginInfo info(Plugin p) {
        PluginMeta m = p.getPluginMeta();
        boolean bukkit = m instanceof PluginDescriptionFile;
        boolean libraries = bukkit && !((PluginDescriptionFile) m).getLibraries().isEmpty();
        return new PluginInfo(m.getName(), m.getVersion(), bukkit ? PluginInfo.Kind.BUKKIT : PluginInfo.Kind.PAPER, m.getMainClass(),
                m.getAPIVersion() == null ? "" : m.getAPIVersion(), m.getPluginDependencies(), m.getPluginSoftDependencies(),
                m.getLoadBeforePlugins(), m.getProvidedPlugins(), m.getLoadOrder() == PluginLoadOrder.STARTUP, libraries, !bukkit);
    }

    @Override
    public Path pluginDirectory() {
        return this.server.getPluginsFolder().toPath().toAbsolutePath().normalize();
    }

    @Override
    public Path updateDirectory() {
        return this.server.getUpdateFolderFile().toPath().toAbsolutePath().normalize();
    }

    @Override
    public String unloadProblem() {
        return PaperPluginInternals.get().problem();
    }

    @Override
    public void unload(String name) {
        PluginManager pm = this.server.getPluginManager();
        Plugin plugin = pm.getPlugin(name);
        if (plugin == null) {
            throw new IllegalStateException("plugin " + name + " is not installed");
        }
        if (!(plugin instanceof JavaPlugin)) {
            throw new IllegalStateException("plugin " + name + " is not a Java plugin");
        }
        String problem = PaperPluginInternals.get().problem();
        if (problem != null) {
            throw new IllegalStateException(problem);
        }
        List<Permission> permissions = plugin.getPluginMeta().getPermissions();
        pm.disablePlugin(plugin);
        removeCommands(plugin);
        for (Permission perm : permissions) {
            pm.removePermission(perm.getName());
        }
        PaperPluginInternals.get().remove(plugin);
    }

    /** Removes every command the plugin owns, including its namespaced labels. */
    private void removeCommands(Plugin plugin) {
        CommandMap map = this.server.getCommandMap();
        Map<String, Command> known = map.getKnownCommands();
        String prefix = plugin.getName().toLowerCase(Locale.ROOT) + ":";
        List<Command> owned = new ArrayList<>();
        for (Map.Entry<String, Command> e : known.entrySet()) {
            if (e.getValue() instanceof PluginIdentifiableCommand pic && pic.getPlugin() == plugin) {
                owned.add(e.getValue());
            }
        }
        // Collect first, then remove through the map: Paper's command map is a view over the
        // Brigadier dispatcher, so its entry set is not modified directly.
        List<String> labels = known.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix) || owned.contains(e.getValue()))
                .map(Map.Entry::getKey).toList();
        labels.forEach(known::remove);
        for (Command c : owned) {
            c.unregister(map);
        }
    }

    @Override
    public LoadResult load(Path jar) {
        PluginManager pm = this.server.getPluginManager();
        Plugin plugin;
        try {
            plugin = pm.loadPlugin(jar.toFile());
        } catch (Exception | LinkageError e) {
            return new LoadResult(null, false, "loading failed: " + e);
        }
        if (plugin == null) {
            return new LoadResult(null, false, "Paper did not recognise " + jar.getFileName() + " as a plugin");
        }
        try {
            pm.enablePlugin(plugin);
        } catch (RuntimeException | LinkageError e) {
            return new LoadResult(plugin.getName(), plugin.isEnabled(), "enabling failed: " + e);
        }
        return new LoadResult(plugin.getName(), plugin.isEnabled(), plugin.isEnabled() ? null
                : "the plugin is disabled after onEnable (see its error in the server log)");
    }

    @Override
    public void commandsChanged() {
        for (Player player : this.server.getOnlinePlayers()) {
            player.updateCommands();
        }
    }
}
