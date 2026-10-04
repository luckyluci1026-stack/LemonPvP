package de.lemonpvp.smpproxy.netzwerk;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.lemonpvp.smpproxy.SMPProxy;
import de.lemonpvp.smpproxy.ban.Durations;
import de.lemonpvp.smpproxy.command.RtpCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ChatRelay {

    public static final MinecraftChannelIdentifier CHAT = MinecraftChannelIdentifier.create("bettersmp", "chat");
    public static final MinecraftChannelIdentifier STUMM = MinecraftChannelIdentifier.create("bettersmp", "mute");

    private static final byte ART_WEITERGEBEN = 0;
    private static final byte ART_ZUSTELLEN = 1;
    private static final int MAX_EMPFAENGER = 200;

    private final SMPProxy plugin;

    public ChatRelay(SMPProxy plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void beimPluginKanal(PluginMessageEvent event) {
        if (event.getIdentifier().equals(RtpCommand.RTP_CHANNEL)) {
            if (event.getSource() instanceof Player) {
                event.setResult(PluginMessageEvent.ForwardResult.handled());
            }
            return;
        }
        boolean chat = event.getIdentifier().equals(CHAT);
        boolean stumm = event.getIdentifier().equals(STUMM);
        if (!chat && !stumm) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection verbindung)) {
            return;
        }
        try (DataInputStream ein = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            if (stumm) {
                UUID spieler = new UUID(ein.readLong(), ein.readLong());
                long rest = ein.readLong();
                plugin.stummListe().setzen(spieler, rest);
                plugin.moderation().stummPush(spieler, rest);
            } else {
                chatVerarbeiten(verbindung, ein);
            }
        } catch (IOException | RuntimeException fehler) {
            plugin.log().warn("Unlesbare Nachricht auf {} von {}: {}", event.getIdentifier().getId(),
                    verbindung.getServerInfo().getName(), fehler.getMessage());
        }
    }

    private void chatVerarbeiten(ServerConnection verbindung, DataInputStream ein) throws IOException {
        byte art = ein.readByte();
        Component nachricht = GsonComponentSerializer.gson().deserialize(ein.readUTF());
        int anzahl = ein.readInt();
        Set<UUID> empfaenger = null;
        if (anzahl >= 0) {
            empfaenger = new HashSet<>();
            for (int i = 0; i < Math.min(anzahl, MAX_EMPFAENGER); i++) {
                empfaenger.add(new UUID(ein.readLong(), ein.readLong()));
            }
        }
        Player sender = verbindung.getPlayer();
        String herkunft = verbindung.getServerInfo().getName();
        if (art == ART_ZUSTELLEN) {
            long stummRest = plugin.stummListe().restMillis(sender.getUniqueId());
            if (stummRest != 0) {
                sender.sendMessage(stummHinweis(stummRest));
                return;
            }
            plugin.log().info("[Chat {}] {}", herkunft, PlainTextComponentSerializer.plainText().serialize(nachricht));
        }
        if (empfaenger != null) {
            for (UUID id : empfaenger) {
                plugin.proxy().getPlayer(id).ifPresent(spieler -> spieler.sendMessage(nachricht));
            }
            return;
        }
        boolean teilen = plugin.config().chatSyncEnabled() && plugin.config().chatServers().contains(herkunft);
        Component mitHerkunft = MiniMessage.miniMessage()
                .deserialize(plugin.config().chatServerTag().replace("%server%", herkunft))
                .append(nachricht);
        for (Player spieler : plugin.proxy().getAllPlayers()) {
            String server = serverVon(spieler);
            if (server.equals(herkunft)) {
                if (art == ART_ZUSTELLEN) {
                    spieler.sendMessage(nachricht);
                }
            } else if (teilen && plugin.config().chatServers().contains(server)) {
                spieler.sendMessage(mitHerkunft);
            }
        }
    }

    public Component stummHinweis(long restMillis) {
        String dauer = restMillis < 0 ? plugin.config().banPermanentWord() : Durations.humanize(restMillis);
        return Texte.mitSpielertext(plugin.config().mutedMessage(), plugin.config().prefix(), "%dauer%", dauer);
    }

    public static String serverVon(Player spieler) {
        return spieler.getCurrentServer().map(verbindung -> verbindung.getServerInfo().getName()).orElse("");
    }
}
