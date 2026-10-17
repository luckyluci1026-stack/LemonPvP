package dev.paperswap.example.plugin;

import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A normal Bukkit plugin (no PaperSwap API) that registers one of each resource an update must
 * handle: a command, a listener and a repeating task. {@code /paperswaptest} reports what is
 * registered, so a test can see whether the old version left anything behind. A version
 * containing {@code broken} throws in {@code onEnable} to test failed updates.
 */
public final class PaperSwapTestPlugin extends JavaPlugin implements Listener {

    private int ticks;

    @Override
    public void onEnable() {
        String version = getPluginMeta().getVersion();
        if (version.contains("broken")) {
            throw new IllegalStateException(getName() + " " + version + ": simulated failure in onEnable");
        }
        getServer().getScheduler().runTaskTimer(this, () -> this.ticks++, 20L, 20L);
        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand command = getCommand("paperswaptest");
        if (command != null) {
            command.setExecutor((sender, cmd, label, args) -> {
                long tasks = getServer().getScheduler().getPendingTasks().stream()
                        .filter(t -> t.getOwner().getName().equals(getName())).count();
                sender.sendMessage(getName() + " " + version + " active: " + tasks + " scheduled task(s) owned by " + getName()
                        + ", " + HandlerList.getRegisteredListeners(this).size() + " listener(s), ticks " + this.ticks);
                return true;
            });
        }
        getLogger().info(getName() + " " + version + " enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info(getName() + " " + getPluginMeta().getVersion() + " disabled");
    }

    /** @param event join event */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(getName() + " " + getPluginMeta().getVersion() + " greets you");
    }
}
