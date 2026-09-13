package de.lemonpvp.smpproxy.command;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.command.SimpleCommand;
import de.lemonpvp.smpproxy.SMPProxy;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * /rtp - von ueberall im Netzwerk, nicht nur auf dem SMP.
 *
 * Steht der Spieler schon auf einem Server, der /rtp selbst mitbringt
 * (BetterRTP ist dort installiert - siehe config.yml "rtp.passthrough-
 * servers"), geht der Befehl unveraendert dorthin durch: Fuer diese
 * Spieler aendert sich nichts.
 *
 * Steht er woanders (Lobby, Kreativserver, ...), passiert das, was in
 * der Schule gebraucht wurde: erst rueber auf den eingetragenen
 * SMP ("rtp.redirect-server"), und sobald die Verbindung steht, wird
 * dort automatisch /rtp ausgeloest - ueber eine eigene Kanal-Nachricht
 * an BetterRTP, nicht ueber "BungeeCord" (der Kanal hat ein festes,
 * bekanntes Nachrichtenformat und ist nicht fuer beliebige eigene
 * Anweisungen gedacht).
 */
public final class RtpCommand implements SimpleCommand {

    /** Eigener Kanal, den BetterRTP auf dem Ziel-Server abhoert. */
    public static final MinecraftChannelIdentifier RTP_CHANNEL =
            MinecraftChannelIdentifier.create("betterrtp", "run");

    /**
     * Wie lange nach einem erfolgreichen Serverwechsel gewartet wird, bevor
     * die RTP-Nachricht rausgeht. Ohne die kurze Pause kann es passieren,
     * dass die Nachricht ankommt, bevor der Backend-Server den Spieler und
     * seinen Kanal ueberhaupt fertig eingerichtet hat.
     */
    private static final long NACHLAUF_MILLIS = 400L;

    private final SMPProxy plugin;

    public RtpCommand(SMPProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(plugin.message("player-only"));
            return;
        }
        String[] args = invocation.arguments();
        List<String> passthrough = plugin.config().rtpPassthroughServers();
        String hier = player.getCurrentServer()
                .map(c -> c.getServer().getServerInfo().getName())
                .orElse("");

        if (passthrough.contains(hier)) {
            // Genau das, was der Spieler auch ohne diesen Befehl bekaeme -
            // wir schieben es nur unveraendert an den Backend-Server durch.
            player.spoofChatInput("/rtp" + (args.length == 0 ? "" : " " + String.join(" ", args)));
            return;
        }

        String ziel = plugin.config().rtpRedirectServer();
        if (ziel.isEmpty()) {
            player.sendMessage(plugin.message("rtp-not-configured"));
            return;
        }
        Optional<RegisteredServer> zielServer = plugin.proxy().getServer(ziel);
        if (zielServer.isEmpty()) {
            player.sendMessage(plugin.message("rtp-not-configured"));
            return;
        }
        if (ziel.equals(hier)) {
            // rtp.redirect-server steht selbst nicht in den passthrough-
            // Servern (Einstellungsfehler) - trotzdem einfach durchreichen,
            // statt den Spieler mit "verbinde mit dem Server, auf dem du
            // schon bist" zu verwirren.
            player.spoofChatInput("/rtp" + (args.length == 0 ? "" : " " + String.join(" ", args)));
            return;
        }

        player.sendMessage(plugin.message("rtp-sending", "%server%", ziel));
        player.createConnectionRequest(zielServer.get()).connect().whenComplete((result, error) -> {
            boolean ok = error == null && result != null && result.isSuccessful();
            if (!ok) {
                player.sendMessage(plugin.message("switch-failed", "%server%", ziel));
                return;
            }
            plugin.proxy().getScheduler().buildTask(plugin, () -> sendeRtpAuftrag(player, args))
                    .delay(NACHLAUF_MILLIS, TimeUnit.MILLISECONDS)
                    .schedule();
        });
    }

    private void sendeRtpAuftrag(Player player, String[] args) {
        // Derselbe Lebendigkeits-Test wie in ConnectListener.notifyLater():
        // ohne aktuellen Server ist die Verbindung in der Zwischenzeit weg.
        if (player.getCurrentServer().isEmpty()) {
            return;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream aus = new DataOutputStream(bytes);
            aus.writeUTF(String.join(" ", args));
            player.sendPluginMessage(RTP_CHANNEL, bytes.toByteArray());
        } catch (IOException fehler) {
            plugin.log().warn("RTP-Auftrag fuer {} liess sich nicht senden: {}",
                    player.getUsername(), fehler.getMessage());
        }
    }
}
