package de.lemonpvp.smpproxy.listener;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.lemonpvp.smpproxy.SMPProxy;
import de.lemonpvp.smpproxy.util.Msg;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KampfSperre {

    public static final MinecraftChannelIdentifier KANAL = MinecraftChannelIdentifier.create("bettersmp", "combat");

    private static final long HOECHSTENS_MILLIS = 10L * 60L * 1000L;
    private static final byte ART_DUELL = 1;

    private record Sperre(long bis, boolean duell) {
    }

    private final SMPProxy plugin;
    private final Map<UUID, Sperre> sperren = new ConcurrentHashMap<>();

    public KampfSperre(SMPProxy plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void beimPluginKanal(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(KANAL)) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        byte[] daten = event.getData();
        if (!(event.getSource() instanceof ServerConnection verbindung)
                || (daten.length != Long.BYTES && daten.length != Long.BYTES + 1)) {
            return;
        }
        boolean vomAktuellenServer = verbindung.getPlayer().getCurrentServer()
                .map(jetzt -> jetzt.getServerInfo().equals(verbindung.getServerInfo()))
                .orElse(false);
        if (!vomAktuellenServer) {
            return;
        }
        ByteBuffer puffer = ByteBuffer.wrap(daten);
        long rest = puffer.getLong();
        boolean duell = puffer.hasRemaining() && puffer.get() == ART_DUELL;
        UUID spieler = verbindung.getPlayer().getUniqueId();
        if (rest <= 0) {
            sperren.remove(spieler);
        } else {
            sperren.put(spieler, new Sperre(System.currentTimeMillis() + Math.min(rest, HOECHSTENS_MILLIS), duell));
        }
    }

    @Subscribe
    public void beimBefehl(CommandExecuteEvent event) {
        CommandSource quelle = event.getCommandSource();
        if (!(quelle instanceof Player spieler) || !event.getResult().isAllowed() || !plugin.config().combatEnabled()) {
            return;
        }
        Sperre sperre = aktiveSperre(spieler.getUniqueId());
        if (sperre == null || !plugin.config().combatBlockedCommands().contains(befehlsName(event.getCommand()))) {
            return;
        }
        event.setResult(CommandExecuteEvent.CommandResult.denied());
        long rest = sperre.bis() - System.currentTimeMillis();
        String text = sperre.duell() ? plugin.config().duelBlockedMessage() : plugin.config().combatBlockedMessage();
        spieler.sendMessage(Msg.of(text, plugin.config().prefix(), "%seconds%", String.valueOf(Math.max(1, (rest + 999) / 1000))));
    }

    @Subscribe
    public void beimServerwechsel(ServerConnectedEvent event) {
        if (event.getPreviousServer().isPresent()) {
            sperren.remove(event.getPlayer().getUniqueId());
        }
    }

    @Subscribe
    public void beimTrennen(DisconnectEvent event) {
        sperren.remove(event.getPlayer().getUniqueId());
    }

    private Sperre aktiveSperre(UUID spieler) {
        Sperre sperre = sperren.get(spieler);
        if (sperre == null) {
            return null;
        }
        if (sperre.bis() <= System.currentTimeMillis()) {
            sperren.remove(spieler, sperre);
            return null;
        }
        return sperre;
    }

    private static String befehlsName(String befehl) {
        String text = befehl.trim();
        if (text.startsWith("/")) {
            text = text.substring(1);
        }
        int leerzeichen = text.indexOf(' ');
        String name = (leerzeichen < 0 ? text : text.substring(0, leerzeichen)).toLowerCase(Locale.ROOT);
        int doppelpunkt = name.indexOf(':');
        return doppelpunkt < 0 ? name : name.substring(doppelpunkt + 1);
    }
}
