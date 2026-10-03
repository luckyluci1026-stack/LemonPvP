package de.lemonpvp.duelplus.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Schickt Spieler ueber den BungeeCord-Kanal an einen anderen Server -
 * Velocity versteht ihn ebenfalls (gleiches Vorbild wie SMPLobbys
 * Server-Waehler). Kein eigenes Velocity-Plugin noetig: Der Proxy
 * braucht dafuer nur den Zielserver in seiner velocity.toml zu kennen.
 */
public final class ProxyBridge {

    private final JavaPlugin plugin;

    public ProxyBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
    }

    public void sende(Player spieler, String server) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("Connect");
            out.writeUTF(server);
            spieler.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte " + spieler.getName() + " nicht zu " + server
                    + " schicken: " + e.getMessage());
        }
    }
}
