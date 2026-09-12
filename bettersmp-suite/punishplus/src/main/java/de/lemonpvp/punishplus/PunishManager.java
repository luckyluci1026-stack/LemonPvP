package de.lemonpvp.punishplus;

import de.lemonpvp.punishplus.store.Grund;
import de.lemonpvp.punishplus.store.PunishRecord;
import de.lemonpvp.punishplus.util.Durations;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Die eigentliche Offend-/Punish-Logik - von den Befehlen UND von der
 * statischen API (PunishPlusApi) genutzt, damit beide exakt dasselbe tun.
 *
 * Nimmt bewusst nur online Spieler als Ziel, genau wie die meisten
 * anderen Befehle in der Suite (z.B. /freeze): ohne das liesse sich
 * punishplus.exempt bei Offline-Spielern gar nicht zuverlaessig pruefen
 * (OfflinePlayer kennt keine Rechte).
 *
 * Weder /offend noch /punish lassen sich per Befehl aufheben - das ist
 * Absicht, nicht vergessen.
 */
public final class PunishManager {

    public enum Ergebnis { OK, UNBEKANNTER_GRUND, AUSGENOMMEN }

    private final PunishPlus plugin;

    public PunishManager(PunishPlus plugin) {
        this.plugin = plugin;
    }

    /** Temporaere Sperre - Dauer kommt aus dem konfigurierten Grund. */
    public Ergebnis offend(Player spieler, String grundId, String ausfuehrer) {
        return sperren(spieler, grundId, ausfuehrer, "OFFEND", false);
    }

    /** Dauerhafte Sperre - unabhaengig von der im Grund hinterlegten Dauer. */
    public Ergebnis punish(Player spieler, String grundId, String ausfuehrer) {
        return sperren(spieler, grundId, ausfuehrer, "PUNISH", true);
    }

    private Ergebnis sperren(Player spieler, String grundId, String ausfuehrer, String art, boolean immerDauerhaft) {
        if (spieler.hasPermission("punishplus.exempt")) {
            return Ergebnis.AUSGENOMMEN;
        }
        Optional<Grund> grund = plugin.gruende().get(grundId);
        if (grund.isEmpty()) {
            return Ergebnis.UNBEKANNTER_GRUND;
        }
        long dauer = immerDauerhaft ? 0L : grund.get().offendDauerMillis();
        PunishRecord record = plugin.store().sperren(spieler.getUniqueId(), spieler.getName(),
                grund.get(), art, dauer, ausfuehrer);

        String schluessel = immerDauerhaft ? "punish" : "offend";
        String dauerText = record.dauerhaft() ? "dauerhaft" : Durations.humanize(record.bis() - record.von());
        plugin.msgs().send(spieler, schluessel + ".notice", "grund", record.grundText(), "dauer", dauerText);

        for (Player empfaenger : Bukkit.getOnlinePlayers()) {
            plugin.msgs().send(empfaenger, schluessel + ".broadcast",
                    "spieler", spieler.getName(), "ausfuehrer", ausfuehrer, "grund", record.grundText());
        }

        String bisText = record.dauerhaft() ? "" : Durations.humanize(record.bis() - System.currentTimeMillis());
        spieler.kick(plugin.msgs().format(record.dauerhaft() ? "kick.perm" : "kick.temp",
                "grund", record.grundText(), "bis", bisText));

        return Ergebnis.OK;
    }
}
