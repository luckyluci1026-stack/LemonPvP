package de.lemonpvp.dbwipe;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * /dbwipe - nur ueber die Serverkonsole nutzbar. Drei Warnungen, jede
 * einzeln per erneutem /dbwipe zu bestaetigen (60 Sekunden Zeit
 * zwischen den Schritten, danach faengt es von vorne an); die dritte
 * verlangt zusaetzlich das in der config.yml hinterlegte Passwort
 * direkt als Argument.
 *
 * Erst danach passiert wirklich etwas - und auch dann nur, nachdem ein
 * vollstaendiges Backup als .tar.gz erfolgreich geschrieben wurde
 * (siehe DatabaseDumper.sichern()). Schlaegt das Backup fehl, wird
 * NICHTS geloescht.
 */
public final class WipeCommand implements CommandExecutor {

    private static final long ABLAUF_MILLIS = 60_000L;

    private final DBWipe plugin;
    private final DatabaseDumper dumper;
    private int stufe = 0;
    private long letzteAktion = 0L;

    public WipeCommand(DBWipe plugin) {
        this.plugin = plugin;
        this.dumper = new DatabaseDumper(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("Dieser Befehl ist nur ueber die Serverkonsole nutzbar.");
            return true;
        }
        if (System.currentTimeMillis() - letzteAktion > ABLAUF_MILLIS) {
            stufe = 0;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("cancel")) {
            stufe = 0;
            plugin.getLogger().info("DBWipe abgebrochen.");
            return true;
        }

        if (stufe < 3) {
            stufe++;
            letzteAktion = System.currentTimeMillis();
            warnung(stufe);
            return true;
        }

        // stufe == 3: hier wird das Passwort erwartet.
        String erwartet = plugin.getConfig().getString("passwort", "");
        if (args.length < 1 || erwartet == null || erwartet.isBlank() || !erwartet.equals(args[0])) {
            plugin.getLogger().severe("DBWipe: falsches oder fehlendes Passwort - Vorgang abgebrochen. "
                    + "/dbwipe cancel war nicht noetig, das Abbrechen ist bereits passiert.");
            stufe = 0;
            return true;
        }

        stufe = 0;
        ausfuehren();
        return true;
    }

    private void warnung(int stufe) {
        switch (stufe) {
            case 1 -> plugin.getLogger().warning("""

                    ==================== DBWIPE - WARNUNG 1/3 ====================
                    Dieser Befehl kann die GESAMTE Datenbank unwiderruflich leeren -
                    nicht nur die Tabellen dieses Plugins, WIRKLICH ALLE (jedes
                    installierte Plugin verliert dadurch seine Daten).
                    Zum Fortfahren /dbwipe erneut eingeben (Ablauf nach 60s).
                    Zum Abbrechen: /dbwipe cancel
                    ================================================================
                    """);
            case 2 -> plugin.getLogger().warning("""

                    ==================== DBWIPE - WARNUNG 2/3 ====================
                    Es wird zwar vorher automatisch ein vollstaendiges Backup als
                    .tar.gz erstellt (ALLE Tabellen, nicht nur die dieses Plugins) -
                    aber DANACH sind alle Tabellen aus der Datenbank tatsaechlich weg.
                    Zum Fortfahren /dbwipe ein drittes Mal eingeben (Ablauf nach 60s).
                    Zum Abbrechen: /dbwipe cancel
                    ================================================================
                    """);
            case 3 -> plugin.getLogger().warning("""

                    ================= DBWIPE - LETZTE WARNUNG 3/3 =================
                    Das ist der Punkt ohne Wiederkehr. Um JETZT wirklich fortzufahren
                    (Backup wird zuerst erstellt, danach werden ALLE Tabellen
                    geloescht), innerhalb von 60 Sekunden eingeben:
                        /dbwipe <Passwort>
                    Zum Abbrechen: /dbwipe cancel
                    ================================================================
                    """);
            default -> {
                // Unerreichbar - stufe ist hier immer 1, 2 oder 3.
            }
        }
    }

    private void ausfuehren() {
        plugin.getLogger().warning("DBWipe: Passwort korrekt - erstelle zuerst ein vollstaendiges Backup ...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File backup;
            try {
                backup = dumper.sichern();
            } catch (Exception e) {
                plugin.getLogger().severe("DBWipe: Backup fehlgeschlagen (" + e.getMessage()
                        + ") - ES WURDE NICHTS GELOESCHT. Vorgang komplett abgebrochen.");
                return;
            }
            plugin.getLogger().warning("DBWipe: Backup erfolgreich unter " + backup.getAbsolutePath()
                    + " (" + (backup.length() / 1024) + " KB) - loesche jetzt alle Tabellen ...");
            try {
                int anzahl = dumper.alleTabellenLoeschen();
                plugin.getLogger().warning("DBWipe: fertig - " + anzahl + " Tabelle(n) geloescht. "
                        + "Backup liegt unter " + backup.getAbsolutePath());
            } catch (Exception e) {
                plugin.getLogger().severe("DBWipe: Loeschen fehlgeschlagen (" + e.getMessage()
                        + ") - das Backup unter " + backup.getAbsolutePath() + " existiert trotzdem bereits.");
            }
        });
    }
}
