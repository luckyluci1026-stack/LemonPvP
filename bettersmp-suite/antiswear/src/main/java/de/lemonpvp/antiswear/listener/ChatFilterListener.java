package de.lemonpvp.antiswear.listener;

import de.lemonpvp.antiswear.AntiSwear;
import de.lemonpvp.antiswear.filter.Treffer;
import de.lemonpvp.antiswear.util.Durations;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Haengt sich so frueh wie moeglich (LOWEST) in den Chat ein, damit eine
 * Zensur schon steht, bevor irgendein anderes Plugin (z.B. BetterSMPs
 * Chat-Formatierung) die Nachricht liest oder weiterreicht.
 *
 * Die eigentliche Pruefung passiert direkt im Async-Handler (reine
 * String-Verarbeitung + das noetige event.setCancelled()/event.message()),
 * alles danach (Nachrichten, Team-Hinweis, Stufen-Aktion inkl.
 * Konsolenbefehl) laeuft ueber runTask auf dem Hauptthread - gleiches
 * Vorbild wie ReportPlus' ChatCaptureListener.
 */
public final class ChatFilterListener implements Listener {

    private final AntiSwear plugin;

    public ChatFilterListener(AntiSwear plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player spieler = event.getPlayer();

        if (plugin.strikes().istStummgeschaltet(spieler.getUniqueId())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> {
                long rest = plugin.strikes().stummRestMillis(spieler.getUniqueId());
                plugin.msgs().send(spieler, "stummgeschaltet", "rest", Durations.humanize(rest));
            });
            return;
        }
        if (spieler.hasPermission("antiswear.bypass")) {
            return;
        }

        String klartext = PlainTextComponentSerializer.plainText().serialize(event.message());
        List<Treffer> treffer = plugin.filter().pruefen(klartext);
        if (treffer.isEmpty()) {
            return;
        }

        int punkte = treffer.stream().mapToInt(Treffer::punkte).sum();
        String woerterListe = treffer.stream().map(Treffer::wort).distinct().collect(Collectors.joining(", "));

        final boolean blockiert;
        if (plugin.modus() == AntiSwear.Modus.BLOCKIEREN) {
            event.setCancelled(true);
            blockiert = true;
        } else {
            String zensiert = plugin.filter().zensieren(klartext);
            if (!plugin.filter().pruefen(zensiert).isEmpty()) {
                // Zensur hat die Umgehung nicht sauber erwischt - dann
                // lieber die ganze Nachricht blockieren als etwas
                // Anstoessiges durchrutschen zu lassen.
                event.setCancelled(true);
                blockiert = true;
            } else {
                event.message(Component.text(zensiert));
                blockiert = false;
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (blockiert) {
                plugin.msgs().send(spieler, "blockiert");
            }
            benachrichtigeTeam(spieler, klartext, woerterListe, punkte);
            verarbeiteVerstoss(spieler, punkte);
        });
    }

    private void benachrichtigeTeam(Player spieler, String original, String woerter, int punkte) {
        for (Player empfaenger : Bukkit.getOnlinePlayers()) {
            if (empfaenger.hasPermission("antiswear.notify")) {
                plugin.msgs().send(empfaenger, "team-hinweis",
                        "spieler", spieler.getName(), "woerter", woerter,
                        "text", original, "punkte", String.valueOf(punkte));
            }
        }
    }

    private void verarbeiteVerstoss(Player spieler, int punkte) {
        plugin.strikes().verstoss(spieler, punkte).ifPresent(stufe -> {
            switch (stufe.aktion()) {
                case WARNEN -> plugin.msgs().send(spieler, "stufe-warnen");
                case STUMMSCHALTEN -> {
                    plugin.strikes().stummschalten(spieler.getUniqueId(), stufe.dauerMillis());
                    plugin.msgs().send(spieler, "stufe-stummschalten",
                            "dauer", Durations.humanize(stufe.dauerMillis()));
                }
                case BEFEHL -> {
                    String befehl = stufe.befehl().replace("%spieler%", spieler.getName());
                    if (!befehl.isBlank()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), befehl);
                    }
                }
            }
        });
    }
}
