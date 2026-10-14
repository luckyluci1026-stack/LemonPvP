package dev.lemonpvp.example.plugin;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Second test plugin, packaged by the CI from the same jar with its own {@code plugin.yml}
 * ({@code depend: [LemonTestPlugin]}). While it is enabled, LemonTestPlugin has a dependent
 * and must not be swapped live.
 */
public final class LemonTestAddon extends JavaPlugin {

    @Override
    public void onEnable() {
        Plugin base = getServer().getPluginManager().getPlugin("LemonTestPlugin");
        getLogger().info(getName() + " " + getPluginMeta().getVersion() + " enabled (uses LemonTestPlugin "
                + (base == null ? "-" : base.getPluginMeta().getVersion()) + ")");
    }

    @Override
    public void onDisable() {
        getLogger().info(getName() + " " + getPluginMeta().getVersion() + " disabled");
    }
}
