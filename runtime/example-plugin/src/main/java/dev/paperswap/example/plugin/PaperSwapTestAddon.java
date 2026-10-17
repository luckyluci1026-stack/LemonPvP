package dev.paperswap.example.plugin;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Second test plugin, packaged by the CI from the same jar with its own {@code plugin.yml}
 * ({@code depend: [PaperSwapTestPlugin]}). While it is enabled, PaperSwapTestPlugin has a dependent
 * and must not be swapped live.
 */
public final class PaperSwapTestAddon extends JavaPlugin {

    @Override
    public void onEnable() {
        Plugin base = getServer().getPluginManager().getPlugin("PaperSwapTestPlugin");
        getLogger().info(getName() + " " + getPluginMeta().getVersion() + " enabled (uses PaperSwapTestPlugin "
                + (base == null ? "-" : base.getPluginMeta().getVersion()) + ")");
    }

    @Override
    public void onDisable() {
        getLogger().info(getName() + " " + getPluginMeta().getVersion() + " disabled");
    }
}
