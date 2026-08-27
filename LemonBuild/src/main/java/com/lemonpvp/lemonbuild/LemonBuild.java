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
            cmd.setTabCompleter((s, c, l, a) ->
                    a.length == 1 && "confirm".startsWith(a[0].toLowerCase())
                            ? java.util.List.of("confirm") : java.util.List.of());
        }
        getLogger().info("LemonBuild aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("LemonBuild deaktiviert.");
    }
}
