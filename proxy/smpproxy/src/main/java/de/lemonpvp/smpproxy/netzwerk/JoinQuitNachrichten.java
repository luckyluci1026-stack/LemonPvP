package de.lemonpvp.smpproxy.netzwerk;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.smpproxy.SMPProxy;
import net.kyori.adventure.text.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class JoinQuitNachrichten {

    private final SMPProxy plugin;
    private final Set<UUID> neu = ConcurrentHashMap.newKeySet();
    private final Set<UUID> angekuendigt = ConcurrentHashMap.newKeySet();

    public JoinQuitNachrichten(SMPProxy plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void beimLogin(LoginEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (event.getResult().isAllowed() && !plugin.bans().kennt(id)) {
            neu.add(id);
        }
    }

    @Subscribe
    public void beimVerbinden(ServerConnectedEvent event) {
        if (event.getPreviousServer().isPresent()) {
            return;
        }
        Player spieler = event.getPlayer();
        UUID id = spieler.getUniqueId();
        boolean erstesMal = neu.remove(id);
        angekuendigt.add(id);
        if (!plugin.config().joinQuitEnabled()) {
            return;
        }
        Component text = Texte.mitSpielertext(
                erstesMal ? plugin.config().firstJoinMessage() : plugin.config().joinMessage(),
                plugin.config().prefix(), "%player%", spieler.getUsername());
        String ziel = event.getServer().getServerInfo().getName();
        for (Player empfaenger : plugin.proxy().getAllPlayers()) {
            boolean selbst = empfaenger.getUniqueId().equals(id);
            String server = selbst ? ziel : ChatRelay.serverVon(empfaenger);
            if (plugin.config().chatServers().contains(server)) {
                empfaenger.sendMessage(text);
            }
        }
    }

    @Subscribe
    public void beimTrennen(DisconnectEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        neu.remove(id);
        if (!angekuendigt.remove(id) || !plugin.config().joinQuitEnabled()) {
            return;
        }
        Component text = Texte.mitSpielertext(plugin.config().quitMessage(), plugin.config().prefix(),
                "%player%", event.getPlayer().getUsername());
        for (Player empfaenger : plugin.proxy().getAllPlayers()) {
            if (!empfaenger.getUniqueId().equals(id)
                    && plugin.config().chatServers().contains(ChatRelay.serverVon(empfaenger))) {
                empfaenger.sendMessage(text);
            }
        }
    }
}
