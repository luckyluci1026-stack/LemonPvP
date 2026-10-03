package de.lemonpvp.bettersmp.home;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class HomeBefehle implements TabExecutor, Listener {

    private final BetterSMP plugin;
    private final HomeSpeicher speicher;
    private final HomeTeleport teleport;
    private final HomeMenue menue;

    public HomeBefehle(BetterSMP plugin, HomeSpeicher speicher, HomeTeleport teleport, HomeMenue menue) {
        this.plugin = plugin;
        this.speicher = speicher;
        this.teleport = teleport;
        this.menue = menue;
    }

    public boolean aktiv() {
        return plugin.getConfig().getBoolean("homes.enabled", true);
    }

    static String befehlFuer(String label) {
        return switch (label.toLowerCase(Locale.ROOT)) {
            case "home", "homes" -> "home";
            case "sethome" -> "sethome";
            case "delhome", "deletehome" -> "delhome";
            default -> null;
        };
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player spieler)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        String befehl = befehlFuer(command.getName());
        if (befehl != null) {
            ausfuehren(spieler, befehl, args);
        }
        return true;
    }

    public void ausfuehren(Player spieler, String befehl, String[] args) {
        if (!aktiv()) {
            plugin.msgs().send(spieler, "homes.aus");
            return;
        }
        if (!spieler.hasPermission("bettersmp.homes")) {
            plugin.msgs().send(spieler, "no-permission");
            return;
        }
        UUID id = spieler.getUniqueId();
        String eingabe = String.join(" ", args).trim();
        switch (befehl) {
            case "home" -> {
                if (eingabe.isEmpty()) {
                    menue.oeffnen(spieler);
                    return;
                }
                Home home = gesucht(spieler, eingabe);
                if (home != null) {
                    teleport.starten(spieler, home);
                }
            }
            case "sethome" -> {
                if (eingabe.isEmpty()) {
                    int frei = speicher.freierPlatz(id);
                    if (frei < 0) {
                        plugin.msgs().send(spieler, "homes.alle-belegt", "anzahl", String.valueOf(speicher.plaetze()));
                        menue.oeffnen(spieler);
                        return;
                    }
                    setzen(spieler, frei, null);
                    return;
                }
                Integer platz = speicher.platzAus(eingabe);
                if (platz != null) {
                    if (platz < 1 || platz > speicher.plaetze()) {
                        plugin.msgs().send(spieler, "homes.platz-ungueltig", "anzahl", String.valueOf(speicher.plaetze()));
                        return;
                    }
                    setzen(spieler, platz, null);
                    return;
                }
                Home gleich = speicher.finden(id, eingabe);
                if (gleich != null) {
                    setzen(spieler, gleich.platz(), gleich.name());
                    return;
                }
                if (!HomeSpeicher.nameGueltig(eingabe)) {
                    plugin.msgs().send(spieler, "homes.ungueltig");
                    return;
                }
                int frei = speicher.freierPlatz(id);
                if (frei < 0) {
                    plugin.msgs().send(spieler, "homes.alle-belegt", "anzahl", String.valueOf(speicher.plaetze()));
                    return;
                }
                setzen(spieler, frei, eingabe);
            }
            case "delhome" -> {
                if (eingabe.isEmpty()) {
                    menue.oeffnen(spieler);
                    return;
                }
                Home home = gesucht(spieler, eingabe);
                if (home != null) {
                    loeschen(spieler, home.platz());
                }
            }
            default -> {
            }
        }
    }

    private Home gesucht(Player spieler, String eingabe) {
        Integer platz = speicher.platzAus(eingabe);
        if (platz != null && (platz < 1 || platz > speicher.plaetze())) {
            plugin.msgs().send(spieler, "homes.platz-ungueltig", "anzahl", String.valueOf(speicher.plaetze()));
            return null;
        }
        Home home = speicher.finden(spieler.getUniqueId(), eingabe);
        if (home == null) {
            plugin.msgs().send(spieler, "homes.unbekannt", "name", de.lemonpvp.bettersmp.util.Text.sicher(eingabe));
        }
        return home;
    }

    public void setzen(Player spieler, int platz, String name) {
        UUID id = spieler.getUniqueId();
        Home alt = speicher.home(id, platz);
        String endName = name != null ? name : alt != null ? alt.name() : HomeSpeicher.standardName(platz);
        speicher.setzen(id, Home.von(platz, endName, spieler.getLocation()));
        plugin.msgs().send(spieler, alt == null ? "homes.gesetzt" : "homes.neu-gesetzt", "name", endName,
                "belegt", String.valueOf(speicher.belegt(id)), "anzahl", String.valueOf(speicher.plaetze()));
    }

    public void loeschen(Player spieler, int platz) {
        Home weg = speicher.loeschen(spieler.getUniqueId(), platz);
        if (weg != null) {
            plugin.msgs().send(spieler, "homes.geloescht", "name", weg.name());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimBefehl(PlayerCommandPreprocessEvent event) {
        String nachricht = event.getMessage();
        if (!aktiv() || nachricht.length() < 2 || nachricht.charAt(0) != '/') {
            return;
        }
        String[] teile = nachricht.substring(1).trim().split("\\s+");
        String befehl = befehlFuer(teile[0]);
        if (befehl == null) {
            return;
        }
        event.setCancelled(true);
        ausfuehren(event.getPlayer(), befehl, Arrays.copyOfRange(teile, 1, teile.length));
    }

    @EventHandler
    public void beimJoin(PlayerJoinEvent event) {
        if (!aktiv()) {
            return;
        }
        Player spieler = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!spieler.isOnline()) {
                return;
            }
            int importiert = speicher.beimJoin(spieler.getUniqueId());
            if (importiert == 1) {
                Home home = speicher.home(spieler.getUniqueId(), 1);
                plugin.msgs().send(spieler, "homes.uebernommen", "name", home == null ? HomeSpeicher.standardName(1) : home.name(),
                        "anzahl", String.valueOf(speicher.plaetze()));
            } else if (importiert > 1) {
                plugin.msgs().send(spieler, "homes.uebernommen-mehrere", "zahl", String.valueOf(importiert),
                        "anzahl", String.valueOf(speicher.plaetze()));
            }
        }, 60L);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1 || !(sender instanceof Player spieler) || !aktiv()) {
            return List.of();
        }
        String anfang = args[0].toLowerCase(Locale.ROOT);
        List<String> vorschlaege = new ArrayList<>();
        boolean setzen = "sethome".equals(befehlFuer(command.getName()));
        for (int platz = 1; platz <= speicher.plaetze(); platz++) {
            Home home = speicher.home(spieler.getUniqueId(), platz);
            if (home == null) {
                if (setzen) {
                    vorschlaege.add(String.valueOf(platz));
                }
                continue;
            }
            vorschlaege.add(home.name().equals(HomeSpeicher.standardName(platz)) || home.name().contains(" ")
                    ? String.valueOf(platz) : home.name());
        }
        vorschlaege.removeIf(v -> !v.toLowerCase(Locale.ROOT).startsWith(anfang));
        return vorschlaege;
    }
}
