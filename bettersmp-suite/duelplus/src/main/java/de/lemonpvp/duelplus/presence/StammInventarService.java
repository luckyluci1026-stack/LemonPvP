package de.lemonpvp.duelplus.presence;

import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.db.SpielerSnapshot;
import de.lemonpvp.duelplus.loot.Nachlieferung;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * NUR auf dem Server mit ist-loot-quelle: true (in der Praxis: SMP)
 * registriert. Haelt duelplus_stamm_inventar als staendig aktuellen
 * Spiegel des ECHTEN Inventars - das schickt AnfragePollTask tatsaechlich
 * mit in die Arena, wenn von einem ANDEREN Server aus (z.B. der Lobby)
 * herausgefordert wird, statt des dort lokalen, meist leeren/anderen
 * Live-Inventars.
 *
 * Genauso wichtig wie der Weg HIN: ein Duell-Ergebnis, das entstanden
 * ist, waehrend man NICHT auf der Loot-Quelle war (siehe AnfragePollTask.
 * ergebnisAnwenden), landet ebenfalls nur im Spiegel - erst beim
 * naechsten Beitritt HIER wird es wirklich ins echte Inventar
 * uebernommen (beimJoin unten).
 *
 * Gleiches Herzschlag-Muster wie PresenceService: ein Quit alleine
 * wuerde bei einem Absturz ohne sauberes PlayerQuitEvent fuer immer
 * veraltet stehen bleiben.
 */
public final class StammInventarService implements Listener {

    private static final int HERZSCHLAG_SEKUNDEN = 30;
    private static final long JOIN_SPERRE_MILLIS = 15_000L;

    private final DuelPlus plugin;
    private final ArrayDeque<UUID> reihe = new ArrayDeque<>();
    private int proSekunde = 1;

    public StammInventarService(DuelPlus plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::herzschlag, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void beimJoin(PlayerJoinEvent event) {
        Player spieler = event.getPlayer();
        plugin.inventarSperre().sperren(spieler.getUniqueId(), JOIN_SPERRE_MILLIS);
        plugin.db().stammInventarLesen(spieler.getUniqueId()).thenAccept(snapshotOpt ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!spieler.isOnline()) {
                        return;
                    }
                    // Leerer Spiegel (z.B. beim allerersten Login) heisst:
                    // nichts zu uebernehmen, das frisch geladene Inventar
                    // bleibt einfach wie es ist.
                    snapshotOpt.ifPresent(snap -> snap.anwenden(spieler.getInventory()));
                    sichern(spieler);
                    nachlieferungZustellen(spieler);
                    plugin.inventarSperre().freigebenWennNichtsOffen(spieler.getUniqueId());
                }));
    }

    private void nachlieferungZustellen(Player spieler) {
        plugin.db().nachlieferungLesen(spieler.getUniqueId()).thenAccept(eintraege -> {
            if (eintraege.isEmpty()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!spieler.isOnline()) {
                    return;
                }
                List<ItemStack> alle = new ArrayList<>();
                eintraege.values().forEach(alle::addAll);
                plugin.db().nachlieferungLoeschen(eintraege.keySet());
                Nachlieferung.zustellen(plugin, spieler, alle);
                sichern(spieler);
            });
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimVerlassen(PlayerQuitEvent event) {
        sichern(event.getPlayer());
        plugin.db().stammVergessen(event.getPlayer().getUniqueId());
    }

    private void herzschlag() {
        if (reihe.isEmpty()) {
            for (Player spieler : Bukkit.getOnlinePlayers()) {
                reihe.add(spieler.getUniqueId());
            }
            proSekunde = Math.max(1, (reihe.size() + HERZSCHLAG_SEKUNDEN - 1) / HERZSCHLAG_SEKUNDEN);
        }
        for (int i = 0; i < proSekunde && !reihe.isEmpty(); i++) {
            Player spieler = Bukkit.getPlayer(reihe.poll());
            if (spieler != null) {
                plugin.db().stammInventarSchreiben(spieler.getUniqueId(), SpielerSnapshot.von(spieler.getInventory()), false);
            }
        }
    }

    public void allesSichern() {
        for (Player spieler : Bukkit.getOnlinePlayers()) {
            sichern(spieler);
        }
    }

    private void sichern(Player spieler) {
        plugin.db().stammInventarSchreiben(spieler.getUniqueId(), SpielerSnapshot.von(spieler.getInventory()));
    }
}
