package com.lemonpvp.lemonbuild;

import com.lemonpvp.lemonbuild.commands.AowBuildLobbyCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LemonBuild extends JavaPlugin {

    @Override
    public void onEnable() {
        var cmd = getCommand("aowbuildlobby");
        if (cmd != null) {
            var handler = new AowBuildLobbyCommand(this);
            cmd.setExecutor(handler);
        }
        getLogger().info("LemonBuild aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("LemonBuild deaktiviert.");
    }
}
