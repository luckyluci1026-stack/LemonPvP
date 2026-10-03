package de.lemonpvp.antiswear.netz;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.lemonpvp.antiswear.AntiSwear;
import de.lemonpvp.antiswear.filter.ChatPruefung;
import de.lemonpvp.antiswear.voice.VoiceDaten;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class NetzwerkBruecke implements PluginMessageListener, Listener {

    public static final String MSG = "antiswear:msg";
    public static final String HALLO = "antiswear:hallo";
    public static final String MUTE = "antiswear:mute";
    public static final String PROXY_MUTE = "bettersmp:mute";
    public static final String VOICE = "antiswear:voice";

    public static final byte OK = 0;
    public static final byte GEAENDERT = 1;
    public static final byte BLOCKIERT = 2;

    private static final int MAX_TEXT = 512;

    private final AntiSwear plugin;

    public NetzwerkBruecke(AntiSwear plugin) {
        this.plugin = plugin;
    }

    public void start() {
        var messenger = Bukkit.getMessenger();
        messenger.registerOutgoingPluginChannel(plugin, MSG);
        messenger.registerOutgoingPluginChannel(plugin, HALLO);
        messenger.registerOutgoingPluginChannel(plugin, PROXY_MUTE);
        messenger.registerOutgoingPluginChannel(plugin, VOICE);
        messenger.registerIncomingPluginChannel(plugin, MSG, this);
        messenger.registerIncomingPluginChannel(plugin, MUTE, this);
        messenger.registerIncomingPluginChannel(plugin, VOICE, this);
    }

    public void stop() {
        var messenger = Bukkit.getMessenger();
        messenger.unregisterOutgoingPluginChannel(plugin);
        messenger.unregisterIncomingPluginChannel(plugin);
    }

    @EventHandler
    public void beimJoin(PlayerJoinEvent event) {
        Player spieler = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (bereit(spieler, HALLO)) {
                spieler.sendPluginMessage(plugin, HALLO, new byte[0]);
            }
        }, 20L);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String kanal, @NotNull Player spieler, byte @NotNull [] daten) {
        try {
            ByteArrayDataInput ein = ByteStreams.newDataInput(daten);
            if (MUTE.equals(kanal)) {
                plugin.strikes().stummVomNetzwerk(spieler.getUniqueId(), ein.readLong());
            } else if (MSG.equals(kanal)) {
                privatnachrichtPruefen(spieler, ein.readLong(), ein.readUTF());
            } else if (VOICE.equals(kanal)) {
                byte art = ein.readByte();
                UUID betroffen = new UUID(ein.readLong(), ein.readLong());
                VoiceDaten.Eintrag eintrag = new VoiceDaten.Eintrag(ein.readLong(), ein.readLong(),
                        ein.readUTF(), ein.readUTF(), ein.readUTF());
                if (VoiceDaten.bekannteArt(art)) {
                    plugin.voice().vomNetzwerk(art, betroffen, eintrag);
                }
            }
        } catch (RuntimeException fehler) {
            plugin.getLogger().warning("Unlesbare Nachricht auf " + kanal + ": " + fehler.getMessage());
        }
    }

    private void privatnachrichtPruefen(Player spieler, long id, String text) {
        String gekuerzt = text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
        if (plugin.strikes().istStummgeschaltet(spieler.getUniqueId())) {
            plugin.stummHinweis(spieler);
            antworten(spieler, id, BLOCKIERT, gekuerzt);
            return;
        }
        if (!plugin.getConfig().getBoolean("pruefen.privatnachrichten", true) || spieler.hasPermission("antiswear.bypass")) {
            antworten(spieler, id, OK, gekuerzt);
            return;
        }
        ChatPruefung.Ergebnis ergebnis = plugin.pruefung().pruefen(spieler.getUniqueId(), gekuerzt, true,
                System.currentTimeMillis());
        byte status = switch (ergebnis.aktion()) {
            case DURCHLASSEN -> OK;
            case GEAENDERT -> GEAENDERT;
            case BLOCKIERT -> BLOCKIERT;
        };
        antworten(spieler, id, status, ergebnis.aktion() == ChatPruefung.Aktion.BLOCKIERT ? gekuerzt : ergebnis.text());
        if (ergebnis.verstoss() || ergebnis.hinweis() != null) {
            plugin.moderator().verarbeiten(spieler, ergebnis, plugin.msgs().raw("ort.privat"), gekuerzt);
        }
    }

    private void antworten(Player spieler, long id, byte status, String text) {
        ByteArrayDataOutput aus = ByteStreams.newDataOutput();
        aus.writeLong(id);
        aus.writeByte(status);
        aus.writeUTF(text);
        if (bereit(spieler, MSG)) {
            spieler.sendPluginMessage(plugin, MSG, aus.toByteArray());
        }
    }

    public void stummMelden(UUID spieler, long restMillis) {
        Player traeger = traeger(spieler, PROXY_MUTE);
        if (traeger == null) {
            return;
        }
        ByteArrayDataOutput aus = ByteStreams.newDataOutput();
        aus.writeLong(spieler.getMostSignificantBits());
        aus.writeLong(spieler.getLeastSignificantBits());
        aus.writeLong(restMillis);
        traeger.sendPluginMessage(plugin, PROXY_MUTE, aus.toByteArray());
    }

    public void voiceMelden(byte art, UUID spieler, VoiceDaten.Eintrag eintrag) {
        Player traeger = traeger(spieler, VOICE);
        if (traeger == null) {
            return;
        }
        ByteArrayDataOutput aus = ByteStreams.newDataOutput();
        aus.writeByte(art);
        aus.writeLong(spieler.getMostSignificantBits());
        aus.writeLong(spieler.getLeastSignificantBits());
        aus.writeLong(eintrag.wert());
        aus.writeLong(eintrag.zeit());
        aus.writeUTF(eintrag.name());
        aus.writeUTF(eintrag.von());
        aus.writeUTF(eintrag.grund());
        traeger.sendPluginMessage(plugin, VOICE, aus.toByteArray());
    }

    private Player traeger(UUID bevorzugt, String kanal) {
        Player traeger = Bukkit.getPlayer(bevorzugt);
        if (bereit(traeger, kanal)) {
            return traeger;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (bereit(online, kanal)) {
                return online;
            }
        }
        return null;
    }

    private boolean bereit(Player spieler, String kanal) {
        return spieler != null && spieler.isOnline() && plugin.isEnabled()
                && spieler.getListeningPluginChannels().contains(kanal);
    }
}
