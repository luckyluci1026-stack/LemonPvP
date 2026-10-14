package dev.lemonpvp.runtime.paper;

import io.papermc.paper.plugin.configuration.PluginMeta;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.Plugin;

/**
 * Removes a disabled plugin from Paper's plugin manager. Paper has no public API for this
 * (it never unloads plugins); the fields used here exist unchanged in all Paper versions
 * Lemon supports (1.21.11 through 26.3). Access is checked once: if anything does not
 * match, live plugin updates report "restart required" instead of guessing.
 */
final class PaperPluginInternals {

    private static final PaperPluginInternals INSTANCE = new PaperPluginInternals();

    private final Object instanceManager;
    private final List<Plugin> plugins;
    private final Map<String, Plugin> lookupNames;
    private final Object dependencyTree;
    private final Method treeRemove;
    private final String problem;

    @SuppressWarnings("unchecked")
    private PaperPluginInternals() {
        Object im = null;
        List<Plugin> list = null;
        Map<String, Plugin> names = null;
        Object tree = null;
        Method remove = null;
        String why = null;
        try {
            Class<?> impl = Class.forName("io.papermc.paper.plugin.manager.PaperPluginManagerImpl");
            Object manager = impl.getMethod("getInstance").invoke(null);
            Field imField = impl.getDeclaredField("instanceManager");
            imField.setAccessible(true);
            im = imField.get(manager);
            Class<?> imClass = im.getClass();
            Field pluginsField = imClass.getDeclaredField("plugins");
            Field namesField = imClass.getDeclaredField("lookupNames");
            Field treeField = imClass.getDeclaredField("dependencyTree");
            for (Field f : new Field[] {pluginsField, namesField, treeField}) {
                f.setAccessible(true);
            }
            if (!List.class.isAssignableFrom(pluginsField.getType()) || !Map.class.isAssignableFrom(namesField.getType())) {
                throw new IllegalStateException("unexpected field types in " + imClass.getName());
            }
            list = (List<Plugin>) pluginsField.get(im);
            names = (Map<String, Plugin>) namesField.get(im);
            tree = treeField.get(im);
            remove = tree.getClass().getMethod("remove", PluginMeta.class);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            why = "Paper's plugin manager internals are not as expected (" + e + ")";
            im = null;
        }
        this.instanceManager = im;
        this.plugins = list;
        this.lookupNames = names;
        this.dependencyTree = tree;
        this.treeRemove = remove;
        this.problem = why;
    }

    static PaperPluginInternals get() {
        return INSTANCE;
    }

    /** @return why plugins cannot be removed, or {@code null} */
    String problem() {
        return this.problem;
    }

    /**
     * Removes a (disabled) plugin from the plugin list, the name lookup and the dependency
     * tree, so that a new version with the same name can be loaded.
     *
     * @param plugin disabled plugin
     */
    void remove(Plugin plugin) {
        if (this.problem != null) {
            throw new IllegalStateException(this.problem);
        }
        synchronized (this.instanceManager) {
            this.plugins.remove(plugin);
            this.lookupNames.values().removeIf(p -> p == plugin);
            try {
                this.treeRemove.invoke(this.dependencyTree, plugin.getPluginMeta());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("could not remove " + plugin.getName() + " from the dependency tree: " + e, e);
            }
        }
    }
}
