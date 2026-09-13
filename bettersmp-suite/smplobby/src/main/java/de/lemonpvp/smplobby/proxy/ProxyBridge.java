package de.lemonpvp.smplobby.proxy;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.lemonpvp.smplobby.SMPLobby;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Die Leitung zum Proxy.
 *
 * Ein Spielserver kann einen Spieler nicht selbst auf einen anderen
 * Server schieben - er weiss von den anderen gar nichts. Was er kann:
 * dem Proxy eine Nachricht schicken und ihn darum bitten. Der Kanal
 * dafuer heisst seit BungeeCord-Zeiten "BungeeCord"; Velocity versteht
 * ihn genauso, deshalb bleibt der alte Name.
 *
 * Wichtig und leicht zu uebersehen: Die Nachricht geht ueber die
 * Verbindung EINES Spielers. Ist niemand online, kommt sie nirgends an -
 * deshalb werden die Spielerzahlen nur abgefragt, solange jemand da ist,
 * und der Zaehler sonst einfach auf dem letzten Stand gelassen.
 *
 * Die Zahlen kommen nicht sofort zurueck, sondern als Antwort irgendwann
 * danach. Wir merken sie uns und zeigen beim naechsten Oeffnen des
 * Waehlers den letzten bekannten Stand - eine kurz veraltete Zahl ist
 * besser als ein leeres Feld.
 */
public final class ProxyBridge implements PluginMessageListener {

    /** So heisst der Kanal - auch bei Velocity. */
    public static final String KANAL = "BungeeCord";

    private final SMPLobby plugin;

    /** Servername -> zuletzt gemeldete Spielerzahl. */
    private final Map<String, Integer> zahlen = new HashMap<>();

    /** Spieler im ganzen Netzwerk, -1 solange nichts bekannt ist. */
    private int netzwerk = -1;

    /** Auf welchem Server der Proxy uns selbst fuehrt. */
    private String eigenerName = "";

    public ProxyBridge(SMPLobby plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, KANAL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, KANAL, this);
    }

    public void stop() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, KANAL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, KANAL, this);
    }

    // ------------------------------------------------------------ senden

    /**
     * Einen Spieler auf einen anderen Server schicken.
     *
     * @return false, wenn die Nachricht gar nicht erst rausging
     */
    public boolean verbinde(Player spieler, String server) {
        ByteArrayDataOutput aus = ByteStreams.newDataOutput();
        aus.writeUTF("Connect");
        aus.writeUTF(server);
        return sende(spieler, aus.toByteArray());
    }

    /** Nach den Spielerzahlen fragen. Die Antwort kommt spaeter. */
    public void frageZahlen(Iterable<String> server) {
        Player bote = irgendwer();
        if (bote == null) {
            return;
        }
        for (String name : server) {
            ByteArrayDataOutput aus = ByteStreams.newDataOutput();
            aus.writeUTF("PlayerCount");
            aus.writeUTF(name);
            sende(bote, aus.toByteArray());
        }
        ByteArrayDataOutput alle = ByteStreams.newDataOutput();
        alle.writeUTF("PlayerCount");
        alle.writeUTF("ALL");
        sende(bote, alle.toByteArray());

        if (eigenerName.isEmpty()) {
            ByteArrayDataOutput wer = ByteStreams.newDataOutput();
            wer.writeUTF("GetServer");
            sende(bote, wer.toByteArray());
        }
    }

    private boolean sende(Player spieler, byte[] daten) {
        if (spieler == null || !spieler.isOnline()) {
            return false;
        }
        spieler.sendPluginMessage(plugin, KANAL, daten);
        return true;
    }

    private Player irgendwer() {
        for (Player spieler : Bukkit.getOnlinePlayers()) {
            return spieler;
        }
        return null;
    }

    // ------------------------------------------------------------ empfangen

    @Override
    public void onPluginMessageReceived(@NotNull String kanal, @NotNull Player spieler,
                                        byte @NotNull [] daten) {
        if (!KANAL.equals(kanal)) {
            return;
        }
        try (DataInputStream ein = new DataInputStream(new ByteArrayInputStream(daten))) {
            String art = ein.readUTF();
            if ("PlayerCount".equals(art)) {
                String server = ein.readUTF();
                int anzahl = ein.readInt();
                if ("ALL".equals(server)) {
                    netzwerk = anzahl;
                } else {
                    zahlen.put(server, anzahl);
                }
            } else if ("GetServer".equals(art)) {
                eigenerName = ein.readUTF();
            }
        } catch (IOException fehler) {
            // Eine unverstaendliche Nachricht vom Proxy ist kein Grund,
            // den Server mit einem Fehlerbericht zu fluten - das kaeme
            // dann alle paar Sekunden wieder.
            plugin.getLogger().fine("Nachricht vom Proxy nicht lesbar: " + fehler.getMessage());
        }
    }

    // ------------------------------------------------------------ abfragen

    /** Zuletzt bekannte Spielerzahl, oder -1 wenn noch keine da war. */
    public int zahl(String server) {
        Integer wert = zahlen.get(server);
        return wert == null ? -1 : wert;
    }

    public int netzwerkZahl() {
        return netzwerk;
    }

    public String eigenerName() {
        return eigenerName;
    }

    /** Ob der Proxy sich ueberhaupt schon gemeldet hat. */
    public boolean erreichbar() {
        return !eigenerName.isEmpty() || netzwerk >= 0 || !zahlen.isEmpty();
    }
}
