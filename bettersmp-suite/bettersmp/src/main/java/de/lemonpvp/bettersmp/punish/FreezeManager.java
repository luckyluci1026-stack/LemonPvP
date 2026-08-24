package de.lemonpvp.bettersmp.punish;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wer gerade eingefroren ist, und seit wann.
 *
 * Bewusst nur im Arbeitsspeicher: Ein Einfrieren soll die Ausnahme fuer
 * die naechsten paar Minuten sein, waehrend ein Teammitglied sich einen
 * Verdachtsfall ansieht - keine Strafe, die einen Serverneustart
 * ueberleben muesste. Nach einem Neustart ist jeder wieder frei, und
 * das ist richtig so.
 */
public final class FreezeManager {

    private final BetterSMP plugin;
    /** UUID -> Zeitpunkt des Einfrierens (Unix-Millisekunden). */
    private final Map<UUID, Long> eingefroren = new ConcurrentHashMap<>();
    private BukkitTask task;

    public FreezeManager(BetterSMP plugin) {
        this.plugin = plugin;
    }

    public boolean istEingefroren(UUID spieler) {
        return eingefroren.containsKey(spieler);
    }

    /** @return true, wenn danach eingefroren ist (vorher war er es nicht) */
    public boolean umschalten(UUID spieler) {
        if (eingefroren.remove(spieler) != null) {
            return false;
        }
        eingefroren.put(spieler, System.currentTimeMillis());
        return true;
    }

    public void vergessen(UUID spieler) {
        eingefroren.remove(spieler);
    }

    /** Alle aktuell eingefrorenen UUIDs mit Startzeitpunkt - fuer /freeze ohne Ziel. */
    public Map<UUID, Long> alle() {
        return Map.copyOf(eingefroren);
    }

    /**
     * Actionbar-Erinnerung, 1x pro Sekunde - gleiches Muster wie
     * CombatManager. Ohne sie merkt eine eingefrorene Person nur an den
     * stumm ins Leere laufenden Aktionen, dass etwas nicht stimmt.
     */
    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void tick() {
        if (eingefroren.isEmpty() || !plugin.getConfig().getBoolean("freeze.actionbar", true)) {
            return;
        }
        for (UUID id : eingefroren.keySet()) {
            Player spieler = Bukkit.getPlayer(id);
            if (spieler != null) {
                spieler.sendActionBar(plugin.msgs().format("freeze.actionbar"));
            }
        }
    }
}
