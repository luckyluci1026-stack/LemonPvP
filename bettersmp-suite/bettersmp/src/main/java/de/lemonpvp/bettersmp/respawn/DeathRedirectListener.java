package de.lemonpvp.bettersmp.respawn;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Nach dem Tod zurueck in die Lobby, statt am Bett/Weltspawn weiterzuspielen.
 *
 * Bewusst AUS in der Standard-Config: Das hier ist etwas, das dieser
 * eine Schulserver so will, nicht jedes SMP, das dieses Plugin
 * einsetzt. Wer nichts eintraegt, merkt von diesem Modul nichts.
 *
 * Die eigentliche Uebergabe laeuft ueber den "BungeeCord"-Kanal - denselben,
 * ueber den auch SMPLobbys Serverwaehler mit dem Proxy redet (siehe dort
 * ProxyBridge.verbinde()). Ein "Connect" mit dem Zielservernamen reicht;
 * den Rest (Spieler auf den anderen Server bringen) macht der Proxy.
 *
 * Warum ueber PlayerRespawnEvent und nicht ueber PlayerDeathEvent: Das
 * Todesevent laeuft, bevor der Spieler ueberhaupt wieder eine Welt hat -
 * ein Serverwechsel waere dort verfrueht. PlayerRespawnEvent ist der
 * Moment, in dem der Spieler wieder wirklich im Spiel steht.
 */
public final class DeathRedirectListener implements Listener {

    /** Derselbe Kanalname wie bei SMPLobbys ProxyBridge - Velocity kennt ihn immer. */
    private static final String KANAL = "BungeeCord";

    private final BetterSMP plugin;

    public DeathRedirectListener(BetterSMP plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("death-redirect.enabled", false);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!enabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("bettersmp.deathredirect.bypass")) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        String ziel = plugin.getConfig().getString("death-redirect.server", "lobby");
        if (ziel == null || ziel.isBlank()) {
            return;
        }
        long verzoegerung = Math.max(0, plugin.getConfig().getLong("death-redirect.delay-ticks", 40));

        plugin.msgs().send(player, "death-redirect.notice");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Zwischen dem Respawn und jetzt koennen ein paar Sekunden liegen -
            // in der Zeit kann der Spieler laengst wieder gegangen sein.
            if (player.isOnline()) {
                sendeConnect(player, ziel);
            }
        }, verzoegerung);
    }

    private void sendeConnect(Player player, String server) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream aus = new DataOutputStream(bytes);
            aus.writeUTF("Connect");
            aus.writeUTF(server);
            player.sendPluginMessage(plugin, KANAL, bytes.toByteArray());
        } catch (IOException fehler) {
            plugin.getLogger().warning("Tod-Umleitung fuer " + player.getName()
                    + " ist fehlgeschlagen: " + fehler.getMessage());
        }
    }
}
