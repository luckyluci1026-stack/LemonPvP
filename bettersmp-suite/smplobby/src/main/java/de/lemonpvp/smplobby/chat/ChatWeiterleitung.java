package de.lemonpvp.smplobby.chat;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.lemonpvp.smplobby.SMPLobby;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class ChatWeiterleitung implements Listener {

    public static final String KANAL = "bettersmp:chat";

    private static final GsonComponentSerializer JSON = GsonComponentSerializer.builder()
            .options(JSONOptions.compatibility())
            .build();
    private static final int MAX_JSON_LAENGE = 30_000;

    private final SMPLobby plugin;

    public ChatWeiterleitung(SMPLobby plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, KANAL);
    }

    public void stop() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, KANAL);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimChat(AsyncChatEvent event) {
        Player spieler = event.getPlayer();
        if (!plugin.getConfig().getBoolean("chat.netzwerk", true) || !plugin.isEnabled()
                || !spieler.getListeningPluginChannels().contains(KANAL)) {
            return;
        }
        Component fertig = event.renderer().render(spieler, spieler.displayName(), event.message(),
                Bukkit.getConsoleSender());
        String json = JSON.serialize(fertig);
        if (json.length() > MAX_JSON_LAENGE) {
            return;
        }
        event.setCancelled(true);
        ByteArrayDataOutput aus = ByteStreams.newDataOutput();
        aus.writeByte(1);
        aus.writeUTF(json);
        aus.writeInt(-1);
        spieler.sendPluginMessage(plugin, KANAL, aus.toByteArray());
    }
}
