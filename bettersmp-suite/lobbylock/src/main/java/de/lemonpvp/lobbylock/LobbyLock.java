package de.lemonpvp.lobbylock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Sperrt Item-Drop, Inventar-Verschieben, Offhand-Tausch sowie
 * Tueren/Falltueren/Schilder.
 *
 * Bewusst ein eigenes, kleines Plugin statt ein Teil von SMPLobby: laesst
 * sich dazu-installieren, ohne ein bestehendes Plugin ersetzen oder neu
 * bauen zu muessen.
 */
public final class LobbyLock extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new LockListener(this), this);
        var command = getCommand("lobbylock");
        if (command != null) {
            command.setExecutor(this);
        }
        getLogger().info("LobbyLock aktiviert.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("lobbylock.bypass")) {
            sender.sendMessage(mm(getConfig().getString("no-permission-message",
                    "<red>Dazu hast du keine Rechte.</red>")));
            return true;
        }
        reloadConfig();
        sender.sendMessage(mm(getConfig().getString("reload-message", "<green>Neu geladen.</green>")));
        return true;
    }

    static Component mm(String text) {
        return MiniMessage.miniMessage().deserialize(text == null ? "" : text);
    }
}
