package de.lemonpvp.smplobby.board;

import de.lemonpvp.smplobby.SMPLobby;
import de.lemonpvp.smplobby.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Die Anzeigetafel am rechten Bildschirmrand.
 *
 * Sie flackert nicht, und das ist der ganze Aufwand hier wert: Wuerde man
 * die Zeilen bei jeder Aktualisierung loeschen und neu setzen, blinkte
 * die Tafel jede Sekunde. Stattdessen bekommt jede Zeile einen festen,
 * unsichtbaren Eintrag (eine Farbcode-Kombination, die nie zweimal
 * vorkommt) und ein Team; geaendert wird nur noch der Text im Team.
 *
 * Dasselbe macht BoardManager im BetterSMP - hier nur kleiner, weil eine
 * Lobby keine Statistiken anzeigt.
 */
public final class LobbyBoard {

    /** Unsichtbare, eindeutige Eintraege - einer je Zeile. */
    private static final char[] ZEICHEN = "0123456789abcdef".toCharArray();

    private final SMPLobby plugin;
    private final Map<UUID, Scoreboard> tafeln = new HashMap<>();
    private final Map<UUID, Integer> zeilenZahl = new HashMap<>();

    private BukkitTask takt;

    public LobbyBoard(SMPLobby plugin) {
        this.plugin = plugin;
    }

    public boolean aktiv() {
        return plugin.getConfig().getBoolean("tafel.aktiv", true);
    }

    public void start() {
        if (!aktiv()) {
            return;
        }
        for (Player spieler : Bukkit.getOnlinePlayers()) {
            baueAuf(spieler);
        }
        long intervall = Math.max(10L, plugin.getConfig().getLong("tafel.takt", 20L));
        takt = Bukkit.getScheduler().runTaskTimer(plugin, this::alleAuffrischen, 20L, intervall);
    }

    public void stop() {
        if (takt != null) {
            takt.cancel();
            takt = null;
        }
        for (Player spieler : Bukkit.getOnlinePlayers()) {
            spieler.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        tafeln.clear();
        zeilenZahl.clear();
    }

    public void baueAuf(Player spieler) {
        if (!aktiv()) {
            return;
        }
        Scoreboard tafel = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective ziel = tafel.registerNewObjective("lobby", Criteria.DUMMY,
                Text.mm(plugin.getConfig().getString("tafel.titel", "Lobby")));
        ziel.setDisplaySlot(DisplaySlot.SIDEBAR);
        tafeln.put(spieler.getUniqueId(), tafel);
        spieler.setScoreboard(tafel);
        frischeAuf(spieler);
    }

    public void entferne(Player spieler) {
        tafeln.remove(spieler.getUniqueId());
        zeilenZahl.remove(spieler.getUniqueId());
    }

    private void alleAuffrischen() {
        for (Player spieler : Bukkit.getOnlinePlayers()) {
            frischeAuf(spieler);
        }
    }

    private void frischeAuf(Player spieler) {
        Scoreboard tafel = tafeln.get(spieler.getUniqueId());
        if (tafel == null) {
            return;
        }
        Objective ziel = tafel.getObjective("lobby");
        if (ziel == null) {
            return;
        }
        List<String> zeilen = plugin.getConfig().getStringList("tafel.zeilen");
        int anzahl = Math.min(zeilen.size(), ZEICHEN.length);

        // Wurde die Tafel kuerzer, muessen die alten Zeilen weg - sonst
        // stehen sie fuer immer unten drunter.
        int vorher = zeilenZahl.getOrDefault(spieler.getUniqueId(), 0);
        for (int i = anzahl; i < vorher; i++) {
            tafel.resetScores(eintrag(i));
            Team alt = tafel.getTeam("zeile" + i);
            if (alt != null) {
                alt.unregister();
            }
        }
        zeilenZahl.put(spieler.getUniqueId(), anzahl);

        for (int i = 0; i < anzahl; i++) {
            String text = ersetze(zeilen.get(i), spieler);
            Team team = tafel.getTeam("zeile" + i);
            if (team == null) {
                team = tafel.registerNewTeam("zeile" + i);
                team.addEntry(eintrag(i));
            }
            team.prefix(Text.mm(text));
            // Oben steht die hoechste Punktzahl - deshalb rueckwaerts.
            ziel.getScore(eintrag(i)).setScore(anzahl - i);
        }
    }

    /**
     * Ein unsichtbarer, eindeutiger Eintrag je Zeile.
     *
     * Zwei Zeilen mit demselben Text waeren fuer Minecraft derselbe
     * Eintrag, und die zweite fiele weg. Ein Farbcode ist unsichtbar und
     * macht sie trotzdem unterscheidbar.
     */
    private static String eintrag(int nummer) {
        return "§" + ZEICHEN[nummer % ZEICHEN.length] + "§r";
    }

    private String ersetze(String text, Player spieler) {
        int netzwerk = plugin.proxy().netzwerkZahl();
        return text
                .replace("%name%", spieler.getName())
                .replace("%spieler%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%netzwerk%", netzwerk < 0 ? "–" : String.valueOf(netzwerk))
                .replace("%ping%", String.valueOf(spieler.getPing()));
    }
}
