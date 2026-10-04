package de.lemonpvp.smplobby.util;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lädt eine YAML-Datei und erklärt Fehler in verständlichem Deutsch.
 *
 * SnakeYAML meldet so etwas wie "expected &lt;block end&gt;, but found
 * '&lt;block mapping start&gt;'" - damit kann niemand etwas anfangen. Diese
 * Klasse macht daraus die betroffene Zeile, die Nachbarzeilen und einen
 * konkreten Tipp (fast immer: Einrückung).
 */
public final class ConfigProblem {

    private static final Pattern POSITION = Pattern.compile("line (\\d+), column (\\d+)");

    /** Was schiefgelaufen ist, in einer Form, die man jemandem zeigen kann. */
    public record Report(String file, int line, int column, String hint, List<String> context) {
    }

    /** Ergebnis des Ladens: entweder eine Datei oder ein Bericht. */
    public record Result(YamlConfiguration config, Report problem) {
        public boolean ok() {
            return problem == null;
        }
    }

    private ConfigProblem() {
    }

    public static Result load(File file) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
            return new Result(config, null);
        } catch (Exception ex) {
            return new Result(null, describe(file, ex));
        }
    }

    /** Schreibt den Bericht in die Konsole - mit den Zeilen rundherum. */
    public static void log(Logger logger, Report report) {
        logger.warning("---------------------------------------------");
        logger.warning(report.file() + " konnte nicht gelesen werden.");
        if (report.line() > 0) {
            logger.warning("Fehler in Zeile " + report.line() + ":");
            for (String line : report.context()) {
                logger.warning(line);
            }
        }
        logger.warning("Tipp: " + report.hint());
        logger.warning("Die zuletzt funktionierenden Einstellungen bleiben aktiv.");
        logger.warning("---------------------------------------------");
    }

    // ------------------------------------------------------------------

    private static Report describe(File file, Throwable error) {
        String message = chain(error);
        List<int[]> spots = new ArrayList<>();
        Matcher matcher = POSITION.matcher(message);
        while (matcher.find()) {
            spots.add(new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))});
        }

        List<String> lines = readLines(file);

        // SnakeYAML nennt oft zwei Stellen: zuerst den Anfang des Blocks
        // (die heile Nachbarzeile), dann die Zeile, an der es kracht.
        int[] broken = spots.isEmpty() ? new int[]{0, 0} : spots.get(spots.size() - 1);
        int[] reference = spots.size() > 1 ? spots.get(0) : new int[]{0, 0};

        String hint = hintFor(message, lines, broken, reference);
        return new Report(file.getName(), broken[0], broken[1], hint, context(lines, broken));
    }

    private static String hintFor(String message, List<String> lines, int[] broken, int[] reference) {
        String brokenLine = lineAt(lines, broken[0]);

        if (brokenLine.contains("\t") || message.contains("'\\t'")) {
            return "In der Zeile steckt ein Tabulator. YAML erlaubt nur Leerzeichen - "
                    + "ersetze den Tab durch Leerzeichen.";
        }
        if (message.contains("found duplicate key")) {
            return "Der Name kommt zweimal in derselben Liste vor. Jeder Eintrag darf "
                    + "nur einmal auftauchen.";
        }
        if (message.contains("mapping values are not allowed")) {
            return "In dem Wert steckt ein Doppelpunkt. Setze den Text in "
                    + "Anführungszeichen, zum Beispiel: name: \"Laser: Türkis\"";
        }
        if (message.contains("could not find expected ':'")) {
            return "Am Ende des Namens fehlt der Doppelpunkt.";
        }

        // Der häufigste Fall: die Einrückung passt nicht zu den Nachbarn.
        if (reference[1] > 0 && reference[1] != broken[1]) {
            String refName = keyOf(lineAt(lines, reference[0]));
            String brokenName = keyOf(brokenLine);
            return "Die Einrückung passt nicht. \"" + refName + "\" steht mit "
                    + (reference[1] - 1) + " Leerzeichen, \"" + brokenName + "\" mit "
                    + (broken[1] - 1) + ". Einträge auf derselben Ebene brauchen "
                    + "genau gleich viele Leerzeichen (ihre Unterpunkte je 2 mehr).";
        }
        return "Prüfe die Einrückung dieser Zeile - nur Leerzeichen, keine Tabs, und "
                + "genau so viele wie bei den Einträgen darüber.";
    }

    private static List<String> context(List<String> lines, int[] broken) {
        List<String> out = new ArrayList<>();
        if (broken[0] <= 0) {
            return out;
        }
        int from = Math.max(1, broken[0] - 2);
        int to = Math.min(lines.size(), broken[0] + 2);
        for (int number = from; number <= to; number++) {
            String text = lines.get(number - 1).replace("\t", "→   ");
            out.add(String.format("  %4d | %s", number, text));
            if (number == broken[0] && broken[1] > 0) {
                out.add("       | " + " ".repeat(Math.max(0, broken[1] - 1)) + "^ hier");
            }
        }
        return out;
    }

    private static String keyOf(String line) {
        String trimmed = line.trim();
        int colon = trimmed.indexOf(':');
        return colon > 0 ? trimmed.substring(0, colon) : trimmed;
    }

    private static String lineAt(List<String> lines, int number) {
        return number > 0 && number <= lines.size() ? lines.get(number - 1) : "";
    }

    private static List<String> readLines(File file) {
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static String chain(Throwable error) {
        StringBuilder text = new StringBuilder();
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current.getMessage() != null) {
                text.append(current.getMessage()).append('\n');
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return text.toString();
    }

    /** Entfernt Zeichen, die MiniMessage als Tag lesen würde. */
    public static String safeForChat(String text) {
        return text == null ? "" : text.replace("<", "(").replace(">", ")");
    }
}
