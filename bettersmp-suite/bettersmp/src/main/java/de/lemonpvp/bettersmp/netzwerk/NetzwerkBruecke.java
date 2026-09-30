package de.lemonpvp.bettersmp.netzwerk;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.combat.CombatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public final class NetzwerkBruecke {

    public static final String CHAT = "bettersmp:chat";
    public static final String STUMM = "bettersmp:mute";
    public static final String LAST = "smpproxy:last";

    private static final GsonComponentSerializer JSON = GsonComponentSerializer.builder()
            .options(JSONOptions.compatibility())
            .build();
    private static final int MAX_JSON_LAENGE = 30_000;
    private static final byte ART_FREEZE = 2;

    private final BetterSMP plugin;
    private BukkitTask lastTakt;

    public NetzwerkBruecke(BetterSMP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHAT);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, STUMM);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, LAST);
        lastTakt = Bukkit.getScheduler().runTaskTimer(plugin, this::lastMelden, 40L, 40L);
    }

    public void stop() {
        if (lastTakt != null) {
            lastTakt.cancel();
            lastTakt = null;
        }
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHAT);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, STUMM);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, LAST);
    }

    public void lastMelden() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (bereit(online, LAST)) {
                ByteArrayDataOutput aus = ByteStreams.newDataOutput();
                aus.writeDouble(Bukkit.getAverageTickTime());
                aus.writeDouble(Bukkit.getTPS()[0]);
                online.sendPluginMessage(plugin, LAST, aus.toByteArray());
                return;
            }
        }
    }

    public void chatWeitergeben(Player sender, Component nachricht) {
        if (!bereit(sender, CHAT)) {
            return;
        }
        String json = JSON.serialize(nachricht);
        if (json.length() > MAX_JSON_LAENGE) {
            return;
        }
        ByteArrayDataOutput aus = ByteStreams.newDataOutput();
        aus.writeByte(0);
        aus.writeUTF(json);
        aus.writeInt(-1);
        sender.sendPluginMessage(plugin, CHAT, aus.toByteArray());
    }

    public void freezeSperre(Player spieler, long restMillis) {
        if (!bereit(spieler, CombatManager.PROXY_KANAL)) {
            return;
        }
        ByteArrayDataOutput aus = ByteStreams.newDataOutput();
        aus.writeLong(restMillis);
        aus.writeByte(ART_FREEZE);
        spieler.sendPluginMessage(plugin, CombatManager.PROXY_KANAL, aus.toByteArray());
    }

    public void stummMelden(UUID spieler, long restMillis) {
        Player traeger = Bukkit.getPlayer(spieler);
        if (!bereit(traeger, STUMM)) {
            traeger = null;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (bereit(online, STUMM)) {
                    traeger = online;
                    break;
                }
            }
        }
        if (traeger == null) {
            return;
        }
        ByteArrayDataOutput aus = ByteStreams.newDataOutput();
        aus.writeLong(spieler.getMostSignificantBits());
        aus.writeLong(spieler.getLeastSignificantBits());
        aus.writeLong(restMillis);
        traeger.sendPluginMessage(plugin, STUMM, aus.toByteArray());
    }

    private boolean bereit(Player spieler, String kanal) {
        return spieler != null && spieler.isOnline() && plugin.isEnabled()
                && spieler.getListeningPluginChannels().contains(kanal);
    }
}
