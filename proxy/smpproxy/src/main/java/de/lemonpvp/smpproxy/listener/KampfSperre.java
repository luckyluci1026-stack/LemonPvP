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
import de.lemonpvp.smpproxy.config.ProxyConfig;
import de.lemonpvp.smpproxy.netzwerk.ChatRelay;
import de.lemonpvp.smpproxy.util.Msg;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KampfSperre {

    public static final MinecraftChannelIdentifier KANAL = MinecraftChannelIdentifier.create("bettersmp", "combat");

    private static final long HOECHSTENS_MILLIS = 10L * 60L * 1000L;
    private static final byte ART_KAMPF = 0;
    private static final byte ART_DUELL = 1;
    private static final byte ART_FREEZE = 2;

    private record Sperre(long bis, byte art) {
    }

    private final SMPProxy plugin;
    private final Map<UUID, Map<Byte, Long>> sperren = new ConcurrentHashMap<>();

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
        byte art = puffer.hasRemaining() ? puffer.get() : ART_KAMPF;
        UUID spieler = verbindung.getPlayer().getUniqueId();
        if (rest <= 0) {
            sperren.computeIfPresent(spieler, (id, arten) -> {
                arten.remove(art);
                return arten.isEmpty() ? null : arten;
            });
        } else {
            sperren.computeIfAbsent(spieler, id -> new ConcurrentHashMap<>())
                    .put(art, System.currentTimeMillis() + Math.min(rest, HOECHSTENS_MILLIS));
        }
    }

    @Subscribe
    public void beimBefehl(CommandExecuteEvent event) {
        CommandSource quelle = event.getCommandSource();
        if (!(quelle instanceof Player spieler) || !event.getResult().isAllowed() || !plugin.config().combatEnabled()) {
            return;
        }
        Sperre sperre = aktiveSperre(spieler.getUniqueId());
        if (sperre == null) {
            return;
        }
        String name = befehlsName(event.getCommand());
        if (!plugin.config().combatBlockedCommands().contains(name) && !wechseltServer(spieler, name)) {
            return;
        }
        event.setResult(CommandExecuteEvent.CommandResult.denied());
        long rest = sperre.bis() - System.currentTimeMillis();
        String text = switch (sperre.art()) {
            case ART_DUELL -> plugin.config().duelBlockedMessage();
            case ART_FREEZE -> plugin.config().message("freeze-blocked");
            default -> plugin.config().combatBlockedMessage();
        };
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

    private boolean wechseltServer(Player spieler, String name) {
        ProxyConfig config = plugin.config();
        String hier = ChatRelay.serverVon(spieler);
        if (name.equals("rtp") && config.rtpEnabled() && !config.rtpRedirectServer().isEmpty()) {
            return !config.rtpPassthroughServers().contains(hier) && !config.rtpRedirectServer().equals(hier);
        }
        if (config.ahEnabled() && config.ahAliases().contains(name)) {
            return !config.ahPassthroughServers().contains(hier) && !config.ahRedirectServer().equals(hier);
        }
        return false;
    }

    private Sperre aktiveSperre(UUID spieler) {
        Map<Byte, Long> arten = sperren.get(spieler);
        if (arten == null) {
            return null;
        }
        long jetzt = System.currentTimeMillis();
        arten.values().removeIf(bis -> bis <= jetzt);
        for (byte art : new byte[]{ART_DUELL, ART_FREEZE, ART_KAMPF}) {
            Long bis = arten.get(art);
            if (bis != null) {
                return new Sperre(bis, art);
            }
        }
        return null;
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
