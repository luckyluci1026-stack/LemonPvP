package de.lemonpvp.smplobby.join;

import de.lemonpvp.smplobby.SMPLobby;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.time.Duration;

/**
 * Ankommen, Gehen, Hinunterfallen.
 *
 * Was hier zusammenkommt, gehoert zusammen: Wer die Lobby betritt, soll
 * am selben Punkt stehen, dieselben Gegenstaende haben und in demselben
 * Zustand sein - egal ob er neu ist, aus einem SMP zurueckkommt oder
 * gerade in die Leere gefallen ist.
 */
public final class JoinListener implements Listener {

    private final SMPLobby plugin;

    public JoinListener(SMPLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void beiAnkunft(PlayerJoinEvent ereignis) {
        Player spieler = ereignis.getPlayer();

        if (!plugin.getConfig().getBoolean("ankunft.ankunft-melden", false)) {
            ereignis.joinMessage(null);
        } else {
            ereignis.joinMessage(plugin.msgs().format("ankunft-melden",
                    "spieler", spieler.getName()));
        }

        bereite(spieler);

        if (plugin.getConfig().getBoolean("ankunft.titel-aktiv", true)) {
            spieler.showTitle(Title.title(
                    plugin.msgs().format("ankunft-titel"),
                    plugin.msgs().format("ankunft-untertitel"),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3),
                            Duration.ofMillis(700))));
        }
        spieler.sendMessage(plugin.msgs().format("ankunft-chat", "spieler", spieler.getName()));

        String geraeusch = plugin.getConfig().getString("ankunft.geraeusch", "");
        if (geraeusch != null && !geraeusch.isEmpty()) {
            spieler.playSound(Sound.sound(Key.key(geraeusch), Sound.Source.MASTER, 0.6f, 1.0f));
        }

        plugin.hide().beiAnkunft(spieler);
        plugin.board().baueAuf(spieler);
    }

    /** Der immer gleiche Ausgangszustand. */
    public void bereite(Player spieler) {
        if (plugin.getConfig().getBoolean("spawn.bei-ankunft", true)) {
            plugin.spawn().bringe(spieler);
        }
        if (spieler.getGameMode() != GameMode.CREATIVE
                && spieler.getGameMode() != GameMode.SPECTATOR) {
            spieler.setGameMode(GameMode.ADVENTURE);
        }
        // getMaxHealth() ist seit Jahren veraltet; in 1.21 fuehrt der Weg
        // ueber das Attribut. Ist es nicht da, bleibt die Gesundheit, wie
        // sie ist - besser als eine Ausnahme mitten im Ankommen.
        AttributeInstance maximum = spieler.getAttribute(Attribute.MAX_HEALTH);
        if (maximum != null) {
            spieler.setHealth(maximum.getValue());
        }
        spieler.setFoodLevel(20);
        spieler.setSaturation(20f);
        spieler.setFireTicks(0);
        spieler.setLevel(0);
        spieler.setExp(0f);
        plugin.items().gib(spieler);
        plugin.doppelsprung().erlaube(spieler);
    }

    @EventHandler
    public void beimGehen(PlayerQuitEvent ereignis) {
        if (!plugin.getConfig().getBoolean("ankunft.ankunft-melden", false)) {
            ereignis.quitMessage(null);
        } else {
            ereignis.quitMessage(plugin.msgs().format("abgang-melden",
                    "spieler", ereignis.getPlayer().getName()));
        }
        plugin.hide().beimGehen(ereignis.getPlayer());
        plugin.board().entferne(ereignis.getPlayer());
    }

    @EventHandler
    public void beimWiederbeleben(PlayerRespawnEvent ereignis) {
        if (plugin.getConfig().getBoolean("spawn.bei-tod", true) && plugin.spawn().gesetzt()) {
            ereignis.setRespawnLocation(plugin.spawn().ort());
        }
    }

    /**
     * Wer hinunterfaellt, kommt zurueck statt zu sterben.
     *
     * Geprueft wird nur bei einem Blockwechsel und nur die Hoehe - das
     * laeuft bei jedem Schritt jedes Spielers, da gehoert nichts Teures
     * hinein.
     */
    @EventHandler
    public void beiBewegung(PlayerMoveEvent ereignis) {
        double grenze = plugin.getConfig().getDouble("spawn.rettungshoehe", 0);
        if (ereignis.getTo().getY() > grenze) {
            return;
        }
        Player spieler = ereignis.getPlayer();
        if (!plugin.spawn().bringe(spieler)) {
            return;
        }
        spieler.setFallDistance(0f);
        plugin.doppelsprung().erlaube(spieler);
    }
}
