package de.lemonpvp.smplobby.hide;

import de.lemonpvp.smplobby.SMPLobby;
import de.lemonpvp.smplobby.items.LobbyItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Andere Spieler ausblenden.
 *
 * In einer Lobby mit dreissig Leuten sieht man den Weg nicht mehr. Ein
 * Klick, und alle sind weg - nur fuer den, der geklickt hat.
 *
 * Zwei Stellen, an die man leicht nicht denkt: Wer neu ankommt, muss fuer
 * alle, die gerade versteckt haben, ebenfalls unsichtbar sein - sonst
 * tauchen nach und nach doch wieder Leute auf. Und wer geht, muss aus der
 * Liste raus, sonst waechst sie bis zum naechsten Neustart.
 */
public final class HideModule {

    private final SMPLobby plugin;
    private final Set<UUID> versteckend = new HashSet<>();

    public HideModule(SMPLobby plugin) {
        this.plugin = plugin;
    }

    public boolean aktiv() {
        return plugin.getConfig().getBoolean("verstecken.aktiv", true);
    }

    public boolean verstecktGerade(Player spieler) {
        return versteckend.contains(spieler.getUniqueId());
    }

    /** Umschalten und die Anzeige im Gegenstand mitfuehren. */
    public void schalte(Player spieler) {
        if (verstecktGerade(spieler)) {
            versteckend.remove(spieler.getUniqueId());
            for (Player anderer : Bukkit.getOnlinePlayers()) {
                spieler.showPlayer(plugin, anderer);
            }
            zeigeGegenstand(spieler, false);
            spieler.sendMessage(plugin.msgs().format("verstecken-aus"));
        } else {
            versteckend.add(spieler.getUniqueId());
            for (Player anderer : Bukkit.getOnlinePlayers()) {
                if (!anderer.equals(spieler)) {
                    spieler.hidePlayer(plugin, anderer);
                }
            }
            zeigeGegenstand(spieler, true);
            spieler.sendMessage(plugin.msgs().format("verstecken-an"));
        }
    }

    /** Ein neu angekommener Spieler bleibt fuer alle Versteckenden unsichtbar. */
    public void beiAnkunft(Player neuer) {
        for (Player anderer : Bukkit.getOnlinePlayers()) {
            if (!anderer.equals(neuer) && verstecktGerade(anderer)) {
                anderer.hidePlayer(plugin, neuer);
            }
        }
    }

    public void beimGehen(Player spieler) {
        versteckend.remove(spieler.getUniqueId());
    }

    public void alleVergessen() {
        versteckend.clear();
    }

    private void zeigeGegenstand(Player spieler, boolean versteckt) {
        LobbyItems items = plugin.items();
        if (!items.aktiv()) {
            return;
        }
        // Aussehen und Text kommen aus derselben Liste wie beim Anlegen,
        // damit hier nicht ein zweites Mal steht, wie das Item heisst.
        for (var roh : plugin.getConfig().getMapList("gegenstaende.liste")) {
            if (!LobbyItems.AKTION_VERSTECKEN.equals(String.valueOf(roh.get("aktion")))) {
                continue;
            }
            Material normal = Material.matchMaterial(
                    String.valueOf(roh.get("material")).toUpperCase(Locale.ROOT));
            Material aus = Material.matchMaterial(plugin.getConfig()
                    .getString("verstecken.material-versteckt", "GRAY_DYE")
                    .toUpperCase(Locale.ROOT));
            Material zeigen = versteckt ? (aus == null ? Material.GRAY_DYE : aus)
                                        : (normal == null ? Material.PLAYER_HEAD : normal);
            String name = versteckt
                    ? "<gray><bold>Spieler ausgeblendet</bold></gray>"
                    : String.valueOf(roh.getOrDefault("name", "Spieler anzeigen"));
            items.tausche(spieler, LobbyItems.AKTION_VERSTECKEN, zeigen, name,
                    List.of(versteckt
                            ? "<gray>Rechtsklick zeigt sie wieder</gray>"
                            : "<gray>Rechtsklick blendet die anderen aus</gray>"));
            return;
        }
    }
}
