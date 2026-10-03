package de.lemonpvp.antiswear.listener;

import de.lemonpvp.antiswear.AntiSwear;
import de.lemonpvp.antiswear.filter.ChatPruefung;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InhaltListener implements Listener {

    private static final long AMBOSS_HINWEIS_ABSTAND = 3_000L;

    private final AntiSwear plugin;
    private final Map<UUID, Long> letzterAmbossHinweis = new ConcurrentHashMap<>();

    public InhaltListener(AntiSwear plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimSchild(SignChangeEvent event) {
        Player spieler = event.getPlayer();
        if (!plugin.getConfig().getBoolean("pruefen.schilder", true) || spieler.hasPermission("antiswear.bypass")) {
            return;
        }
        List<String> zeilen = new ArrayList<>();
        for (Component zeile : event.lines()) {
            zeilen.add(zeile == null ? "" : PlainTextComponentSerializer.plainText().serialize(zeile));
        }
        String zusammen = String.join(" ", zeilen);
        ChatPruefung.Ergebnis ergebnis = plugin.pruefung().pruefenOhneSpam(zusammen);
        if (!ergebnis.verstoss()) {
            return;
        }
        boolean leeren = ergebnis.kategorie() != ChatPruefung.Kategorie.WORT;
        if (!leeren) {
            List<String> zensiert = new ArrayList<>();
            for (String zeile : zeilen) {
                zensiert.add(plugin.filter().zensieren(zeile));
            }
            leeren = !plugin.filter().pruefen(String.join(" ", zensiert)).isEmpty();
            if (!leeren) {
                for (int i = 0; i < zensiert.size(); i++) {
                    event.line(i, Component.text(zensiert.get(i)));
                }
            }
        }
        if (leeren) {
            for (int i = 0; i < zeilen.size(); i++) {
                event.line(i, Component.empty());
            }
        }
        plugin.msgs().send(spieler, "schild-geaendert");
        String ort = plugin.msgs().raw("ort.schild") + (event.getSide() == Side.BACK ? " (hinten)" : "");
        plugin.moderator().verarbeiten(spieler, ergebnis.ohneHinweis(), ort, zusammen);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimBuch(PlayerEditBookEvent event) {
        Player spieler = event.getPlayer();
        if (!plugin.getConfig().getBoolean("pruefen.buecher", true) || spieler.hasPermission("antiswear.bypass")) {
            return;
        }
        BookMeta buch = event.getNewBookMeta();
        List<String> texte = new ArrayList<>();
        if (event.isSigning() && buch.hasTitle() && buch.getTitle() != null) {
            texte.add(buch.getTitle());
        }
        for (Component seite : buch.pages()) {
            texte.add(PlainTextComponentSerializer.plainText().serialize(seite));
        }
        for (String text : texte) {
            ChatPruefung.Ergebnis ergebnis = plugin.pruefung().pruefenOhneSpam(text);
            if (ergebnis.verstoss()) {
                event.setCancelled(true);
                plugin.msgs().send(spieler, "buch-blockiert");
                plugin.moderator().verarbeiten(spieler, ergebnis.ohneHinweis(), plugin.msgs().raw("ort.buch"), text);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void beimAmboss(PrepareAnvilEvent event) {
        if (!plugin.getConfig().getBoolean("pruefen.amboss", true) || !(event.getView().getPlayer() instanceof Player spieler)
                || spieler.hasPermission("antiswear.bypass")) {
            return;
        }
        String name = event.getView().getRenameText();
        if (name == null || name.isBlank() || event.getResult() == null) {
            return;
        }
        if (plugin.pruefung().pruefenOhneSpam(name).verstoss()) {
            event.setResult(null);
            long jetzt = System.currentTimeMillis();
            Long zuletzt = letzterAmbossHinweis.get(spieler.getUniqueId());
            if (zuletzt == null || jetzt - zuletzt > AMBOSS_HINWEIS_ABSTAND) {
                letzterAmbossHinweis.put(spieler.getUniqueId(), jetzt);
                spieler.sendActionBar(plugin.msgs().format("amboss-blockiert"));
            }
        }
    }
}
