package de.lemonpvp.duelplus.request;

import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.db.DuelRecord;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

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
        entscheiden(ziel, herausfordererName, DuelRecord.ANGENOMMEN, anfrage ->
                plugin.msgs().send(ziel, "accepted-both"));
    }

    public void ablehnen(Player ziel, String herausfordererName) {
        entscheiden(ziel, herausfordererName, DuelRecord.ABGELEHNT, anfrage ->
                plugin.msgs().send(ziel, "declined-confirm", "spieler", anfrage.spielerAName()));
    }

    private void entscheiden(Player ziel, String herausfordererName, String neuerStatus, Consumer<DuelRecord> beiErfolg) {
        plugin.db().offeneAnfragenFuerZiel(ziel.getUniqueId()).thenAccept(offen -> {
            Optional<DuelRecord> passend = auswaehlen(offen, herausfordererName);
            if (passend.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> keineAnfrage(ziel, herausfordererName, offen));
                return;
            }
            DuelRecord anfrage = passend.get();
            plugin.db().statusWechseln(anfrage.id(), DuelRecord.WARTEND, neuerStatus).thenAccept(erfolg ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (erfolg) {
                            beiErfolg.accept(anfrage);
                        } else {
                            plugin.msgs().send(ziel, "no-pending", "spieler", anfrage.spielerAName());
                        }
                    }));
        });
    }

    private void keineAnfrage(Player ziel, String herausfordererName, List<DuelRecord> offen) {
        if (offen.isEmpty()) {
            plugin.msgs().send(ziel, herausfordererName == null ? "no-pending-any" : "no-pending",
                    "spieler", herausfordererName == null ? "" : herausfordererName);
            return;
        }
        plugin.msgs().send(ziel, "no-pending", "spieler", herausfordererName);
        for (DuelRecord anfrage : offen) {
            plugin.msgs().send(ziel, "pending-entry", "spieler", anfrage.spielerAName());
        }
    }

    public static Optional<DuelRecord> auswaehlen(List<DuelRecord> offen, String name) {
        if (offen.isEmpty()) {
            return Optional.empty();
        }
        if (name == null || name.isBlank()) {
            return Optional.of(offen.get(0));
        }
        for (DuelRecord anfrage : offen) {
            if (anfrage.spielerAName().equalsIgnoreCase(name)) {
                return Optional.of(anfrage);
            }
        }
        String gesucht = ohnePunkt(name);
        for (DuelRecord anfrage : offen) {
            if (ohnePunkt(anfrage.spielerAName()).equalsIgnoreCase(gesucht)) {
                return Optional.of(anfrage);
            }
        }
        return Optional.empty();
    }

    private static String ohnePunkt(String name) {
        return name.startsWith(".") ? name.substring(1) : name;
    }
}
