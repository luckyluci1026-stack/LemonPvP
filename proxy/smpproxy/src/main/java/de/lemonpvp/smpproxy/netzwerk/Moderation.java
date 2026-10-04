package de.lemonpvp.smpproxy.netzwerk;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.lemonpvp.smpproxy.SMPProxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class Moderation {

    public static final MinecraftChannelIdentifier MSG = MinecraftChannelIdentifier.create("antiswear", "msg");
    public static final MinecraftChannelIdentifier HALLO = MinecraftChannelIdentifier.create("antiswear", "hallo");
    public static final MinecraftChannelIdentifier MUTE = MinecraftChannelIdentifier.create("antiswear", "mute");

    public static final byte OK = 0;
    public static final byte GEAENDERT = 1;
    public static final byte BLOCKIERT = 2;

    public record Pruefung(byte status, String text) {

        static Pruefung ok(String text) {
            return new Pruefung(OK, text);
        }

        public boolean blockiert() {
            return status == BLOCKIERT;
        }
    }

    private static final long WARTEZEIT_MILLIS = 1_500L;
    private static final long NACHLAUF_MILLIS = 500L;

    private final SMPProxy plugin;
    private final Set<String> moderiert = ConcurrentHashMap.newKeySet();
    private final Map<Long, CompletableFuture<Pruefung>> offen = new ConcurrentHashMap<>();
    private final AtomicLong zaehler = new AtomicLong();

    public Moderation(SMPProxy plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void beimPluginKanal(PluginMessageEvent event) {
        boolean msg = event.getIdentifier().equals(MSG);
        boolean hallo = event.getIdentifier().equals(HALLO);
        if (!msg && !hallo && !event.getIdentifier().equals(MUTE)) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection verbindung)) {
            return;
        }
        if (hallo) {
            moderiert.add(verbindung.getServerInfo().getName());
            return;
        }
        if (!msg) {
            return;
        }
        try (DataInputStream ein = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            long id = ein.readLong();
            byte status = ein.readByte();
            String text = ein.readUTF();
            CompletableFuture<Pruefung> wartend = offen.remove(id);
            if (wartend != null) {
                wartend.complete(new Pruefung(status, text));
            }
        } catch (IOException | RuntimeException fehler) {
            plugin.log().warn("Unlesbare Antwort von AntiSwear auf {}: {}", verbindung.getServerInfo().getName(), fehler.getMessage());
        }
    }

    public boolean moderiert(String server) {
        return moderiert.contains(server);
    }

    public CompletableFuture<Pruefung> pruefen(Player sender, String text) {
        Optional<ServerConnection> verbindung = sender.getCurrentServer();
        if (verbindung.isEmpty() || !moderiert.contains(verbindung.get().getServerInfo().getName())) {
            return CompletableFuture.completedFuture(Pruefung.ok(text));
        }
        long id = zaehler.incrementAndGet();
        CompletableFuture<Pruefung> antwort = new CompletableFuture<>();
        offen.put(id, antwort);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream aus = new DataOutputStream(bytes);
            aus.writeLong(id);
            aus.writeUTF(text);
            if (!verbindung.get().sendPluginMessage(MSG, bytes.toByteArray())) {
                offen.remove(id);
                return CompletableFuture.completedFuture(Pruefung.ok(text));
            }
        } catch (IOException fehler) {
            offen.remove(id);
            return CompletableFuture.completedFuture(Pruefung.ok(text));
        }
        return antwort.completeOnTimeout(Pruefung.ok(text), WARTEZEIT_MILLIS, TimeUnit.MILLISECONDS)
                .whenComplete((ergebnis, fehler) -> offen.remove(id));
    }

    public void stummPush(UUID spieler, long restMillis) {
        plugin.proxy().getPlayer(spieler).flatMap(Player::getCurrentServer)
                .ifPresent(verbindung -> senden(verbindung, restMillis));
    }

    @Subscribe
    public void beimServerwechsel(ServerPostConnectEvent event) {
        Player spieler = event.getPlayer();
        if (plugin.stummListe().restMillis(spieler.getUniqueId()) == 0) {
            return;
        }
        plugin.proxy().getScheduler().buildTask(plugin, () -> {
            long rest = plugin.stummListe().restMillis(spieler.getUniqueId());
            if (rest != 0) {
                stummPush(spieler.getUniqueId(), rest);
            }
        }).delay(NACHLAUF_MILLIS, TimeUnit.MILLISECONDS).schedule();
    }

    private static void senden(ServerConnection verbindung, long restMillis) {
        byte[] daten = new byte[Long.BYTES];
        for (int i = 0; i < Long.BYTES; i++) {
            daten[i] = (byte) (restMillis >>> (8 * (Long.BYTES - 1 - i)));
        }
        verbindung.sendPluginMessage(MUTE, daten);
    }
}
