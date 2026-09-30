package de.lemonpvp.duelplus.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.ByteBuffer;

public final class ProxySperre {

    public static final String KANAL = "bettersmp:combat";

    private static final byte ART_DUELL = 1;
    private static final byte ART_DUELL_ENDE = 3;

    private final JavaPlugin plugin;

    public ProxySperre(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, KANAL);
    }

    public void duellSperre(Player spieler, long restMillis) {
        senden(spieler, restMillis, ART_DUELL);
    }

    public void nachDuellSperre(Player spieler, long restMillis) {
        senden(spieler, restMillis, ART_DUELL_ENDE);
    }

    private void senden(Player spieler, long restMillis, byte art) {
        if (!plugin.isEnabled() || !spieler.isOnline()) {
            return;
        }
        byte[] daten = ByteBuffer.allocate(Long.BYTES + 1).putLong(restMillis).put(art).array();
        spieler.sendPluginMessage(plugin, KANAL, daten);
    }
}
