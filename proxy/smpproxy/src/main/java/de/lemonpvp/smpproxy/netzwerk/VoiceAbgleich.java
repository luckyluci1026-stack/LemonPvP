package de.lemonpvp.smpproxy.netzwerk;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.lemonpvp.smpproxy.SMPProxy;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class VoiceAbgleich {

    public static final MinecraftChannelIdentifier KANAL = MinecraftChannelIdentifier.create("antiswear", "voice");
    public static final byte STUMM = 1;
    public static final byte REGELN = 2;

    public record Zustand(long wert, long zeit, String name, String von, String grund) {
    }

    private static final long NACHLAUF_MILLIS = 500L;
    private static final long AUFBEWAHREN_MILLIS = 90L * 24 * 60 * 60 * 1000;

    private final SMPProxy plugin;
    private final Path datei;
    private final Map<UUID, Zustand> stumm = new ConcurrentHashMap<>();
    private final Map<UUID, Zustand> regeln = new ConcurrentHashMap<>();

    public VoiceAbgleich(SMPProxy plugin, Path ordner) {
        this.plugin = plugin;
        this.datei = ordner.resolve("voice.yml");
    }

    private Map<UUID, Zustand> liste(byte art) {
        return art == STUMM ? stumm : art == REGELN ? regeln : null;
    }

    public Zustand zustand(byte art, UUID spieler) {
        Map<UUID, Zustand> liste = liste(art);
        return liste == null ? null : liste.get(spieler);
    }

    @Subscribe
    public void beimPluginKanal(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(KANAL)) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection quelle)) {
            return;
        }
        byte art;
        UUID spieler;
        Zustand neu;
        try (DataInputStream ein = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            art = ein.readByte();
            spieler = new UUID(ein.readLong(), ein.readLong());
            neu = new Zustand(ein.readLong(), ein.readLong(), ein.readUTF(), ein.readUTF(), ein.readUTF());
        } catch (IOException | RuntimeException fehler) {
            plugin.log().warn("Unlesbare Voice-Nachricht von {}: {}", quelle.getServerInfo().getName(), fehler.getMessage());
            return;
        }
        Map<UUID, Zustand> liste = liste(art);
        if (liste == null) {
            return;
        }
        Zustand alt = liste.get(spieler);
        if (alt != null && alt.zeit() > neu.zeit()) {
            senden(quelle, art, spieler, alt);
            return;
        }
        if (alt != null && alt.zeit() == neu.zeit()) {
            return;
        }
        liste.put(spieler, neu);
        speichern();
        String quellServer = quelle.getServerInfo().getName();
        plugin.proxy().getPlayer(spieler).flatMap(Player::getCurrentServer)
                .filter(ziel -> !ziel.getServerInfo().getName().equals(quellServer))
                .ifPresent(ziel -> senden(ziel, art, spieler, neu));
    }

    @Subscribe
    public void beimServerwechsel(ServerPostConnectEvent event) {
        Player spieler = event.getPlayer();
        UUID id = spieler.getUniqueId();
        if (!stumm.containsKey(id) && !regeln.containsKey(id)) {
            return;
        }
        plugin.proxy().getScheduler().buildTask(plugin, () -> spieler.getCurrentServer().ifPresent(verbindung -> {
            for (byte art : new byte[]{STUMM, REGELN}) {
                Zustand zustand = zustand(art, id);
                if (zustand != null) {
                    senden(verbindung, art, id, zustand);
                }
            }
        })).delay(NACHLAUF_MILLIS, TimeUnit.MILLISECONDS).schedule();
    }

    private static void senden(ServerConnection verbindung, byte art, UUID spieler, Zustand zustand) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream aus = new DataOutputStream(bytes);
            aus.writeByte(art);
            aus.writeLong(spieler.getMostSignificantBits());
            aus.writeLong(spieler.getLeastSignificantBits());
            aus.writeLong(zustand.wert());
            aus.writeLong(zustand.zeit());
            aus.writeUTF(zustand.name());
            aus.writeUTF(zustand.von());
            aus.writeUTF(zustand.grund());
            verbindung.sendPluginMessage(KANAL, bytes.toByteArray());
        } catch (IOException fehler) {
            throw new IllegalStateException(fehler);
        }
    }

    @SuppressWarnings("unchecked")
    public void laden() {
        stumm.clear();
        regeln.clear();
        if (Files.notExists(datei)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(datei, StandardCharsets.UTF_8)) {
            Object gelesen = new Yaml().load(reader);
            if (!(gelesen instanceof Map<?, ?> wurzel)) {
                return;
            }
            lesen(wurzel.get("stumm"), stumm);
            lesen(wurzel.get("regeln"), regeln);
        } catch (IOException | RuntimeException fehler) {
            plugin.log().error("voice.yml konnte nicht gelesen werden", fehler);
        }
        long jetzt = System.currentTimeMillis();
        stumm.entrySet().removeIf(e -> e.getValue().wert() <= jetzt && e.getValue().zeit() < jetzt - AUFBEWAHREN_MILLIS);
    }

    private static void lesen(Object bereich, Map<UUID, Zustand> ziel) {
        if (!(bereich instanceof Map<?, ?> eintraege)) {
            return;
        }
        for (Map.Entry<?, ?> eintrag : eintraege.entrySet()) {
            if (!(eintrag.getValue() instanceof Map<?, ?> werte)) {
                continue;
            }
            try {
                ziel.put(UUID.fromString(String.valueOf(eintrag.getKey())), new Zustand(
                        zahl(werte.get("wert")), zahl(werte.get("zeit")), text(werte.get("name")),
                        text(werte.get("von")), text(werte.get("grund"))));
            } catch (IllegalArgumentException ignoriert) {
            }
        }
    }

    private static long zahl(Object wert) {
        if (wert instanceof Number n) {
            return n.longValue();
        }
        return wert == null ? 0 : Long.parseLong(String.valueOf(wert).trim());
    }

    private static String text(Object wert) {
        return wert == null ? "" : String.valueOf(wert);
    }

    private synchronized void speichern() {
        Map<String, Object> wurzel = new LinkedHashMap<>();
        wurzel.put("stumm", alsKarte(stumm));
        wurzel.put("regeln", alsKarte(regeln));
        try {
            Files.createDirectories(datei.getParent());
            DumperOptions optionen = new DumperOptions();
            optionen.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Path neu = datei.resolveSibling("voice.yml.neu");
            try (Writer writer = Files.newBufferedWriter(neu, StandardCharsets.UTF_8)) {
                writer.write("# Voice-Chat: Stummschaltungen und \"Regeln akzeptiert\" - von AntiSwear gemeldet.\n\n");
                new Yaml(optionen).dump(wurzel, writer);
            }
            Files.move(neu, datei, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException fehler) {
            plugin.log().error("voice.yml konnte nicht geschrieben werden", fehler);
        }
    }

    private static Map<String, Object> alsKarte(Map<UUID, Zustand> quelle) {
        Map<String, Object> karte = new LinkedHashMap<>();
        quelle.forEach((id, zustand) -> {
            Map<String, Object> werte = new LinkedHashMap<>();
            werte.put("wert", zustand.wert());
            werte.put("zeit", zustand.zeit());
            werte.put("name", zustand.name());
            if (!zustand.von().isEmpty()) {
                werte.put("von", zustand.von());
            }
            if (!zustand.grund().isEmpty()) {
                werte.put("grund", zustand.grund());
            }
            karte.put(id.toString(), werte);
        });
        return karte;
    }
}
