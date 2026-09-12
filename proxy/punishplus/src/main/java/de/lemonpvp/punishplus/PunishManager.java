package de.lemonpvp.punishplus;

import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.punishplus.store.Grund;
import de.lemonpvp.punishplus.store.PunishRecord;
import de.lemonpvp.punishplus.util.Durations;

import java.util.Optional;

/**
 * Die eigentliche Offend-/Punish-Logik - von den Befehlen UND von der
 * statischen API (PunishPlusApi) genutzt, damit beide exakt dasselbe tun.
 *
 * Nimmt bewusst nur online Spieler als Ziel: ohne das liesse sich
 * punishplus.exempt bei Offline-Spielern gar nicht zuverlaessig pruefen.
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
        long jetzt = System.currentTimeMillis();
        long bis = dauer <= 0 ? 0L : jetzt + dauer;
        PunishRecord record = new PunishRecord(spieler.getUniqueId(), spieler.getUsername(),
                grund.get().id(), grund.get().text(), art, jetzt, bis, ausfuehrer);
        plugin.store().speichern(record);

        String schluessel = immerDauerhaft ? "punish" : "offend";
        String dauerText = record.dauerhaft() ? plugin.config().permanentWord()
                : Durations.humanize(record.bis() - record.von());
        spieler.sendMessage(plugin.message(schluessel + "-notice",
                "%grund%", record.grundText(), "%dauer%", dauerText));

        for (Player empfaenger : plugin.proxy().getAllPlayers()) {
            empfaenger.sendMessage(plugin.message(schluessel + "-broadcast",
                    "%spieler%", spieler.getUsername(), "%ausfuehrer%", ausfuehrer, "%grund%", record.grundText()));
        }

        String bisText = record.dauerhaft() ? "" : Durations.humanize(record.bis() - System.currentTimeMillis());
        spieler.disconnect(plugin.screen(record.dauerhaft() ? "kick-perm" : "kick-temp",
                "%grund%", record.grundText(), "%bis%", bisText));

        return Ergebnis.OK;
    }
}
