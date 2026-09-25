package de.lemonpvp.duelplus.request;

import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.db.DuelRecord;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Die direkten, per Befehl ausgeloesten Aktionen: eine neue
 * Herausforderung anlegen, eine bestehende annehmen oder ablehnen. Was
 * danach passiert (dem Ziel die Anfrage zeigen, bei Annahme zur Arena
 * schicken, ...) macht AnfragePollTask im Hintergrund - das hier prueft
 * nur die Voraussetzungen und schreibt den Zustand in die Datenbank.
 */
public final class AnfrageManager {

    private final DuelPlus plugin;

    public AnfrageManager(DuelPlus plugin) {
        this.plugin = plugin;
    }

    public void anfordern(Player herausforderer, String zielName) {
        if (zielName.equalsIgnoreCase(herausforderer.getName())) {
            plugin.msgs().send(herausforderer, "self");
            return;
        }
        int veraltet = plugin.getConfig().getInt("anfrage.timeout-sekunden", 60) + 30;
        plugin.db().presenceUuidFuerName(zielName, veraltet).thenAccept(zielUuid -> {
            if (zielUuid.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.msgs().send(herausforderer, "target-offline", "spieler", zielName));
                return;
            }
            pruefeUndErstelle(herausforderer, zielUuid.get(), zielName, veraltet);
        });
    }

    private void pruefeUndErstelle(Player herausforderer, UUID zielUuid, String zielName, int veraltet) {
        plugin.db().istBeschaeftigt(herausforderer.getUniqueId()).thenAccept(herausfordererBeschaeftigt -> {
            if (herausfordererBeschaeftigt) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.msgs().send(herausforderer, "already-pending", "spieler", zielName));
                return;
            }
            plugin.db().istBeschaeftigt(zielUuid).thenAccept(zielBeschaeftigt -> {
                if (zielBeschaeftigt) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            plugin.msgs().send(herausforderer, "target-busy", "spieler", zielName));
                    return;
                }
                plugin.db().presenceServerVon(zielUuid, veraltet).thenAccept(zielServer -> {
                    if (zielServer.isEmpty()) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                plugin.msgs().send(herausforderer, "target-offline", "spieler", zielName));
                        return;
                    }
                    DuelRecord anfrage = new DuelRecord(
                            UUID.randomUUID().toString(),
                            herausforderer.getUniqueId(), herausforderer.getName(), plugin.serverName(),
                            zielUuid, zielName, zielServer.get(),
                            DuelRecord.WARTEND, null, null, System.currentTimeMillis(),
                            false, false, false);
                    plugin.db().anfrageErstellen(anfrage).thenRun(() ->
                            Bukkit.getScheduler().runTask(plugin, () -> plugin.msgs().send(herausforderer, "sent",
                                    "spieler", zielName,
                                    "timeout", plugin.getConfig().getInt("anfrage.timeout-sekunden", 60) + "s")));
                });
            });
        });
    }

    public void annehmen(Player ziel, String herausfordererName) {
        aufloesenUndWechseln(ziel, herausfordererName, DuelRecord.ANGENOMMEN, () ->
                plugin.msgs().send(ziel, "accepted-both"));
    }

    public void ablehnen(Player ziel, String herausfordererName) {
        aufloesenUndWechseln(ziel, herausfordererName, DuelRecord.ABGELEHNT, () ->
                plugin.msgs().send(ziel, "declined-confirm", "spieler", herausfordererName));
    }

    private void aufloesenUndWechseln(Player ziel, String herausfordererName, String neuerStatus, Runnable beiErfolg) {
        int veraltet = plugin.getConfig().getInt("anfrage.timeout-sekunden", 60) + 30;
        plugin.db().presenceUuidFuerName(herausfordererName, veraltet).thenAccept(herausfordererUuid -> {
            if (herausfordererUuid.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.msgs().send(ziel, "no-pending", "spieler", herausfordererName));
                return;
            }
            plugin.db().offeneAnfrageZwischen(ziel.getUniqueId(), herausfordererUuid.get()).thenAccept(anfrageOpt -> {
                Optional<DuelRecord> passend = anfrageOpt.filter(a -> a.spielerB().equals(ziel.getUniqueId()));
                if (passend.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            plugin.msgs().send(ziel, "no-pending", "spieler", herausfordererName));
                    return;
                }
                plugin.db().statusWechseln(passend.get().id(), DuelRecord.WARTEND, neuerStatus).thenAccept(erfolg -> {
                    if (erfolg) {
                        Bukkit.getScheduler().runTask(plugin, beiErfolg);
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                plugin.msgs().send(ziel, "no-pending", "spieler", herausfordererName));
                    }
                });
            });
        });
    }
}
