package de.lemonpvp.smpproxy.netzwerk;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.smpproxy.SMPProxy;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Privatnachrichten {

    private final SMPProxy plugin;
    private final Map<UUID, UUID> letzterPartner = new ConcurrentHashMap<>();

    public Privatnachrichten(SMPProxy plugin) {
        this.plugin = plugin;
    }

    public void schreiben(Player sender, String zielName, String text) {
        Optional<Player> ziel = plugin.proxy().getPlayer(zielName);
        if (ziel.isEmpty()) {
            sender.sendMessage(plugin.message("msg-offline", "%target%", zielName));
            return;
        }
        zustellen(sender, ziel.get(), text);
    }

    public void antworten(Player sender, String text) {
        UUID partner = letzterPartner.get(sender.getUniqueId());
        if (partner == null) {
            sender.sendMessage(plugin.message("msg-no-reply"));
            return;
        }
        Optional<Player> ziel = plugin.proxy().getPlayer(partner);
        if (ziel.isEmpty()) {
            sender.sendMessage(plugin.message("msg-offline", "%target%", plugin.bans().nameFuer(partner)));
            return;
        }
        zustellen(sender, ziel.get(), text);
    }

    private void zustellen(Player sender, Player ziel, String text) {
        if (ziel.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage(plugin.message("msg-self"));
            return;
        }
        long stummRest = plugin.stummListe().restMillis(sender.getUniqueId());
        if (stummRest != 0) {
            sender.sendMessage(plugin.chatRelay().stummHinweis(stummRest));
            return;
        }
        plugin.moderation().pruefen(sender, text).thenAccept(pruefung -> {
            if (pruefung.blockiert()) {
                plugin.log().info("[MSG blockiert] {} -> {}: {}", sender.getUsername(), ziel.getUsername(), text);
                return;
            }
            senden(sender, ziel, pruefung.text());
        });
    }

    private void senden(Player sender, Player ziel, String text) {
        String prefix = plugin.config().prefix();
        sender.sendMessage(Texte.mitSpielertext(plugin.config().msgSentMessage(), prefix,
                "%sender%", sender.getUsername(), "%target%", ziel.getUsername(), "%message%", text));
        ziel.sendMessage(Texte.mitSpielertext(plugin.config().msgReceivedMessage(), prefix,
                "%sender%", sender.getUsername(), "%target%", ziel.getUsername(), "%message%", text));
        letzterPartner.put(sender.getUniqueId(), ziel.getUniqueId());
        letzterPartner.put(ziel.getUniqueId(), sender.getUniqueId());
        plugin.log().info("[MSG] {} -> {}: {}", sender.getUsername(), ziel.getUsername(), text);
    }

    @Subscribe
    public void beimTrennen(DisconnectEvent event) {
        letzterPartner.remove(event.getPlayer().getUniqueId());
    }
}
