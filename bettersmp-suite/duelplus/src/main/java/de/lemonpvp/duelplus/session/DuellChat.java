package de.lemonpvp.duelplus.session;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.lemonpvp.duelplus.DuelPlus;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DuellChat implements Listener {

    public static final String KANAL = "bettersmp:chat";

    private static final GsonComponentSerializer JSON = GsonComponentSerializer.builder()
            .options(JSONOptions.compatibility())
            .build();
    private static final int MAX_JSON_LAENGE = 30_000;
    private static final long HINWEIS_ABSTAND_MILLIS = 10_000L;

    private final DuelPlus plugin;
    private final Map<UUID, UUID> partner = new ConcurrentHashMap<>();
    private final Map<UUID, Long> letzterHinweis = new ConcurrentHashMap<>();

    public DuellChat(DuelPlus plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, KANAL);
    }

    public void partnerSetzen(UUID a, UUID b) {
        partner.put(a, b);
        partner.put(b, a);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimChat(AsyncChatEvent event) {
        Player spieler = event.getPlayer();
        Set<UUID> empfaenger = new LinkedHashSet<>();
        empfaenger.add(spieler.getUniqueId());
        UUID gegner = partner.get(spieler.getUniqueId());
        if (gegner != null) {
            empfaenger.add(gegner);
        } else {
            hinweisen(spieler);
        }
        if (!plugin.isEnabled() || !spieler.getListeningPluginChannels().contains(KANAL)) {
            event.viewers().removeIf(zuschauer -> zuschauer instanceof Player anderer
                    && !empfaenger.contains(anderer.getUniqueId()));
            return;
        }
        Component fertig = event.renderer().render(spieler, spieler.displayName(), event.message(),
                Bukkit.getConsoleSender());
        String json = JSON.serialize(fertig);
        if (json.length() > MAX_JSON_LAENGE) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        ByteArrayDataOutput aus = ByteStreams.newDataOutput();
        aus.writeByte(1);
        aus.writeUTF(json);
        aus.writeInt(empfaenger.size());
        for (UUID id : empfaenger) {
            aus.writeLong(id.getMostSignificantBits());
            aus.writeLong(id.getLeastSignificantBits());
        }
        spieler.sendPluginMessage(plugin, KANAL, aus.toByteArray());
    }

    private void hinweisen(Player spieler) {
        long jetzt = System.currentTimeMillis();
        Long zuletzt = letzterHinweis.get(spieler.getUniqueId());
        if (zuletzt == null || jetzt - zuletzt >= HINWEIS_ABSTAND_MILLIS) {
            letzterHinweis.put(spieler.getUniqueId(), jetzt);
            plugin.msgs().send(spieler, "chat-only-opponent");
        }
    }

    @EventHandler
    public void beimVerlassen(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        partner.remove(id);
        partner.values().removeIf(id::equals);
        letzterHinweis.remove(id);
    }
}
