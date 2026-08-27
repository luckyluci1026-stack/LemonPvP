package com.lemonpvp.lemoncore.lemonlang.token;

import com.lemonpvp.lemoncore.lemonlang.LemonLangError;

import java.util.ArrayList;
import java.util.List;

/**
 * Liest LemonLang-Quelltext und zerlegt ihn in bedeutsame {@link Line}-Objekte.
 *
 * <p>Regeln:
 * <ul>
 *   <li>Leere Zeilen (nur Whitespace) werden uebersprungen.</li>
 *   <li>Kommentarzeilen (erstes Nicht-Leerzeichen ist '#') werden uebersprungen.</li>
 *   <li>Die Einrueckung ist die Anzahl fuehrender Leerzeichen.</li>
 *   <li>Ein fuehrender TAB fuehrt zu einem {@link LemonLangError} mit deutschem Hinweis.</li>
 * </ul>
 */
public class LineReader {

    private final String source;
    private final String scriptFile;

    public LineReader(String source, String scriptFile) {
        this.source = source == null ? "" : source;
        this.scriptFile = scriptFile;
    }

    /**
     * Zerlegt die Quelle in Zeilen. Blanke und Kommentarzeilen werden ausgelassen.
     *
     * @throws LemonLangError wenn eine Zeile mit einem Tabulator eingerueckt ist
     */
    public List<Line> read() {
        List<Line> result = new ArrayList<>();
        // Newlines normalisieren: \r\n und \r zu \n.
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
        String[] rawLines = normalized.split("\n", -1);

        for (int i = 0; i < rawLines.length; i++) {
            String raw = rawLines[i];
            int lineNumber = i + 1;

            // Tab-Einrueckung am Zeilenanfang verbieten.
            if (!raw.isEmpty() && raw.charAt(0) == '\t') {
                throw new LemonLangError(
                        scriptFile,
                        lineNumber,
                        "Tabulator am Zeilenanfang ist nicht erlaubt.",
                        "Bitte Leerzeichen statt Tabs verwenden (2 Leerzeichen pro Ebene)."
                );
            }

            // Fuehrende Leerzeichen zaehlen; bei Tab dazwischen ebenfalls melden.
            int indent = 0;
            int idx = 0;
            while (idx < raw.length()) {
                char c = raw.charAt(idx);
                if (c == ' ') {
                    indent++;
                    idx++;
                } else if (c == '\t') {
                    throw new LemonLangError(
                            scriptFile,
                            lineNumber,
                            "Tabulator in der Einrueckung ist nicht erlaubt.",
                            "Bitte Leerzeichen statt Tabs verwenden (2 Leerzeichen pro Ebene)."
                    );
                } else {
                    break;
                }
            }

            // Rest der Zeile betrachten (ab erstem Nicht-Leerzeichen).
            String content = raw.substring(idx);
            // Abschliessende Leerzeichen entfernen.
            content = stripTrailing(content);

            // Leere Zeile -> ueberspringen.
            if (content.isEmpty()) {
                continue;
            }

            // Kommentarzeile -> ueberspringen.
            if (content.charAt(0) == '#') {
                continue;
            }

            result.add(new Line(indent, content, lineNumber));
        }

        return result;
    }

    private static String stripTrailing(String s) {
        int end = s.length();
        while (end > 0) {
            char c = s.charAt(end - 1);
            if (c == ' ' || c == '\t') {
                end--;
            } else {
                break;
            }
        }
        return s.substring(0, end);
    }
}