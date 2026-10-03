package de.lemonpvp.betterrtp.network;

import de.lemonpvp.betterrtp.BetterRTP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hört auf den Kanal "betterrtp:run", über den der Proxy (SMPProxy)
 * "/rtp" für einen Spieler auslöst, der gerade erst von woanders her
 * (typischerweise der Lobby) auf diesen Server geschickt wurde.
 *
 * Warum ein eigener Kanal statt "BungeeCord": Der hat ein festes,
 * bekanntes Nachrichtenformat (Connect, PlayerCount, ...) und ist nicht
 * für beliebige eigene Anweisungen gedacht. Ein eigener Kanal macht
 * außerdem klar, wer hier mit wem redet - nur der Proxy und BetterRTP.
 *
 * Es wird bewusst der ganz normale Befehl ausgelöst (Bukkit.dispatchCommand),
 * nicht die Teleport-Logik direkt angesprochen: Damit laufen Cooldown,
 * Warmup, Welt-Profile und Rechte-Prüfung genauso wie bei jedem Spieler,
 * der /rtp selbst eintippt - keine zweite, abweichende Ausnahme-Regel.
 */
public final class RtpChannelListener implements PluginMessageListener, Listener {

    public static final String KANAL = "betterrtp:run";

    private static final long GUELTIG_NACH_JOIN_MILLIS = 15_000L;

    private final Map<UUID, Long> beigetreten = new ConcurrentHashMap<>();

    private final BetterRTP plugin;

    public RtpChannelListener(BetterRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String kanal, @NotNull Player spieler,
                                        byte @NotNull [] daten) {
        Long joinZeit = beigetreten.remove(spieler.getUniqueId());
        if (!KANAL.equals(kanal) || joinZeit == null
                || System.currentTimeMillis() - joinZeit > GUELTIG_NACH_JOIN_MILLIS) {
            return;
        }
        String argument;
        try (DataInputStream ein = new DataInputStream(new ByteArrayInputStream(daten))) {
            argument = ein.readUTF();
        } catch (IOException fehler) {
            plugin.getLogger().warning("RTP-Auftrag vom Proxy war nicht lesbar: " + fehler.getMessage());
            return;
        }
        // Der Spieler ist gerade erst angekommen - eine Angriffsfläche
        // gibt es hier nicht (der Proxy ist die einzige Quelle dieses
        // Kanals), aber der Befehl soll auf dem Hauptthread laufen wie
        // jeder andere Spielerbefehl auch.
        Bukkit.getScheduler().runTask(plugin,
                () -> Bukkit.dispatchCommand(spieler, ("rtp " + argument).trim()));
    }

    @EventHandler
    public void beimJoin(PlayerJoinEvent event) {
        beigetreten.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void beimVerlassen(PlayerQuitEvent event) {
        beigetreten.remove(event.getPlayer().getUniqueId());
    }
}
