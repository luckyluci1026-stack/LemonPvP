package dev.paperswap.runtime.paper;

import dev.paperswap.runtime.PaperSwapRuntime;
import dev.paperswap.runtime.platform.ModuleHandle;
import dev.paperswap.runtime.platform.PlatformBinding;
import dev.paperswap.runtime.platform.RuntimePlatform;
import io.papermc.paper.ServerBuildInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;

/** {@link RuntimePlatform} backed by the public Paper API. */
final class PaperPlatform implements RuntimePlatform {

    private final Server server;
    private final RuntimeHostPlugin host;
    private final Logger logger;

    PaperPlatform(Server server, RuntimeHostPlugin host, Logger logger) {
        this.server = server;
        this.host = host;
        this.logger = logger;
    }

    @Override
    public String minecraftVersion() {
        return this.server.getMinecraftVersion();
    }

    @Override
    public String serverBrand() {
        return ServerBuildInfo.buildInfo().brandName();
    }

    @Override
    public String serverVersion() {
        return ServerBuildInfo.buildInfo().asString(ServerBuildInfo.StringRepresentation.VERSION_SIMPLE);
    }

    @Override
    public Executor mainThreadExecutor() {
        return task -> {
            if (!this.host.isEnabled()) {
                throw new RejectedExecutionException("PaperSwap runtime is not running");
            }
            try {
                this.server.getScheduler().runTask(this.host, task);
            } catch (IllegalPluginAccessException e) {
                throw new RejectedExecutionException(e.getMessage(), e);
            }
        };
    }

    @Override
    public boolean isMainThread() {
        return this.server.isPrimaryThread();
    }

    @Override
    public Logger logger() {
        return this.logger;
    }

    @Override
    public ClassLoader serverClassLoader() {
        return PaperSwapRuntime.class.getClassLoader();
    }

    @Override
    public List<ClassLoader> pluginClassLoaders(List<String> pluginNames) {
        List<ClassLoader> loaders = new ArrayList<>(pluginNames.size());
        for (String name : pluginNames) {
            Plugin plugin = this.server.getPluginManager().getPlugin(name);
            if (plugin == null || !plugin.isEnabled()) {
                throw new IllegalStateException("plugin '" + name + "' is not installed or not enabled");
            }
            loaders.add(plugin.getClass().getClassLoader());
        }
        return loaders;
    }

    @Override
    public PlatformBinding bind(ModuleHandle handle) {
        return new PaperBinding(this.server, this.host, handle, this.logger);
    }

    @Override
    public java.util.Optional<dev.paperswap.runtime.plugins.PluginHost> pluginHost() {
        return java.util.Optional.of(new PaperPluginHost(this.server));
    }

    @Override
    public String serverBuildId() {
        return dev.paperswap.runtime.livepatch.BuildIdentity.ofCodeSource(this.server.getClass());
    }

    @Override
    public void commandsChanged() {
        for (Player player : this.server.getOnlinePlayers()) {
            player.updateCommands();
        }
    }
}
